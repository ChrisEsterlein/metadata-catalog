package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.domain.GranuleMetadataRepository
import ncei.catalog.service.ControllerService
import ncei.catalog.service.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.cassandra.repository.CassandraRepository
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
@RequestMapping(value = '/granules', produces = 'application/json')
class GranuleController {

  @Autowired
  GranuleMetadataRepository granuleMetadataRepository
  
  @Autowired 
  RepoService repoService

  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  Map save(@RequestBody GranuleMetadata metadataObject, HttpServletResponse response) {
    repoService.save(response, granuleMetadataRepository, metadataObject)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  @ResponseBody
  Map update(@PathVariable id, @RequestBody Map metadataObject, HttpServletResponse response) {
    if(!metadataObject.last_update){
      response.status = HttpServletResponse.SC_BAD_REQUEST
      return [errors:['To update a record, you must provide the record\'s id and last_update field, ' +
                       'as well as any other fields you do not want to update to null']]
    }
    metadataObject.id = UUID.fromString(metadataObject.id)
    metadataObject.last_update = new Date (metadataObject.last_update as Long)
    repoService.update(response, granuleMetadataRepository, new GranuleMetadata(metadataObject))
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map list(@RequestParam Map params, HttpServletResponse response) {
    repoService.list(response, granuleMetadataRepository, params)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @ResponseBody
  Map listById(@PathVariable id, @RequestParam Map params, HttpServletResponse response) {
    params.id = id
    repoService.list(response, granuleMetadataRepository, params)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  @ResponseBody
  Map delete(@PathVariable id, @RequestBody Map metadataObject, HttpServletResponse response) {
    UUID uuid = UUID.fromString(id)
    Date timestamp = new Date(metadataObject.last_update as Long)
    repoService.softDelete(response, granuleMetadataRepository, uuid, timestamp) ?: [:]
  }

  @RequestMapping(value = '/purge', method = RequestMethod.DELETE)
  @ResponseBody
  Map purge(@RequestBody Map params, HttpServletResponse response) {
    repoService.purge(response, granuleMetadataRepository, params)
  }

}
