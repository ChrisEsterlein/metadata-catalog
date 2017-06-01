package ncei.catalog.filters.pre

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import com.netflix.zuul.http.ServletInputStreamWrapper
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import ncei.catalog.filters.utils.RequestConversionUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import java.nio.charset.Charset

@Slf4j
@Component
class LegacyPreFilter extends ZuulFilter {

  @Autowired
  RequestConversionUtil requestConversionUtil

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
    String method = request.getMethod()
    return path == "/catalog-metadata/files" && (method == 'POST' || method == 'PUT')
  }

  @Override
  Object run() {
    RequestContext ctx = RequestContext.getCurrentContext()
    HttpServletRequest request = ctx.getRequest()
    log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()))
    InputStream input = (InputStream) ctx.get("requestEntity")
    if (input == null) {
      input = request.getInputStream()
    }
    String body = StreamUtils.copyToString(input, Charset.forName("UTF-8"))
    Map postBody = new JsonSlurper().parseText(body)
    String transformedPostBody = requestConversionUtil.transformRecorderPost(postBody) as String
    byte[] bytes = transformedPostBody.getBytes("UTF-8")
    ctx.setRequest(new HttpServletRequestWrapper(request) {
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
