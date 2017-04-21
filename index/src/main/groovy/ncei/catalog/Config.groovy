package ncei.catalog

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories (basePackages =  'ncei.catalog.model')
class Config {

    @Value('${elasticsearch.cluster.name}')
    String clusterName

    @Value('${elasticsearch.host}')
    String host

    @Value('${elasticsearch.port}')
    Integer port


/*    @Autowired
    private MetadataRepository repository;

    @Autowired
    private ElasticsearchTemplate template;*/

    @Bean
    Client client() throws Exception {
        def settingsBuilder = Settings.builder()
        /*final Path tmpDir = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")),
                'elasticsearch_data')
*/
        settingsBuilder.put('cluster.name', clusterName)
//        settingsBuilder.put('path.data', tmpDir.toAbsolutePath().toString())
        def client = TransportClient.builder().settings(settingsBuilder.build()).build()

        client.addTransportAddress(
                new InetSocketTransportAddress(InetAddress.getByName(host), port))
    }

    @Bean
    ElasticsearchOperations elasticsearchTemplate() throws Exception {
        return new ElasticsearchTemplate(client())
    }

    //Embedded Elasticsearch Server
    /*@Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchTemplate(nodeBuilder().local(true).node().client());
    }*/


    /*    @Bean
    ElasticsearchTemplate elasticsearchTemplate() {
        return new ElasticsearchTemplate(getNodeClient());
    }

    private static NodeClient getNodeClient() {
        return (NodeClient) nodeBuilder().clusterName(UUID.randomUUID().toString()).local(true).node()
                .client()
    }*/

/*
    @Bean
    public NodeBuilder nodeBuilder() {
        return new NodeBuilder();
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        Settings.Builder elasticsearchSettings =
                Settings.settingsBuilder()
                        .put("http.enabled", "false") // 1
                        .put("path.data", "") // 2
                        .put("path.home", "PATH_TO_YOUR_ELASTICSEARCH_DIRECTORY"); // 3

        return new ElasticsearchTemplate(nodeBuilder()
                .local(true)
                .settings(elasticsearchSettings.build())
                .node()
                .client());
    }
*/
}
