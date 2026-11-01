package com.manuelfabri.configserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@SpringBootTest(properties = {
    "encrypt.key=isolated-compatibility-test-key",
    "config.crypto.username=test-manager",
    "config.crypto.password=isolated-test-password"
})
@AutoConfigureMockMvc
class ConfigServerCryptoSecurityTest extends ConfigServerTestSupport {

  @Test
  void anonymousAndIncorrectCredentialsCannotReachCryptoEndpoints() throws Exception {
    for (String endpoint : new String[] {"/encrypt", "/decrypt", "/encrypt/expensapp/dev", "/decrypt/expensapp/dev"}) {
      mockMvc.perform(post(endpoint).contentType(MediaType.TEXT_PLAIN).content("test-value"))
          .andExpect(status().isUnauthorized());
      mockMvc.perform(post(endpoint).with(httpBasic("test-manager", "incorrect"))
          .contentType(MediaType.TEXT_PLAIN).content("test-value"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void authenticatedCryptoRoundTripUsesRandomizedCiphertext() throws Exception {
    String plaintext = "isolated test + secret";
    String ciphertext = encrypt(plaintext);
    assertThat(ciphertext).isNotBlank().isNotEqualTo(plaintext).isNotEqualTo(encrypt(plaintext));
    mockMvc.perform(post("/decrypt").with(httpBasic("test-manager", "isolated-test-password"))
        .contentType(MediaType.TEXT_PLAIN).content(ciphertext))
        .andExpect(status().isOk())
        .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo(plaintext));
  }

  @Test
  void configurationRemainsAnonymousAndCiphertextOnlyWhenManagementIsEnabled() throws Exception {
    mockMvc.perform(get("/expensapp/dev"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.propertySources[0].source['fixture.encrypted']")
            .value("{cipher}not-server-decryptable"));
  }

  private String encrypt(String plaintext) throws Exception {
    return mockMvc.perform(post("/encrypt").with(httpBasic("test-manager", "isolated-test-password"))
        .contentType(MediaType.TEXT_PLAIN).content(plaintext))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  }
}
