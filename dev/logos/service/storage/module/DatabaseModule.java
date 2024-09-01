package dev.logos.service.storage.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.logos.app.register.registerModule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.postgresql.Driver;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Optional;

class RdsIamAuthHikariDataSource extends HikariDataSource {
    private static final String CLUSTER_RW_CNAME = "db-rw-service";
    private static final int CLUSTER_RW_PORT = 5432;

    public RdsIamAuthHikariDataSource(HikariConfig config) {
        super(config);
    }

    private String resolveEndpoint() {
        Record[] records;

        try {
            records = new Lookup(CLUSTER_RW_CNAME, Type.CNAME).run();
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }

        if (records != null && records.length > 0) {
            CNAMERecord cname = (CNAMERecord) records[0];
            String hostname = cname.getTarget().toString();
            return hostname.substring(0, hostname.length() - 1);
        } else {
            throw new RuntimeException("No CNAME found for: " + CLUSTER_RW_CNAME);
        }
    }

    @Override
    public String getPassword() {
        /*
         * This allows the hostname to be overridden when DNS resolution is
         * not possible, such as at build-time or in dev environments. In
         * deployment, this should be set by resolving via DNS.
         */
        String hostname = Optional.ofNullable(System.getenv("STORAGE_PG_BACKEND_HOST")).orElse(resolveEndpoint());

        return RdsUtilities.builder()
                           .region(new DefaultAwsRegionProviderChain().getRegion())
                           .build()
                           .generateAuthenticationToken(
                               GenerateAuthenticationTokenRequest
                                   .builder()
                                   .credentialsProvider(DefaultCredentialsProvider.create())
                                   .hostname(hostname)
                                   .port(CLUSTER_RW_PORT)
                                   .username(getUsername())
                                   .build());
    }
}

@registerModule
public class DatabaseModule extends AbstractModule {
    static String DB_URL = Optional.ofNullable(System.getenv("STORAGE_PG_BACKEND_JDBC_URL"))
                                   .orElse("jdbc:postgresql://localhost:15432/logos");
    static String DB_USER = Optional.ofNullable(System.getenv("STORAGE_PG_BACKEND_USER"))
                                    .orElse("storage");

    @Provides
    @Singleton // otherwise a new DataSource is created for each injection which results in a huge number of connections
    public DataSource provideDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.addDataSourceProperty("minimumIdle", 5);
        config.addDataSourceProperty("maximumPoolSize", 25);
        // config.addDataSourceProperty("cachePrepStmts", "true");
        // config.addDataSourceProperty("prepStmtCacheSize", "200");
        // config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");
        return new RdsIamAuthHikariDataSource(config);
    }

    @Provides
    Jdbi provideJdbi(DataSource dataSource) {
        return Jdbi.create(dataSource).installPlugin(new PostgresPlugin());
    }
}
