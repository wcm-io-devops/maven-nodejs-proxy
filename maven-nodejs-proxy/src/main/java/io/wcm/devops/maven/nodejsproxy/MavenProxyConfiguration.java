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

import io.dropwizard.Configuration;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

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
  private String nodeJsBinariesUrlWindowsX86;
  @NotEmpty
  private String npmBinariesUrl;
  @NotEmpty
  private String nodeJsChecksumUrl;
  private int httpClientConnectTimeout = 5000;
  private int httpClientSocketTimeout = 15000;

  @JsonProperty
  public String getGroupId() {
    return this.groupId;
  }

  @JsonProperty
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  @JsonProperty
  public String getNodeJsArtifactId() {
    return this.nodeJsArtifactId;
  }

  @JsonProperty
  public void setNodeJsArtifactId(String nodeJsArtifactId) {
    this.nodeJsArtifactId = nodeJsArtifactId;
  }

  @JsonProperty
  public String getNpmArtifactId() {
    return this.npmArtifactId;
  }

  @JsonProperty
  public void setNpmArtifactId(String npmArtifactId) {
    this.npmArtifactId = npmArtifactId;
  }

  @JsonProperty
  public String getNodeJsBinariesRootUrl() {
    return this.nodeJsBinariesRootUrl;
  }

  @JsonProperty
  public void setNodeJsBinariesRootUrl(String nodeJsBinariesRootUrl) {
    this.nodeJsBinariesRootUrl = nodeJsBinariesRootUrl;
  }

  @JsonProperty
  public String getNodeJsBinariesUrl() {
    return this.nodeJsBinariesUrl;
  }

  @JsonProperty
  public void setNodeJsBinariesUrl(String nodeJsBinariesUrl) {
    this.nodeJsBinariesUrl = nodeJsBinariesUrl;
  }

  @JsonProperty
  public String getNodeJsBinariesUrlWindows() {
    return this.nodeJsBinariesUrlWindows;
  }

  @JsonProperty
  public void setNodeJsBinariesUrlWindows(String nodeJsBinariesUrlWindows) {
    this.nodeJsBinariesUrlWindows = nodeJsBinariesUrlWindows;
  }

  @JsonProperty
  public String getNodeJsBinariesUrlWindowsX86() {
    return this.nodeJsBinariesUrlWindowsX86;
  }

  @JsonProperty
  public void setNodeJsBinariesUrlWindowsX86(String nodeJsBinariesUrlWindowsX86) {
    this.nodeJsBinariesUrlWindowsX86 = nodeJsBinariesUrlWindowsX86;
  }

  @JsonProperty
  public String getNpmBinariesUrl() {
    return this.npmBinariesUrl;
  }

  @JsonProperty
  public void setNpmBinariesUrl(String npmBinariesUrl) {
    this.npmBinariesUrl = npmBinariesUrl;
  }
  @JsonProperty
  public String getNodeJsChecksumUrl() {
    return this.nodeJsChecksumUrl;
  }

  @JsonProperty
  public void setNodeJsChecksumUrl(String nodeJsChecksumUrl) {
    this.nodeJsChecksumUrl = nodeJsChecksumUrl;
  }

  @JsonProperty
  public int getHttpClientConnectTimeout() {
    return this.httpClientConnectTimeout;
  }

  @JsonProperty
  public void setHttpClientConnectTimeout(int httpClientConnectTimeout) {
    this.httpClientConnectTimeout = httpClientConnectTimeout;
  }

  @JsonProperty
  public int getHttpClientSocketTimeout() {
    return this.httpClientSocketTimeout;
  }

  @JsonProperty
  public void setHttpClientSocketTimeout(int httpClientSocketTimeout) {
    this.httpClientSocketTimeout = httpClientSocketTimeout;
  }

}
