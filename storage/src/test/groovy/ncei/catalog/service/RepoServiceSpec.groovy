package ncei.catalog.service

import ncei.catalog.domain.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

@Ignore
@Unroll
class RepoServiceSpec extends Specification {


  @Autowired
  RabbitTemplate rabbitTemplate

  RepoService repoService
  MessageService messageService
  CassandraRepository granuleMetadataRepository
  CassandraRepository collectionMetadataRepository
  CassandraRepository schemaMetadataRepository

  HttpServletResponse response


  def setup() {
    repoService = new RepoService()
    messageService = new MessageService()
    response = Mock(HttpServletResponse)
    response = Mock(HttpServletResponse)

//    mockRabbitTemplate = Mock(RabbitTemplate)
    messageService.rabbitTemplate = rabbitTemplate
    repoService.messageService = messageService
    granuleMetadataRepository = Mock(GranuleMetadataRepository)
    collectionMetadataRepository = Mock(CollectionMetadataRepository)
    schemaMetadataRepository = Mock(MetadataSchemaRepository)

  }

  Map createDataItem(MetadataRecord metadataRecord) {
    [id: metadataRecord.id, type: getTableFromClass(metadataRecord), attributes: metadataRecord.asMap()]
  }

  def getTableFromClass(MetadataRecord metadataRecord) {
    switch (metadataRecord.class) {
      case CollectionMetadata:
        return 'collection'
        break
      case GranuleMetadata:
        return 'granule'
        break
      case MetadataSchema:
        return 'schema'
        break
    }
  }

  def 'test repoService save'() {
    setup: 'instantiate a new granuleMetadata pogo'
    def granuleMetadataMap = [
            "id"              : UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6188"),
            "tracking_id"     : "test-id-1",
            "filename"        : "test.txt",
            "dataset"         : "test-dataset-1",
            "type"            : "file",
            "granule_size"    : 1024,
            "granule_metadata": "{blah: blah}",
            "granule_schema"  : "schema",
            "geometry"        : "point(1.1, 1.1)",
            "collections"     : ["FOS"]
    ]

    GranuleMetadata granuleMetadata = new GranuleMetadata(granuleMetadataMap)

    Map serviceResponse = [data: createDataItem(granuleMetadata), meta: [action: 'insert']]

    when:
    Map result = repoService.save(response, granuleMetadataRepository, granuleMetadata)

    then:
    //the service will try to find the record, which does not exist
    1 * granuleMetadataRepository.findByMetadataId(granuleMetadata.id) >> null

    //so it will create a new one
    1 * granuleMetadataRepository.save(granuleMetadata) >> granuleMetadata

    1 * messageService.notifyIndex(serviceResponse)

//    1 * messageService.rabbitTemplate.convertAndSend('index-consumer', serviceResponse)

//    1 * mockRabbitTemplate.convertAndSend(_)
    //and build the appropriate response
    result == serviceResponse

  }

}