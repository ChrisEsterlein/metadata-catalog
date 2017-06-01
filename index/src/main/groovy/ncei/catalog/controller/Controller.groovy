package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.service.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import static org.springframework.web.bind.annotation.RequestMethod.GET

@Slf4j
@RestController
@RequestMapping(value = '/')
class Controller {

  @Autowired
  Service service

  @RequestMapping(value = "/search", method = [GET])
  @ResponseBody
  Map search(@RequestParam(required = false) String q,
             @RequestParam(required = false) String offset,
             @RequestParam(required = false) String max) {
    log.info "Received search request: q=$q offset=$offset max=$max"
    return service.search(q, offset, max)
  }
}
