/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.devops.maven.nodejsproxy.resource;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;

import io.wcm.devops.maven.nodejsproxy.MavenProxyConfiguration;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Proxies NodeJS binaries.
 */
@Path("/")
public class MavenProxyResource {

  private final MavenProxyConfiguration config;
  private final CloseableHttpClient httpClient;

  private static final String VERSION_PLACEHOLDER = "${version}";
  private static final String SHA1_SUFFIX = ".sha1";

  private static final Logger log = LoggerFactory.getLogger(MavenProxyResource.class);

  /**
   * Creates a new resource instance.
   * @param config Configuration
   * @param httpClient HTTP client
   */
  public MavenProxyResource(MavenProxyConfiguration config, CloseableHttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
  }

  /**
   * Shows index page
   * @return HTML index page
   */
  @GET
  @Path("/")
  @Timed
  @Produces(MediaType.TEXT_HTML)
  public String getIndex() {
    return IndexPageBuilder.build(config);
  }

  /**
   * Maps POM requests simulating a Maven 2 directory structure.
   * @param groupIdPath Group ID path
   * @param artifactId Artifact ID
   * @param version Version
   * @param artifactIdFilename Artifact ID from the file name
   * @param versionFilename Version from the file name
   * @param fileExtension File extension
   * @return Response
   * @throws IOException I/O exception
   */
  @GET
  @Path("{groupIdPath:[a-zA-Z0-9\\-\\_]+(/[a-zA-Z0-9\\-\\_]+)*}"
      + "/{artifactId:[a-zA-Z0-9\\-\\_\\.]+}"
      + "/{version:\\d+(\\.\\d+)*}"
      + "/{artifactIdFilename:[a-zA-Z0-9\\-\\_\\.]+}"
      + "-{versionFilename:\\d+(\\.\\d+)*}"
      + ".{fileExtension:pom(\\.sha1)?}")
  @Timed
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Response getPom(
      @PathParam("groupIdPath") String groupIdPath,
      @PathParam("artifactId") String artifactId,
      @PathParam("version") String version,
      @PathParam("artifactIdFilename") String artifactIdFilename,
      @PathParam("versionFilename") String versionFilename,
      @PathParam("fileExtension") String fileExtension) throws IOException {

    String groupId = mapGroupId(groupIdPath);
    if (!validateBasicParams(groupId, artifactId, version, artifactIdFilename, versionFilename)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    ArtifactType artifactType = getArtifactType(artifactId);

    // validate that version exists
    String url = getPomValidateUrl(artifactType, version);
    log.info("Validate file: {}", url);
    HttpHead get = new HttpHead(url);
    int statusCode = httpClient.execute(get, response -> {
      EntityUtils.consume(response.getEntity());
      return response.getCode();
    });
    if (statusCode != HttpServletResponse.SC_OK) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    String xml = PomBuilder.build(groupId, artifactId, version);

    if (Strings.CS.equals(fileExtension, "pom")) {
      return Response.ok(xml)
        .type(MediaType.APPLICATION_XML)
        .build();
    }
    if (Strings.CS.equals(fileExtension, "pom.sha1")) {
      return Response.ok(DigestUtils.sha1Hex(xml))
        .type(MediaType.TEXT_PLAIN)
        .build();
    }
    return Response.status(Response.Status.NOT_FOUND).build();
  }

  /**
   * Maps all requests to NodeJS binaries simulating a Maven 2 directory structure.
   * @param groupIdPath Group ID path
   * @param artifactId Artifact ID
   * @param version Version
   * @param artifactIdFilename Artifact ID from the file name
   * @param versionFilename Version from the file name
   * @param os Operating system
   * @param arch Architecture
   * @param type File type
   * @return Response
   * @throws IOException I/O exception
   */
  @GET
  @Path("{groupIdPath:[a-zA-Z0-9\\-\\_]+(/[a-zA-Z0-9\\-\\_]+)*}"
      + "/{artifactId:[a-zA-Z0-9\\-\\_\\.]+}"
      + "/{version:\\d+(\\.\\d+)*}"
      + "/{artifactIdFilename:[a-zA-Z0-9\\-\\_\\.]+}"
      + "-{versionFilename:\\d+(\\.\\d+)*}"
      + "-{os:[a-zA-Z0-9\\_]+}"
      + "-{arch:[a-zA-Z0-9\\_]+}"
      + ".{type:[a-z]+(\\.[a-z]+)*(\\.sha1)?}")
  @Timed
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Response getBinary(
      @PathParam("groupIdPath") String groupIdPath,
      @PathParam("artifactId") String artifactId,
      @PathParam("version") String version,
      @PathParam("artifactIdFilename") String artifactIdFilename,
      @PathParam("versionFilename") String versionFilename,
      @PathParam("os") String os,
      @PathParam("arch") String arch,
      @PathParam("type") String type) throws IOException {

    String groupId = mapGroupId(groupIdPath);
    if (!validateBasicParams(groupId, artifactId, version, artifactIdFilename, versionFilename)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    ArtifactType artifactType = getArtifactType(artifactId);
    if (artifactType != ArtifactType.NODEJS) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    boolean getChecksum = false;
    if (Strings.CS.endsWith(type, SHA1_SUFFIX)) {
      getChecksum = true;
    }

    String url = buildBinaryUrl(artifactType, version, os, arch, Strings.CS.removeEnd(type, SHA1_SUFFIX));
    return getBinaryWithChecksumValidation(url, version, getChecksum);
  }

  /**
   * Maps all requests to NPM binaries simulating a Maven 2 directory structure.
   * @param groupIdPath Group ID path
   * @param artifactId Artifact ID
   * @param version Version
   * @param artifactIdFilename Artifact ID from the file name
   * @param versionFilename Version from the file name
   * @param type File type
   * @return Response
   * @throws IOException I/O exception
   */
  @GET
  @Path("{groupIdPath:[a-zA-Z0-9\\-\\_]+(/[a-zA-Z0-9\\-\\_]+)*}"
      + "/{artifactId:[a-zA-Z0-9\\-\\_\\.]+}"
      + "/{version:\\d+(\\.\\d+)*}"
      + "/{artifactIdFilename:[a-zA-Z0-9\\-\\_\\.]+}"
      + "-{versionFilename:\\d+(\\.\\d+)*}"
      + ".{type:[a-z]+(\\.[a-z]+)*(\\.sha1)?}")
  @Timed
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Response getBinary(
      @PathParam("groupIdPath") String groupIdPath,
      @PathParam("artifactId") String artifactId,
      @PathParam("version") String version,
      @PathParam("artifactIdFilename") String artifactIdFilename,
      @PathParam("versionFilename") String versionFilename,
      @PathParam("type") String type) throws IOException {

    String groupId = mapGroupId(groupIdPath);
    if (!validateBasicParams(groupId, artifactId, version, artifactIdFilename, versionFilename)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    ArtifactType artifactType = getArtifactType(artifactId);
    if (artifactType != ArtifactType.NPM) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    boolean getChecksum = false;
    if (Strings.CS.endsWith(type, SHA1_SUFFIX)) {
      getChecksum = true;
    }

    String url = buildBinaryUrl(artifactType, version, null, null, Strings.CS.removeEnd(type, SHA1_SUFFIX));
    return getBinary(url, getChecksum, null);
  }

  private Response getBinaryWithChecksumValidation(String url, String version, boolean getChecksum) throws IOException {
    // get original checksum from source directory
    Checksums checksums = getChecksums(version);
    if (checksums == null) {
      log.info("File not found: {} - no checksum file found.", url);
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    String checksum = checksums.get(url);
    if (checksum == null) {
      log.info("File not found: {} - no checksum found in checkum file.", url);
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    return getBinary(url, getChecksum, checksum);
  }

  private Response getBinary(String url, boolean getChecksum, String expectedChecksum) throws IOException {
    log.info("Proxy file: {}", url);
    HttpGet get = new HttpGet(url);
    BinaryResponse binaryResponse = httpClient.execute(get, response -> {
      if (response.getCode() != HttpServletResponse.SC_OK) {
        EntityUtils.consume(response.getEntity());
        return null;
      }
      byte[] data = EntityUtils.toByteArray(response.getEntity());
      String contentLength = response.containsHeader(CONTENT_LENGTH) ? response.getFirstHeader(CONTENT_LENGTH).getValue() : null;
      return new BinaryResponse(data, contentLength);
    });
    if (binaryResponse == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    byte[] data = binaryResponse.data();

    // validate checksum
    if (expectedChecksum != null) {
      String remoteChecksum = DigestUtils.sha256Hex(data);
      if (!Strings.CS.equals(expectedChecksum, remoteChecksum)) {
        log.warn("Reject file: {} - checksum comparison failed - expected: {}, actual: {}", url, expectedChecksum, remoteChecksum);
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    }

    if (getChecksum) {
      return Response.ok(DigestUtils.sha1Hex(data))
        .type(MediaType.TEXT_PLAIN)
        .build();
    }
    return Response.ok(data)
      .type(MediaType.APPLICATION_OCTET_STREAM)
      .header(CONTENT_LENGTH, binaryResponse.contentLength())
      .build();
  }

  /**
   * Holds the downloaded binary data together with the upstream Content-Length header value.
   * This is only used as a simple internal value holder and is never compared or used as a map key,
   * so the default array-identity-based equals/hashCode/toString are sufficient.
   */
  @SuppressWarnings("java:S6218") // no need for content-aware equals/hashCode/toString for this internal value holder
  private record BinaryResponse(byte[] data, String contentLength) {
    // value holder
  }

  private String mapGroupId(String groupIdPath) {
    return Strings.CS.replace(groupIdPath, "/", ".");
  }

  /**
   * Validate that group/artifactid are correct and version is consistent within the path.
   */
  private boolean validateBasicParams(
      String groupId,
      String artifactId,
      String version,
      String artifactIdFilename,
      String versionFilename) {
    return Strings.CS.equals(artifactId, artifactIdFilename)
        && Strings.CS.equals(version, versionFilename)
        && Strings.CS.equals(groupId, config.getGroupId())
        && (Strings.CS.equals(artifactId, config.getNodeJsArtifactId())
            || Strings.CS.equals(artifactId, config.getNpmArtifactId()));
  }

  private ArtifactType getArtifactType(String artifactId) {
    if (Strings.CS.equals(artifactId, config.getNodeJsArtifactId())) {
      return ArtifactType.NODEJS;
    }
    if (Strings.CS.equals(artifactId, config.getNpmArtifactId())) {
      return ArtifactType.NPM;
    }
    throw new IllegalArgumentException("Invalid artifactId: " + artifactId);
  }

  private Checksums getChecksums(String version) throws IOException {
    String url = config.getNodeJsBinariesRootUrl()
        + Strings.CS.replace(config.getNodeJsChecksumUrl(), VERSION_PLACEHOLDER, version);
    log.info("Get file: {}", url);
    HttpGet get = new HttpGet(url);
    return httpClient.execute(get, response -> {
      if (response.getCode() == HttpServletResponse.SC_OK) {
        return new Checksums(EntityUtils.toString(response.getEntity()));
      }
      EntityUtils.consume(response.getEntity());
      return null;
    });
  }

  private String getPomValidateUrl(ArtifactType artifactType, String version) {
    switch (artifactType) {
      case NODEJS:
        return config.getNodeJsBinariesRootUrl()
            + Strings.CS.replace(config.getNodeJsChecksumUrl(), VERSION_PLACEHOLDER, version);
      case NPM:
        return config.getNodeJsBinariesRootUrl()
            + Strings.CS.replace(Strings.CS.replace(config.getNpmBinariesUrl(), VERSION_PLACEHOLDER, version), "${type}", "tgz");
      default:
        throw new IllegalArgumentException("Illegal artifact type: " + artifactType);
    }
  }

  private String buildBinaryUrl(ArtifactType artifactType, String version, String os, String arch, String type) {
    String url;
    switch (artifactType) {
      case NODEJS:
        if (Strings.CS.equals(os, "windows") && Strings.CS.equals(type, "exe")) {
          if (isVersion4Up(version)) {
            url = config.getNodeJsBinariesUrlWindows();
          }
          else if (Strings.CS.equals(arch, "x86")) {
            url = config.getNodeJsBinariesUrlWindowsX86Legacy();
          }
          else {
            url = config.getNodeJsBinariesUrlWindowsX64Legacy();
          }
        }
        else {
          url = config.getNodeJsBinariesUrl();
        }
        break;
      case NPM:
        url = config.getNpmBinariesUrl();
        break;
      default:
        throw new IllegalArgumentException("Illegal artifact type: " + artifactType);
    }
    url = config.getNodeJsBinariesRootUrl() + url;
    url = Strings.CS.replace(url, VERSION_PLACEHOLDER, StringUtils.defaultString(version));
    url = Strings.CS.replace(url, "${os}", StringUtils.defaultString(Strings.CS.replace(os, "windows", "win")));
    url = Strings.CS.replace(url, "${arch}", StringUtils.defaultString(arch));
    url = Strings.CS.replace(url, "${type}", StringUtils.defaultString(type));
    return url;
  }

  private boolean isVersion4Up(String version) {
    DefaultArtifactVersion givenVersion = new DefaultArtifactVersion(version);
    DefaultArtifactVersion minVersion = new DefaultArtifactVersion("4.0.0");
    return givenVersion.compareTo(minVersion) >= 0;
  }

}
