package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.MetadataSchema
import ncei.catalog.service.SchemaService
import org.springframework.beans.factory.annotation.Autowired
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

    //new endpoint
    @RequestMapping(value = "/create", method = [RequestMethod.POST, RequestMethod.PUT])
    @ResponseBody
    Map saveMetadataSchema(@RequestBody MetadataSchema metadataSchema, HttpServletResponse response) {
      schemaService.save(metadataSchema)
    }

    @RequestMapping(value = "/delete", method=RequestMethod.DELETE)
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

    //new end point
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    Map listMetadataSchema(@RequestParam Map params, HttpServletResponse response) {
      try {
        [
          metadata_schemas : schemaService.list(params),
          searchTerms : params
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

  
}
