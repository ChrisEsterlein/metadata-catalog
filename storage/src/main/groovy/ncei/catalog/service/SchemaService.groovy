package ncei.catalog.service

import ncei.catalog.domain.MetadataSchema
import ncei.catalog.domain.MetadataSchemaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletResponse

@Component
class SchemaService {

  @Autowired
  MetadataSchemaRepository metadataSchemasRepository

  Map save(MetadataSchema granuleMetadata){
    Map saveDetails = [:]
    //get existing row if there is one
    Iterable<MetadataSchema> result = metadataSchemasRepository.findByMetadataId(granuleMetadata.schema_id)

    //if we have a result, we want to 'update' row by inserting the same schema_id with a new last_update time
    if(result){
      metadataSchemasRepository.save(granuleMetadata)
      saveDetails.totalResultsUpdated = 1
      saveDetails.code = HttpServletResponse.SC_OK

    }else{ //create a new one
      metadataSchemasRepository.save(granuleMetadata)
      saveDetails.code = HttpServletResponse.SC_CREATED
    }

    saveDetails
  }

  List<MetadataSchema> list(Map params){

    String schemaName = params?.schema_name
    String schema = params?.schema_schema
    UUID schema_id = params?.schema_id

    Iterable<MetadataSchema> allResults
    List<MetadataSchema> metadataList = []

    if(schema_id){
      metadataSchemasRepository.findByMetadataId(schema_id).each{metadataList.add(it)}
    }
    else if(schemaName && schema){
      allResults = metadataSchemasRepository.findBySchemaNameAndSchema(schemaName, schema)
      metadataList = getMostRecent(allResults)
    }
    else if(schemaName){
      allResults = metadataSchemasRepository.findBySchemaName(schemaName)
      metadataList = getMostRecent(allResults)
    }
    else if (schema){
      allResults = metadataSchemasRepository.findBySchema(schema)
      metadataList = getMostRecent(allResults)
    }
    else{
      allResults = metadataSchemasRepository.findAll()
      metadataList = getMostRecent(allResults)
    }

    metadataList
  }
  List<MetadataSchema> getMostRecent(Iterable<MetadataSchema> allResults){
    Map<String, MetadataSchema> metadataSchemasMap = [:]
    List<MetadataSchema> mostRecent
    allResults.each{ gm ->
      String metadataId = gm.schema_id as String
      if(metadataSchemasMap[metadataId]){
        if(metadataSchemasMap[metadataId].last_update < gm.last_update){
          metadataSchemasMap[metadataId] = gm
        }
      }else{
        metadataSchemasMap[metadataId] = gm
      }
    }
    mostRecent = metadataSchemasMap.collect{key, value ->
      value
    }
    mostRecent

  }

  def delete(UUID schema_id){
    Date timestamp = metadataSchemasRepository.findByMetadataId(schema_id as UUID).first().last_update as Date
    def result = metadataSchemasRepository.deleteByMetadataId(schema_id , timestamp)

  }
}
