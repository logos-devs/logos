package dev.logos.service.storage.module;

import com.google.inject.*;
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

    @Provides
    @DatabaseEndpoint
    String provideDatabaseEndpoint() throws TextParseException {
        return ((CNAMERecord) Optional.ofNullable(new Lookup(CLUSTER_RW_CNAME, Type.CNAME).run())
                .filter(r -> r.length > 0)
                .orElseThrow(() -> new ProvisionException("No CNAME found for: " + CLUSTER_RW_CNAME))[0])
                .getTarget().toString(true);
    }

    @Provides
    @DatabaseJdbcUrl
    String provideDatabaseJdbcUrl(@DatabaseEndpoint String endpoint) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setServerNames(new String[]{endpoint});
        return dataSource.getUrl();
    }

    @Provides
    @Singleton
    DataSource provideHikariDataSource(HikariConfig config, @DatabaseEndpoint String endpoint) {
        return new HikariDataSource(config) {
            @Override
            public String getPassword() {
                return RdsUtilities.builder()
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
        return config;
    }

    @Provides
    Jdbi provideJdbi(DataSource dataSource) {
        return Jdbi.create(dataSource).installPlugin(new PostgresPlugin());
    }
}
