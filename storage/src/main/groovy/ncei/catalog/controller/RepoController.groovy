package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.service.ControllerService
import ncei.catalog.service.RepoService
import ncei.catalog.service.ResponseGenerationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = '/{table}', produces = 'application/json')
class RepoController {

  @Autowired
  ControllerService controllerService

  @Autowired
  RepoService repoService

  @Autowired
  ResponseGenerationService responseGenerationService

  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  Map save(@PathVariable table, @RequestBody Map metadataObject, HttpServletResponse response) {
    CassandraRepository repo = controllerService.getRepo(table)
    Map saveDetails = repoService.save(controllerService.getRepo(table), controllerService.toMetadataRecord(table, metadataObject))
    responseGenerationService.generateResponse(response, saveDetails,'insert', table)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  @ResponseBody
  Map update(@PathVariable table, @PathVariable id, @RequestBody Map metadataObject, HttpServletResponse response) {
    //need try/catch
    Map updateDetails = [:]
    if (metadataObject?.last_update) {
      metadataObject.last_update = new Date(metadataObject.last_update as Long)
      metadataObject.id = UUID.fromString(id)
      updateDetails = repoService.update(controllerService.getRepo(table), controllerService.toMetadataRecord(table, metadataObject))
      updateDetails.code
    } else {
      updateDetails.error = 'To update a record you must provide the previous versions \'last_update\' field ' +
              '(and any other fields you don\'t want to update to null'
      updateDetails.code = HttpServletResponse.SC_BAD_REQUEST
    }
    responseGenerationService.generateResponse(response, updateDetails, 'update', table)
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map list(@PathVariable table, @RequestParam Map params, HttpServletResponse response) {
    try {
      params.table = table
      Map listDetails = repoService.list(controllerService.getRepo(table), params)
      response.status = listDetails.records ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND
      responseGenerationService.generateResponse(response, listDetails, 'read', table)

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

  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @ResponseBody
  Map listById(@PathVariable table, @PathVariable id, @RequestParam Map params, HttpServletResponse response) {
    try {
      params.id = id
      params.table = table
      Map listDetails = repoService.list(controllerService.getRepo(table), params)
      response.status = listDetails.records ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND
      responseGenerationService.generateResponse(response, listDetails,'read', table)
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

  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  @ResponseBody
  Map delete(@PathVariable table, @PathVariable id, @RequestBody Map metadataObject, HttpServletResponse response) {
    try {
      UUID uuid = UUID.fromString(id)
      Date timestamp = new Date(metadataObject.last_update as Long)
      Map deleteDetails = repoService.softDelete(controllerService.getRepo(table), uuid, timestamp) ?: [:]
      response.status = deleteDetails?.success ? response.SC_OK : response.SC_BAD_REQUEST
      responseGenerationService.generateResponse(response, deleteDetails,'delete', table)
    } catch (e) {
      def msg = metadataObject.id ?
              'failed to delete records for ' + metadataObject.id + ' from the metadata catalog' :
              'please specify a id'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }
  }

  @RequestMapping(value = '/purge', method = RequestMethod.DELETE)
  @ResponseBody
  Map purge(@PathVariable table, @RequestBody Map params, HttpServletResponse response) {

    try {
      Map deleteDetails = repoService.purge(controllerService.getRepo(table), params)

      log.info("count deleted:${deleteDetails.totalResultsDeleted} deleteDetails.code:${deleteDetails.code}")
      response.status = response.SC_OK
      String msg = 'Successfully purged ' + deleteDetails.totalResultsDeleted + ' rows matching ' + deleteDetails.searchTerms
      deleteDetails.message = msg
      responseGenerationService.generateResponse(response, deleteDetails,'delete', table)

    } catch (e) {
      def msg = params.id ?
              'failed to delete records for ' + params.id + ' from the metadata catalog'
              : 'please specify purge criteria'
      log.error(msg, e)
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: msg]
    }

  }
}
