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
package io.wcm.devops.maven.nodejsproxy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.core.Configuration;

/**
 * Configuration for Maven NodeJS Proxy.
 */
public class MavenProxyConfiguration extends Configuration {

  @NotEmpty
  private String groupId;
  @NotEmpty
  private String nodeJsArtifactId;
  @NotEmpty
  private String npmArtifactId;
  @NotEmpty
  private String nodeJsBinariesRootUrl;
  @NotEmpty
  private String nodeJsBinariesUrl;
  @NotEmpty
  private String nodeJsBinariesUrlWindows;
  @NotEmpty
  private String nodeJsBinariesUrlWindowsX86Legacy;
  @NotEmpty
  private String nodeJsBinariesUrlWindowsX64Legacy;
  @NotEmpty
  private String npmBinariesUrl;
  @NotEmpty
  private String nodeJsChecksumUrl;
  @NotEmpty
  private String nodeJsSampleVersion;
  @NotEmpty
  private String npmSampleVersion;

  @Valid
  @NotNull
  private HttpClientConfiguration httpClient = new HttpClientConfiguration();

  /**
   * Gets the Maven group ID.
   * @return Maven group ID
   */
  @JsonProperty
  public String getGroupId() {
    return this.groupId;
  }

  /**
   * Gets the Maven artifact ID for NodeJS.
   * @return Maven artifact ID for NodeJS
   */
  @JsonProperty
  public String getNodeJsArtifactId() {
    return this.nodeJsArtifactId;
  }

  /**
   * Gets the Maven artifact ID for NPM.
   * @return Maven artifact ID for NPM
   */
  @JsonProperty
  public String getNpmArtifactId() {
    return this.npmArtifactId;
  }

  /**
   * Gets the root URL for the NodeJS binaries.
   * @return Root URL for the NodeJS binaries
   */
  @JsonProperty
  public String getNodeJsBinariesRootUrl() {
    return this.nodeJsBinariesRootUrl;
  }

  /**
   * Gets the URL pattern for the NodeJS binaries.
   * @return URL pattern for the NodeJS binaries
   */
  @JsonProperty
  public String getNodeJsBinariesUrl() {
    return this.nodeJsBinariesUrl;
  }

  /**
   * Gets the URL pattern for the NodeJS Windows binaries.
   * @return URL pattern for the NodeJS Windows binaries
   */
  @JsonProperty
  public String getNodeJsBinariesUrlWindows() {
    return this.nodeJsBinariesUrlWindows;
  }

  /**
   * Gets the URL pattern for the legacy NodeJS Windows x86 binaries.
   * @return URL pattern for the legacy NodeJS Windows x86 binaries
   */
  @JsonProperty
  public String getNodeJsBinariesUrlWindowsX86Legacy() {
    return this.nodeJsBinariesUrlWindowsX86Legacy;
  }

  /**
   * Gets the URL pattern for the legacy NodeJS Windows x64 binaries.
   * @return URL pattern for the legacy NodeJS Windows x64 binaries
   */
  @JsonProperty
  public String getNodeJsBinariesUrlWindowsX64Legacy() {
    return this.nodeJsBinariesUrlWindowsX64Legacy;
  }

  /**
   * Gets the URL pattern for the NPM binaries.
   * @return URL pattern for the NPM binaries
   */
  @JsonProperty
  public String getNpmBinariesUrl() {
    return this.npmBinariesUrl;
  }

  /**
   * Gets the URL pattern for the NodeJS checksum file.
   * @return URL pattern for the NodeJS checksum file
   */
  @JsonProperty
  public String getNodeJsChecksumUrl() {
    return this.nodeJsChecksumUrl;
  }

  /**
   * Gets the sample NodeJS version.
   * @return Sample NodeJS version
   */
  @JsonProperty
  public String getNodeJsSampleVersion() {
    return this.nodeJsSampleVersion;
  }

  /**
   * Gets the sample NPM version.
   * @return Sample NPM version
   */
  @JsonProperty
  public String getNpmSampleVersion() {
    return this.npmSampleVersion;
  }

  /**
   * Gets the HTTP client configuration.
   * @return HTTP client configuration
   */
  @JsonProperty("httpClient")
  public HttpClientConfiguration getHttpClient() {
    return httpClient;
  }

}
