package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.service.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse

import static org.springframework.web.bind.annotation.RequestMethod.GET

@Slf4j
@RestController
@RequestMapping(value = '/')
class Controller {

  @Autowired
  Service service

  @RequestMapping(value = "/search", method = [GET])
  @ResponseBody
  Map search(@RequestParam(required = false) Map searchParams, HttpServletResponse response) {
      log.info "Received search request: $searchParams"
      return service.search(searchParams)
  }
}
