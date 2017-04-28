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

      //if we have a result, we want to let the user know it 'updated'
      if(result){
        if(result.first().last_update != metadataSchema.last_update){
          saveDetails.message = 'You are not editing the most recent version.'
          saveDetails.code = HttpServletResponse.SC_CONFLICT
          return saveDetails
        }else{
          metadataSchema.last_update = new Date()
          saveDetails.totalResultsUpdated = 1
          saveDetails.code = HttpServletResponse.SC_OK
        }
      }else{ //create a new one
        saveDetails.recordsCreated = 1
        saveDetails.code = HttpServletResponse.SC_CREATED
      }

    saveDetails.newRecord = metadataSchemaRepository.save(metadataSchema)

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

  //This might need optimization.
  Map purge(Map params){
    Map purgeDetails = [:]
    purgeDetails.searchTerms = params
    int count = 0
    Iterable<MetadataSchema> items

    if(params.schema_id) {
      items = metadataSchemaRepository.findByMetadataId(UUID.fromString(params.schema_id))
      println "schemas found for purging: $items"
    }
    else{
      purgeDetails.message = 'A [schema_id] parameter is required to purge'
      purgeDetails.totalResultsDeleted = count
      purgeDetails.code = HttpServletResponse.SC_BAD_REQUEST
      return purgeDetails
    }

    items.each{
      delete(it.schema_id, it.last_update)
      count++
    }

    purgeDetails.totalResultsDeleted = count
    purgeDetails.code = HttpServletResponse.SC_OK
    purgeDetails
  }

  //if a timestamp is not defined, we want to delete the most recent one
  //if a timestamp is defined, it means this method is being used to purge all rows with a common id
  def delete(UUID schema_id, Date timestamp = null){
    if(timestamp){
      metadataSchemaRepository.deleteByMetadataIdAndLastUpdate(schema_id , timestamp)
    }else{
      Iterable<MetadataSchema> gm = metadataSchemaRepository.findByMetadataId(schema_id as UUID)
      if(gm){
        timestamp = gm.first().last_update as Date
      }else{
        throw RuntimeException("No such schema_id")
      }
      metadataSchemaRepository.deleteByMetadataIdAndLastUpdate(schema_id , timestamp)
    }
  }
}
