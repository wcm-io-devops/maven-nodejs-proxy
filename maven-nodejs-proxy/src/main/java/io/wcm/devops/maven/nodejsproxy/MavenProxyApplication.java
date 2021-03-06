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

import io.dropwizard.Application;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.wcm.devops.maven.nodejsproxy.health.NodeJsDistHealthCheck;
import io.wcm.devops.maven.nodejsproxy.resource.MavenProxyResource;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Dropwizard Application for Maven NodeJS Proxy.
 */
public class MavenProxyApplication extends Application<MavenProxyConfiguration> {

  @Override
  public String getName() {
    return "maven-nodejs-proxy";
  }

  @Override
  public void initialize(Bootstrap<MavenProxyConfiguration> bootstrap) {
    // nothing to do yet
  }

  @Override
  public void run(MavenProxyConfiguration config, Environment environment) {
    final CloseableHttpClient httpClient = new HttpClientBuilder(environment)
    .using(config.getHttpClient())
    .build("default");

    final MavenProxyResource resource = new MavenProxyResource(config, httpClient);

    final NodeJsDistHealthCheck healthCheck = new NodeJsDistHealthCheck(config, httpClient);
    environment.healthChecks().register("nodeJsDist", healthCheck);

    environment.jersey().register(resource);
  }

  //CHECKSTYLE:OFF
  public static void main(String[] args) throws Exception {
    new MavenProxyApplication().run(args);
  }
  //CHECKSTYLE:ON

}
