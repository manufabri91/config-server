package com.manuelfabri.configserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@SpringBootTest(properties = {"encrypt.key=isolated-compatibility-test-key", "config.crypto.password="})
@AutoConfigureMockMvc
class ConfigServerCompatibilityTest extends ConfigServerTestSupport {

  @Test
  void servesDefaultProfileFromExistingGitSearchPath() throws Exception {
    mockMvc.perform(get("/expensapp/default"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.propertySources[0].source['fixture.plain']").value("default-value"));
  }

  @Test
  void preservesCiphertextForClientSideDecryption() throws Exception {
    mockMvc.perform(get("/expensapp/dev"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.propertySources[0].source['fixture.plain']").value("development-value"))
        .andExpect(jsonPath("$.propertySources[0].source['fixture.encrypted']")
            .value("{cipher}not-server-decryptable"));
  }

  @Test
  void cryptoManagementEndpointsAreForbiddenWithoutConfiguredCredentials() throws Exception {
    for (String endpoint : new String[] {"/encrypt", "/decrypt", "/encrypt/expensapp/dev", "/decrypt/expensapp/dev"}) {
      mockMvc.perform(post(endpoint).contentType(MediaType.TEXT_PLAIN).content("test-value"))
          .andExpect(status().isForbidden());
    }
  }
}
