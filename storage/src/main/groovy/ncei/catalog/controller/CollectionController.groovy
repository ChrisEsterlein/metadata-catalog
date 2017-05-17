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
  Map update(@PathVariable id, @RequestBody Map metadataObject, HttpServletResponse response) {
    if (!metadataObject.last_update) {
      response.status = HttpServletResponse.SC_BAD_REQUEST
      return [errors: ['To update a record, you must provide the previous record\'s last_update field, ' +
                               'as well as any other fields you do not want to update to null']]
    }
    metadataObject.id = UUID.fromString(metadataObject.id)
    metadataObject.last_update = new Date(metadataObject.last_update as Long)
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
  Map recover(@RequestBody params, HttpServletResponse response) {
    log.info 'Attempting to recover all collection metadata records'
    int limit = params?.limit ?: 0
    repoService.recover(response, collectionMetadataRepository, limit)
  }

}
