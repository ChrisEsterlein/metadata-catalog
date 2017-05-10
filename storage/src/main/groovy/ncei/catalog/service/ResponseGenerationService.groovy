package ncei.catalog.service

import groovy.util.logging.Slf4j
import ncei.catalog.domain.MetadataRecord
import org.springframework.stereotype.Component

import javax.servlet.http.HttpServletResponse

@Slf4j
@Component
class ResponseGenerationService {

  Map generateResponse(HttpServletResponse response, Map details, String action, String table){
    Map jsonResponse = [:]
    jsonResponse.meta = [:]
    jsonResponse.meta.action = action
    details.each{ key, value ->
      if(key == 'error' || key == 'errors'){
        jsonResponse.errors = [value]
      }else if (key == 'records'){
        jsonResponse.data =[]
        value.each{jsonResponse.data.add(buildDataItem(it, table))}
      }else{
        jsonResponse.meta["$key"] = value
      }
      if(key == 'code'){
        response.status = value
      }else{
        response.status = jsonResponse.data? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND
      }
    }

    jsonResponse
  }

  Map buildDataItem(MetadataRecord metadataRecord, String table){
    String type = table - 's' //make singlular
    return [type: type, id: metadataRecord.id, attributes: metadataRecord]
  }

}
