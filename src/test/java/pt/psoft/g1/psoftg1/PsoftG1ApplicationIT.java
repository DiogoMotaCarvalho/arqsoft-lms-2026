package pt.psoft.g1.psoftg1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class PsoftG1ApplicationIT {

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping mappings;

  @Test
  void theContextExposesTheRestApiOfEveryModule() {
    Set<String> paths =
        mappings.getHandlerMethods().keySet().stream()
            .flatMap(info -> info.getPatternValues().stream())
            .collect(Collectors.toSet());

    assertThat(paths)
        .contains(
            "/api/public/login",
            "/api/authors",
            "/api/books/{isbn}",
            "/api/genres/top5",
            "/api/lendings/{year}/{seq}/renew",
            "/api/readers/{year}/{seq}",
            "/api/admin/users/{id}");
  }
}
