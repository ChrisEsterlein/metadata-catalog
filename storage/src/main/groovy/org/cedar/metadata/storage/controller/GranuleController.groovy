package org.cedar.metadata.storage.controller

import groovy.util.logging.Slf4j
import org.cedar.metadata.storage.domain.GranuleMetadata
import org.cedar.metadata.storage.domain.GranuleMetadataRepository
import org.cedar.metadata.storage.service.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

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
  Map save(@RequestBody Map metadataObject, HttpServletResponse response) {
    metadataObject.id = metadataObject?.id ? UUID.fromString(metadataObject.id) : UUID.randomUUID()

    repoService.save(response, granuleMetadataRepository, new GranuleMetadata(metadataObject))
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  @ResponseBody
  Map update(@PathVariable String id, @RequestParam(required = false) Long version, @RequestBody Map metadataObject, HttpServletResponse response) {
    // coerce id to a UUID and remove client-provided last_update
    metadataObject.id = UUID.fromString(id)
    metadataObject.last_update = null

    def previousUpdate = version ?new Date(version) : null
    repoService.update(response, granuleMetadataRepository, new GranuleMetadata(metadataObject), previousUpdate)
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

  @RequestMapping(value = '/recover', method = RequestMethod.PUT)
  @ResponseBody
  Map recover(HttpServletResponse response) {
    log.info 'Attempting to recover all granule metadata records'
    repoService.recover(response, granuleMetadataRepository)
  }

}
