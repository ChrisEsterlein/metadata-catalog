package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.FileMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.service.GranuleService
import ncei.catalog.utils.ClassConversionUtil
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
@RequestMapping(value = '/')
class GranuleController {

  @Autowired
  GranuleService granuleService

  @Value('${purgeEnabled:false}')
  Boolean purgeEnabled

  @Autowired
  GranuleMetadataRepository granuleMetadataRepository

  //support old endpoint
  @RequestMapping(value = "/files", method = RequestMethod.GET)
  @ResponseBody
  List<FileMetadata> listFileMetadata(@RequestParam Map params, HttpServletResponse response) {
    try {
      List<GranuleMetadata> granuleMetadataList = granuleService.list(params)
      List<FileMetadata> fileMetadataList = []
      granuleMetadataList.each{
        fileMetadataList.add(ClassConversionUtil.convertToFileMetadata(it))
      }
      fileMetadataList
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

  //new end point
  @RequestMapping(value = "/granules", method = RequestMethod.GET)
  @ResponseBody
  List<GranuleMetadata> listGranuleMetadata(@RequestParam Map params, HttpServletResponse response) {
    try {
      granuleService.list(params)
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

//support old endpoint
  @RequestMapping(value="/files", method = [RequestMethod.POST, RequestMethod.PUT])
  @ResponseBody
  Map saveFileMetadata(@RequestBody FileMetadata fileMetadata, HttpServletResponse response) {
    log.info("Received post with params: $fileMetadata")
    GranuleMetadata granuleMetadata = ClassConversionUtil.convertToGranuleMetadata(fileMetadata)
    granuleService.save(granuleMetadata)
  }

//new endpoint
  @RequestMapping(value = "/granules", method = [RequestMethod.POST, RequestMethod.PUT])
  @ResponseBody
  Map saveGranuleMetadata(@RequestBody GranuleMetadata granuleMetadata, HttpServletResponse response) {
    granuleService.save(granuleMetadata)
  }

  @RequestMapping(value = "/delete", method=RequestMethod.DELETE)
  @ResponseBody
  Map deleteEntry(@RequestBody GranuleMetadata granuleMetadata, HttpServletResponse response ){
    try {
      UUID granule_id = granuleMetadata.granule_id
      def content = granuleService.delete(granule_id) ?: [:]

      response.status = response.SC_OK
      String msg = 'Successfully deleted row with granule_id: ' + granule_id
      content.message = msg
      content

    } catch (e) {
      def msg = granuleMetadata.granule_id ?
              'failed to delete records for ' + granuleMetadata.granule_id + ' from the metadata catalog' :
              'please specify a granule_id'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }

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
