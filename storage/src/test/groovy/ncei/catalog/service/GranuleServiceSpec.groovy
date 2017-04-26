package ncei.catalog.service

import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import spock.lang.Specification
import javax.servlet.http.HttpServletResponse

class GranuleServiceSpec extends Specification {

  GranuleService granuleService

  def setup(){
    granuleService = new GranuleService()
    granuleService.granuleMetadataRepository = Mock(GranuleMetadataRepository)
  }

  def 'test granuleService save'(){
    setup: 'instantiate a new granuleMetadata pogo'
    def granuleMetadataMap = [
            "granule_id": UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6188"),
            "tracking_id":"test-id-1",
            "filename" : "test.txt",
            "dataset": "test-dataset-1",
            "type":"file",
            "granule_size":1024,
            "granule_metadata": "{blah: blah}",
            "granule_schema":"schema",
            "geometry" : "point(1.1, 1.1)",
            "collection":["FOS"]
    ]

    GranuleMetadata granuleMetadata = new GranuleMetadata(granuleMetadataMap)

    Map serviceResponse =  [newRecord: granuleMetadata, recordsCreated: 1, code:HttpServletResponse.SC_CREATED]

    when:
    Map result = granuleService.save(granuleMetadata)

    then:
    //the service will try to find the record, which does not exist
    1 * granuleService.granuleMetadataRepository.findByMetadataId(granuleMetadata.granule_id) >> null

    //so it will create a new one
    1 * granuleService.granuleMetadataRepository.save(granuleMetadata) >> granuleMetadata

    //and build the appropriate response
    result == serviceResponse

  }

}