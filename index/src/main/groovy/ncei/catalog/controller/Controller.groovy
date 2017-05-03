package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.service.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

import javax.servlet.http.HttpServletResponse

import static org.springframework.web.bind.annotation.RequestMethod.GET

@Slf4j
@org.springframework.stereotype.Controller
@RequestMapping(value = '/')
@ConfigurationProperties
class Controller {

  @Autowired
  Service service

  @RequestMapping(value = "/search", method = [GET])
  @ResponseBody
  def showMetadata(@RequestParam Map params, HttpServletResponse response) {
    try {
      log.info "Received search request parameter=$params"
      service.search(params)
    } catch (e) {
      log.error "Error trying to search with params=$params Exception=$e"
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: "Failed to query with params=$params Exception=$e"]
    }
  }
}
