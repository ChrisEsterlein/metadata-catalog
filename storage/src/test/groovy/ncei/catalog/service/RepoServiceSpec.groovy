package ncei.catalog.service

import ncei.catalog.domain.GranuleMetadata
import org.springframework.data.cassandra.repository.CassandraRepository
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

//@CompileStatic
@Unroll
class RepoServiceSpec extends Specification {


  RepoService repoService
  MessageService messageService = Mock()


  final def granuleMetadataMap = [
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

  def setup(){
    repoService = new RepoService(messageService: messageService)
  }

  def 'test repoService save'(){
    setup: 'findByMetadataId returns something false'

    CassandraRepository granuleMetadataRepository = Mock()
    granuleMetadataRepository.metaClass.findByMetadataId = {UUID id ->
      return null
    }
    HttpServletResponse response = Mock()

    GranuleMetadata granuleMetadata = new GranuleMetadata(granuleMetadataMap)

    when: 'calling service save'
    Map result = repoService.save(response, granuleMetadataRepository, granuleMetadata)

    then: 'the metadata is saved to the repository'
    1 * granuleMetadataRepository.save(granuleMetadata) >> granuleMetadata

    and: 'an insert notification is sent'
    1 * messageService.notifyIndex({
      it.meta.action == 'insert'
    })

    and: 'the status is good'
    1 * response.setStatus(HttpServletResponse.SC_CREATED)
    result.meta.action == 'insert'
    result.errors == null
  }

  def 'test repoService save conflict'(){
    setup: 'findByMetadataId returns an object'
    CassandraRepository granuleMetadataRepository = Mock()
    HttpServletResponse response = Mock()


    GranuleMetadata granuleMetadata = new GranuleMetadata(granuleMetadataMap)

    granuleMetadataRepository.metaClass.findByMetadataId = {UUID id ->
      [granuleMetadata]
    }

    when: 'calling service save'
    Map result = repoService.save(response, granuleMetadataRepository, granuleMetadata)

    then: 'nothing is saved'
    0 * granuleMetadataRepository.save(granuleMetadata) >> granuleMetadata

    and: 'there is no notification'
    0 * messageService.notifyIndex(_)

    and: 'the conflict is returned'
    1 * response.setStatus(HttpServletResponse.SC_CONFLICT)
    result.meta.action == 'insert'
    !result.errors.isEmpty()
  }
}
