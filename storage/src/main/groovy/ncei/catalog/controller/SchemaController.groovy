package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.MetadataSchema
import ncei.catalog.domain.MetadataSchemaRepository
import ncei.catalog.service.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = '/schemas')
class SchemaController {

  @Autowired
  RepoService repoService

  @Autowired
  MetadataSchemaRepository metadataSchemaRepository

  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  Map saveMetadataSchema(@RequestBody MetadataSchema metadataSchema, HttpServletResponse response) {
    repoService.save(metadataSchemaRepository, metadataSchema)
  }

  @RequestMapping(value = '/{schemaId}', method = RequestMethod.PUT)
  @ResponseBody
  Map updateMetadataSchema(@PathVariable schemaId, @RequestBody Map metadataSchema, HttpServletResponse response) {
    metadataSchema.schema_id = schemaId
    if (metadataSchema?.last_update) {
      metadataSchema.last_update = new Date(metadataSchema.last_update as Long)
      metadataSchema.schema_id = UUID.fromString(metadataSchema.schema_id)
      repoService.update(metadataSchemaRepository, new MetadataSchema(metadataSchema))
    } else {
      return ['message': 'To update a record you must provide a schema_id and the last_update field from the previous version']
    }
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map listMetadataSchema(@RequestParam Map params, HttpServletResponse response) {
    try {
      List results = repoService.list(metadataSchemaRepository, params)
      [
              schemas     : results,
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

  @RequestMapping(value = "/{schemaId}", method = RequestMethod.GET)
  @ResponseBody
  Map listMetadataSchemaById(@PathVariable schemaId, @RequestParam Map params, HttpServletResponse response) {
    try {
      params.schema_id = schemaId
      List results = repoService.list(metadataSchemaRepository, params)
      response.status = results ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND

      [
              schemas     : results,
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

  @RequestMapping(value = "/{schemaId}", method = RequestMethod.DELETE)
  @ResponseBody
  Map deleteEntry(@PathVariable schemaId, @RequestBody MetadataSchema metadataSchema, HttpServletResponse response) {
    try {
      UUID schema_id = UUID.fromString(schemaId)
      Date timestamp = metadataSchema.last_update
      def content = repoService.softDelete(metadataSchemaRepository, schema_id, timestamp) ?: [:]

      response.status = response.SC_OK
      String msg = 'Successfully deleted row with schema_id: ' + schema_id
      content.message = msg
      content

    } catch (e) {
      def msg = metadataSchema.schema_id ?
              'failed to delete records for ' + metadataSchema.schema_id + ' from the metadata catalog' :
              'please specify a schema_id'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }
  }

  @RequestMapping(value = '/purge', method = RequestMethod.DELETE)
  @ResponseBody
  Map purge(@RequestBody Map params, HttpServletResponse response) {

    try {
      Map content = repoService.purge(metadataSchemaRepository, params)

      log.info("count deleted:${content.totalResultsDeleted} content.searchTerms:${content.searchTerms}" +
              " content.code:${content.code}")
      response.status = response.SC_OK
      String msg = 'Successfully purged ' + content.totalResultsDeleted + ' rows matching ' + content.searchTerms
      content.message = msg
      content

    } catch (e) {
      def msg = params.schema_id ?
              'failed to delete records for ' + params.schema_id + ' from the metadata catalog'
              : 'please specify purge criteria'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }

  }

}
