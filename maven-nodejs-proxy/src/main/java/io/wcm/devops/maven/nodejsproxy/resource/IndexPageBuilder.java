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

import io.wcm.devops.maven.nodejsproxy.MavenProxyConfiguration;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds HTML index page
 */
public final class IndexPageBuilder {

  private static final String[] EXAMPLE_URLS = new String[] {
    "${groupIdPath}/${nodeJsArtifactId}/0.12.0/${nodeJsArtifactId}-0.12.0.pom",
    "${groupIdPath}/${nodeJsArtifactId}/0.12.0/${nodeJsArtifactId}-0.12.0-windows-x86.exe",
    "${groupIdPath}/${nodeJsArtifactId}/0.12.0/${nodeJsArtifactId}-0.12.0-windows-x64.exe",
    "${groupIdPath}/${nodeJsArtifactId}/0.12.0/${nodeJsArtifactId}-0.12.0-linux-x86.tar.gz",
    "${groupIdPath}/${nodeJsArtifactId}/0.12.0/${nodeJsArtifactId}-0.12.0-linux-x64.tar.gz",
    "${groupIdPath}/${nodeJsArtifactId}/0.12.0/${nodeJsArtifactId}-0.12.0-darwin-x86.tar.gz",
    "${groupIdPath}/${nodeJsArtifactId}/0.12.0/${nodeJsArtifactId}-0.12.0-darwin-x64.tar.gz",
    "${groupIdPath}/${npmArtifactId}/1.4.9/${npmArtifactId}-1.4.9.pom",
    "${groupIdPath}/${npmArtifactId}/1.4.9/${npmArtifactId}-1.4.9.tgz"
  };

  private IndexPageBuilder() {
    // static methods only
  }

  /**
   * Build HTML index page
   */
  public static String build(MavenProxyConfiguration config) {
    StringBuilder exampleUrlsMarkup = new StringBuilder();
    for (String exampleUrl : EXAMPLE_URLS) {
      String url = exampleUrl;
      url = StringUtils.replace(url, "${groupIdPath}", StringUtils.replace(config.getGroupId(), ".", "/"));
      url = StringUtils.replace(url, "${nodeJsArtifactId}", config.getNodeJsArtifactId());
      url = StringUtils.replace(url, "${npmArtifactId}", config.getNpmArtifactId());
      exampleUrlsMarkup.append("<li><a href=\"").append(url).append("\">").append(url).append("</a></li>");

    }

    String serviceVersion = IndexPageBuilder.class.getPackage().getImplementationVersion();

    return "<html>"
    + "<head><title>Maven NodeJS Proxy</title></head>"
    + "<body>"
    + "<h1>Maven NodeJS Proxy</h1>"
    + "<p>This is a Maven Artifact Proxy for NodeJS binaries located at: "
    + "<a href=\"" + config.getNodeJsBinariesRootUrl() + "\">" + config.getNodeJsBinariesRootUrl() + "</a></p>"
    + "<p>Every call to this repository is routed directly to this URL.</p>"
    + "<p><strong>Please never use this Maven repository directly in your maven builds, but only via an Repository Manager "
    + "which caches the resolved artifacts.</strong></p>"
    + "<p>If you want to setup your own proxy get the source code:"
    + "<a href=\"https://github.com/wcm-io-devops/maven-nodejs-proxy\">https://github.com/wcm-io-devops/maven-nodejs-proxy</a></p>"
    + "<hr/>"
    + "<p>Examples:</p>"
    + "<ul>"
    + exampleUrlsMarkup
    + "</ul>"
    + "<p>For all files SHA1 checksums are supported (.sha1 suffix). MD5 checksums are not supported.</p>"
    + (serviceVersion != null ? "<hr/><p>Version " + IndexPageBuilder.class.getPackage().getImplementationVersion() + ".</p>" : "")
    + "</body>"
    + "</html>";
  }

}