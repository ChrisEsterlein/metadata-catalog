package ncei.catalog.util

import ncei.catalog.domain.FileMetadata
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.utils.ClassConversionUtil
import spock.lang.Specification


class ClassConversionUtilSpec extends Specification {

  def 'test granuleMetadata to fileMetadata'(){ //todo fix names to match test
    setup:

    def fileMetadata = [
      "trackingId": "test-id-1",
      "filename": "test.txt",
      "dataset": "test-dataset-1",
      "type": "file",
      "fileSize": 1024,
      "fileMetadata": "{blah: blah}",
      "geometry": "point(1.1, 1.1)"
    ]

    FileMetadata fm = new FileMetadata(fileMetadata)

    when:
    GranuleMetadata resultingGm = ClassConversionUtil.convertToGranuleMetadata(fm)

    println "the result $resultingGm"
    then:


    assert resultingGm.tracking_id == fileMetadata.trackingId
    assert resultingGm.dataset == fileMetadata.dataset
    assert resultingGm.granule_metadata == fileMetadata.fileMetadata
    assert resultingGm.granule_size == fileMetadata.fileSize

  }

  def 'test fileMetadata to granuleMetadata'(){
    setup:
    def granuleMetadata = [
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

    GranuleMetadata gm = new GranuleMetadata(granuleMetadata)

    when:
    FileMetadata resultingFm = ClassConversionUtil.convertToFileMetadata(gm)

    println "result $resultingFm"
    then:
    assert resultingFm.trackingId == granuleMetadata.tracking_id
    assert resultingFm.dataset == granuleMetadata.dataset
    assert resultingFm.fileMetadata == granuleMetadata.granule_metadata
    assert resultingFm.fileSize == granuleMetadata.granule_size

  }

}
