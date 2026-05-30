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
package io.wcm.devops.maven.nodejsproxy.health;

import io.wcm.devops.maven.nodejsproxy.MavenProxyConfiguration;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

/**
 * Checks if the NodeJS root directory is available via HTTP.
 */
public class NodeJsDistHealthCheck extends HealthCheck {

  private final MavenProxyConfiguration config;
  private final CloseableHttpClient httpClient;

  private static final Logger log = LoggerFactory.getLogger(NodeJsDistHealthCheck.class);

  /**
   * @param config Configuration
   */
  public NodeJsDistHealthCheck(MavenProxyConfiguration config, CloseableHttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
  }

  @Override
  protected Result check() throws Exception {
    String url = config.getNodeJsBinariesRootUrl();

    log.info("Validate file: {}", url);
    HttpGet get = new HttpGet(url);
    return httpClient.execute(get, response -> {
      EntityUtils.consume(response.getEntity());
      if (response.getCode() == HttpServletResponse.SC_OK) {
        return Result.healthy();
      }
      return Result.unhealthy("Got status code " + response.getCode()
          + " when accessing URL " + url);
    });
  }

}
