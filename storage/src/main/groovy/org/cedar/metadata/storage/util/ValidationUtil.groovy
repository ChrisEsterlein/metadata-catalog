package org.cedar.metadata.storage.util

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.ListReportProvider
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.cedar.metadata.storage.domain.CollectionMetadataRepository
import org.cedar.metadata.storage.domain.MetadataRecord
import com.github.fge.jsonschema.main.JsonSchema
import org.cedar.metadata.storage.domain.MetadataSchema
import org.cedar.metadata.storage.domain.MetadataSchemaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository

@Slf4j
class ValidationUtil {

  @Autowired
  MetadataSchemaRepository metadataSchemaRepository

  boolean validate(MetadataRecord metadataRecord){
    if(!metadataRecord.metadata_schema){
      return true
    }

    UUID schemaId = metadataRecord.metadata_schema instanceof  UUID ? metadataRecord.metadata_schema : UUID.fromString(metadataRecord.metadata_schema)
    Iterable<MetadataRecord> results = metadataSchemaRepository.findByMetadataId(schemaId)
    MetadataSchema metadataSchema = results.first()
    JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault()
//    JsonSchemaFactory schemaFactory = JsonSchemaFactory.newBuilder()
//        .setReportProvider(new ListReportProvider(LogLevel.ERROR, LogLevel.ERROR))
//        .freeze()
    ObjectMapper objectMapper = new ObjectMapper()
    JsonSlurper jsonSlurper = new JsonSlurper()

    Map metadataJson = jsonSlurper.parseText(metadataRecord.metadata as String)
    Map schemaJson = jsonSlurper.parseText(metadataSchema.json_schema)

    log.info "Metadata: \n $metadataJson"
    log.info "Schema: \n $schemaJson"

    JsonNode metadataNode = objectMapper.valueToTree(metadataJson)
    JsonNode metadataSchemaNode = objectMapper.valueToTree(schemaJson)

    JsonSchema schema = schemaFactory.getJsonSchema(metadataSchemaNode)

    ProcessingReport report = schema.validate(metadataNode)
    println(report)


    return report.isSuccess()
  }

}
