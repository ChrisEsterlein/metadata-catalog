package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.MetadataSchema
import ncei.catalog.service.SchemaService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = '/schemas')
class SchemaController {
  
    @Autowired
    SchemaService schemaService


    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    Map saveMetadataSchema(@RequestBody MetadataSchema metadataSchema, HttpServletResponse response) {
      schemaService.save(metadataSchema)
    }

    @RequestMapping(value= '/{schemaId}', method = RequestMethod.PUT)
    @ResponseBody
    Map updateMetadataSchema(@PathVariable schemaId, @RequestBody Map metadataSchema, HttpServletResponse response) {
        metadataSchema.schema_id = schemaId
        if(metadataSchema?.last_update){
            metadataSchema.last_update = new Date(metadataSchema.last_update as Long)
            metadataSchema.schema_id = UUID.fromString(metadataSchema.schema_id)
            schemaService.save(new MetadataSchema(metadataSchema))
        }else{
            return ['message': 'To update a record you must provide a schema_id and the last_update field from the previous version']
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    Map listMetadataSchema(@RequestParam Map params, HttpServletResponse response) {
      try {
        List results = schemaService.list(params)
        [
          schemas : results,
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

    @RequestMapping(value= "/{schemaId}", method = RequestMethod.GET)
    @ResponseBody
    Map listMetadataSchemaById(@PathVariable schemaId, @RequestParam Map params, HttpServletResponse response) {
        try {
            params.schema_ids = schemaId
            List results = schemaService.list(params)
            [
                    schemas : results,
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

    @RequestMapping(method=RequestMethod.DELETE)
    @ResponseBody
    Map deleteEntry(@RequestBody MetadataSchema metadataSchema, HttpServletResponse response ){
        try {
            UUID schema_id = metadataSchema.schema_id
            def content = schemaService.delete(schema_id) ?: [:]

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

    @RequestMapping(value='/purge', method=RequestMethod.DELETE)
    @ResponseBody
    Map  purge(@RequestBody Map params, HttpServletResponse response) {

        try {
            Map content = schemaService.purge(params)

            log.info( "count deleted:${content.totalResultsDeleted} content.searchTerms:${content.searchTerms}" +
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
