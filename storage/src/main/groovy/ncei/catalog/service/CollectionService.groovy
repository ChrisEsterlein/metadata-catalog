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

  Map save(List<CollectionMetadata> collectionMetadataList){
    Map saveDetails = [recordsCreated:0, results : []]

    collectionMetadataList.each{ collectionMetadata ->

      //get existing row if there is one
      Iterable<CollectionMetadata> result = collectionMetadataRepository.findByMetadataId(collectionMetadata.collection_id)

      //save the row
      saveDetails.results.add(collectionMetadataRepository.save(collectionMetadata))

      //if we have a result, we want to let the user know it 'updated'
      if(result){
        saveDetails.totalResultsUpdated = saveDetails.totalResultsUpdated ?
                saveDetails.totalResultsUpdated + 1
                : 1

      }else{ //create a new one
        saveDetails.recordsCreated = saveDetails.recordsCreated ?
                saveDetails.recordsCreated + 1
                : 1
        saveDetails.code = HttpServletResponse.SC_CREATED
      }

    }

    saveDetails
  }
  
  List<CollectionMetadata> list(Map params){

    Boolean versions = params?.versions
    String collectionName = params?.collection_name
    String schema = params?.collection_schema
    List<String> collection_ids = params?.collection_ids?.tokenize(',')
    List<UUID> collection_uuids = []

    if (collection_ids) {
      collection_ids.each { id ->
        collection_uuids.add(UUID.fromString(id))
      }
    }

    Iterable<CollectionMetadata> allResults
    List<CollectionMetadata> metadataList = []

    if(collection_uuids){
      collection_uuids.each{ id ->
        allResults = allResults ?
                allResults + collectionMetadataRepository.findByMetadataId(id)
                : collectionMetadataRepository.findByMetadataId(id)
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

  def delete(UUID collection_id){
    Date timestamp = collectionMetadataRepository.findByMetadataId(collection_id as UUID).first().last_update as Date
    def result = collectionMetadataRepository.deleteByMetadataId(collection_id , timestamp)

  }
  
}
