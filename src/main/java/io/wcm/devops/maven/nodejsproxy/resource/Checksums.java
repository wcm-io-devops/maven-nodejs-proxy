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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Parses checksums file
 */
public class Checksums {

  private final Map<String, String> checksums = new HashMap<>();

  private static final Pattern LEVEL_1_RELATIVE_NAME = Pattern.compile("^.*/([^/]+)$");
  private static final Pattern LEVEL_2_RELATIVE_NAME = Pattern.compile("^.*/([^/]+/[^/]+)$");

  /**
   * @param data Checksums file content
   */
  public Checksums(String data) {
    String[] lines = StringUtils.split(data, "\n");
    for (String line : lines) {
      String checksum = StringUtils.substringBefore(line, "  ");
      String filename = StringUtils.substringAfter(line, "  ");
      if (StringUtils.isNoneBlank(checksum, filename)) {
        checksums.put(filename, checksum);
      }
    }
  }

  /**
   * Get checksum
   * @param filename File URL. the filename and filename including last directory level is checked.
   * @return Checksum or null
   */
  public String get(String filename) {
    // try 2nd level first
    String checksum = get(filename, LEVEL_2_RELATIVE_NAME);
    if (checksum != null) {
      return checksum;
    }
    // then 1st level
    return get(filename, LEVEL_1_RELATIVE_NAME);
  }

  private String get(String filename, Pattern pattern) {
    Matcher matcher = pattern.matcher(filename);
    if (matcher.matches()) {
      String relativeFilename = matcher.group(1);
      return checksums.get(relativeFilename);
    }
    return null;
  }

}
