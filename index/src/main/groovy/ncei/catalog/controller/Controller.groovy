package ncei.catalog.controller

import groovy.util.logging.Slf4j
import ncei.catalog.model.Metadata
import ncei.catalog.service.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody

import javax.servlet.http.HttpServletResponse

@Slf4j
@org.springframework.stereotype.Controller
@RequestMapping(value = '/')
@ConfigurationProperties
class Controller {

  @Autowired
  Service service

  @RequestMapping(value = "/files", method = [RequestMethod.POST, RequestMethod.PUT])
  @ResponseBody
  def saveFileMetadata(@RequestBody Metadata metadataEntry, HttpServletResponse response) {
    try {
      log.info "Received save: $metadataEntry"
      def saveResult = service.save(metadataEntry)
      log.info "Saved with result: $saveResult"
      saveResult
    } catch (e) {
      log.error "Error trying to save: $metadataEntry: $e"
      response.status = response.SC_INTERNAL_SERVER_ERROR
      [message: "Failed to save index: $metadataEntry"]
    }
  }
}
