# Logos — Engineering Contract for Agents

Operate with principal-engineer rigor. Logos stitches gRPC services, AWS infrastructure, and gRPC-web clients through a single Guice/Java stack. This document is the binding contract for how to design, implement, and validate changes in this repo.

## Non-Negotiable Invariants
- ModuleLoader is the only injection root. Every runtime or infrastructure capability ships as a Guice module annotated with `@registerModule`. Never create ad-hoc injectors, static singletons, or manual wiring.
- AppModule orchestrates services, interceptors, clients, and stacks. GuardServerInterceptor must remain first on both the in-process and Netty servers; both servers must load the identical module sets.
- `Service.guard` is the single authorization/validation gate. New RPCs must express their checks through `allow` + `validate` (or override `guard`) so the GuardServerInterceptor can stop requests before user code runs. No parallel gating layers.
- One storage pipeline. Connections come from DatabaseModule, credentials from IAM auth tokens, and queries stream via generated Jdbi helpers. Skip the exporter or the validator and you are out of policy.
- One infrastructure definition. AWS stacks live in the Java CDK modules (InfrastructureModule + Ecr/Vpc/Rds/Eks); Kubernetes manifests are produced via K8sModule. Do not handcraft secondary Terraform/YAML copies.
- AppController is the sole writer of logos.dev/v1 Apps, Route53 records, gateway resources, and the client domain map. Anything else that mutates those objects is a bug.

## Dependency Injection & Module Loading
- `RegisterModuleProcessor` emits `META-INF/app-modules-*.txt` at compile time. ModuleLoader walks those tickets on the runtime classpath (jar list comes from `LOGOS_SERVICE_JAR_CONFIG_PATH` when running in cluster). Maintain additive lists; never hardcode module classes.
- Extend `dev.logos.app.AppModule` for application features. Use the helpers:
  - `service(...)` / `services(...)` for `dev.logos.service.Service` implementations.
  - `interceptor(...)` to contribute `ServerInterceptor`s.
  - `client(...)` / `clients(...)` to vend stubs; let AppModule attach call credentials via the optional binder.
  - `stack(...)` / `stacks(...)` for AWS CDK `Stack` implementations.
- Prefer `Multibinder` and `OptionalBinder` for overrides. Provide defaults at module definition time; override via environment-specific modules (e.g., DevModule). Symmetric teardown is mandatory—anything you bind strongly must be released or closed on shutdown.
- Never touch `ModuleLoader.createInjector` outside of entrypoints (`ServerExecutor`, `Synthesizer`, exporters). If a new executable needs injections, give it a tiny `main` that calls ModuleLoader exactly once.

## Service Surfaces & Interceptors
- Implement RPCs by overriding the generated base class or `BindableService`. Pattern:
  1. `allow(request)` returns true only for operations the caller is permitted to perform.
  2. `validate(request, Validator)` fills granular error messages; never throw for user problems.
  3. Business logic runs only after GuardServerInterceptor lets the message through.
- Do not mutate gRPC metadata in handlers. Interceptor responsibilities:
  - GuardServerInterceptor (provided) must stay first; it looks up services by descriptor name and aborts with the `Status` from `guard`.
  - Additional interceptors bind through AppModule so the in-process and network servers stay consistent.
- `dev.logos.client.module.ClientModule` creates a single in-process channel and fan-out interceptors. Any network channel/offload logic must stay inside `ManagedChannel` providers; do not instantiate stubs manually.
- When adding call credentials, bind a `CallCredentials` via `@ProvidesIntoOptional` so AppModule can automatically attach it to generated stubs. Never call `stub.withCallCredentials` in user code.

## Authentication Contexts
- Authentication lives in interceptors:
  - `CognitoServerInterceptor` verifies JWTs against the configured user pool, populates `UserContext`, and works with `CookieServerInterceptor` to surface cookies via metadata.
  - `ServiceAccountInterceptor` verifies cluster service-account tokens and fills `MachineContext`.
- Context keys are `Context.Key`s (`USER_CONTEXT_KEY`, `MACHINE_CONTEXT_KEY`). Only interceptors set them; services may read but must tolerate absence.
- Secrets reside in AWS Secrets Manager or IAM; modules retrieve them via Guice providers and close clients on shutdown. No environment-variable secrets.
- The development override (`app.auth.dev.module.DevModule`) is the template for environment-specific credentials—follow the same pattern if you need another scoped credential.

## Storage, Jdbi, and Migrations
- DatabaseModule is authoritative for JDBC URLs, connection pools, and IAM signing. It resolves `db-rw-service` via CNAME when inside the cluster and falls back to localhost when running locally. Never bypass it with ad-hoc `DriverManager` connections.
- Generated credentials are short lived; every `getPassword()` recomputes via `RdsUtilities`. Do not cache or log tokens. Rotate or close data sources via Guice scopes rather than static singletons.
- SQL migrations live under `dev/logos/service/storage/migrations`. They register through the Postgres `migrations.apply(...)` function and the `pg_migrate` Bazel rule. Each migration must include forward SQL, tests in the `tests.*` schema, and (where feasible) a revert clause.
- The migration runner takes a global advisory lock (00003). Never remove that lock or introduce side channels that apply migrations concurrently.

## Schema Exporter Workflow
- Define Postgres functions (RETURNS TABLE or RETURNS SETOF composite) and register them in the schema export rule. `Exporter.lookupFunctions` introspects signatures and raises if names or types are missing—fix the database before patching the generator.
- Use `schema_export`, `schema_proto_src`, and `java_storage_service` Bazel helpers to generate JSON, protobuf, and Java base classes. Resulting services stream Jdbi rows into protobuf builders; do not handwrite duplicate serializers.
- Type mappers live under `pg/exporter/codegen/type`. If you need a new postgres type, add a mapper and bind it in `ExportModule`. Arrays must be handled intentionally; the base class enforces consistent repeated vs scalar semantics.
- Generated base classes expect you to fill validation hooks and optionally override binding logic. Keep query assembly inside the base so that guards, logging, and error handling remain uniform.

## Infrastructure Stacks

### AWS CDK Modules
- `InfrastructureModule` installs the ECR, VPC, RDS, and EKS modules. Every stack derives IDs from `@RootConstructId`; re-use that helper to keep names stable.
- `EcrModule` seeds repository names. Extend via `Multibinder<String>` and map binders if you need extra repositories—never fork the module.
- `VpcModule` configures NAT instances, subnet tagging, and security groups. Keep EKS-compatible annotations on subnets; any change must preserve ELB role tags.
- `RdsModule` provisions the Aurora cluster and grants IAM auth. Writer props default to serverless v2; override via `@WriterClusterInstanceProps` if you genuinely need a different class.
- `EksModule` wires add-ons, autoscaling, service accounts, and database service manifests. Use the provided `@ProvidesIntoSet` hooks (`ServiceAccountPolicyBuilder`, `WorkerNodePolicyBuilder`, `RpcServerDatabaseRoles`) instead of writing raw IAM statements.
- `dev/logos/stack/aws/synthesizer/Synthesizer` and `aws_stack_zip` are the only deployment entrypoints. To ship stacks run `bazel run @logos//dev/logos/stack/aws/cdk -- synth` or `-- deploy --all --require-approval never`.

### Kubernetes Module
- `dev.logos.stack.k8s.module.K8sModule` builds cdk8s charts. `RpcServer` records capture workloads, env, volume mounts, sidecars, and probes. Compose them via multibinders—never write raw YAML.
- Default env wiring injects storage connection settings and the config map volume for app definitions. If you extend env vars, merge with the provided map so IAM/RDS settings survive.
- Ensure every container runs non-root UID/GID 1000, attaches the config volume, and honours the security context.

## Application Controller & CRD Flow
- `AppController` is the control plane. It watches logos.dev/v1 Apps, keeps `logos-apps` ConfigMap synced, restarts the `client-deployment` in place, maintains Gateway/HTTPRoute/GRPCRoute/SecurityPolicy objects, and ensures `external-dns` can publish.
- Route53 integration (`Route53ZoneCreator`) is idempotent: first list zones, then create and seed the TXT record. Preserve that flow so repeated watches do not explode.
- All reconciler paths must log through `LoggerFactory` (`logger.atInfo()/atError()`) with key/value context. Make every API call idempotent; the watch loop replays events and expects harmless retries.
- When adding CRD fields, update the schema in `customresourcedefinition.yaml`, the `App` record in controller code, and every place that serialises spec.

## Build & Operational Tooling
- Bazel is the only build tool. Use the repo macros:
  - `bzl/private/service.bzl:service` for Java service libraries.
  - `bzl/private/server.bzl:server` for runnable server binaries.
  - `bzl/app/defs.bzl:app` to sync web bundles and apply CRDs (targets produce `.apply/.delete/.diff/.replace` helpers).
  - `bzl/push_image.bzl:push_image` for container pushes; it respects `LOGOS_AWS_REGISTRY`.
  - `bzl/pg_migrate.bzl:pg_migrate` for migrations—run via `bazel run //dev/logos/service/storage/migrations:migrations`.
- `tools/aws.sh` is the canonical dev harness. Notable commands:
  - `./tools/aws.sh setup` → configure AWS SSO + local `.bazelrc.local`.
  - `./tools/aws.sh dev server|client` → telepresence intercept + ibazel dev loop.
  - `./tools/aws.sh rds tunnel` → establish local port forwarding for database work.
  - `./tools/aws.sh deploy` → CDK deploy + apply k8s stack.
- Always build Node deps via `bazel build //:node_modules`; never run `pnpm install` manually.

## Coding Rules & Review Bar
- Keep changes minimal and reversible. No sweeping refactors bundled with feature work.
- Never bypass Guice. If you need a new singleton, bind it. If a class needs a dependency, inject it.
- Shared mutable state must live inside an injected scope; prefer constructor injection and final fields.
- Logging uses `LoggerFactory`; no direct `LoggerFactory.getLogger` calls in user code.
- gRPC Context writes happen only in interceptors. Services read context but do not mutate it.
- Prefer streaming + try-with-resources exactly as in the generated base classes; close handles explicitly or rely on try-with resources.
- Frontend modules mirror backend module loading. Use `@registerModule` in TypeScript, and interact with `rootContainer` rather than creating new Inversify containers.
- If a change touches infra or Kubernetes resources, update both the CDK module and any dependent manifests or tests in lock step.

## Hazard Scan Before Shipping
- `grep -R "createInjector" src` – no new injector sites.
- Verify every new `Service` implementation is registered through an AppModule and appears in the GuardServerInterceptor service map.
- Search for direct JDBC usage outside DatabaseModule or generated classes.
- Confirm new migrations bump the `head` in `pg_migrate` and include validation in the `tests` schema.
- For new interceptors, ensure order is correct and unit tests cover both pass and fail cases.
- For new AWS resources, ensure tags/IDs derive from `@RootConstructId` and that outputs are exported through CloudAssembly when needed.
- For new cdk8s constructs, run the synthesizer (`bazel build //dev/logos/stack/k8s:stack.k8s.yaml`) and inspect the diff.

## Validation Checklist
- `bazel test //...` (no flakes; run until green).
- `bazel build //dev/logos/service/backend/server` and `//dev/logos/service/console:image` to confirm container builds.
- `bazel run //dev/logos/service/storage/migrations:migrations` against an environment clone if the schema changed.
- `bazel build //dev/logos/stack/k8s:stack.k8s.yaml` and review generated manifests.
- `bazel run @logos//dev/logos/stack/aws/cdk -- synth` (and `-- deploy --all --require-approval never` in staging when infra changes).
- `bazel run //dev/logos:logos.diff` to review Kubernetes diffs before applying.
- If the schema exporter changed, rerun `bazel build` on the generated Java + proto targets and ensure downstream services compile.

Operate from these invariants. When in doubt, pause, design, and strengthen the model instead of layering workarounds.
