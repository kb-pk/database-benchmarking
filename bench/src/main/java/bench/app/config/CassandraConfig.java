package bench.app.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.net.InetSocketAddress;

@Configuration
@EnableCassandraRepositories(
        cassandraTemplateRef = "cassandraTemplate",
        basePackages = "com.myapp.repository.cassandra"
)
public class CassandraConfig {

    @Value("${spring.cassandra.contact-points}") private String contactPoints;
    @Value("${spring.cassandra.local-datacenter}") private String localDc;
    @Value("${spring.cassandra.keyspace-name}") private String keyspace;

    @Primary
    @Bean(name = "cassandraSession")
    public CqlSession cassandraSession() {
        String[] parts = contactPoints.split(":");
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])))
                .withLocalDatacenter(localDc)
                .withKeyspace(keyspace)
                .build();
    }

    @Primary
    @Bean(name = "cassandraTemplate")
    public CassandraTemplate cassandraTemplate(@Qualifier("cassandraSession") CqlSession session, CassandraConverter converter) {
        return new CassandraTemplate(session, converter);
    }
}