package ncei.catalog.filters.post

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import ncei.catalog.filters.utils.RequestConversionUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils

import javax.servlet.http.HttpServletRequest
import java.nio.charset.Charset

@Component
@Slf4j
class LegacyPostFilter extends ZuulFilter {

  @Value('${zuul.routes.metadata-catalog-granules.path}')
  String METADATA_CATALOG_GRANULES_ENDPOINT

  @Value('${zuul.routes.metadata-catalog-search.path}')
  String METADATA_CATALOG_SEARCH_ENDPOINT

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
    AntPathMatcher matcher = new AntPathMatcher()
    return (request.getMethod().equals('POST') && matcher.match(METADATA_CATALOG_GRANULES_ENDPOINT, path)) ||
        (request.getMethod().equals('GET') && matcher.match(METADATA_CATALOG_SEARCH_ENDPOINT, path))
  }

  @Override
  Object run() {
    RequestContext ctx = RequestContext.getCurrentContext()
    InputStream stream = ctx.getResponseDataStream()
    String body = StreamUtils.copyToString(stream, Charset.forName("UTF-8"))

    Map responseBody = new JsonSlurper().parseText(body)
    String transformedPostBody = RequestConversionUtil.transformResponse(responseBody, ctx.getResponse().getStatus()) as String
    ctx.setResponseDataStream(new ByteArrayInputStream(transformedPostBody.getBytes("UTF-8")))
  }
}
