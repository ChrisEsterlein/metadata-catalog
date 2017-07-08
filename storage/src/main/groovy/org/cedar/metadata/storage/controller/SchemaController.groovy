package org.cedar.metadata.storage.controller

import groovy.util.logging.Slf4j
import org.cedar.metadata.storage.domain.MetadataSchema
import org.cedar.metadata.storage.domain.MetadataSchemaRepository
import org.cedar.metadata.storage.service.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = '/schemas', produces = 'application/json')
class SchemaController {

  @Autowired
  MetadataSchemaRepository schemaRepository

  @Autowired
  RepoService repoService

  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  Map save(@RequestBody MetadataSchema metadataObject, HttpServletResponse response) {
    repoService.save(response, schemaRepository, metadataObject)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  @ResponseBody
  Map update(@PathVariable String id, @RequestParam(required=false) Long version, @RequestBody Map metadataObject, HttpServletResponse response) {
    // coerce id to a UUID and remove client-provided last_update
    metadataObject.id = UUID.fromString(id)
    metadataObject.last_update = null

    def previousUpdate = version ? new Date(version) : null
    repoService.update(response, schemaRepository, new MetadataSchema(metadataObject), previousUpdate)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.PATCH)
  @ResponseBody
  Map patch(@PathVariable String id, @RequestParam(required=false) Long version, @RequestBody Map metadataObject, HttpServletResponse response) {
    // coerce id to a UUID and remove client-provided last_update
    metadataObject.id = UUID.fromString(id)
    metadataObject.last_update = null

    def previousUpdate = version ? new Date(version) : null
    repoService.patch(response, schemaRepository, new MetadataSchema(metadataObject), previousUpdate)
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map list(@RequestParam Map params, HttpServletResponse response) {
    repoService.list(response, schemaRepository, params)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @ResponseBody
  Map listById(@PathVariable id, @RequestParam Map params, HttpServletResponse response) {
    params.id = id
    repoService.list(response, schemaRepository, params)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  @ResponseBody
  Map delete(@PathVariable id, @RequestBody Map metadataObject, HttpServletResponse response) {
    UUID uuid = UUID.fromString(id)
    Date timestamp = new Date(metadataObject.last_update as Long)
    repoService.softDelete(response, schemaRepository, uuid, timestamp) ?: [:]
  }

  @RequestMapping(value = '/purge', method = RequestMethod.DELETE)
  @ResponseBody
  Map purge(@RequestBody Map params, HttpServletResponse response) {
    repoService.purge(response, schemaRepository, params)
  }

  @RequestMapping(value = '/recover', method = RequestMethod.PUT)
  @ResponseBody
  Map recover(HttpServletResponse response) {
    log.info 'Attempting to recover all metadata schemas'
    repoService.recover(response, schemaRepository)
  }
}
