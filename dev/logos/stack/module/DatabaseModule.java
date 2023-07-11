package dev.logos.stack.module;

import com.google.inject.AbstractModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class DatabaseModule extends AbstractModule {

    @Override
    protected void configure() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("STORAGE_PG_BACKEND_JDBC_URL"));
        config.setUsername(System.getenv("STORAGE_PG_BACKEND_USER"));
        config.setPassword(System.getenv("STORAGE_PG_BACKEND_PASSWORD"));
//        config.addDataSourceProperty("cachePrepStmts", "true");
//        config.addDataSourceProperty("prepStmtCacheSize", "200");
//        config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");

        bind(DataSource.class).toInstance(new HikariDataSource(config));

        super.configure();
    }
}
