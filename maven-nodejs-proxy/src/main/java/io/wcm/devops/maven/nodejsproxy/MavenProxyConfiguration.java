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
import io.dropwizard.client.HttpClientConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

  @Valid
  @NotNull
  private HttpClientConfiguration httpClient = new HttpClientConfiguration();

  @JsonProperty
  public String getGroupId() {
    return this.groupId;
  }

  @JsonProperty
  public String getNodeJsArtifactId() {
    return this.nodeJsArtifactId;
  }

  @JsonProperty
  public String getNpmArtifactId() {
    return this.npmArtifactId;
  }

  @JsonProperty
  public String getNodeJsBinariesRootUrl() {
    return this.nodeJsBinariesRootUrl;
  }

  @JsonProperty
  public String getNodeJsBinariesUrl() {
    return this.nodeJsBinariesUrl;
  }

  @JsonProperty
  public String getNodeJsBinariesUrlWindows() {
    return this.nodeJsBinariesUrlWindows;
  }

  @JsonProperty
  public String getNodeJsBinariesUrlWindowsX86() {
    return this.nodeJsBinariesUrlWindowsX86;
  }

  @JsonProperty
  public String getNpmBinariesUrl() {
    return this.npmBinariesUrl;
  }

  @JsonProperty
  public String getNodeJsChecksumUrl() {
    return this.nodeJsChecksumUrl;
  }

  @JsonProperty("httpClient")
  public HttpClientConfiguration getHttpClient() {
    return httpClient;
  }

}
