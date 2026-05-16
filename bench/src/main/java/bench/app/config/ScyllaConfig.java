package bench.app.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import java.net.InetSocketAddress;

@Configuration
@EnableCassandraRepositories(
        cassandraTemplateRef = "scyllaTemplate",
        basePackages = "bench.app.repository.cql.scylla"
)
public class ScyllaConfig {
    @Value("${app.datasource.scylla.contact-points}") private String contactPoints;
    @Value("${app.datasource.scylla.local-datacenter}") private String localDc;
    @Value("${app.datasource.scylla.keyspace-name}") private String keyspace;

    @Bean
    public CqlSession scyllaSession() {
        String[] parts = contactPoints.split(":");
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])))
                .withLocalDatacenter(localDc)
                .withKeyspace(keyspace)
                .build();
    }

    @Bean
    public CassandraTemplate scyllaTemplate(CqlSession scyllaSession, CassandraConverter converter) {
        return new CassandraTemplate(scyllaSession, converter);
    }
}