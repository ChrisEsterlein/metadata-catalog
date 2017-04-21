package ncei.catalog.service

import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletResponse

@Component
class GranuleService {

  @Autowired
  GranuleMetadataRepository granuleMetadataRepository

  Map save(GranuleMetadata granuleMetadata){
    Map saveDetails = [:]

    if(granuleMetadataRepository.findTimestampByMetadataId(granuleMetadata.metadata_id)){
      //when the time comes to implement versioning, one way to do this is to
      Date timestamp = granuleMetadataRepository.findTimestampByMetadataId(granuleMetadata.metadata_id).first().last_update as Date
      granuleMetadata.last_update = timestamp

      granuleMetadataRepository.save(granuleMetadata)
      saveDetails.totalResultsUpdated = 1
      saveDetails.code = HttpServletResponse.SC_OK
    }else{
      granuleMetadataRepository.save(granuleMetadata)
      saveDetails.code = HttpServletResponse.SC_CREATED
    }

    saveDetails
  }

  //This might need optimization.
  //You cant delete anything without the primary key, so we first have to find rows with matching dataset
  Map purge(Map params){
    Map purgeDetails = [:]
    purgeDetails.searchTerms = params
    String dataset
    int count = 0

    if(!params.dataset) {
      purgeDetails.message = 'A [dataset] parameter is required to purge'
      purgeDetails.totalResultsDeleted = count
      purgeDetails.code = HttpServletResponse.SC_BAD_REQUEST
      return purgeDetails
    }else{
      dataset = params.dataset
    }

    List items = granuleMetadataRepository.findByDataset(dataset)
    items.each{
      granuleMetadataRepository.delete(it.tracking_id)
      count++
    }

    purgeDetails.totalResultsDeleted = count
    purgeDetails.code = HttpServletResponse.SC_OK
    purgeDetails
  }
}
