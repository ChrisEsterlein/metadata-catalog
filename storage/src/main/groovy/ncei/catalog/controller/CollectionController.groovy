package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.CollectionMetadata
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.service.CollectionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = '/collections')
class CollectionController {

  @Autowired
  CollectionService collectionService

  @RequestMapping(value = "/create", method = RequestMethod.POST)
  @ResponseBody
  Map saveCollectionMetadata(@RequestBody CollectionMetadata  collectionMetadata, HttpServletResponse response) {
    //need try/catch
    collectionService.save(collectionMetadata)
  }

  @RequestMapping(value = "/update", method = RequestMethod.PUT)
  @ResponseBody
  Map updateCollectionMetadata(@RequestBody CollectionMetadata  collectionMetadata, HttpServletResponse response) {
    //need try/catch
    collectionService.save(collectionMetadata)
  }

  @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
  @ResponseBody
  Map deleteEntry(@RequestBody CollectionMetadata collectionMetadata, HttpServletResponse response ){
    try {
      UUID collection_id = collectionMetadata.collection_id
      def content = collectionService.delete(collection_id) ?: [:]

      response.status = response.SC_OK
      String msg = 'Successfully deleted row with collection_id: ' + collection_id
      content.message = msg
      content

    } catch (e) {
      def msg = collectionMetadata.collection_id ?
              'failed to delete records for ' + collectionMetadata.collection_id + ' from the metadata catalog' :
              'please specify a collection_id'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }
  }

  //new end point
  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map listCollectionMetadata(@RequestParam Map params, HttpServletResponse response) {
    try {
      List results = collectionService.list(params)
      [
        collections : results,
        searchTerms : params,
        totalResults : results.size()
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
  
  @RequestMapping(value = "/purge", method=RequestMethod.DELETE)
  @ResponseBody
  Map  purge(@RequestBody Map params, HttpServletResponse response) {

    try {
      Map content = collectionService.purge(params)

      log.info( "count deleted:${content.totalResultsDeleted} content.searchTerms:${content.searchTerms}" +
              " content.code:${content.code}")
      response.status = response.SC_OK
      String msg = 'Successfully purged ' + content.totalResultsDeleted + ' rows matching ' + content.searchTerms
      content.message = msg
      content

    } catch (e) {
      def msg = params.collection_id ?
              'failed to delete records for ' + params.collection_id + ' from the metadata catalog'
              : 'please specify purge criteria'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }

  }
  
}
