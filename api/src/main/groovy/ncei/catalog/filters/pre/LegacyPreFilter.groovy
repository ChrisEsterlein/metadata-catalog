package ncei.catalog.filters.pre

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import com.netflix.zuul.http.ServletInputStreamWrapper
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import ncei.catalog.filters.utils.RequestConversionUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import java.nio.charset.Charset

@Slf4j
@Component
class LegacyPreFilter extends ZuulFilter {

  @Value('${zuul.routes.metadata-catalog-granules.path}')
  String METADATA_CATALOG_GRANULES_ENDPOINT

  @Value('${zuul.routes.metadata-catalog-search.path}')
  String METADATA_CATALOG_SEARCH_ENDPOINT

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
    AntPathMatcher matcher = new AntPathMatcher()
    return (request.getMethod().equals('POST') && matcher.match(METADATA_CATALOG_GRANULES_ENDPOINT, path)) ||
        (request.getMethod().equals('GET') && matcher.match(METADATA_CATALOG_SEARCH_ENDPOINT, path))
  }

  @Override
  Object run() {
    RequestContext ctx = RequestContext.getCurrentContext()
    log.info(String.format("%s request to %s", ctx.getRequest().getMethod(), ctx.getRequest().getRequestURL().toString()))

    if (ctx.getRequest().getMethod() == 'GET') {
      Map<String, List<String>> params = ctx.getRequestQueryParams()

      def newParams = RequestConversionUtil.transformParams(params)
      ctx.setRequestQueryParams(newParams)

    } else {
      InputStream input = (InputStream) ctx.get("requestEntity")
      if (input == null) {
        input = ctx.getRequest().getInputStream()
      }

      String body = StreamUtils.copyToString(input, Charset.forName("UTF-8"))
      Map postBody = body ? new JsonSlurper().parseText(body) : null
      String transformedPostBody = RequestConversionUtil.transformLegacyMetadataRecorderPostBody(postBody)
      byte[] bytes = transformedPostBody.getBytes("UTF-8")
      setRequestPostBody(ctx, bytes)
    }
  }

  static setRequestPostBody(RequestContext ctx, byte[] bytes) {
    ctx.setRequest(new HttpServletRequestWrapper(ctx.getRequest()) {
      @Override
      ServletInputStream getInputStream() throws IOException {
        return new ServletInputStreamWrapper(bytes)
      }

      @Override
      int getContentLength() {
        return bytes.length
      }

      @Override
      long getContentLengthLong() {
        return bytes.length
      }
    })
  }
}
