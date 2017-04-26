package ncei.catalog.service

import ncei.catalog.domain.CollectionMetadataRepository
import ncei.catalog.domain.CollectionMetadata
import spock.lang.Specification
import javax.servlet.http.HttpServletResponse


class CollectionServiceSpec extends Specification {

    CollectionService collectionService

    def setup(){
        collectionService = new CollectionService()
        collectionService.collectionMetadataRepository = Mock(CollectionMetadataRepository)
    }

    def 'test collectionService save'(){
        setup: 'instantiate a new collectionMetadata pogo'

        def collectionMetadataMap = [
                "collection_id": UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6188"),
                "collection_name":'a collection name',
                "collection_metadata": "{blah: blah}",
                "collection_schema":"schema",
                "type":"a type of collection"
        ]

        CollectionMetadata collectionMetadata = new CollectionMetadata(collectionMetadataMap)

        Map serviceResponse =  [newRecord: collectionMetadata, recordsCreated: 1, code:HttpServletResponse.SC_CREATED]

        when:
        Map result = collectionService.save(collectionMetadata)

        then:
        //the service will try to find the record, which does not exist
        1 * collectionService.collectionMetadataRepository.findByMetadataId(collectionMetadata.collection_id) >> null

        //so it will create a new one
        1 * collectionService.collectionMetadataRepository.save(collectionMetadata) >> collectionMetadata

        //and build the appropriate response
        result == serviceResponse

    }

}
