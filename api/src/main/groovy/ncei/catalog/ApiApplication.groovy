package ncei.catalog

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.zuul.EnableZuulProxy

@EnableZuulProxy
@SpringBootApplication
class ApiApplication {

  static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args)
  }
}
