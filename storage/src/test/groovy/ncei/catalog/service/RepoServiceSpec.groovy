package ncei.catalog.service

import ncei.catalog.domain.CollectionMetadataRepository
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import ncei.catalog.domain.MetadataSchemaRepository
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.cassandra.repository.CassandraRepository
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse


@Ignore
@Unroll
class RepoServiceSpec extends Specification {

  RepoService repoService
  MessageService messageService
  RabbitTemplate mockRabbitTemplate
  CassandraRepository granuleMetadataRepository
  CassandraRepository collectionMetadataRepository
  CassandraRepository schemaMetadataRepository



  def setup(){
    repoService = new RepoService()
    messageService = new MessageService()
    mockRabbitTemplate = Mock(RabbitTemplate)
    messageService.rabbitTemplate = mockRabbitTemplate

    repoService.messageService = messageService
    granuleMetadataRepository = Mock(GranuleMetadataRepository)
    collectionMetadataRepository = Mock(CollectionMetadataRepository)
    schemaMetadataRepository = Mock(MetadataSchemaRepository)

  }

  def 'test repoService save'(){
    setup: 'instantiate a new granuleMetadata pogo'
    def granuleMetadataMap = [
            "id": UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6188"),
            "tracking_id":"test-id-1",
            "filename" : "test.txt",
            "dataset": "test-dataset-1",
            "type":"file",
            "granule_size":1024,
            "granule_metadata": "{blah: blah}",
            "granule_schema":"schema",
            "geometry" : "point(1.1, 1.1)",
            "collections":["FOS"]
    ]

    GranuleMetadata granuleMetadata = new GranuleMetadata(granuleMetadataMap)

    Map serviceResponse =  [granule: granuleMetadata, recordsCreated: 1, code:HttpServletResponse.SC_CREATED]

    when:
    Map result = repoService.save(granuleMetadataRepository, granuleMetadata)

    then:
    //the service will try to find the record, which does not exist
    1 * granuleMetadataRepository.findByMetadataId(granuleMetadata.id) >> null

    //so it will create a new one
    1 * granuleMetadataRepository.save(granuleMetadata) >> granuleMetadata

    1 * mockRabbitTemplate.convertAndSend(_)
    //and build the appropriate response
    result == serviceResponse

  }

}
