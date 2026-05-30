/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2026 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.devops.maven.nodejsproxy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;

/**
 * Phase 0 safety-net integration tests. Boots the whole Dropwizard application and exercises the real
 * Jersey/Jetty/HttpClient wiring against a stubbed (WireMock) upstream "nodejs.org/dist" server, so that the
 * HTTP contract is locked before the Dropwizard 5 migration.
 */
class MavenProxyApplicationIT {

  private static final int WIREMOCK_PORT = 8089;
  private static final int HTTP_OK = 200;

  private static final String NODEJS_VERSION = "10.15.0";
  private static final String NODEJS_LEGACY_VERSION = "3.0.0";
  private static final String NPM_VERSION = "1.4.9";

  private static final DropwizardTestSupport<MavenProxyConfiguration> APP = new DropwizardTestSupport<>(
      MavenProxyApplication.class, ResourceHelpers.resourceFilePath("config-test.yml"));

  private static WireMockServer wireMock;
  private static Client client;

  @BeforeAll
  static void startServers() throws Exception {
    wireMock = new WireMockServer(options().port(WIREMOCK_PORT));
    wireMock.start();
    APP.before();
    client = ClientBuilder.newClient();
  }

  @AfterAll
  static void stopServers() {
    if (client != null) {
      client.close();
    }
    APP.after();
    if (wireMock != null) {
      wireMock.stop();
    }
  }

  @BeforeEach
  void setUp() {
    wireMock.resetAll();
    stubAllUpstream();
  }

  // ----- index page -----

  @Test
  void testGetIndex() {
    Response response = client.target(url("/")).request().get();
    assertEquals(HTTP_OK, response.getStatus());
    assertEquals(MediaType.TEXT_HTML, response.getMediaType().toString());
    String body = response.readEntity(String.class);
    assertTrue(body.contains("Maven NodeJS Proxy"));
    assertTrue(body.contains("org/nodejs/dist/nodejs-binaries/10.15.0/nodejs-binaries-10.15.0.pom"));
    assertTrue(body.contains("org/nodejs/dist/npm-binaries/1.4.9/npm-binaries-1.4.9.tgz"));
  }

  // ----- POM artifacts -----

  @Test
  void testGetPomNodeJs() {
    assertPom("nodejs-binaries", NODEJS_VERSION);
  }

  @Test
  void testGetPomNpm() {
    assertPom("npm-binaries", NPM_VERSION);
  }

  // ----- NodeJS binaries -----

  @Test
  void testGetBinaryNodeJs() {
    Map<String, String> targets = new LinkedHashMap<>();
    targets.put("-linux-x64.tar.gz", "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-linux-x64.tar.gz");
    targets.put("-darwin-x64.tar.gz", "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-darwin-x64.tar.gz");
    targets.put("-win-x64.zip", "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-win-x64.zip");
    targets.put("-win-x86.zip", "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-win-x86.zip");
    targets.put("-windows-x64.zip", "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-win-x64.zip");
    targets.put("-windows-x86.zip", "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-win-x86.zip");
    targets.put("-windows-x64.exe", "/v" + NODEJS_VERSION + "/win-x64/node.exe");
    targets.put("-windows-x86.exe", "/v" + NODEJS_VERSION + "/win-x86/node.exe");
    for (Map.Entry<String, String> entry : targets.entrySet()) {
      assertBinaryDownload(NODEJS_VERSION, entry.getKey(), entry.getValue());
    }
  }

  @Test
  void testGetBinaryNodeJsLegacyWindows() {
    assertBinaryDownload(NODEJS_LEGACY_VERSION, "-windows-x86.exe", "/v" + NODEJS_LEGACY_VERSION + "/node.exe");
    assertBinaryDownload(NODEJS_LEGACY_VERSION, "-windows-x64.exe", "/v" + NODEJS_LEGACY_VERSION + "/x64/node.exe");
  }

  // ----- NPM binary -----

  @Test
  void testGetBinaryNpm() {
    String upstreamPath = "/npm/npm-" + NPM_VERSION + ".tgz";
    byte[] expected = binaryContent(upstreamPath);
    String path = "/org/nodejs/dist/npm-binaries/" + NPM_VERSION + "/npm-binaries-" + NPM_VERSION + ".tgz";

    Response response = client.target(url(path)).request().get();
    assertEquals(HTTP_OK, response.getStatus());
    assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getMediaType().toString());
    byte[] body = response.readEntity(byte[].class);
    assertArrayEquals(expected, body);
    assertNotNull(response.getHeaderString("Content-Length"));
    assertEquals(expected.length, response.getLength());

    assertSha1Binary(path, expected);
  }

  // ----- negative cases -----

  @Test
  void testChecksumMismatchReturns404() {
    wireMock.resetAll();
    stubHealthy();
    String upstreamPath = "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-linux-x64.tar.gz";
    wireMock.stubFor(get(urlEqualTo(upstreamPath))
      .willReturn(aResponse().withStatus(200).withBody(binaryContent(upstreamPath))));
    // serve a deliberately wrong checksum so the integrity check rejects the download
    String shasums = "0000000000000000000000000000000000000000000000000000000000000000"
        + "  node-v" + NODEJS_VERSION + "-linux-x64.tar.gz\n";
    wireMock.stubFor(get(urlEqualTo("/v" + NODEJS_VERSION + "/SHASUMS256.txt"))
      .willReturn(aResponse().withStatus(200).withBody(shasums)));

    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION
        + "-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testMalformedChecksumReturns404() {
    wireMock.resetAll();
    stubHealthy();
    String upstreamPath = "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-linux-x64.tar.gz";
    wireMock.stubFor(get(urlEqualTo(upstreamPath))
      .willReturn(aResponse().withStatus(200).withBody(binaryContent(upstreamPath))));
    // serve a checksum that is not valid hex so decoding fails and the download is rejected
    String shasums = "not-a-valid-hex-checksum"
        + "  node-v" + NODEJS_VERSION + "-linux-x64.tar.gz\n";
    wireMock.stubFor(get(urlEqualTo("/v" + NODEJS_VERSION + "/SHASUMS256.txt"))
      .willReturn(aResponse().withStatus(200).withBody(shasums)));

    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION
        + "-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUnknownVersionMissingChecksumReturns404() {
    wireMock.resetAll();
    stubHealthy();
    // no SHASUMS256.txt stub for this version -> upstream responds 404 -> proxy returns 404
    String path = "/org/nodejs/dist/nodejs-binaries/99.99.99/nodejs-binaries-99.99.99-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testInvalidGroupIdReturns404() {
    String path = "/com/example/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION
        + "-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testInvalidArtifactIdReturns404() {
    String path = "/org/nodejs/dist/bogus-binaries/" + NODEJS_VERSION + "/bogus-binaries-" + NODEJS_VERSION
        + "-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testInconsistentVersionReturns404() {
    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-9.9.9-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testPomValidationUpstreamMissingReturns404() {
    wireMock.resetAll();
    stubHealthy();
    // no SHASUMS256.txt stub for this version -> upstream HEAD responds 404 -> proxy returns 404
    String path = "/org/nodejs/dist/nodejs-binaries/99.99.99/nodejs-binaries-99.99.99.pom";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testPomValidationUpstreamErrorReturns404() {
    wireMock.resetAll();
    stubHealthy();
    // upstream HEAD validation responds with a server error -> proxy returns 404
    wireMock.stubFor(head(urlEqualTo("/v" + NODEJS_VERSION + "/SHASUMS256.txt"))
      .willReturn(aResponse().withStatus(503)));
    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION + ".pom";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testPomInvalidGroupIdReturns404() {
    String path = "/com/example/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION + ".pom";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testPomInconsistentVersionReturns404() {
    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-9.9.9.pom";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testBinaryDownloadUpstreamErrorReturns404() {
    wireMock.resetAll();
    stubHealthy();
    // checksum file is present with a valid entry, but the binary download itself fails with a non-200 status
    String upstreamPath = "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-linux-x64.tar.gz";
    String shasums = DigestUtils.sha256Hex(binaryContent(upstreamPath))
        + "  node-v" + NODEJS_VERSION + "-linux-x64.tar.gz\n";
    wireMock.stubFor(get(urlEqualTo("/v" + NODEJS_VERSION + "/SHASUMS256.txt"))
      .willReturn(aResponse().withStatus(200).withBody(shasums)));
    wireMock.stubFor(get(urlEqualTo(upstreamPath)).willReturn(aResponse().withStatus(503)));

    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION
        + "-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testBinaryMissingChecksumEntryReturns404() {
    wireMock.resetAll();
    stubHealthy();
    // checksum file is present but contains no entry for the requested binary -> proxy returns 404
    String shasums = "0000000000000000000000000000000000000000000000000000000000000000"
        + "  node-v" + NODEJS_VERSION + "-darwin-x64.tar.gz\n";
    wireMock.stubFor(get(urlEqualTo("/v" + NODEJS_VERSION + "/SHASUMS256.txt"))
      .willReturn(aResponse().withStatus(200).withBody(shasums)));

    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION
        + "-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testNpmBinaryDownloadUpstreamErrorReturns404() {
    wireMock.resetAll();
    stubHealthy();
    // npm download skips checksum validation -> a non-200 upstream status must still yield 404
    wireMock.stubFor(get(urlEqualTo("/npm/npm-" + NPM_VERSION + ".tgz"))
      .willReturn(aResponse().withStatus(503)));
    String path = "/org/nodejs/dist/npm-binaries/" + NPM_VERSION + "/npm-binaries-" + NPM_VERSION + ".tgz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testNpmArtifactWithNodeJsBinaryPathReturns404() {
    // npm artifact requested through the NodeJS binary path (os/arch) -> artifact type mismatch
    String path = "/org/nodejs/dist/npm-binaries/" + NPM_VERSION + "/npm-binaries-" + NPM_VERSION + "-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testNodeJsArtifactWithNpmBinaryPathReturns404() {
    // NodeJS artifact requested through the NPM binary path (no os/arch) -> artifact type mismatch
    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION + ".tgz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testArtifactIdFilenameMismatchReturns404() {
    // directory artifactId differs from the filename artifactId -> basic parameter validation fails
    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/npm-binaries-" + NODEJS_VERSION + ".tgz";
    Response response = client.target(url(path)).request().get();
    assertEquals(404, response.getStatus());
  }

  @Test
  void testBinaryDownloadChunkedTransfer() {
    wireMock.resetAll();
    stubHealthy();
    // upstream streams the binary with chunked transfer encoding (delivered in multiple chunks)
    String upstreamPath = "/v" + NODEJS_VERSION + "/node-v" + NODEJS_VERSION + "-linux-x64.tar.gz";
    byte[] content = binaryContent(upstreamPath);
    String shasums = DigestUtils.sha256Hex(content) + "  node-v" + NODEJS_VERSION + "-linux-x64.tar.gz\n";
    wireMock.stubFor(get(urlEqualTo("/v" + NODEJS_VERSION + "/SHASUMS256.txt"))
      .willReturn(aResponse().withStatus(200).withBody(shasums)));
    wireMock.stubFor(get(urlEqualTo(upstreamPath))
      .willReturn(aResponse().withStatus(200).withBody(content).withChunkedDribbleDelay(2, 20)));

    String path = "/org/nodejs/dist/nodejs-binaries/" + NODEJS_VERSION + "/nodejs-binaries-" + NODEJS_VERSION
        + "-linux-x64.tar.gz";
    Response response = client.target(url(path)).request().get();
    assertEquals(HTTP_OK, response.getStatus());
    assertArrayEquals(content, response.readEntity(byte[].class));
  }

  // ----- health check -----

  @Test
  void testHealthCheckHealthy() {
    Response response = client.target(adminUrl("/healthcheck")).request().get();
    assertEquals(HTTP_OK, response.getStatus());
    String body = response.readEntity(String.class);
    assertTrue(body.contains("nodeJsDist"));
  }

  @Test
  void testHealthCheckUnhealthy() {
    wireMock.resetAll();
    wireMock.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withStatus(503)));
    Response response = client.target(adminUrl("/healthcheck")).request().get();
    assertEquals(500, response.getStatus());
  }

  // ----- helpers -----

  private void assertPom(String artifactId, String version) {
    String path = "/org/nodejs/dist/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";
    Response response = client.target(url(path)).request().get();
    assertEquals(HTTP_OK, response.getStatus());
    assertEquals(MediaType.APPLICATION_XML, response.getMediaType().toString());
    String xml = response.readEntity(String.class);
    assertTrue(xml.contains("<groupId>org.nodejs.dist</groupId>"));
    assertTrue(xml.contains("<artifactId>" + artifactId + "</artifactId>"));
    assertTrue(xml.contains("<version>" + version + "</version>"));

    Response sha1Response = client.target(url(path + ".sha1")).request().get();
    assertEquals(HTTP_OK, sha1Response.getStatus());
    assertEquals(MediaType.TEXT_PLAIN, sha1Response.getMediaType().toString());
    assertEquals(DigestUtils.sha1Hex(xml), sha1Response.readEntity(String.class));
  }

  private void assertBinaryDownload(String version, String target, String upstreamPath) {
    byte[] expected = binaryContent(upstreamPath);
    String path = "/org/nodejs/dist/nodejs-binaries/" + version + "/nodejs-binaries-" + version + target;

    Response response = client.target(url(path)).request().get();
    assertEquals(HTTP_OK, response.getStatus(), "HTTP status " + path);
    assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getMediaType().toString(), "Media type " + path);
    byte[] body = response.readEntity(byte[].class);
    assertArrayEquals(expected, body, "Body " + path);
    assertNotNull(response.getHeaderString("Content-Length"), "Content-Length " + path);
    assertEquals(expected.length, response.getLength(), "Content-Length value " + path);

    assertSha1Binary(path, expected);
  }

  private void assertSha1Binary(String path, byte[] expected) {
    Response sha1Response = client.target(url(path + ".sha1")).request().get();
    assertEquals(HTTP_OK, sha1Response.getStatus(), "HTTP status " + path + ".sha1");
    assertEquals(MediaType.TEXT_PLAIN, sha1Response.getMediaType().toString(), "Media type " + path + ".sha1");
    assertEquals(DigestUtils.sha1Hex(expected), sha1Response.readEntity(String.class), "SHA-1 " + path);
  }

  private void stubAllUpstream() {
    stubHealthy();
    stubNodeJsModern(NODEJS_VERSION);
    stubNodeJsLegacy(NODEJS_LEGACY_VERSION);
    stubNpm(NPM_VERSION);
  }

  private void stubHealthy() {
    wireMock.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200).withBody("ok")));
  }

  private void stubNodeJsModern(String version) {
    Map<String, String> binaries = new LinkedHashMap<>();
    binaries.put("/v" + version + "/node-v" + version + "-linux-x64.tar.gz", "node-v" + version + "-linux-x64.tar.gz");
    binaries.put("/v" + version + "/node-v" + version + "-darwin-x64.tar.gz", "node-v" + version + "-darwin-x64.tar.gz");
    binaries.put("/v" + version + "/node-v" + version + "-win-x64.zip", "node-v" + version + "-win-x64.zip");
    binaries.put("/v" + version + "/node-v" + version + "-win-x86.zip", "node-v" + version + "-win-x86.zip");
    binaries.put("/v" + version + "/win-x64/node.exe", "win-x64/node.exe");
    binaries.put("/v" + version + "/win-x86/node.exe", "win-x86/node.exe");
    stubNodeJsVersion(version, binaries);
  }

  private void stubNodeJsLegacy(String version) {
    Map<String, String> binaries = new LinkedHashMap<>();
    binaries.put("/v" + version + "/node.exe", "node.exe");
    binaries.put("/v" + version + "/x64/node.exe", "x64/node.exe");
    stubNodeJsVersion(version, binaries);
  }

  private void stubNodeJsVersion(String version, Map<String, String> binaries) {
    StringBuilder shasums = new StringBuilder();
    for (Map.Entry<String, String> entry : binaries.entrySet()) {
      String upstreamPath = entry.getKey();
      byte[] content = binaryContent(upstreamPath);
      wireMock.stubFor(get(urlEqualTo(upstreamPath)).willReturn(aResponse().withStatus(200).withBody(content)));
      shasums.append(DigestUtils.sha256Hex(content)).append("  ").append(entry.getValue()).append("\n");
    }
    String shasumsPath = "/v" + version + "/SHASUMS256.txt";
    wireMock.stubFor(get(urlEqualTo(shasumsPath))
      .willReturn(aResponse().withStatus(200).withBody(shasums.toString())));
    wireMock.stubFor(head(urlEqualTo(shasumsPath)).willReturn(aResponse().withStatus(200)));
  }

  private void stubNpm(String version) {
    String path = "/npm/npm-" + version + ".tgz";
    wireMock.stubFor(get(urlEqualTo(path))
      .willReturn(aResponse().withStatus(200).withBody(binaryContent(path))));
    wireMock.stubFor(head(urlEqualTo(path)).willReturn(aResponse().withStatus(200)));
  }

  /**
   * Generates deterministic binary content for the given seed so that checksums are reproducible across stub
   * setup and assertions.
   */
  private static byte[] binaryContent(String seed) {
    String base = "BINARY:" + seed + ":";
    StringBuilder sb = new StringBuilder();
    while (sb.length() < 512) {
      sb.append(base);
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String url(String path) {
    return "http://localhost:" + APP.getLocalPort() + path;
  }

  private static String adminUrl(String path) {
    return "http://localhost:" + APP.getAdminPort() + path;
  }

}
