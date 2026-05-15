package bench.app.config;

import bench.app.service.userpermission.BenchmarkEngineResolver;
import bench.app.service.userpermission.DatabaseEngine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DynamicDataSourceConfig {
    
    private final BenchmarkEngineResolver engineResolver;

    public DynamicDataSourceConfig(BenchmarkEngineResolver engineResolver) {
        this.engineResolver = engineResolver;
    }

    @Bean
    public DataSource dataSource() {
        DatabaseEngine engine = engineResolver.resolveEngine();
        
        return switch (engine) {
            case POSTGRESQL -> postgresqlDataSource();
            case MSSQL -> mssqlDataSource();
            case CASSANDRA, SCYLLA -> postgresqlDataSource(); // default - nie obsługujemy jeszcze NoSQL przez JDBC
        };
    }

    private DataSource postgresqlDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        config.setUsername("postgres");
        config.setPassword("@testtest123A");
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    private DataSource mssqlDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlserver://localhost:1433;databaseName=master;TrustServerCertificate=true");
        config.setUsername("sa");
        config.setPassword("YourStrong!Passw0rd");
        config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }
}
