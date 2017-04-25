package ncei.catalog.service

import ncei.catalog.domain.MetadataSchema
import ncei.catalog.domain.MetadataSchemaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletResponse

@Component
class SchemaService {

  @Autowired
  MetadataSchemaRepository metadataSchemaRepository

  Map save(MetadataSchema metadataSchema){
    Map saveDetails = [:]

      //get existing row if there is one
      Iterable<MetadataSchema> result = metadataSchemaRepository.findByMetadataId(metadataSchema.schema_id)

      saveDetails.newRecord = metadataSchemaRepository.save(metadataSchema)

      //if we have a result, we want to let the user know it 'updated'
      if(result){
        saveDetails.totalResultsUpdated = 1
        saveDetails.code = HttpServletResponse.SC_OK

      }else{ //create a new one
        saveDetails.recordsCreated = 1
        saveDetails.code = HttpServletResponse.SC_CREATED
      }

    saveDetails
  }

  List<MetadataSchema> list(Map params){

    Boolean versions = params?.versions
    String schemaName = params?.schema_name
    List<String> schema_ids = params?.schema_ids?.tokenize(',')
    Iterable<MetadataSchema> allResults
    List<MetadataSchema> metadataList = []

    if (schema_ids) {
      schema_ids.each { id ->
        allResults = allResults ?
                allResults + metadataSchemaRepository.findByMetadataId(UUID.fromString(id))
                : metadataSchemaRepository.findByMetadataId(UUID.fromString(id))
      }
    }
    else if(schemaName){
      allResults = metadataSchemaRepository.findBySchemaName(schemaName)
    }
    else{
      allResults = metadataSchemaRepository.findAll()
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
    Date timestamp = metadataSchemaRepository.findByMetadataId(schema_id as UUID).first().last_update as Date
    def result = metadataSchemaRepository.deleteByMetadataId(schema_id , timestamp)

  }
}
