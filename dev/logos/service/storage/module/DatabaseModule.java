package dev.logos.service.storage.module;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.logos.app.register.registerModule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

import javax.sql.DataSource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.Properties;
import java.util.Map;

@registerModule
public class DatabaseModule extends AbstractModule {
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DatabaseEndpoint {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DatabaseJdbcUrl {
    }

    private static final String CLUSTER_RW_CNAME = "db-rw-service";
    private static final int CLUSTER_RW_PORT = 5432;
    static String DB_URL = Optional.ofNullable(System.getenv("STORAGE_PG_BACKEND_JDBC_URL"))
            .orElse("jdbc:postgresql://localhost:15432/logos");
    static String DB_USER = Optional.ofNullable(System.getenv("STORAGE_PG_BACKEND_USER"))
            .orElse("storage");

    private static void logDebugSnapshot(String phase, Object... extra) {
        System.err.println("=== [AUTHOR-DB-DEBUG] DatabaseModule: " + phase + " ===");
        System.err.println("  user.dir=" + System.getProperty("user.dir"));
        System.err.println("  java.class.path=" + System.getProperty("java.class.path"));
        for (Map.Entry<String,String> env : System.getenv().entrySet()) {
            String k = env.getKey().toLowerCase();
            if(k.contains("pg") || k.contains("db") || k.contains("jdbc"))
                System.err.println("  ENV " + env.getKey() + "=" + env.getValue());
        }
        for (Map.Entry<Object,Object> e : System.getProperties().entrySet()) {
            String k = e.getKey().toString().toLowerCase();
            if(k.contains("pg") || k.contains("db") || k.contains("jdbc"))
                System.err.println("  PROP " + e.getKey() + "=" + e.getValue());
        }
        if (extra != null) {
            for (Object obj : extra) {
                System.err.println("  EXTRA: " + obj);
            }
        }
        System.err.println("=== [AUTHOR-DB-DEBUG] END SNAPSHOT ===");
    }

    @Provides
    @DatabaseEndpoint
    String provideDatabaseEndpoint() throws TextParseException {
        logDebugSnapshot("provideDatabaseEndpoint()");
        return Optional.ofNullable(new Lookup(CLUSTER_RW_CNAME, Type.CNAME).run())
                .filter(r -> r.length > 0)
                .map(record -> ((CNAMERecord) record[0]).getTarget().toString(true))
                .orElse(CLUSTER_RW_CNAME);
    }

    @Provides
    @DatabaseJdbcUrl
    String provideDatabaseJdbcUrl(@DatabaseEndpoint String endpoint) {
        logDebugSnapshot("provideDatabaseJdbcUrl()", "endpoint=" + endpoint, "DB_URL=" + DB_URL);
        if (endpoint.equals(CLUSTER_RW_CNAME)) {
            return DB_URL;
        }
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setServerNames(new String[]{endpoint});
        return dataSource.getUrl();
    }

    @Provides
    @Singleton
    DataSource provideHikariDataSource(HikariConfig config, @DatabaseEndpoint String endpoint) {
        logDebugSnapshot("provideHikariDataSource()", "config.jdbcUrl=" + config.getJdbcUrl(), "config.username=" + config.getUsername(), "endpoint=" + endpoint);
        return new HikariDataSource(config) {
            @Override
            public String getPassword() {
                String token = RdsUtilities.builder()
                        .region(new DefaultAwsRegionProviderChain().getRegion())
                        .build()
                        .generateAuthenticationToken(
                                GenerateAuthenticationTokenRequest
                                        .builder()
                                        .credentialsProvider(DefaultCredentialsProvider.create())
                                        .hostname(
                                                Optional.ofNullable(System.getenv("STORAGE_PG_BACKEND_HOST"))
                                                        .orElse(endpoint))
                                        .port(CLUSTER_RW_PORT)
                                        .username(getUsername())
                                        .build());
                logDebugSnapshot("getPassword()", "hostname=" + Optional.ofNullable(System.getenv("STORAGE_PG_BACKEND_HOST")).orElse(endpoint), "port=" + CLUSTER_RW_PORT, "username=" + getUsername());
                return token;
            }
        };
    }

    @Provides
    @Singleton
    HikariConfig provideHikariConfig(@DatabaseJdbcUrl String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(DB_USER);
        config.addDataSourceProperty("minimumIdle", 5);
        config.addDataSourceProperty("maximumPoolSize", 25);
        logDebugSnapshot("provideHikariConfig()", "jdbcUrl=" + jdbcUrl, "DB_USER=" + DB_USER);
        return config;
    }

    @Provides
    Jdbi provideJdbi(DataSource dataSource) {
        logDebugSnapshot("provideJdbi()", "dataSource.class=" + dataSource.getClass().getName());
        return Jdbi.create(dataSource).installPlugin(new PostgresPlugin());
    }
}
