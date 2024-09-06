package dev.logos.module;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleLoader extends AbstractModule {
    private static final String META_INF_DIR = "META-INF";
    private static final String APP_MODULE_PREFIX = META_INF_DIR + "/app-modules-";
    private static final Logger logger = LoggerFactory.getLogger(ModuleLoader.class);

    @Override
    protected void configure() {
        URLClassLoader urlClassLoader = getURLClassLoader();
        try {
            discoverModules(urlClassLoader);
        } catch (IOException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    URLClassLoader getURLClassLoader() {
        return new URLClassLoader(
                readConfigMap().stream().map(jarPathStr -> {
                    Path jarPath = Paths.get(getRequiredEnv("LOGOS_JAR_DIR"), jarPathStr);
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

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();

            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    String entryName = entries.nextElement().getName();

                    if (entryName.startsWith(APP_MODULE_PREFIX)) {
                        logger.atInfo().addKeyValue("jarEntry", entryName).log("Loading jar entry");

                        try (InputStream inputStream = classLoader.getResourceAsStream(entryName)) {
                            if (inputStream != null) {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        try {
                                            Class<?> clazz = Class.forName(line, true, classLoader);
                                            logger.atInfo().addKeyValue("module", clazz.getCanonicalName()).log("Loading module");
                                            install((AbstractModule) clazz.getDeclaredConstructor().newInstance());
                                        } catch (ClassNotFoundException e) {
                                            logger.atError().setCause(e).addKeyValue("class", line).log("Error loading class");
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static String getRequiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Required environment variable " + key + " is not set");
        }
        return value;
    }

    private List<String> readConfigMap() {
        if (System.getenv("KUBERNETES_SERVICE_HOST") == null) {
            return List.of();
        }

        String configPath = getRequiredEnv("LOGOS_SERVICE_JAR_CONFIG_PATH");
        try (Reader reader = Files.newBufferedReader(Paths.get(configPath))) {
            return Arrays.asList(new Gson().fromJson(reader, String[].class));
        } catch (IOException e) {
            logger.atError().setCause(e).addKeyValue("CONFIG_PATH", configPath).log("Apps configMap file not found");
            throw new RuntimeException(e);
        }
    }

    public static Injector createInjector() {
        return com.google.inject.Guice.createInjector(new ModuleLoader());
    }
}
