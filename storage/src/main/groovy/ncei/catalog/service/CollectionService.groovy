package ncei.catalog.service

import ncei.catalog.domain.CollectionMetadata
import ncei.catalog.domain.CollectionMetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletResponse

@Component
class CollectionService {

  @Autowired
  CollectionMetadataRepository collectionMetadataRepository

  Map save(CollectionMetadata collectionMetadata){
    Map saveDetails = [:]

      //get existing row if there is one
      Iterable<CollectionMetadata> result = collectionMetadataRepository.findByMetadataId(collectionMetadata.collection_id)

      //if we have a result, we want to let the user know it 'updated'
      if(result){
        if(result.first().last_update != collectionMetadata.last_update){
          saveDetails.message = 'You are not editing the most recent version.'
          saveDetails.code = HttpServletResponse.SC_CONFLICT
          return saveDetails
        }else{
          collectionMetadata.last_update = new Date()
          saveDetails.totalResultsUpdated = 1
          saveDetails.code = HttpServletResponse.SC_OK
        }
      }else{ //create a new one
        saveDetails.recordsCreated = 1
        saveDetails.code = HttpServletResponse.SC_CREATED
      }

    //save the row
    saveDetails.newRecord = collectionMetadataRepository.save(collectionMetadata)

    saveDetails
  }
  
  List<CollectionMetadata> list(Map params){

    Boolean versions = params?.versions
    String collectionName = params?.collection_name
    String schema = params?.collection_schema
    List<String> collection_ids = params?.collection_ids?.tokenize(',')
    Iterable<CollectionMetadata> allResults
    List<CollectionMetadata> metadataList = []

    if (collection_ids) {
      collection_ids.each { id ->
        allResults = allResults ?
                allResults + collectionMetadataRepository.findByMetadataId(UUID.fromString(id))
                : collectionMetadataRepository.findByMetadataId(UUID.fromString(id))
      }
    }
    else if(collectionName && schema){
      allResults = collectionMetadataRepository.findByCollectionNameAndSchema(collectionName, schema)
    }
    else if(collectionName){
      allResults = collectionMetadataRepository.findByCollectionName(collectionName)
    }
    else if (schema){
      allResults = collectionMetadataRepository.findBySchema(schema)
    }
    else{
      allResults = collectionMetadataRepository.findAll()
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

  List<CollectionMetadata> getMostRecent(Iterable<CollectionMetadata> allResults){
    Map<String, CollectionMetadata> collectionMetadataMap = [:]
    List<CollectionMetadata> mostRecent
    allResults.each{ gm ->
      String metadataId = gm.collection_id as String
      if(collectionMetadataMap[metadataId]){
        if(collectionMetadataMap[metadataId].last_update < gm.last_update){
          collectionMetadataMap[metadataId] = gm
        }
      }else{
        collectionMetadataMap[metadataId] = gm
      }
    }
    mostRecent = collectionMetadataMap.collect{key, value ->
      value
    }
    mostRecent

  }

  //This might need optimization.
  Map purge(Map params){
    Map purgeDetails = [:]
    purgeDetails.searchTerms = params
    int count = 0
    Iterable<CollectionMetadata> items

    if(params.collection_id) {
      items = collectionMetadataRepository.findByMetadataId(UUID.fromString(params.collection_id))
    }
    else{
      purgeDetails.message = 'A [collection_id] parameter is required to purge'
      purgeDetails.totalResultsDeleted = count
      purgeDetails.code = HttpServletResponse.SC_BAD_REQUEST
      return purgeDetails
    }

    items.each{
      delete(it.collection_id, it.last_update)
      count++
    }

    purgeDetails.totalResultsDeleted = count
    purgeDetails.code = HttpServletResponse.SC_OK
    purgeDetails
  }

  //if a timestamp is not defined, we want to delete the most recent one
  //if a timestamp is defined, it means this method is being used to purge all rows with a common id
  def delete(UUID collection_id, Date timestamp = null){
    if(timestamp){
      collectionMetadataRepository.deleteByMetadataIdAndLastUpdate(collection_id , timestamp)
    }else{
      Iterable<CollectionMetadata> gm = collectionMetadataRepository.findByMetadataId(collection_id as UUID)
      if(gm){
        //get the most recent one
        timestamp = gm.first().last_update as Date
      }else{
        throw RuntimeException("No such collection_id")
      }
      collectionMetadataRepository.deleteByMetadataIdAndLastUpdate(collection_id , timestamp)
    }
  }
  
}
