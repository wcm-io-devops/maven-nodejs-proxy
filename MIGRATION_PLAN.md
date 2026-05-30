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
| Java (bytecode/compiler) | 21 | **21** | Keep `maven.compiler.release` at 21 for broad compatibility; DW5 baseline is JDK 17+ |
| Java (deploy/runtime) | 21 / 25 (CI matrix) | **25 (LTS)** | Run the service on the Java 25 LTS runtime; bytecode stays at 21 |
| JAX-RS / Servlet / Validation | `javax.*` | `jakarta.*` | Namespace change across all sources |
| Apache HttpClient | 4.x (`org.apache.http`) | 5.x (`org.apache.hc.*`) | Provided via `dropwizard-client` 5.x |
| Test framework | JUnit 4.x | **JUnit 6.1.0** | `junit-jupiter`; Dropwizard `ResourceExtension` |
| Build helper | `ResourceTestRule` (`@Rule`) | `ResourceExtension` (`@RegisterExtension`) | JUnit Jupiter style |

---

## Phase 0 — Safety net: integration tests on the EXISTING codebase (do this first)

Goal: lock current behavior before changing anything. These tests run against the
unmodified Dropwizard 1.3.29 app and become the regression oracle for the migration.

### 0.1 Establish the baseline
- [x] Run `mvn clean install` on the current code; confirm existing unit tests pass.
- [x] Record current behavior of every endpoint (status codes, content types,
      headers, body shape) as the source of truth.

### 0.2 Add full-application integration tests
Add tests that boot the **whole** Dropwizard application (not just the resource) so
we exercise the real Jersey/Jetty/HttpClient wiring that the migration will touch.

- [x] Use `DropwizardAppExtension`/`DropwizardAppRule` to start the app with
      `config.yml` on a random port.
- [x] Stub the upstream `nodejs.org/dist` server (e.g. WireMock) so tests are
      deterministic and offline — serve canned `SHASUMS256.txt`, binaries, and 404s.
- [x] Point `nodeJsBinariesRootUrl` at the WireMock base URL via a test config.

Cover at minimum:
- [x] `GET /` index page → 200, `text/html`, contains expected example URLs.
- [x] `GET …/nodejs-binaries-<v>.pom` → 200, `application/xml`, valid POM body.
- [x] `GET …/nodejs-binaries-<v>.pom.sha1` → 200, `text/plain`, correct SHA1.
- [x] `GET …/nodejs-binaries-<v>-linux-x64.tar.gz` → 200, `application/octet-stream`,
      `Content-Length` header present, body bytes match upstream.
- [x] `GET …-<v>-linux-x64.tar.gz.sha1` → 200, SHA1 of the binary.
- [x] Windows variants (`win-x64.zip`, `windows-x86.exe`, legacy `<4.0.0` paths).
- [x] NPM artifact (`npm-binaries-<v>.tgz`, `.pom`).
- [x] **Checksum mismatch** → upstream byte tampered → expect 404 (integrity reject).
- [x] **Unknown version / missing checksum file** → 404.
- [x] Invalid groupId / artifactId / version path params → 404.
- [x] Health check `GET /healthcheck` (admin port) → 200 healthy when upstream up,
      unhealthy when upstream returns non-200.

### 0.3 Lock the baseline green
- [x] All new integration tests pass against Dropwizard 1.3.29.
- [x] Commit this as a standalone change **before** touching any production code.

> These tests deliberately avoid Dropwizard-version-specific assertions where
> possible (assert on HTTP contract, not framework internals) so they survive the
> jump to 5.x with minimal edits.

---

## Phase 1 — Runtime baseline to Java 25 (bytecode stays at Java 21)

> **Decision:** keep the compiled bytecode at Java 21 for broad compatibility, but
> build, test and deploy on the Java 25 LTS runtime. No `maven.compiler.release`
> change is required (it stays at 21, inherited from the parent POM).

- [x] Leave `maven.compiler.release` at `21` — do **not** raise the bytecode level.
- [x] Ensure CI in `.github/workflows/maven-build.yml` builds/tests on Java 25
      (keep Sonar run on Java 25).
- [x] Confirm `.github/workflows/maven-deploy.yml` deploys on Java 25.
- [x] Re-run Phase 0 tests — still green on Java 25 with Dropwizard 1.3.29.

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
- [x] Bump `dropwizard.version` → `5.0.1`.
- [x] Keep `dropwizard-core`, `dropwizard-client`, `dropwizard-testing` aligned to
      `${dropwizard.version}`.
- [x] Remove the explicit Apache HttpClient 4 expectations; rely on HttpClient 5
      brought transitively by `dropwizard-client` 5.x.
- [x] Review `commons-io`, `commons-codec`, `maven-artifact` versions — keep current,
      bump only if needed for Jakarta/Java 25 runtime compatibility.
- [x] Verify the `maven-shade-plugin` fat-jar config still produces a runnable
      `server config.yml` jar (main class unchanged:
      `io.wcm.devops.maven.nodejsproxy.MavenProxyApplication`).
- [x] Regenerate `dependency-reduced-pom.xml` via the shade plugin.

### 2b. Namespace migration (`javax.*` → `jakarta.*`)
Affected files: `MavenProxyResource.java`, `MavenProxyConfiguration.java`,
`NodeJsDistHealthCheck.java` (and any other `javax.*` import).
- [x] `javax.ws.rs.*` → `jakarta.ws.rs.*` (`@GET`, `@Path`, `@PathParam`, `@Produces`,
      `Response`, `MediaType`, `HttpHeaders.CONTENT_LENGTH`).
- [x] `javax.servlet.http.HttpServletResponse` → `jakarta.servlet.http.HttpServletResponse`.
- [x] `javax.validation.*` → `jakarta.validation.*`.
- [x] Replace `org.hibernate.validator.constraints.NotEmpty` with
      `jakarta.validation.constraints.NotEmpty` (the constraint moved to Bean
      Validation core).
- [x] Confirm `com.fasterxml.jackson.annotation.JsonProperty` unchanged.

### 2c. Apache HttpClient 4 → 5 migration
Affected files: `MavenProxyApplication.java`, `MavenProxyResource.java`,
`NodeJsDistHealthCheck.java`.
- [x] `io.dropwizard.client.HttpClientBuilder` (DW5) now returns an HttpClient 5
      `CloseableHttpClient` (`org.apache.hc.client5.http.impl.classic.CloseableHttpClient`).
- [x] `org.apache.http.client.methods.HttpGet/HttpHead` →
      `org.apache.hc.client5.http.classic.methods.HttpGet/HttpHead`.
- [x] Switched to the HttpClient 5 `execute(request, HttpClientResponseHandler)`
      pattern (no `ClassicHttpResponse` field needed — the handler receives it).
- [x] `response.getStatusLine().getStatusCode()` → `response.getCode()`.
- [x] `org.apache.http.util.EntityUtils` →
      `org.apache.hc.core5.http.io.entity.EntityUtils`
      (`toByteArray`, `toString`, `consume` — checked-exception changes handled).
- [x] `HttpServletResponse.SC_OK` checks kept consistent.
- [x] Adopted the HttpClient 5 `execute(request, HttpClientResponseHandler)` pattern
      for safe response/entity handling, replacing the `try/finally`
      `consumeQuietly` blocks (binary data + Content-Length wrapped in a record).

> **Note on tests:** the production code will compile after 2a–2c, but the existing
> `dropwizard-testing` / `javax.ws.rs` test code (the Phase 0 `MavenProxyApplicationIT`
> suite) also breaks on the DW5 bump. The test-side jakarta changes (Phase 4) must
> therefore land in the **same commit** as Phase 2 to get a compiling, green build.
> Phases 2 and 4 are effectively one deliverable; Phase 3 (config review) sits in
> between as verification.

---

## Phase 3 — Configuration (`config.yml`) review

- [x] Verify `httpClient` block still binds to `HttpClientConfiguration` in DW5
      (connectionTimeout/timeout/timeToLive/cookiesEnabled/retries/userAgent).
- [x] Verify the `server.gzip.enabled: false` setting under Jetty 12 / DW5 — key
      unchanged; app boots and tar.gz responses stay uncompressed (ITs green).
- [x] Verify `logging` appenders config (file appender) still valid in DW5.
- [x] Confirm default admin/application connectors and ports behavior unchanged.

---

## Phase 4 — Test stack migration to JUnit 6

> Lands together with Phase 2 (the DW5 bump breaks the `javax.ws.rs` /
> `dropwizard-testing` imports in the test sources, so the test migration cannot be
> deferred to a later compiling build).

> **Note:** the legacy JUnit 4 `MavenProxyResourceTest` (and its `TestContext` helper)
> was removed during Phase 0 — it no longer ran (the build already uses JUnit Jupiter 6
> with no vintage engine) and its contract is fully covered by the offline
> `MavenProxyApplicationIT`. The remaining test work is jakarta namespace + Dropwizard
> testing API updates in that IT.

In `MavenProxyApplicationIT.java`:

- [x] Tests already use `org.junit.jupiter:junit-jupiter:6.1.0` (provided via the
      parent POM); JUnit 4 is no longer used.
- [x] `javax.ws.rs.*` in the IT → `jakarta.ws.rs.*`.
- [x] Re-verified the manual `DropwizardTestSupport` lifecycle against DW5
      (`before()` now declares `throws Exception`; kept manual lifecycle rather than
      the JUnit-Platform-6-incompatible DW extension).
- [x] Integration tests (`*IT`) run via `maven-failsafe-plugin` in the
      `integration-test` / `verify` phases (kept `parallel=none` / `threadCount=0`).

---

## Phase 5 — Verification

- [x] `mvn clean install` green on the Java 25 runtime (bytecode target Java 21).
- [x] **All Phase 0 integration tests pass unchanged in contract** against DW5
      (24 ITs green).
- [x] Build the fat jar and smoke-test:
      `java -jar target/io.wcm.devops.maven.nodejs-proxy-<version>.jar server config.yml`
- [x] Manually hit `http://localhost:8080/` and one real binary + `.sha1`
      (index → 200 `text/html`; `nodejs-binaries-10.15.0.pom` → 200 `application/xml`;
      `…-linux-x64.tar.gz` → 200 `application/octet-stream`, `Content-Length` 18630524;
      `.sha1` → 200).
- [x] Confirm health check endpoint reports healthy (admin `/healthcheck` → 200,
      `deadlocks` + `nodeJsDist` both `healthy: true`).
- [x] Confirm tar.gz downloads are byte-identical to upstream (no gzip corruption) —
      the SHA-256 integrity gate validates the upstream bytes before serving, and the
      served `Content-Length` equals the read byte count.
- [x] **Automated smoke test** added to `.github/workflows/maven-build.yml`
      (`smoke-test` job, Java 21 + 25): builds the shaded jar, boots it with
      `config.yml`, and asserts `GET /` → 200 so every CI run verifies the built JAR
      is actually runnable with the given configuration.

---

## Phase 6 — Security hardening (public-facing service)

- [ ] Keep / strengthen the existing SHA-256 checksum integrity validation
      (`Checksums` + `getBinaryWithChecksumValidation`).
- [ ] Add rate limiting / response size caps at the reverse proxy in front of the
      service (Ansible role) to limit upstream-fetch DoS amplification.
- [ ] Pin Java 25 LTS as the deploy runtime (bytecode remains Java 21).

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
- `src/test/java/io/wcm/devops/maven/nodejsproxy/MavenProxyApplicationIT.java`
- `.github/workflows/maven-build.yml`, `.github/workflows/maven-deploy.yml`
