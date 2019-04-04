/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2016 wcm.io
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

import java.io.File;
import java.io.IOException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.wcm.devops.maven.nodejsproxy.MavenProxyConfiguration;

final class TestContext {

  private static final ObjectMapper OBJECT_MAPPER = Jackson.newObjectMapper();

  private TestContext() {
    // static methods only
  }

  static MavenProxyConfiguration getConfiguration() {
    ConfigurationFactory factory = new YamlConfigurationFactory<MavenProxyConfiguration>(
        MavenProxyConfiguration.class, null, OBJECT_MAPPER, "override");
    try {
      File configFile = new File("config.yml");
      if (!configFile.exists()) {
        throw new RuntimeException("Configuration file not found: " + configFile.getCanonicalPath());
      }
      return (MavenProxyConfiguration)factory.build(new File("config.yml"));
    }
    catch (IOException | ConfigurationException ex) {
      throw new RuntimeException(ex);
    }
  }

  static CloseableHttpClient getHttpClient() {
    return HttpClients.createDefault();
  }

}
