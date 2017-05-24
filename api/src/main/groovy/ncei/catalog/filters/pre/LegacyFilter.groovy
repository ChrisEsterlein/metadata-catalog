package ncei.catalog.filters.pre

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import groovy.util.logging.Slf4j

import javax.servlet.http.HttpServletRequest

@Slf4j
class LegacyFilter extends ZuulFilter{
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
    return true
  }

  @Override
  Object run() {
    RequestContext ctx = RequestContext.getCurrentContext();
    HttpServletRequest request = ctx.getRequest();

    log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));

    return null
  }
}
