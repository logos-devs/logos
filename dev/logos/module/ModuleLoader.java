package dev.logos.module;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleLoader extends AbstractModule {
    private static final String META_INF_DIR = "META-INF";
    private static final String APP_MODULE_PREFIX = META_INF_DIR + "/app-modules-";
    private static final Logger logger = LoggerFactory.getLogger(ModuleLoader.class);

    @Override
    protected void configure() {
        //URLClassLoader urlClassLoader = getURLClassLoader();
        try {
            discoverModules(ModuleLoader.class.getClassLoader());
        } catch (IOException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    URLClassLoader getURLClassLoader() {
        return new URLClassLoader(
                readConfigMap().stream().map(jarPathStr -> {
                    Path jarPath = Paths.get(getEnvOrDefault("LOGOS_JAR_DIR", ""), jarPathStr);
                    if (!Files.exists(jarPath)) {
                        throw new IllegalStateException("Jar file not found: " + jarPath);
                    }

                    if (!Files.isReadable(jarPath)) {
                        throw new IllegalStateException("Jar file is not readable: " + jarPath);
                    }
                    return jarPath.toUri();
                }).map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).toArray(URL[]::new),
                getClass().getClassLoader()
        );
    }

    private void discoverModules(ClassLoader classLoader) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Enumeration<URL> resources = classLoader.getResources(META_INF_DIR);

        Set<String> appModules = new HashSet<>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();

            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    String entryName = entries.nextElement().getName();

                    if (entryName.startsWith(APP_MODULE_PREFIX)) {
                        logger.atInfo()
                              .addKeyValue("appModule", entryName)
                              .addKeyValue("jarFile", jarFile.getName())
                              .log("Loading jar " + META_INF_DIR + " app-module entry");

                        try (InputStream inputStream = classLoader.getResourceAsStream(entryName)) {
                            if (inputStream != null) {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                                    String appModuleClassName;
                                    while ((appModuleClassName = reader.readLine()) != null) {
                                        appModules.add(appModuleClassName);
                                        logger.atInfo()
                                              .addKeyValue("requestedModule", appModuleClassName)
                                              .addKeyValue("requestedBy", jarFile.getName())
                                              .log("Loading jar entry");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (String appModule : appModules) {
            try {
                Class<?> clazz = Class.forName(appModule, true, classLoader);
                if (clazz.getSimpleName().equals("InfrastructureModule")
                        && System.getenv("AWS_REGION") == null) {
                    logger.atInfo()
                          .addKeyValue("module", clazz.getCanonicalName())
                          .log("Skipping module due to missing AWS_REGION");
                    continue;
                }

                logger.atInfo()
                      .addKeyValue("module", clazz.getCanonicalName())
                      .log("Loading module");
                install((AbstractModule) clazz.getDeclaredConstructor().newInstance());
            } catch (ClassNotFoundException e) {
                logger.atError()
                      .setCause(e)
                      .addKeyValue("class", appModule)
                      .log("Error loading class");
                throw new RuntimeException(e);
            }
        }
    }

    private static String getEnvOrDefault(String key, String defaultVal) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            return defaultVal;
        }
        return value;
    }

    private List<String> readConfigMap() {
        if (System.getenv("KUBERNETES_SERVICE_HOST") == null) {
            return List.of();
        }

        String configPath = getEnvOrDefault("LOGOS_SERVICE_JAR_CONFIG_PATH", "");
        if (configPath.isEmpty()) {
            return List.of();
        }
        try (Reader reader = Files.newBufferedReader(Paths.get(configPath))) {
            return Arrays.asList(new Gson().fromJson(reader, String[].class));
        } catch (IOException e) {
            logger.atError().setCause(e).addKeyValue("CONFIG_PATH", configPath).log("Apps configMap file not found");
            throw new RuntimeException(e);
        }
    }

    public static Injector createInjector(Module... modules) {
        return createInjector(Stage.DEVELOPMENT, modules);
    }

    public static Injector createInjector(Stage stage, Module... modules) {
        ArrayList<Module> moduleList = new ArrayList<>(Arrays.asList(modules));
        moduleList.add(new ModuleLoader());
        return Guice.createInjector(stage, moduleList);
    }
}
