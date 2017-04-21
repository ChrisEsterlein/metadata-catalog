package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.GranuleMetadataRepository
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.service.GranuleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse


@Slf4j
@RestController
@RequestMapping(value = ['/', '/granule'])
class GranuleController {

  @Autowired
  GranuleService granuleService

  @Value('${purgeEnabled:false}')
  Boolean purgeEnabled

  @Autowired
  GranuleMetadataRepository granuleMetadataRepository

  @RequestMapping(value = "/files",method = RequestMethod.GET)
  @ResponseBody
  List<GranuleMetadata> FileMetadata(HttpServletResponse response) {
    try {
      List<GranuleMetadata> MetadataList = new ArrayList<>()
      granuleMetadataRepository.findAll().each{MetadataList.add(it)}
      MetadataList
    }
    catch (e) {
      String exceptionMessage = e.hasProperty('undeclaredThrowable') ? e.undeclaredThrowable.message : e.message
      // Place the error message into the returned content
      def msg = 'Failing metadata catalog list request with: ' + exceptionMessage
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }
  }

//support new and old endpoint
  @RequestMapping(value = ["/files", "/granules"], method = [RequestMethod.POST, RequestMethod.PUT])
  @ResponseBody
  Map saveFileMetadata(@RequestBody GranuleMetadata granuleMetadata, HttpServletResponse response) {
    log.info("Received post with params: $granuleMetadata")
    granuleService.save(granuleMetadata)
  }

  @RequestMapping(value = "/files/search",method = RequestMethod.GET)
  @ResponseBody
  List<GranuleMetadata> ListByDataset(@RequestParam Map params) {
    String dataset = params?.dataset
    List<GranuleMetadata> FileMetadataList = new ArrayList<>()
    granuleMetadataRepository.findByDataset(dataset).each{FileMetadataList.add(it)}
    return FileMetadataList
  }

  @RequestMapping(method=RequestMethod.DELETE)
  @ResponseBody
  Map  purge(@RequestParam Map params, HttpServletResponse response) {
    if (purgeEnabled) {
      try {
        Map content = granuleService.purge(params)

        log.debug( "count deleted:${content.totalResultsDeleted} content.searchTerms:${content.searchTerms}" +
            " content.code:${content.code}")
        response.status = response.SC_OK
        String msg = 'Successfully purged rows with dataset: ' + params.dataset
        content.message = msg
        content

      } catch (e) {
        def msg = params?.dataset ?
            'failed to delete records for ' + params.dataset + ' from the metadata catalog' :
            'please specify a dataset'
        log.error(msg, e)
        response.status = response.SC_INTERNAL_SERVER_ERROR
        [message: msg]
      }
    } else {
      def msg = 'Purge access is denied'
      log.error(msg)
      response.status = response.SC_FORBIDDEN
      [message: msg]
    }

  }
}
