# Logos Framework

Logos is a microservice framework built with **Bazel**. It assembles Java
services and TypeScript clients into a Kubernetes stack. Modules are provided
as Guice bindings and discovered at runtime.

## Repository layout

- `app/` – example services, including the Cognito authentication module
- `dev/` – framework libraries and service stacks
- `vendor/` – third-party dependencies used by Bazel
- `.devcontainer/` – container image for local development

## Getting started

1. **Use the dev container.** It includes all needed prerequisites.
2. **Install Node dependencies**
   ```bash
   bazel build //:node_modules
   ```
3. **Run the stack**
   ```bash
   bazel run //dev/logos
   ```
   This builds the backend and client and applies the Kubernetes manifests.

## How it works

- gRPC services are defined in protobuf (see [`cognito.proto`](app/auth/cognito/proto/cognito.proto)).
- Dependency injection and module discovery are handled by [`ModuleLoader`](dev/logos/module/ModuleLoader.java).
- The backend server is assembled in [`ServerModule`](dev/logos/service/backend/server/ServerModule.java).
- AWS infrastructure stacks are defined with the **AWS CDK**. [`InfrastructureModule`](dev/logos/stack/aws/module/InfrastructureModule.java)
  installs modules like [`EcrModule`](dev/logos/stack/aws/module/EcrModule.java) and
  [`VpcModule`](dev/logos/stack/aws/module/VpcModule.java) which Guice uses to
  build the stack. The [`Synthesizer`](dev/logos/stack/aws/synthesizer/Synthesizer.java)
  creates an injector via `ModuleLoader` and obtains the `CloudAssembly` for deployment.
- Front-end modules under `dev/logos/web` use Inversify for dependency injection.

Use this repository as a starting point for your own Logos-based services.
