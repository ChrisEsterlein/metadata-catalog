package org.cedar.metadata.storage.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.cedar.metadata.storage.domain.MetadataRecord
import org.cedar.metadata.storage.domain.MetadataSchema
import org.cedar.metadata.storage.domain.MetadataSchemaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class ValidationUtil {

  @Autowired
  MetadataSchemaRepository metadataSchemaRepository

  JsonSlurper jsonSlurper = new JsonSlurper()

  boolean validate(MetadataRecord metadataRecord){
    if(!metadataRecord.metadata_schema){
      return true
    }

    UUID schemaId = metadataRecord.metadata_schema instanceof  UUID ? metadataRecord.metadata_schema : UUID.fromString(metadataRecord.metadata_schema)
    Iterable<MetadataRecord> results = metadataSchemaRepository.findByMetadataId(schemaId)
    MetadataSchema metadataSchema = results.first()
    JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault()

    ObjectMapper objectMapper = new ObjectMapper()


    Map metadataJson = jsonSlurper.parseText(metadataRecord.metadata as String)
    Map schemaJson = jsonSlurper.parseText(metadataSchema.json_schema)
    schemaJson.definitions = fetchDefinitions(schemaJson)
    log.info "Schema: \n ${new JsonBuilder(schemaJson).toPrettyString()}"

    JsonNode metadataNode = objectMapper.valueToTree(metadataJson)
    JsonNode metadataSchemaNode = objectMapper.valueToTree(schemaJson)

    JsonSchema schema = schemaFactory.getJsonSchema(metadataSchemaNode)

    ProcessingReport report = schema.validate(metadataNode)
    println(report)

    return report.isSuccess()
  }

  Map fetchDefinitions(Map schemaJson, Map definitions = [:]){
    Set refs = findRefsUpdateValues(schemaJson) as Set
    refs.each{
      if(!(it in schemaJson) && !(it in definitions)){
        MetadataRecord subSchema = metadataSchemaRepository.findBySchemaName(it)?.first()
        Map js = jsonSlurper.parseText(subSchema.json_schema)
        js.remove('id')
        definitions."$it" = js
      }
    }

    List definitionRefs = findRefsUpdateValues(definitions)
    definitionRefs.each{
      if(!(it in definitions)){
        MetadataRecord subSchema = metadataSchemaRepository.findBySchemaName(it)?.first()
        Map js = jsonSlurper.parseText(subSchema.json_schema)
        js.remove('id')
        definitions."$it" = js
      }
    }
    definitions
  }

  List findRefsUpdateValues(Map map){
    map.collectMany{it ->
      if (it.key == '$ref') {
        String s = it.value
        it.value = '#/definitions/'+it.value
        return [s]
      }
      else if (it.value instanceof Map){
        return findRefsUpdateValues(it.value)
      }else{return []}
    }
  }
}
