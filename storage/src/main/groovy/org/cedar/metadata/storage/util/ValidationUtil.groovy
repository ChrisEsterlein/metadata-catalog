package org.cedar.metadata.storage.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory
import groovy.util.logging.Slf4j
import org.cedar.metadata.storage.domain.CollectionMetadataRepository
import org.cedar.metadata.storage.domain.MetadataRecord
import com.github.fge.jsonschema.main.JsonSchema
import org.cedar.metadata.storage.domain.MetadataSchemaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository

@Slf4j
class ValidationUtil {

  @Autowired
  MetadataSchemaRepository metadataSchemaRepository

  boolean validate(CassandraRepository repository, MetadataRecord metadataRecord){
    if(!metadataRecord.schema){
      return true
    }

    UUID schemaId = metadataRecord.schema instanceof  UUID ? metadataRecord.schema : UUID.fromString(metadataRecord.schema)
    MetadataRecord metadataSchema = metadataSchemaRepository.findByMetadataId(schemaId)

    JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault()

    ObjectMapper objectMapper = new ObjectMapper()
    JsonNode schemaNode = objectMapper.valueToTree(metadataSchema.json_schema)

    JsonSchema schema = schemaFactory.getJsonSchema(schemaNode)

    ProcessingReport report = schema.validate(metadataRecord)
    println(report)

    return report.isSuccess()
  }

}
