package pt.psoft.g1.psoftg1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DeploymentEndpointsIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void readinessIsPublicAndUp() throws Exception {
    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void infoIsPublicAndIdentifiesTheBuildAndEnvironment() throws Exception {
    mockMvc
        .perform(get("/actuator/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.environment").value("local"))
        .andExpect(jsonPath("$.build.artifact").value("psoft-g1"))
        .andExpect(jsonPath("$.build.commit").isNotEmpty());
  }

  @Test
  void otherActuatorEndpointsAreNotPublic() throws Exception {
    mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }
}
