package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.service.ControllerService
import ncei.catalog.service.RepoService

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

  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  Map save(@PathVariable table, @RequestBody Map metadataObject, HttpServletResponse response) {
    CassandraRepository repo = controllerService.getRepo(table)
    repoService.save(response, controllerService.getRepo(table), controllerService.toMetadataRecord(table, metadataObject))
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  @ResponseBody
  Map update(@PathVariable table, @PathVariable id, @RequestBody Map metadataObject, HttpServletResponse response) {
    //need try/catch
    Map updateDetails = [:]
    if (metadataObject?.last_update) {
      metadataObject.last_update = new Date(metadataObject.last_update as Long)
      metadataObject.id = UUID.fromString(id)
      updateDetails = repoService.update(response, controllerService.getRepo(table), controllerService.toMetadataRecord(table, metadataObject))
    } else {
      updateDetails.error = 'To update a record you must provide the previous versions \'last_update\' field ' +
              '(and any other fields you don\'t want to update to null)'
      updateDetails.code = HttpServletResponse.SC_BAD_REQUEST
    }
    updateDetails
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map list(@PathVariable table, @RequestParam Map params, HttpServletResponse response) {
    try {
      params.table = table
      repoService.list(response, controllerService.getRepo(table), params)
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
      repoService.list(response, controllerService.getRepo(table), params)
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
      repoService.softDelete(response, controllerService.getRepo(table), uuid, timestamp) ?: [:]
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
      repoService.purge(response, controllerService.getRepo(table), params)
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
