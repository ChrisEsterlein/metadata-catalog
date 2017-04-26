package ncei.catalog.service

import ncei.catalog.domain.MetadataSchema
import ncei.catalog.domain.MetadataSchemaRepository
import spock.lang.Specification
import javax.servlet.http.HttpServletResponse

class MetadataSchemaServiceSpec extends Specification {

    SchemaService schemaService

    def setup(){
        schemaService = new SchemaService()
        schemaService.metadataSchemaRepository = Mock(MetadataSchemaRepository)
    }

    def 'test schemaService save'(){
        setup: 'instantiate a new schemaMetadata pogo'
        def schemaMetadataMap = [
                "schema_id": UUID.fromString("10686c20-27cc-11e7-9fdf-ef7bfecc6188"),
                "schema_name":"a schema name",
                "json_schema": "{blah: blah}"
        ]

        MetadataSchema schemaMetadata = new MetadataSchema(schemaMetadataMap)

        Map serviceResponse =  [newRecord: schemaMetadata, recordsCreated: 1, code:HttpServletResponse.SC_CREATED]

        when:
        Map result = schemaService.save(schemaMetadata)

        then:
        //the service will try to find the record, which does not exist
        1 * schemaService.metadataSchemaRepository.findByMetadataId(schemaMetadata.schema_id) >> null

        //so it will create a new one
        1 * schemaService.metadataSchemaRepository.save(schemaMetadata) >> schemaMetadata

        //and build the appropriate response
        result == serviceResponse

    }

}
