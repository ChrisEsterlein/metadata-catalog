package ncei.catalog.service

import groovy.util.logging.Slf4j
import ncei.catalog.domain.CollectionMetadata
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletResponse

@Slf4j
@Component
class GranuleService {

  @Autowired
  GranuleMetadataRepository granuleMetadataRepository

  Map save(GranuleMetadata granuleMetadata, Boolean isLegacyEndpoint = false){
    Map saveDetails = [:]

    //get existing row if there is one
    Iterable<GranuleMetadata> result = granuleMetadataRepository.findByMetadataId(granuleMetadata.granule_id)

    //build response, set update time
    //if we have a result, we want to let the user know it 'updated'
    if(result){
        // only allow updating on the most recent version
        // except for metadata-recorder because it doesn't post last_update
      if(result.first().last_update != granuleMetadata.last_update && !isLegacyEndpoint){
        saveDetails.message = 'You are not editing the most recent version.'
        saveDetails.code = HttpServletResponse.SC_CONFLICT
        return saveDetails
      }else{
        granuleMetadata.last_update = new Date()
        saveDetails.totalResultsUpdated = 1
        saveDetails.code = HttpServletResponse.SC_OK
      }
    }else{ //create a new one
      granuleMetadata.last_update = new Date()
      saveDetails.recordsCreated =  1
      saveDetails.code = HttpServletResponse.SC_CREATED
    }

    //save the record
    saveDetails.newRecord = granuleMetadataRepository.save(granuleMetadata)

    saveDetails
  }

  List<GranuleMetadata> list(Map params){
    Boolean versions = params?.versions
    String dataset = params?.dataset
    String schema = params?.granule_schema
    Date startTime = params?.start_time ? new Date(params.start_time as long) : null
    List<String> granule_ids = params?.granule_ids?.tokenize(',') ?: []

    //the iterable returned from the repository class
    Iterable<GranuleMetadata> allResults
    //the list we will populate with rows matching criteria
    List<GranuleMetadata> metadataList = []


    if (granule_ids) {
      granule_ids.each { id ->
        allResults = allResults ?
                allResults + granuleMetadataRepository.findByMetadataId(UUID.fromString(id))
                : granuleMetadataRepository.findByMetadataId(UUID.fromString(id))
      }
    }
    else if(dataset){
      allResults = granuleMetadataRepository.findByDataset(dataset)
    }
    else if (schema){
      allResults = granuleMetadataRepository.findBySchema(schema)
    }
    else{
      allResults = granuleMetadataRepository.findAll()
    }

    //get most recent or show all versions
    if(versions){
      allResults.each{ gm ->
        metadataList.add(gm)
      }
    }else{
      metadataList = getMostRecent(allResults, startTime)
    }
    metadataList
  }

  List<GranuleMetadata> getMostRecent(Iterable<GranuleMetadata> allResults, Date startTime = new Date(0 as long)){
    Map<String, GranuleMetadata> granuleMetadataMap = [:]
    List<GranuleMetadata> mostRecent
    allResults.each{ gm ->
      if(gm.last_update > startTime){
        String metadataId = gm.granule_id as String
        if(granuleMetadataMap[metadataId]){
          if(granuleMetadataMap[metadataId].last_update < gm.last_update){
            granuleMetadataMap[metadataId] = gm
          }
        }else{
          granuleMetadataMap[metadataId] = gm
        }
      }
    }
    mostRecent = granuleMetadataMap.collect{key, value ->
      value
    }
    mostRecent
  }

  //This might need optimization.
  Map purge(Map params){
    Map purgeDetails = [:]
    purgeDetails.searchTerms = params
    int count = 0
    Iterable<GranuleMetadata> items

    if(params.granule_id) {
      items = granuleMetadataRepository.findByMetadataId(UUID.fromString(params.granule_id))
    }
    else{
      purgeDetails.message = 'A [dataset] or [granule_id] parameter is required to purge'
      purgeDetails.totalResultsDeleted = count
      purgeDetails.code = HttpServletResponse.SC_BAD_REQUEST
      return purgeDetails
    }

    items.each{
      delete(it.granule_id, it.last_update)
      count++
    }

    purgeDetails.totalResultsDeleted = count
    purgeDetails.code = HttpServletResponse.SC_OK
    purgeDetails
  }

  //if a timestamp is not defined, we want to delete the most recent one
  //if a timestamp is defined, it means this method is being used to purge all rows with a common id
  def delete(UUID granule_id, Date timestamp = null){
    if(timestamp){
      granuleMetadataRepository.deleteByMetadataIdAndLastUpdate(granule_id , timestamp)
    }else{
      Iterable<CollectionMetadata> gm = granuleMetadataRepository.findByMetadataId(granule_id as UUID)
      if(gm){
        timestamp = gm.first().last_update as Date
      }else{
        throw RuntimeException("No such granule_id")
      }
      granuleMetadataRepository.deleteByMetadataIdAndLastUpdate(granule_id , timestamp)
    }
  }

}
