package org.cedar.metadata.storage.controller

import groovy.util.logging.Slf4j
import org.cedar.metadata.storage.domain.CollectionMetadata
import org.cedar.metadata.storage.domain.CollectionMetadataRepository
import org.cedar.metadata.storage.service.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = '/collections', produces = 'application/json')
class CollectionController {

  @Autowired
  CollectionMetadataRepository collectionMetadataRepository

  @Autowired
  RepoService repoService

  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  Map save(@RequestBody CollectionMetadata metadataObject, HttpServletResponse response) {
    repoService.save(response, collectionMetadataRepository, metadataObject)
  }

  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  Map list(@RequestParam Map params, HttpServletResponse response) {
    repoService.list(response, collectionMetadataRepository, params)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  @ResponseBody
  Map update(@PathVariable String id, @RequestParam(required = false) Long version, @RequestBody Map metadataObject, HttpServletResponse response) {
    // coerce id to a UUID and remove client-provided last_update
    metadataObject.id = UUID.fromString(id)
    metadataObject.last_update = null

    def previousUpdate = version ? new Date(version) : null
    repoService.update(response, collectionMetadataRepository, new CollectionMetadata(metadataObject), previousUpdate)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @ResponseBody
  Map listById(@PathVariable id, @RequestParam Map params, HttpServletResponse response) {
    params.id = id
    repoService.list(response, collectionMetadataRepository, params)
  }

  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  @ResponseBody
  Map delete(@PathVariable id, @RequestBody Map metadataObject, HttpServletResponse response) {
    UUID uuid = UUID.fromString(id)
    Date timestamp = new Date(metadataObject.last_update as Long)
    repoService.softDelete(response, collectionMetadataRepository, uuid, timestamp) ?: [:]
  }

  @RequestMapping(value = '/purge', method = RequestMethod.DELETE)
  @ResponseBody
  Map purge(@RequestBody Map params, HttpServletResponse response) {
    repoService.purge(response, collectionMetadataRepository, params)
  }

  @RequestMapping(value = '/recover', method = RequestMethod.PUT)
  @ResponseBody
  Map recover(HttpServletResponse response) {
    log.info 'Attempting to recover all collection metadata records'
    repoService.recover(response, collectionMetadataRepository)
  }

}
