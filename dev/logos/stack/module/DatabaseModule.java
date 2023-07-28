package dev.logos.stack.module;

import com.google.inject.AbstractModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.postgresql.Driver;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;


class RdsIamAuthHikariDataSource extends HikariDataSource {
    private final String CLUSTER_RW_CNAME = "db-rw.logos.dev";
    private final int CLUSTER_RW_PORT = 5432;

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
            return cname.getTarget().toString();
        } else {
            throw new RuntimeException("No CNAME found for: " + CLUSTER_RW_CNAME);
        }
    }

    @Override
    public String getPassword() {
        var url = Driver.parseURL(getJdbcUrl(), null);

        /* This allows the hostname to be overridden when DNS resolution is
           not possible, such as at build-time or in dev environments. In
           deployment, this should be set by resolving via DNS. */
        String hostname = System.getenv("STORAGE_PG_BACKEND_HOST");
        if (hostname == null) {
            // hostname = resolveEndpoint();
            hostname = "dev-logos-dev-rds-cluster.cluster-ccx6cpd92war.us-east-2.rds.amazonaws.com";
        }

        assert url != null;
        return RdsUtilities.builder()
                           .region(new DefaultAwsRegionProviderChain().getRegion())
                           .build()
                           .generateAuthenticationToken(
                               GenerateAuthenticationTokenRequest
                                   .builder()
                                   .credentialsProvider(ProfileCredentialsProvider.create())
                                   .hostname(hostname)
                                   .port(CLUSTER_RW_PORT)
                                   .username(getUsername())
                                   .build());
    }
}

public class DatabaseModule extends AbstractModule {

    @Override
    protected void configure() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("STORAGE_PG_BACKEND_JDBC_URL"));
        config.setUsername(System.getenv("STORAGE_PG_BACKEND_USER"));
//        config.addDataSourceProperty("cachePrepStmts", "true");
//        config.addDataSourceProperty("prepStmtCacheSize", "200");
//        config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");

        bind(DataSource.class).toInstance(new RdsIamAuthHikariDataSource(config));

        super.configure();
    }
}
