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

    Boolean versions = params?.versions
    String schemaName = params?.schema_name
    UUID schema_id = params?.schema_id ? UUID.fromString(params?.schema_id) : null


    Iterable<MetadataSchema> allResults
    List<MetadataSchema> metadataList = []

    if(schema_id){
      allResults =  metadataSchemasRepository.findByMetadataId(schema_id)
    }
    else if(schemaName && schemaName){
      allResults = metadataSchemasRepository.findBySchemaNameAndSchema(schemaName, schemaName)
    }
    else if(schemaName){
      allResults = metadataSchemasRepository.findBySchemaName(schemaName)
    }
    else if (schemaName){
      allResults = metadataSchemasRepository.findBySchema(schemaName)
    }
    else{
      allResults = metadataSchemasRepository.findAll()
    }

    //get most recent or show all versions
    if(versions){
      allResults.each{ gm ->
        metadataList.add(gm)
      }
    }else{
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
