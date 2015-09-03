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

/**
 * Builds POM files
 */
public final class PomBuilder {

  private PomBuilder() {
    // static methods only
  }

  /**
   * Build POM
   * @param groupId
   * @param artifactId
   * @param version
   * @param packaging
   * @return POM file xml
   */
  public static String build(String groupId, String artifactId, String version, String packaging) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
        + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
        + "  <modelVersion>4.0.0</modelVersion>\n"
        + "  <groupId>" + groupId + "</groupId>\n"
        + "  <artifactId>" + artifactId + "</artifactId>\n"
        + "  <version>" + version + "</version>\n"
        + "  <packaging>pom</packaging>\n"
        + "</project>";
  }

}
