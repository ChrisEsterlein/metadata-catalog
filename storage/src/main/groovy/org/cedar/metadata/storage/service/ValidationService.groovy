package org.cedar.metadata.storage.service

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
class ValidationService {

  @Autowired
  MetadataSchemaRepository metadataSchemaRepository

  JsonSchemaFactory schemaFactory
  ObjectMapper objectMapper
  JsonSlurper jsonSlurper

  ValidationService(){
    schemaFactory = JsonSchemaFactory.byDefault()
    objectMapper = new ObjectMapper()
    jsonSlurper = new JsonSlurper()
  }

  ProcessingReport validate(MetadataRecord metadataRecord){
    log.info "Attempting to validate record $metadataRecord.id against schema $metadataRecord.metadata_schema"
    //todo- determine if metadata_schema is going to store an id or a schema name?
    UUID schemaId = metadataRecord.metadata_schema instanceof  UUID ? metadataRecord.metadata_schema : UUID.fromString(metadataRecord.metadata_schema)

    Iterable<MetadataRecord> results = metadataSchemaRepository.findByMetadataId(schemaId)
    if(!results && !results.size()){throw new IllegalArgumentException('Record references non-existent schema: ' + schemaId)} //controller advice catches it and returns 400
    MetadataSchema metadataSchema = results.first()

    Map metadataJson = jsonSlurper.parseText(metadataRecord.metadata as String)
    Map schemaJson = jsonSlurper.parseText(metadataSchema.json_schema)

    Map completeJsonSchema = fetchDefinitions(schemaJson)
    updateRefs(completeJsonSchema)

    log.debug "Validating ${jsonSlurper.parseText(metadataRecord.metadata)} against schema: \n ${completeJsonSchema}"
    JsonNode metadataNode = objectMapper.valueToTree(metadataJson)
    JsonNode metadataSchemaNode = objectMapper.valueToTree(completeJsonSchema)
    JsonSchema schema = schemaFactory.getJsonSchema(metadataSchemaNode)
    ProcessingReport report = schema.validate(metadataNode)
    return report
  }

  Map fetchDefinitions(Map schemaJson){
    Set refs = findRefs(schemaJson) as Set
    Set defs = schemaJson?.definitions ? schemaJson.definitions.keySet() : []

    if(!refs || defs.sort() == refs.sort()){return schemaJson}

    Map definitions = schemaJson?.definitions ?: [:]

    (refs - defs).collectEntries(definitions){
      if(!(it in definitions)){
        Iterable<MetadataRecord> schemaList = metadataSchemaRepository.findBySchemaName(it)
        if(!schemaList && !schemaList.size()){throw new IllegalArgumentException('Schema references non-existent object: ' + it)} //controller advice catches it and returns 400
        Map js = jsonSlurper.parseText( schemaList.first().json_schema )
        js.remove('id') //todo determine if id will be there when we load these schemas
        [(it): js]
      }
    }

    schemaJson.definitions = schemaJson?.definitions ? schemaJson.definitions + definitions : definitions
    fetchDefinitions(schemaJson)
  }

  List findRefs(Map map){
    map.collectMany{ it ->
      if (it.key == '$ref' && it.value != '#' ) {
        return [it.value]
      }
      else if (it.value instanceof Map){
        return findRefs(it.value)
      }else{return []}
    }
  }

  //destructively corrects refs (e.g. Link -> #/definitions/Link)
  void updateRefs(Map map){
    map.each{it ->
      if (it.key == '$ref') {
        String s = it.value
        it.value = '#/definitions/'+it.value
      }
      else if (it.value instanceof Map){
        updateRefs(it.value)
      }
    }
  }
}
