package ncei.catalog.filters.pre

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

@Slf4j
@Component
class LegacyPreFilter extends ZuulFilter{

  @Autowired
  RequestConversionUtil filterHelper

  @Override
  String filterType() {
    return "pre"
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
    return path == "/catalog-metadata/files" && request.getMethod() == 'POST'
  }

  @Override
  Object run() {
    RequestContext ctx = RequestContext.getCurrentContext()
    HttpServletRequest request = ctx.getRequest()
    log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()))
    InputStream input = (InputStream) ctx.get("requestEntity")
    if (input == null) {input = ctx.getRequest().getInputStream()}
    String body = StreamUtils.copyToString(input, Charset.forName("UTF-8"))
    JsonSlurper slurper = new JsonSlurper()
    Map postBody = slurper.parseText(body)
    String transformedPostBody = filterHelper.transformRecorderPost(postBody) as String
    ctx.set("requestEntity", new ByteArrayInputStream(transformedPostBody.getBytes("UTF-8")))
  }
}
