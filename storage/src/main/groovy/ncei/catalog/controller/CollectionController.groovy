package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.CollectionMetadata
import ncei.catalog.domain.CollectionMetadataRepository
import ncei.catalog.service.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = '/collections')
class CollectionController {

  @Autowired
  RepoService repoService

  @Autowired
  CollectionMetadataRepository collectionMetadataRepository

  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  Map saveCollectionMetadata(@RequestBody CollectionMetadata collectionMetadata, HttpServletResponse response) {
    //need try/catch
    repoService.save(collectionMetadataRepository, collectionMetadata)
  }

  @RequestMapping(value = "/{collectionId}", method = RequestMethod.PUT)
  @ResponseBody
  Map updateCollectionMetadata(@RequestBody Map collectionMetadata, HttpServletResponse response) {
    //need try/catch
    if (collectionMetadata?.collection_id && collectionMetadata?.last_update) {
      collectionMetadata.last_update = new Date(collectionMetadata.last_update as Long)
      collectionMetadata.collection_id = UUID.fromString(collectionMetadata.collection_id)
      repoService.update(collectionMetadataRepository, new CollectionMetadata(collectionMetadata))
    } else {
      return ['message': 'To update a record you must provide a collection_id and the last_update field from the previous version']
    }
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map listCollectionMetadata(@RequestParam Map params, HttpServletResponse response) {
    try {
      List results = repoService.list(collectionMetadataRepository, params)
      [
              collections : results,
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

  @RequestMapping(value = "/{collectionId}", method = RequestMethod.GET)
  @ResponseBody
  Map listCollectionMetadataById(@PathVariable collectionId, @RequestParam Map params, HttpServletResponse response) {
    try {
      UUID collection_id = UUID.fromString(collectionId)
      params.collection_id = collection_id
      List results = repoService.list(collectionMetadataRepository, params)
      response.status = results ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND

      [
              collections : results,
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

  @RequestMapping(value = "/{collectionId}", method = RequestMethod.DELETE)
  @ResponseBody
  Map deleteEntry(@PathVariable collectionId, @RequestBody Map collectionMetadata, HttpServletResponse response) {
    try {
      log.info("delete collection metadata: $collectionMetadata")
      Map content = [:]
      collectionMetadata.collection_id = collectionId
      if (collectionMetadata?.last_update) {
        UUID collection_id = UUID.fromString(collectionMetadata.collection_id)
        Date timestamp = new Date(collectionMetadata.last_update as Long)
        content = repoService.softDelete(collectionMetadataRepository, collection_id, timestamp) ?: [:]
        response.status = response.SC_OK
        content
      } else {
        content.message = 'Please include the last_update field'
        content
      }
    } catch (e) {
      def msg = collectionMetadata.collection_id ?
              'failed to delete records for ' + collectionMetadata.collection_id + ' from the metadata catalog' :
              'please specify a collection_id'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }
  }

  @RequestMapping(value = '/purge', method = RequestMethod.DELETE)
  @ResponseBody
  Map purge(@RequestBody Map params, HttpServletResponse response) {

    try {
      Map content = repoService.purge(collectionMetadataRepository, params)

      log.info("count deleted:${content.totalResultsDeleted} content.searchTerms:${content.searchTerms}" +
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
