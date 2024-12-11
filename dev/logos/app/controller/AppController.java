package dev.logos.app.controller;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Watch;
import io.vavr.Function5;
import io.vavr.Function6;
import io.vavr.Tuple;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

class OffsetDateTimeAdapter implements JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public JsonElement serialize(OffsetDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.format(formatter));
    }

    @Override
    public OffsetDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return OffsetDateTime.parse(json.getAsString(), formatter);
    }
}

record App(
        String apiVersion,
        String kind,
        V1ObjectMeta metadata,
        AppSpec spec
) {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

    public static App fromApiResult(Object result) {
        return gson.fromJson(gson.toJsonTree(result), App.class);
    }
}

record RpcServerSpec(
        String domain,
        @SerializedName("service-name")
        String serviceName
) {
}

record AppSpec(
        @SerializedName("web-bundle")
        String webBundle,

        @SerializedName("rpc-servers")
        List<RpcServerSpec> rpcServers
) {
    public AppSpec {
        if (rpcServers == null) {
            rpcServers = List.of();
        }
    }
}

public class AppController {
    private static final String CRD_GROUP = "logos.dev";
    private static final String CRD_VERSION = "v1";
    private static final String CRD_PLURAL = "apps";
    private static final String CONFIGMAP_NAME = "logos-apps";
    private static final String CONFIGMAP_NAMESPACE = "default";
    private static final String CONFIGMAP_CLIENT_NGINX_DOMAIN_MAP_KEY = "client-nginx-domain-map";
    private static final String CLIENT_DEPLOYMENT_NAME = "client-deployment";
    private static final String CLIENT_DEPLOYMENT_NAMESPACE = "default";
    private static final Logger logger = getLogger(AppController.class);

    private static final Route53ZoneCreator route53ZoneCreator = new Route53ZoneCreator();
    private static final Gson gson = new GsonBuilder().create();

    private static void updateConfigMap(HashMap<String, App> appList) throws IOException, ApiException {
        CoreV1Api api = new CoreV1Api(Config.defaultClient());
        V1ConfigMap configMap = api.readNamespacedConfigMap(CONFIGMAP_NAME, CONFIGMAP_NAMESPACE).execute();

        configMap.putDataItem(
                CONFIGMAP_CLIENT_NGINX_DOMAIN_MAP_KEY,
                clientNginxDomainMap(appList)
        );

        api.replaceNamespacedConfigMap(CONFIGMAP_NAME, CONFIGMAP_NAMESPACE, configMap).execute();
    }

    private static void triggerRollingUpdate(String deploymentNamespace, String deploymentName) throws ApiException {
        JsonObject annotations = new JsonObject();
        annotations.addProperty("kubectl.kubernetes.io/restartedAt", Instant.now().toString());

        JsonObject metadata = new JsonObject();
        metadata.add("annotations", annotations);

        JsonObject template = new JsonObject();
        template.add("metadata", metadata);

        JsonObject spec = new JsonObject();
        spec.add("template", template);

        JsonObject patchBody = new JsonObject();
        patchBody.add("spec", spec);

        V1Patch patch = new V1Patch(gson.toJson(patchBody));

        AppsV1Api appsV1Api = new AppsV1Api();
        try {
            PatchUtils.patch(
                    V1Deployment.class,
                    () -> appsV1Api.patchNamespacedDeployment(deploymentName, deploymentNamespace, patch)
                                   .buildCall(null),
                    V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH,
                    appsV1Api.getApiClient());

            logger.atInfo()
                  .addKeyValue("deploymentNamespace", deploymentNamespace)
                  .addKeyValue("deploymentName", deploymentName)
                  .log("Rolling update triggered for deployment");
        } catch (ApiException e) {
            logger.atError()
                  .addKeyValue("deploymentNamespace", deploymentNamespace)
                  .addKeyValue("deploymentName", deploymentName)
                  .log("Failed to trigger rolling update for deployment");
            throw e;
        }
    }

    private static String clientNginxDomainMap(HashMap<String, App> apps) {
        return apps.values().stream().map(app -> "%s /app/web-bundles/%s;".formatted(
                app.metadata().getName(), app.spec().webBundle())).collect(Collectors.joining("\n")
        );
    }

    private static void upsertCustomResource(
            String group,
            String version,
            String namespace,
            String plural,
            String resourceName,
            Map<String, Object> resource) {

        var api = new CustomObjectsApi();
        var commonParams = Tuple.of(group, version, namespace, plural);
        var getObject = Function5.of(api::getNamespacedCustomObject).tupled();
        var replaceObject = Function6.of(api::replaceNamespacedCustomObject).tupled();
        var createObject = Function5.of(api::createNamespacedCustomObject).tupled();

        Try.of(() -> getObject.apply(commonParams.concat(Tuple.of(resourceName))).execute())
           .map(existingResource -> {
               String currentVersion = ((Map<String, String>) ((Map<String, Object>) existingResource).get("metadata")).get("resourceVersion");
               ((V1ObjectMeta) resource.get("metadata")).setResourceVersion(currentVersion);
               return resource;
           }).mapTry(updatedResource -> {
               logger.atInfo()
                     .addKeyValue("plural", plural)
                     .addKeyValue("resourceName", resourceName)
                     .log("Updating resource");

               return replaceObject
                       .apply(commonParams.concat(Tuple.of(resourceName, updatedResource)))
                       .execute();
           })
           .recoverWith(exception -> Match(exception).of(
                   Case($(instanceOf(ApiException.class)), apiException -> {
                       logger.atError()
                             .addKeyValue("plural", plural)
                             .addKeyValue("resourceName", resourceName)
                             .addKeyValue("code", apiException.getCode())
                             .addKeyValue("responseBody", apiException.getResponseBody())
                             .log("API Exception occurred");

                       return Match(apiException.getCode()).of(
                               Case($(HTTP_NOT_FOUND), () ->
                                       Try.of(() -> {
                                           logger.atInfo()
                                                 .addKeyValue("plural", plural)
                                                 .addKeyValue("resourceName", resourceName)
                                                 .log("Creating resource");
                                           return createObject.apply(commonParams.concat(Tuple.of(resource)))
                                                              .execute();
                                       })
                               )
                       );
                   }),
                   Case($(), otherException -> {
                       logger.atError()
                             .addKeyValue("plural", plural)
                             .addKeyValue("resourceName", resourceName)
                             .addKeyValue("resource", resource)
                             .addKeyValue("exceptionType", otherException.getClass().getName())
                             .addKeyValue("message", otherException.toString())
                             .log("Unexpected exception type");
                       return Try.failure(otherException);
                   })
           ));
    }

    private static void updateHttpRoutes(HashMap<String, App> apps) {
        apps.values().stream()
            .map(AppController::buildHttpRoute)
            .forEach(httpRoute -> upsertCustomResource(
                    "gateway.networking.k8s.io", "v1", "default", "httproutes",
                    ((V1ObjectMeta) httpRoute.get("metadata")).getName(),
                    httpRoute
            ));
    }

    private static void updateGrpcRoutes(HashMap<String, App> apps) {
        for (App app : apps.values()) {
            for (RpcServerSpec rpcServer : app.spec().rpcServers()) {
                Map<String, Object> grpcRoute = buildGrpcRoute(requireNonNull(app.metadata().getName()), rpcServer.domain(), rpcServer.serviceName());
                upsertCustomResource(
                        "gateway.networking.k8s.io", "v1", "default", "grpcroutes",
                        ((V1ObjectMeta) grpcRoute.get("metadata")).getName(),
                        grpcRoute
                );
            }
        }
    }

    private static void updateGateways(HashMap<String, App> apps) {
        apps.values().stream()
            .map(AppController::buildGateway)
            .forEach(gateway -> upsertCustomResource(
                    "gateway.networking.k8s.io", "v1", "default", "gateways",
                    ((V1ObjectMeta) gateway.get("metadata")).getName(),
                    gateway
            ));

        if (!apps.isEmpty()) {
            App firstApp = apps.values().iterator().next();
            String firstGatewayName = firstApp.metadata().getName().replace(".", "-") + "-gateway";

            upsertCustomResource(
                    "gateway.envoyproxy.io", "v1alpha1", "default", "envoypatchpolicies",
                    "envoy-cookies-patch-policy",
                    buildEnvoyPatchPolicy(firstGatewayName)
            );

            upsertCustomResource(
                    "gateway.envoyproxy.io", "v1alpha1", "default", "backendtrafficpolicies",
                    "default-backend-traffic-policy",
                    buildBackendTrafficPolicy(firstGatewayName)
            );
        }
    }

    private static void updateSecurityPolicy(HashMap<String, App> apps) {
        apps.values().stream()
            .map(AppController::buildSecurityPolicy)
            .forEach(securityPolicy -> upsertCustomResource(
                    "gateway.envoyproxy.io", "v1alpha1", "default", "securitypolicies",
                    ((V1ObjectMeta) securityPolicy.get("metadata")).getName(),
                    securityPolicy
            ));
    }

    private static Map<String, Object> buildGateway(App app) {
        String clientDomain = requireNonNull(app.metadata().getName());
        String apiDomain = "api." + clientDomain;
        String gatewayName = clientDomain.replace(".", "-") + "-gateway";

        return Map.of(
                "apiVersion", "gateway.networking.k8s.io/v1",
                "kind", "Gateway",
                "metadata", new V1ObjectMeta()
                        .name(gatewayName)
                        .namespace("default")
                        .putAnnotationsItem("cert-manager.io/cluster-issuer", "letsencrypt"),
                "spec", Map.of(
                        "gatewayClassName", "logos-gateway-class",
                        "listeners", List.of(
                                Map.of(
                                        "name", "http",
                                        "protocol", "HTTP",
                                        "port", 80),
                                Map.of(
                                        "name", "https-client",
                                        "hostname", clientDomain,
                                        "port", 443,
                                        "protocol", "HTTPS",
                                        "allowedRoutes", Map.of("namespaces", Map.of("from", "All")),
                                        "tls", Map.of(
                                                "mode", "Terminate",
                                                "certificateRefs", List.of(Map.of("name", clientDomain + "-tls")))),
                                Map.of(
                                        "name", "https-api",
                                        "hostname", apiDomain,
                                        "port", 443,
                                        "protocol", "HTTPS",
                                        "allowedRoutes", Map.of("namespaces", Map.of("from", "All")),
                                        "tls", Map.of(
                                                "mode", "Terminate",
                                                "certificateRefs", List.of(Map.of("name", apiDomain + "-tls"))))
                        )));
    }

    private static Map<String, Object> buildEnvoyPatchPolicy(String gatewayName) {
        String listenerName = "default/" + gatewayName + "/https-client";

        return Map.of(
                "apiVersion", "gateway.envoyproxy.io/v1alpha1",
                "kind", "EnvoyPatchPolicy",
                "metadata", new V1ObjectMeta()
                        .name("envoy-cookies-patch-policy")
                        .namespace("default"),
                "spec", Map.of(
                        "targetRef", Map.of(
                                "group", "gateway.networking.k8s.io",
                                "kind", "GatewayClass",
                                "name", "logos-gateway-class"
                        ),
                        "type", "JSONPatch",
                        "jsonPatches", List.of(
                                Map.of(
                                        "type", "type.googleapis.com/envoy.config.listener.v3.Listener",
                                        "name", listenerName,
                                        "operation", Map.of(
                                                "op", "add",
                                                "path", "/filter_chains/1/filters/0/typed_config/http_filters/0",
                                                "value", Map.of(
                                                        "name", "envoy.filters.http.lua",
                                                        "typed_config", Map.of(
                                                                "@type", "type.googleapis.com/envoy.extensions.filters.http.lua.v3.Lua",
                                                                "default_source_code", Map.of(
                                                                        "inline_string", """
                                                                                function envoy_on_request(request_handle)
                                                                                    local cookies = request_handle:headers():get("cookie")
                                                                                    request_handle:logCritical("on_request")
                                                                                
                                                                                    if cookies then
                                                                                        local start = 1
                                                                                        local semicolon_pos
                                                                                        local end_pos
                                                                                        local combined_cookies = ""
                                                                                
                                                                                        repeat
                                                                                            semicolon_pos = string.find(cookies, ";", start, true)
                                                                                
                                                                                            if semicolon_pos == nil then
                                                                                                end_pos = string.len(cookies)
                                                                                            else
                                                                                                end_pos = semicolon_pos - 1
                                                                                            end
                                                                                
                                                                                            local cookie = string.sub(cookies, start, end_pos)
                                                                                
                                                                                            if not (combined_cookies == "") then
                                                                                                combined_cookies = combined_cookies .. "|" .. cookie
                                                                                            else
                                                                                                combined_cookies = cookie
                                                                                            end
                                                                                
                                                                                            if not (semicolon_pos == nil) then
                                                                                                start = semicolon_pos + 1
                                                                                            end
                                                                                        until semicolon_pos == nil
                                                                                
                                                                                        request_handle:headers():add("logos-cookies", combined_cookies)
                                                                                    end
                                                                                end
                                                                                
                                                                                function envoy_on_response(response_handle)
                                                                                    local set_cookies = response_handle:headers():get("logos-set-cookies")
                                                                                    response_handle:logCritical("set_cookies: " .. (set_cookies or "nil"))
                                                                                
                                                                                    if set_cookies then
                                                                                        local start = 1
                                                                                        local delimiter_pos
                                                                                        local end_pos
                                                                                
                                                                                        repeat
                                                                                            delimiter_pos = string.find(set_cookies, "|", start, true)
                                                                                
                                                                                            if delimiter_pos == nil then
                                                                                                end_pos = string.len(set_cookies)
                                                                                            else
                                                                                                end_pos = delimiter_pos - 1
                                                                                            end
                                                                                
                                                                                            local cookie = string.sub(set_cookies, start, end_pos)
                                                                                            response_handle:headers():add("Set-Cookie", cookie)
                                                                                
                                                                                            if not (delimiter_pos == nil) then
                                                                                                start = delimiter_pos + 1
                                                                                            end
                                                                                
                                                                                        until delimiter_pos == nil
                                                                                    end
                                                                                end
                                                                                """
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static Map<String, Object> buildBackendTrafficPolicy(String gatewayName) {
        return Map.of(
                "apiVersion", "gateway.envoyproxy.io/v1alpha1",
                "kind", "BackendTrafficPolicy",
                "metadata", new V1ObjectMeta()
                        .name("logos-backend-traffic-policy")
                        .namespace("default"),
                "spec", Map.of(
                        "targetRef", Map.of(
                                "group", "gateway.networking.k8s.io",
                                "kind", "Gateway",
                                "name", gatewayName,
                                "namespace", "default"
                        ),
                        "timeout", Map.of(
                                "http", Map.of(
                                        "requestTimeout", "600s"
                                )
                        )
                )
        );
    }

    private static Map<String, Object> buildSecurityPolicy(App app) {
        String appDomain = requireNonNull(app.metadata().getName());
        String namePrefix = appDomain.replace(".", "-");
        String policyName = namePrefix + "-security-policy";
        String grpcRouteName = namePrefix + "-grpc-route";

        return Map.of(
                "apiVersion", "gateway.envoyproxy.io/v1alpha1",
                "kind", "SecurityPolicy",
                "metadata", new V1ObjectMeta().namespace("default").name(policyName),
                "spec", Map.of(
                        "targetRefs", List.of(Map.of(
                                "group", "gateway.networking.k8s.io",
                                "kind", "GRPCRoute",
                                "name", grpcRouteName
                        )),
                        "cors", Map.of(
                                "allowOrigins", List.of("https://" + appDomain),
                                "allowMethods", List.of("OPTIONS", "POST"),
                                "allowHeaders", List.of("x-grpc-web", "x-user-agent", "x-grpc-timeout", "content-type", "x-requested-with", "accept", "origin", "authorization", "x-client-rpc-id"),
                                "allowCredentials", true
                        )
                )
        );
    }

    private static Map<String, Object> buildHttpRoute(App app) {
        String gatewayName = requireNonNull(app.metadata().getName()).replace(".", "-") + "-gateway";
        List<String> hostnames = List.of(requireNonNull(app.metadata().getName()));
        String namespace = "default";
        String name = app.metadata().getName().replace(".", "-") + "-http-route";

        return Map.of(
                "apiVersion", "gateway.networking.k8s.io/v1",
                "kind", "HTTPRoute",
                "metadata", new V1ObjectMeta().name(name).namespace(namespace),
                "spec", Map.of(
                        "hostnames", hostnames,
                        "parentRefs", List.of(Map.of("name", gatewayName,
                                                     "namespace", "default")),
                        "rules", List.of(
                                Map.of(
                                        "matches", List.of(Map.of("path", Map.of("type", "PathPrefix", "value", "/"))),
                                        "backendRefs", List.of(Map.of("name", "client-service", "port", 8080))))));
    }

    private static Map<String, Object> buildGrpcRoute(String appDomain, String rpcServerDomain, String rpcServerServiceName) {
        String prefix = appDomain.replace(".", "-");
        String gatewayName = prefix + "-gateway";
        String grpcRouteName = prefix + "-grpc-route";
        return Map.of(
                "apiVersion", "gateway.networking.k8s.io/v1",
                "kind", "GRPCRoute",
                "metadata", new V1ObjectMeta().name(grpcRouteName).namespace("default"),
                "spec", Map.of(
                        "hostnames", List.of(rpcServerDomain),
                        "parentRefs", List.of(Map.of("name", gatewayName, "namespace", "default")),
                        "rules", List.of(
                                Map.of("backendRefs", List.of(Map.of(
                                        "group", "",
                                        "kind", "Service",
                                        "name", rpcServerServiceName,
                                        "port", 8081,
                                        "weight", 1))))));
    }

    public static void main(String[] args) throws Exception {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        CustomObjectsApi customObjectsApi = new CustomObjectsApi();
        HashMap<String, App> apps = new HashMap<>();

        try (
                Watch<Object> watch = Watch.createWatch(
                        client,
                        customObjectsApi
                                .listClusterCustomObject(CRD_GROUP, CRD_VERSION, CRD_PLURAL)
                                .watch(true)
                                .buildCall(null),
                        new TypeToken<Watch.Response<Object>>() {
                        }.getType()
                )
        ) {
            for (Watch.Response<Object> response : watch) {
                App app = App.fromApiResult(response.object);

                LoggingEventBuilder eventLogger = logger.atInfo()
                                                        .addKeyValue("app", app.metadata().getName())
                                                        .addKeyValue("type", response.type);

                switch (response.type) {
                    case "ADDED":
                        route53ZoneCreator.createZoneIfNotExists(app.metadata().getName(), "logos-dns");
                        apps.put(app.metadata().getUid(), app);
                        eventLogger.log("App added");
                        break;

                    case "MODIFIED":
                        route53ZoneCreator.createZoneIfNotExists(app.metadata().getName(), "logos-dns");
                        apps.put(app.metadata().getUid(), app);
                        eventLogger.log("App modified");
                        break;

                    case "DELETED":
                        eventLogger.log("App deleted");
                        break;

                    case "ERROR":
                        logger.atError()
                              .addKeyValue("rawResponse", response.toString())
                              .log("Error event received");
                        break;
                }

                updateConfigMap(apps);
                triggerRollingUpdate(CLIENT_DEPLOYMENT_NAMESPACE, CLIENT_DEPLOYMENT_NAME);
                updateGateways(apps);
                updateHttpRoutes(apps);
                updateGrpcRoutes(apps);
                updateSecurityPolicy(apps);
            }
        } catch (ApiException e) {
            logger.atError()
                  .setCause(e)
                  .addKeyValue("statusCode", e.getCode())
                  .addKeyValue("responseBody", e.getResponseBody())
                  .addKeyValue("responseHeaders", e.getResponseHeaders())
                  .log("Exception returned from CustomObjectsApi");
            throw e;
        }
    }
}