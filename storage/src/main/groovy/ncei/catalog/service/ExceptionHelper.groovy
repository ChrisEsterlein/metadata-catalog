package ncei.catalog.service

import groovy.util.logging.Slf4j
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice

//import org.springframework.security.accessAccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

import javax.servlet.http.HttpServletRequest

@Slf4j
@ControllerAdvice
class ExceptionHelper extends ResponseEntityExceptionHandler {

  private Map<String, Object> buildErrorMap(HttpStatus status, String message, String path, Exception ex) {
    Map<String, Object> responseBody = new HashMap<>()
    responseBody.meta = [:]
    responseBody.meta.put('timestamp', new Date())
    responseBody.meta.put('path', path)
    responseBody.meta.put('status', status)
    responseBody.meta.put('message', message)
    responseBody.put('errors', [ex as String])
    responseBody
  }

  //need this to catch NoHandlerFoundException
  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
    return new ResponseEntity<Object>(buildErrorMap(status, 'Not Found', request.getContextPath(), ex), HttpStatus.NOT_FOUND)
  }

  @ExceptionHandler(Exception.class)
  @ResponseBody
  def handleError(HttpServletRequest request, Exception ex) {
    log.error("Unexpected error", ex)
    return new ResponseEntity<Object>(buildErrorMap(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occured.", request.getContextPath(), ex), HttpStatus.FORBIDDEN)
  }

//  @ExceptionHandler(AccessDeniedException.class )
//  @ResponseBody
//  def handleAuthenticationException(HttpServletRequest request, Exception ex) {
//    return new ResponseEntity<Object>(buildErrorMap(HttpStatus.FORBIDDEN, “Not authenticated”, request.getContextPath(), ex), HttpStatus.FORBIDDEN)
//  }
}