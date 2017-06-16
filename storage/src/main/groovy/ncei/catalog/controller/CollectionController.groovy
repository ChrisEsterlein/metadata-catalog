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
  Map update(@PathVariable id, @RequestParam(required = false) Long version, @RequestBody Map metadataObject, HttpServletResponse response) {
    metadataObject.id = UUID.fromString(id)
    //relaxed optimistic locking- remove their timestamp, set with [version] if specified
    metadataObject.last_update = null //reset last_update
    if(version){ metadataObject.put('last_update',  new Date(version) )}
    repoService.update(response, collectionMetadataRepository, new CollectionMetadata(metadataObject))
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
