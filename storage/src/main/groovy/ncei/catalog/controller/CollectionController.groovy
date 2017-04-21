package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.domain.GranuleMetadata
import ncei.catalog.service.CollectionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping(value = '/collections')
class CollectionController {

  @Autowired
  CollectionService collectionService

  //new endpoint
  @RequestMapping(value = "/create", method = RequestMethod.POST)
  @ResponseBody
  Map saveCollectionMetadata(@RequestBody GranuleMetadata granuleMetadata, HttpServletResponse response) {
//    collectionService.save(granuleMetadata)
  }

  @RequestMapping(value = "/update", method = RequestMethod.PUT)
  @ResponseBody
  Map updateCollectionMetadata(@RequestBody GranuleMetadata granuleMetadata, HttpServletResponse response) {
//    collectionService.save(granuleMetadata)
  }

}
