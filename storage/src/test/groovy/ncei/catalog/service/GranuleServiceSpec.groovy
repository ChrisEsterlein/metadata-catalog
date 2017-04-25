package ncei.catalog.service

import ncei.catalog.domain.GranuleMetadata
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class GranuleServiceSpec extends Specification {

  GranuleService granuleService

  def setup(){
    granuleService = new GranuleService()
    granuleService.granuleMetadataRepository
  }

  def 'test granuleService save'(){
    setup:
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

    when:
    granuleService.save(granuleMetadata)

    then:
    granuleService.list(granuleMetadata)

  }

}
