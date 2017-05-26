package ncei.catalog.filters.post

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import ncei.catalog.filters.utils.RequestConversionUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils

import javax.servlet.http.HttpServletRequest
import java.nio.charset.Charset

@Component
@Slf4j
class LegacyPostFilter extends ZuulFilter {
  @Autowired
  RequestConversionUtil requestConversionUtil

  @Override
  String filterType() {
    return "post"
  }

  @Override
  int filterOrder() {

    return 1
  }

  @Override
  boolean shouldFilter() {
    RequestContext ctx = RequestContext.getCurrentContext()
    HttpServletRequest request = ctx.getRequest()
    String path = request.getServletPath()
    return path.contains("/catalog-metadata/files") //contains because we want to filter GETs coming back from /granule/id
  }

  @Override
  Object run() {
    RequestContext ctx = RequestContext.getCurrentContext()
    InputStream stream = ctx.getResponseDataStream()
    String body = StreamUtils.copyToString(stream, Charset.forName("UTF-8"))
    JsonSlurper slurper = new JsonSlurper()
    Map responseBody = slurper.parseText(body)
    String transformedPostBody = requestConversionUtil.transformRecorderResponse(responseBody, ctx.getResponse().getStatus()) as String
    ctx.setResponseDataStream(new ByteArrayInputStream(transformedPostBody.getBytes("UTF-8")))
  }
}
