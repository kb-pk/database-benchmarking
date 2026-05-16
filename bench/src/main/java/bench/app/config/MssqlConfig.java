package bench.app.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        entityManagerFactoryRef = "mssqlEntityManagerFactory",
        transactionManagerRef = "mssqlTransactionManager",
        basePackages = "bench.app.repository.sql.mssql"
)
public class MssqlConfig {
    @Bean(name = "mssqlProperties")
    @ConfigurationProperties("app.datasource.mssql")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "mssqlDataSource")
    @ConfigurationProperties("app.datasource.mssql")
    public DataSource dataSource(@Qualifier("mssqlProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean mssqlEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("mssqlDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("bench.app.model.sql")
                .persistenceUnit("mssql")
                .build();
    }

    @Bean
    public PlatformTransactionManager mssqlTransactionManager(
            @Qualifier("mssqlEntityManagerFactory") EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
}