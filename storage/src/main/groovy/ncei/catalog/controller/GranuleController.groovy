package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import ncei.catalog.service.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = ['/', '/granules'], produces = 'application/json')
class GranuleController {

  @Autowired
  RepoService repoService

  @Autowired
  GranuleMetadataRepository granuleMetadataRepository

  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  Map saveGranuleMetadata(@RequestBody GranuleMetadata granuleMetadata, HttpServletResponse response) {
    repoService.save(granuleMetadataRepository, granuleMetadata)
  }

  @RequestMapping(value = "/{granuleId}", method = RequestMethod.PUT)
  @ResponseBody
  //we dont want to cast to a GranuleMetadata object here because granule_id and last_update will be instantiated by default
  Map updateGranuleMetadata(@PathVariable granuleId, @RequestBody Map granuleMetadata, HttpServletResponse response) {
    granuleMetadata?.granule_id = granuleId
    if (granuleMetadata?.last_update) {
      granuleMetadata.last_update = new Date(granuleMetadata.last_update as Long)
      granuleMetadata.granule_id = UUID.fromString(granuleMetadata.granule_id)
      repoService.update(granuleMetadataRepository, new GranuleMetadata(granuleMetadata))
    } else {
      return ['message': 'To update a record you must provide a granule_id and the last_update field from the previous version']
    }
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map listGranuleMetadata(@RequestParam Map params, HttpServletResponse response) {
    try {
      List results = repoService.list(granuleMetadataRepository, params)
      [
              granules    : results,
              searchTerms : params,
              totalResults: results.size()
      ]
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

  @RequestMapping(value = "/{granuleId}", method = RequestMethod.GET)
  @ResponseBody
  Map listGranuleMetadataById(@PathVariable granuleId, @RequestParam Map params, HttpServletResponse response) {
    try {
      params.granule_id = granuleId
      List results = repoService.list(granuleMetadataRepository, params)
      response.status = results ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND

      [
              granules    : results,
              searchTerms : params,
              totalResults: results.size(),
              code: results ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND
      ]
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

  @RequestMapping(value = '/{granuleId}', method = RequestMethod.DELETE)
  @ResponseBody
  Map deleteEntry(@PathVariable granuleId, @RequestBody Map granuleMetadata, HttpServletResponse response) {
    try {
      log.info("delete granule metadata: $granuleMetadata")
      Map content = [:]
      granuleMetadata.granule_id = granuleId
      if (granuleMetadata?.last_update) {
        UUID granule_id = UUID.fromString(granuleMetadata.granule_id)
        Date timestamp = new Date(granuleMetadata.last_update as Long)
        content = repoService.softDelete(granuleMetadataRepository, granule_id, timestamp) ?: [:]
        response.status = response.SC_OK
        content
      } else {
        content.message = 'Please include the last_update field'
        content
      }
    } catch (e) {
      def msg = granuleMetadata.granule_id ?
              'failed to delete records for ' + granuleMetadata.granule_id + ' from the metadata catalog' :
              'please specify a granule_id'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }

  }

  @RequestMapping(value = '/purge', method = RequestMethod.DELETE)
  @ResponseBody
  Map purge(@RequestBody Map params, HttpServletResponse response) {
    try {
      Map content = repoService.purge(granuleMetadataRepository, params)

      log.info("count deleted:${content.totalResultsDeleted} content.searchTerms:${content.searchTerms}" +
              " content.code:${content.code}")
      response.status = response.SC_OK
      String msg = 'Successfully purged ' + content.totalResultsDeleted + ' rows matching ' + content.searchTerms
      content.message = msg
      content

    } catch (e) {
      def msg = params?.dataset ?
              'failed to delete records for ' + params.dataset + ' from the metadata catalog'
              : (params.granule_id ?
              'failed to delete records for ' + params.granule_id + ' from the metadata catalog'
              : 'please specify purge criteria')
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }
  }

  //
  ////support old endpoint
  //
//  @RequestMapping(value = "/files", method = RequestMethod.GET)
//  @ResponseBody
//  //  returns List<GranuleMetadata> or a [:]
//  Map listFileMetadata(@RequestParam Map params, HttpServletResponse response) {
//    try {
//      List<GranuleMetadata> granuleMetadataList = repoService.list(params)
//      List<FileMetadata> fileMetadataList = []
//      granuleMetadataList.each{
//        fileMetadataList.add(ClassConversionUtil.convertToFileMetadata(it))
//      }
//
//      [
//              items: fileMetadataList,
//              totalResults : fileMetadataList.size(),
//              searchTerms: params
//      ]
//
//    }
//    catch (e) {
//      String exceptionMessage = e.hasProperty('undeclaredThrowable') ? e.undeclaredThrowable.message : e.message
//      // Place the error message into the returned content
//      def msg = 'Failing metadata catalog list request with: ' + exceptionMessage
//      log.error(msg, e)
//      response.status = response.SC_INTERNAL_SERVER_ERROR
//      [message: msg]
//    }
//  }
//
////support old endpoint
//  @RequestMapping(value="/files", method = [RequestMethod.POST, RequestMethod.PUT])
//  @ResponseBody
//  Map saveFileMetadata(@RequestBody FileMetadata fileMetadata, HttpServletResponse response) {
//    log.info("Received post with params: ${fileMetadata.asMap()}")
//    GranuleMetadata granuleMetadata = ClassConversionUtil.convertToGranuleMetadata(fileMetadata)
//    Map results = repoService.save(granuleMetadata, true)
//    //convert to support old interface
//    [
//            //recordsCreated -> totalResultsUpdated because that is what old catalog-metadata did
//            totalResultsUpdated: results?.recordsCreated ?: (results.totalResultsUpdated ?: 0),
//            code : results.code
//            //flatten the map because that is what old catalog-metadata did
//    ] + (ClassConversionUtil.convertToFileMetadata(results.granule as GranuleMetadata)).asMap()
//  }
}
