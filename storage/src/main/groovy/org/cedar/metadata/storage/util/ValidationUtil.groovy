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


  boolean validate(MetadataRecord metadataRecord){
    //allow records with no schema and metadataSchemas to pass validation
    if(metadataRecord.recordTable() == 'schema' || !metadataRecord?.metadata_schema){
      return true
    }
    log.info "Attempting to validate record $metadataRecord.id against schema $metadataRecord.metadata_schema"
    //todo- determine if metadata_schema is going to store an id or a schema name?
    UUID schemaId = metadataRecord.metadata_schema instanceof  UUID ? metadataRecord.metadata_schema : UUID.fromString(metadataRecord.metadata_schema)

    Iterable<MetadataRecord> results = metadataSchemaRepository.findByMetadataId(schemaId)
    MetadataSchema metadataSchema = results.first()

    JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault()
    ObjectMapper objectMapper = new ObjectMapper()
    JsonSlurper jsonSlurper = new JsonSlurper()
    Map metadataJson = jsonSlurper.parseText(metadataRecord.metadata as String)
    Map schemaJson = jsonSlurper.parseText(metadataSchema.json_schema)

    schemaJson = fetchDefinitions(schemaJson)
    updateRefs(schemaJson)

    log.debug "Validating schema: \n ${schemaJson}"
    JsonNode metadataNode = objectMapper.valueToTree(metadataJson)
    JsonNode metadataSchemaNode = objectMapper.valueToTree(schemaJson)
    JsonSchema schema = schemaFactory.getJsonSchema(metadataSchemaNode)

    return schema.validate(metadataNode).isSuccess()
  }

  Map fetchDefinitions(Map schemaJson){
    Set refs = findRefs(schemaJson) as Set
    Set defs = schemaJson?.definitions ? schemaJson.definitions.keySet() : []

    if(!refs || defs.sort() == refs.sort()){return schemaJson}

    Map definitions = schemaJson?.definitions ?: [:]

    (refs - defs).collectEntries(definitions){
      if(!(it in definitions)){
        Iterable<MetadataRecord> subSchema = metadataSchemaRepository.findBySchemaName(it)
        if(!subSchema){throw Exception("Schema references unrecognized object $it")} //then have controller advice catch it?
//        if(!subSchema){return [(it):[:]]}  //this doesnt work
        Map js = new JsonSlurper().parseText( subSchema.first().json_schema )
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
