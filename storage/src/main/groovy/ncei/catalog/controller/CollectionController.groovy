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

  //new endpoint
  @RequestMapping(value = "/create", method = [RequestMethod.POST, RequestMethod.PUT])
  @ResponseBody
  Map saveCollectionMetadata(@RequestBody CollectionMetadata collectionMetadata, HttpServletResponse response) {
    collectionService.save(collectionMetadata)
  }

  @RequestMapping(value = "/delete", method=RequestMethod.DELETE)
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
  List<CollectionMetadata> listCollectionMetadata(@RequestParam Map params, HttpServletResponse response) {
    try {
      collectionService.list(params)
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
}
