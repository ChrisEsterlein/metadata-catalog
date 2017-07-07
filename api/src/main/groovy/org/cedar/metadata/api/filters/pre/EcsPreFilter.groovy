package org.cedar.metadata.api.filters.pre

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import com.netflix.zuul.http.ServletInputStreamWrapper
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.cedar.metadata.api.filters.utils.RequestConversionUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import java.nio.charset.Charset

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_ENTITY_KEY
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY

@Slf4j
@Component
class EcsPreFilter extends ZuulFilter {


  @Value('${zuul.routes.storage.url}')
  String STORAGE_BASE_URL //= 'http://localhost:8081'

  String ECS_ENDPOINT = 'ecs-catalog'
  String COLLECTION_PATH = '/collections'

  final String GET = 'GET'
  final String POST = 'POST'
  final String PUT = 'PUT'
  final String DELETE = 'DELETE'

  @Override
  String filterType() {
    return "pre"
  }

  @Override
  int filterOrder() {
    return PRE_DECORATION_FILTER_ORDER + 1
  }

  @Override
  boolean shouldFilter() {
    RequestContext ctx = RequestContext.getCurrentContext()
    HttpServletRequest request = ctx.getRequest()
    String path = request.getServletPath()
    return request.getMethod().equals('POST') && path.contains(ECS_ENDPOINT)
  }

  @Override
  Object run() {
    RequestContext ctx = RequestContext.getCurrentContext()
    log.info(String.format("Filtering %s request to %s", ctx.getRequest().getMethod(), ctx.getRequest().getRequestURL().toString()))

    switch (ctx.getRequest().getMethod()) {
      case GET:
        handleGet(ctx)
        break
      case POST:
        handlePost(ctx)
        break
      case PUT:
        break
      case DELETE:
        break
    }
  }

  def handleGet(RequestContext ctx) {
    Map<String, List<String>> params = ctx.getRequestQueryParams()
    def newParams = RequestConversionUtil.transformParams(params)
    ctx.setRequestQueryParams(newParams)
  }

  void handlePost(RequestContext ctx) {
    HttpServletRequest request = ctx.getRequest()
    ctx.setRouteHost(new URL(STORAGE_BASE_URL))
    ctx.set(REQUEST_URI_KEY, COLLECTION_PATH)
    InputStream input = (InputStream) ctx.get(REQUEST_ENTITY_KEY)
    if (input == null) {
      input = request.getInputStream()
    }
    String body = StreamUtils.copyToString(input, Charset.forName("UTF-8"))
    Map postBody = body ? (Map) new JsonSlurper().parseText(body) : null
    String transformedPostBody = new JsonBuilder([metadata: "${new JsonBuilder(postBody)}"]).toString()
    byte[] bytes = transformedPostBody.getBytes("UTF-8")
    RequestConversionUtil.setRequestPostBody(ctx, bytes)
  }

}
