package com.manuelfabri.configserver;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

abstract class ConfigServerTestSupport {

  private static final String REPOSITORY_URI = createRepository();

  @Autowired
  protected MockMvc mockMvc;

  @DynamicPropertySource
  static void repositoryProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.config.server.git.uri", () -> REPOSITORY_URI);
    registry.add("spring.cloud.config.server.git.default-label", () -> "master");
  }

  private static String createRepository() {
    try {
      Path repository = Files.createTempDirectory("config-server-compatibility-");
      Path searchPath = Files.createDirectories(repository.resolve("expensapp-api"));
      Files.writeString(searchPath.resolve("expensapp.properties"), "fixture.plain=default-value\n");
      Files.writeString(searchPath.resolve("expensapp-dev.properties"),
          "fixture.plain=development-value\nfixture.encrypted={cipher}not-server-decryptable\n");
      try (Git git = Git.init().setDirectory(repository.toFile()).setInitialBranch("master").call()) {
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Compatibility fixture").setAuthor("Test", "test@example.invalid").call();
      }
      return repository.toUri().toString();
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot create isolated configuration fixture", exception);
    }
  }
}
