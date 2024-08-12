package dev.logos.app.controller;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import java.util.Optional;
import java.util.function.Supplier;

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

record AppSpec(
        @SerializedName("web-bundle")
        String webBundle,

        @SerializedName("service-jars")
        List<String> serviceJars
) {
}

public class AppController {
    private static final String CRD_GROUP = "logos.dev";
    private static final String CRD_VERSION = "v1";
    private static final String CRD_PLURAL = "apps";
    private static final String CONFIGMAP_NAME = "logos-apps";
    private static final String CONFIGMAP_NAMESPACE = "default";
    private static final String CONFIGMAP_SERVICE_JAR_KEY = "service-jars";
    private static final String CONFIGMAP_CLIENT_NGINX_DOMAIN_MAP_KEY = "client-nginx-domain-map";
    private static final String DEPLOYMENT_NAME = "backend-deployment";
    private static final String DEPLOYMENT_NAMESPACE = "default";
    private static final Logger logger = getLogger(AppController.class);
    private static final Route53ZoneCreator route53ZoneCreator = new Route53ZoneCreator();

    private static void updateConfigMap(HashMap<String, App> appList) throws IOException, ApiException {
        CoreV1Api api = new CoreV1Api(Config.defaultClient());
        V1ConfigMap configMap = api.readNamespacedConfigMap(CONFIGMAP_NAME, CONFIGMAP_NAMESPACE).execute();

        configMap.putDataItem(
                CONFIGMAP_SERVICE_JAR_KEY,
                getServiceJars(appList).stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll).toString()
        ).putDataItem(
                CONFIGMAP_CLIENT_NGINX_DOMAIN_MAP_KEY,
                clientNginxDomainMap(appList)
        );

        api.replaceNamespacedConfigMap(CONFIGMAP_NAME, CONFIGMAP_NAMESPACE, configMap).execute();
    }

    private static void triggerRollingUpdate() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();

        String patchStr = "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"kubectl.kubernetes.io/restartedAt\":\""
                + OffsetDateTime.now() + "\"}}}}}";

        V1Patch patch = new V1Patch(patchStr);

        try {
            appsV1Api.patchNamespacedDeployment(DEPLOYMENT_NAME, DEPLOYMENT_NAMESPACE, patch).execute();
            logger.info("Rolling update triggered for deployment: {}", DEPLOYMENT_NAME);
        } catch (ApiException e) {
            logger.error("Failed to trigger rolling update for deployment: {}", DEPLOYMENT_NAME, e);
            throw e;
        }
    }

    private static String clientNginxDomainMap(HashMap<String, App> apps) {
        return apps.values().stream().map(app -> "%s %s;".formatted(
                app.metadata().getName(), app.spec().webBundle())).collect(Collectors.joining("\n")
        );
    }

    private static List<String> getServiceJars(HashMap<String, App> apps) {
        return apps.entrySet().stream()
                .flatMap(entry -> entry.getValue().spec().serviceJars().stream())
                .collect(Collectors.toList());
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
                        .addKeyValue("type", response.type)
                        .addKeyValue("serviceJars", app.spec().serviceJars());

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
                //triggerRollingUpdate();
            }
        } catch (ApiException e) {
            logger.atError()
                    .setCause(e)
                    .addKeyValue("statusCode", e.getCode())
                    .addKeyValue("responseBody", e.getResponseBody())
                    .addKeyValue("responseHeaders", e.getResponseHeaders())
                    .log("Exception returned from CustomObjectsApi");
            e.printStackTrace();
        }
    }
}