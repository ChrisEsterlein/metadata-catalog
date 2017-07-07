package org.cedar.metadata.api.filters.post

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.cedar.metadata.api.filters.utils.RequestConversionUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils

import javax.servlet.http.HttpServletRequest
import java.nio.charset.Charset

@Component
@Slf4j
class EcsPostFilter extends ZuulFilter{

//  @Value('${zuul.routes.ecs-catalog.path}')
  String ECS_ENDPOINT = 'ecs-catalog'

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
    return path.contains(ECS_ENDPOINT)
  }

  @Override
  Object run() {
    RequestContext ctx = RequestContext.getCurrentContext()
    InputStream stream = ctx.getResponseDataStream()
    String body = StreamUtils.copyToString(stream, Charset.forName("UTF-8"))
    log.info("Response from storage $body")

//    Map jsonApiResponseBody = body ? (Map) new JsonSlurper().parseText(body) : null
//    String transformedPostBody = RequestConversionUtil.transformLegacyGetResponse(jsonApiResponseBody, ctx.getResponse().getStatus())
//    ctx.setResponseDataStream(new ByteArrayInputStream(transformedPostBody.getBytes("UTF-8")))
  }
}
