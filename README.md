# Config Server

Spring Cloud Config Server on Spring Boot 4.1.2, Spring Cloud 2025.1.4, and Java 17.

The server listens on port 8888 and reads [api-config-store](https://github.com/manufabri91/api-config-store), keeping its existing `master` branch and `*api` search paths. Encrypted values remain `{cipher}` text in configuration responses so clients can decrypt them with their own configured key. The previously bootstrap-only encryption configuration is now in `application.yml`, where Boot 4 loads it.

## Build and run

Use patched Java 17.0.21+9 or newer and Maven 3.9. Run `mvn verify` for the local Git fixture compatibility suite, then `mvn package` to produce `target/config-server.jar`. Runtime startup requires `ENCRYPT_KEY`; keep the existing key when migrating and never commit it.

The Dockerfile pins the parent images and verifies the published SHA256 checksums of Java 17.0.21+9 archives because the published Alpine tags lag that security release. Its current archive URLs target Linux amd64. Build with `docker build -t config-server:local .` and provide `ENCRYPT_KEY` through the deployment's secret configuration. The App Engine and Java buildpack runtime selectors are also updated to Java 17.

## Crypto management endpoints

The `/encrypt` and `/decrypt` endpoints, including their application/profile variants, are disabled with HTTP 403 unless `CONFIG_CRYPTO_PASSWORD` is configured. This prevents the public server from becoming a decryption oracle. Configuration reads remain anonymous and continue returning ciphertext for client-side decryption.

If you use HTTP crypto management, set a strong `CONFIG_CRYPTO_PASSWORD` in the deployment secret store, optionally set `CONFIG_CRYPTO_USERNAME` (default `config-admin`), and use HTTP Basic authentication over HTTPS. Anonymous or incorrect credentials then receive HTTP 401. No generated/default password enables access, and the configured password is encoded in memory. Keep the management password separate from `ENCRYPT_KEY` and never put either in a Git config repository, URL, command log or CI artifact.

## Verification and release

Tests create an isolated local Git repository and check default/development profiles, existing directory lookup, encrypted-value passthrough, management endpoint denial/authentication, and randomized encryption/decryption compatibility. They do not read production configuration or require GitHub credentials. CI runs the tests, scans the rebuilt runtime image for high/critical findings, retains scan reports and an SBOM, and runs weekly. Dependabot checks Maven, Docker and Actions updates weekly.

The dependency job inventories the application/test classpath and the Maven plugin classpaths actually loaded during the build. It scans those selected JARs with Trivy `rootfs`, rejects empty scans, and retains artifact coordinates without saving Maven's raw debug output, which can contain credentials. Plugin-scoped dependency patches are separate from the application's Spring Boot BOM.

Before release, verify the current production revision, active deployment path and key settings. Test a representative encrypted property with the upgraded API in staging without printing its plaintext. Keep the prior image available for recovery; rolling back to an affected release reopens the security remediation. No production settings or deployments are changed by this repository update.
