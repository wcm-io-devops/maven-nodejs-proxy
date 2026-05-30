# Migration Plan: Dropwizard 1.3.x → 5.0.x

Migration of **Maven NodeJS Proxy** from the end-of-life Dropwizard 1.3.29 stack to
the current Dropwizard 5.0.x line, including the move from `javax.*` to `jakarta.*`,
Apache HttpClient 4 → 5, and the test stack from JUnit 4 to JUnit 6.

> **Guiding principle:** Add integration tests against the *current* (unchanged)
> codebase **first**, establish a green baseline, and only then start the migration.
> The same tests must stay green after every migration step.

---

## Target versions

| Component | Current | Target | Notes |
|---|---|---|---|
| Dropwizard | `1.3.29` (EOL) | `5.0.1` | Jakarta EE 10, Jersey 3.1, Jetty 12 |
| Java (build + runtime) | 21 / 25 (CI matrix) | **25 (LTS)** | Java 25 is the current LTS; DW5 baseline is JDK 17+ |
| JAX-RS / Servlet / Validation | `javax.*` | `jakarta.*` | Namespace change across all sources |
| Apache HttpClient | 4.x (`org.apache.http`) | 5.x (`org.apache.hc.*`) | Provided via `dropwizard-client` 5.x |
| Test framework | JUnit 4.x | **JUnit 6.1.0** | `junit-jupiter`; Dropwizard `ResourceExtension` |
| Build helper | `ResourceTestRule` (`@Rule`) | `ResourceExtension` (`@RegisterExtension`) | JUnit Jupiter style |

---

## Phase 0 — Safety net: integration tests on the EXISTING codebase (do this first)

Goal: lock current behavior before changing anything. These tests run against the
unmodified Dropwizard 1.3.29 app and become the regression oracle for the migration.

### 0.1 Establish the baseline
- [ ] Run `mvn clean install` on the current code; confirm existing unit tests pass.
- [ ] Record current behavior of every endpoint (status codes, content types,
      headers, body shape) as the source of truth.

### 0.2 Add full-application integration tests
Add tests that boot the **whole** Dropwizard application (not just the resource) so
we exercise the real Jersey/Jetty/HttpClient wiring that the migration will touch.

- [ ] Use `DropwizardAppExtension`/`DropwizardAppRule` to start the app with
      `config.yml` on a random port.
- [ ] Stub the upstream `nodejs.org/dist` server (e.g. WireMock) so tests are
      deterministic and offline — serve canned `SHASUMS256.txt`, binaries, and 404s.
- [ ] Point `nodeJsBinariesRootUrl` at the WireMock base URL via a test config.

Cover at minimum:
- [ ] `GET /` index page → 200, `text/html`, contains expected example URLs.
- [ ] `GET …/nodejs-binaries-<v>.pom` → 200, `application/xml`, valid POM body.
- [ ] `GET …/nodejs-binaries-<v>.pom.sha1` → 200, `text/plain`, correct SHA1.
- [ ] `GET …/nodejs-binaries-<v>-linux-x64.tar.gz` → 200, `application/octet-stream`,
      `Content-Length` header present, body bytes match upstream.
- [ ] `GET …-<v>-linux-x64.tar.gz.sha1` → 200, SHA1 of the binary.
- [ ] Windows variants (`win-x64.zip`, `windows-x86.exe`, legacy `<4.0.0` paths).
- [ ] NPM artifact (`npm-binaries-<v>.tgz`, `.pom`).
- [ ] **Checksum mismatch** → upstream byte tampered → expect 404 (integrity reject).
- [ ] **Unknown version / missing checksum file** → 404.
- [ ] Invalid groupId / artifactId / version path params → 404.
- [ ] Health check `GET /healthcheck` (admin port) → 200 healthy when upstream up,
      unhealthy when upstream returns non-200.

### 0.3 Lock the baseline green
- [ ] All new integration tests pass against Dropwizard 1.3.29.
- [ ] Commit this as a standalone change **before** touching any production code.

> These tests deliberately avoid Dropwizard-version-specific assertions where
> possible (assert on HTTP contract, not framework internals) so they survive the
> jump to 5.x with minimal edits.

---

## Phase 1 — Build/runtime baseline to Java 25

- [ ] Set Maven compiler release to `25` (via parent POM property or
      `maven.compiler.release`).
- [ ] Update CI matrix in `.github/workflows/maven-build.yml` to build on Java 25
      (keep Sonar run on Java 25).
- [ ] Confirm `.github/workflows/maven-deploy.yml` uses Java 25.
- [ ] Re-run Phase 0 tests — still green on Java 25 with Dropwizard 1.3.29.

---

## Phase 2 — Framework migration (Dropwizard 5 + Jakarta + HttpClient 5) — ATOMIC

> **Why one phase:** bumping to Dropwizard 5 forces all three changes at once.
> Dropwizard 5 is Jakarta EE 10 (so every `javax.*` import breaks) **and**
> `dropwizard-client` 5.x ships HttpClient 5 (so `HttpClientBuilder.build()` now
> returns an `org.apache.hc.client5...CloseableHttpClient`, incompatible with the
> HttpClient 4 `org.apache.http...CloseableHttpClient` used today). The code will
> **not compile** until the POM bump, the namespace switch, and the HttpClient 5
> migration are all done together. Ship them in a single commit and run the Phase 0
> suite against the result.

### 2a. Dependency upgrade (`maven-nodejs-proxy/pom.xml`)
- [ ] Bump `dropwizard.version` → `5.0.1`.
- [ ] Keep `dropwizard-core`, `dropwizard-client`, `dropwizard-testing` aligned to
      `${dropwizard.version}`.
- [ ] Remove the explicit Apache HttpClient 4 expectations; rely on HttpClient 5
      brought transitively by `dropwizard-client` 5.x.
- [ ] Review `commons-io`, `commons-codec`, `maven-artifact` versions — keep current,
      bump only if needed for Jakarta/Java 25 compatibility.
- [ ] Verify the `maven-shade-plugin` fat-jar config still produces a runnable
      `server config.yml` jar (main class unchanged:
      `io.wcm.devops.maven.nodejsproxy.MavenProxyApplication`).
- [ ] Regenerate `dependency-reduced-pom.xml` via the shade plugin.

### 2b. Namespace migration (`javax.*` → `jakarta.*`)
Affected files: `MavenProxyResource.java`, `MavenProxyConfiguration.java`,
`NodeJsDistHealthCheck.java` (and any other `javax.*` import).
- [ ] `javax.ws.rs.*` → `jakarta.ws.rs.*` (`@GET`, `@Path`, `@PathParam`, `@Produces`,
      `Response`, `MediaType`, `HttpHeaders.CONTENT_LENGTH`).
- [ ] `javax.servlet.http.HttpServletResponse` → `jakarta.servlet.http.HttpServletResponse`.
- [ ] `javax.validation.*` → `jakarta.validation.*`.
- [ ] Replace `org.hibernate.validator.constraints.NotEmpty` with
      `jakarta.validation.constraints.NotEmpty` (the constraint moved to Bean
      Validation core).
- [ ] Confirm `com.fasterxml.jackson.annotation.JsonProperty` unchanged.

### 2c. Apache HttpClient 4 → 5 migration
Affected files: `MavenProxyApplication.java`, `MavenProxyResource.java`,
`NodeJsDistHealthCheck.java`.
- [ ] `io.dropwizard.client.HttpClientBuilder` (DW5) now returns an HttpClient 5
      `CloseableHttpClient` (`org.apache.hc.client5.http.impl.classic.CloseableHttpClient`).
- [ ] `org.apache.http.client.methods.HttpGet/HttpHead` →
      `org.apache.hc.client5.http.classic.methods.HttpGet/HttpHead`.
- [ ] `org.apache.http.HttpResponse` → `ClassicHttpResponse`
      (`org.apache.hc.core5.http.ClassicHttpResponse`).
- [ ] `response.getStatusLine().getStatusCode()` → `response.getCode()`.
- [ ] `org.apache.http.util.EntityUtils` →
      `org.apache.hc.core5.http.io.entity.EntityUtils`
      (`toByteArray`, `toString`, `consumeQuietly` — note checked-exception changes).
- [ ] Replace `HttpServletResponse.SC_OK` checks consistently (or switch to
      `org.apache.hc.core5.http.HttpStatus.SC_OK`).
- [ ] Prefer the HttpClient 5 `execute(request, HttpClientResponseHandler)` pattern
      for safe response/entity handling where it simplifies the `try/finally`
      `consumeQuietly` blocks.

> **Note on tests:** the production code will compile after 2a–2c, but the existing
> `dropwizard-testing` / `javax.ws.rs` test code (Phase 0 suite +
> `MavenProxyResourceTest`) also breaks on the DW5 bump. The test-side jakarta/JUnit
> changes (Phase 4) must therefore land in the **same commit** as Phase 2 to get a
> compiling, green build. Phases 2 and 4 are effectively one deliverable; Phase 3
> (config review) sits in between as verification.

---

## Phase 3 — Configuration (`config.yml`) review

- [ ] Verify `httpClient` block still binds to `HttpClientConfiguration` in DW5
      (connectionTimeout/timeout/timeToLive/cookiesEnabled/retries/userAgent).
- [ ] Verify the `server.gzip.enabled: false` setting under Jetty 12 / DW5 — the
      gzip/compression config key may have moved; keep tar.gz responses uncompressed.
- [ ] Verify `logging` appenders config (file appender) still valid in DW5.
- [ ] Confirm default admin/application connectors and ports behavior unchanged.

---

## Phase 4 — Test stack migration to JUnit 6

> Lands together with Phase 2 (the DW5 bump breaks the `javax.ws.rs` /
> `dropwizard-testing` imports in the test sources, so the test migration cannot be
> deferred to a later compiling build).

In `MavenProxyResourceTest.java` (and new Phase 0 tests):

- [ ] Bump to `org.junit.jupiter:junit-jupiter:6.1.0` (managed via Dropwizard BOM
      where possible; otherwise pin explicitly).
- [ ] Remove JUnit 4 (`junit:junit`, `@Rule`, `org.junit.Test`, `org.junit.Assert`).
- [ ] `org.junit.Test` → `org.junit.jupiter.api.Test`.
- [ ] `org.junit.Assert.assertEquals/assertTrue` →
      `org.junit.jupiter.api.Assertions.*`.
- [ ] `@Rule public ResourceTestRule` → `@RegisterExtension static ResourceExtension`
      (`io.dropwizard.testing.junit5.ResourceExtension`).
- [ ] App-level tests use `io.dropwizard.testing.junit5.DropwizardAppExtension` with
      `@ExtendWith(DropwizardExtensionsSupport.class)`.
- [ ] `javax.ws.rs.*` in tests → `jakarta.ws.rs.*`.
- [ ] Ensure `maven-surefire-plugin` picks up the JUnit Platform (keep
      `parallel=none` / `threadCount=0` for the existing non-parallel requirement).

---

## Phase 5 — Verification

- [ ] `mvn clean install` green on Java 25.
- [ ] **All Phase 0 integration tests pass unchanged in contract** against DW5.
- [ ] Build the fat jar and smoke-test:
      `java -jar target/io.wcm.devops.maven.nodejs-proxy-<version>.jar server config.yml`
- [ ] Manually hit `http://localhost:8080/` and one real binary + `.sha1`.
- [ ] Confirm health check endpoint reports healthy.
- [ ] Confirm tar.gz downloads are byte-identical to upstream (no gzip corruption).

---

## Phase 6 — Security hardening (public-facing service)

- [ ] Enable **Renovate** (or Dependabot) so Dropwizard/Jetty/Jackson CVEs auto-PR —
      this is the root-cause fix for how the stack went EOL unnoticed.
- [ ] Add **OWASP Dependency-Check** Maven plugin to fail builds on known-vulnerable
      transitive dependencies.
- [ ] Keep / strengthen the existing SHA-256 checksum integrity validation
      (`Checksums` + `getBinaryWithChecksumValidation`).
- [ ] Add rate limiting / response size caps at the reverse proxy in front of the
      service (Ansible role) to limit upstream-fetch DoS amplification.
- [ ] Pin Java 25 LTS as the deploy runtime.

---

## Rollback / risk notes

- Phases 0, 1, 3, 5 and 6 are independently committable and individually testable
  against the Phase 0 suite. **Phases 2 and 4 are a single atomic deliverable**
  (the DW5 bump forces the Jakarta + HttpClient 5 + test-stack changes together)
  and must land in one commit.
- The riskiest changes are the **HttpClient 4→5 migration (Phase 2c)** and the
  **Jetty 12 gzip / server config (Phase 3)** — the integration tests around binary
  download + `Content-Length` + checksum-reject are the key guards there.
- If a blocker appears on 5.0.1, **4.0.x** (also Jakarta + HttpClient 5) is a
  fallback with a smaller Jetty delta; the namespace and test work is identical.

## Affected source files (reference)

- `maven-nodejs-proxy/pom.xml`
- `maven-nodejs-proxy/config.yml`
- `src/main/java/io/wcm/devops/maven/nodejsproxy/MavenProxyApplication.java`
- `src/main/java/io/wcm/devops/maven/nodejsproxy/MavenProxyConfiguration.java`
- `src/main/java/io/wcm/devops/maven/nodejsproxy/resource/MavenProxyResource.java`
- `src/main/java/io/wcm/devops/maven/nodejsproxy/health/NodeJsDistHealthCheck.java`
- `src/test/java/io/wcm/devops/maven/nodejsproxy/resource/MavenProxyResourceTest.java`
- `.github/workflows/maven-build.yml`, `.github/workflows/maven-deploy.yml`
