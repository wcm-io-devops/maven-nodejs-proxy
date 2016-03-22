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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;

import io.dropwizard.testing.junit.ResourceTestRule;


public class MavenProxyResourceTest {

  // test with the following NodeJS and NPM versions
  private static final String[] NODEJS_VERSIONS = {
      "0.12.0"
  };
  private static final String[] NODEJS_TARGETS = {
      "-windows-x86.exe",
      "-windows-x64.exe",
      "-linux-x86.tar.gz",
      "-linux-x64.tar.gz",
      "-darwin-x86.tar.gz",
      "-darwin-x64.tar.gz",
  };
  private static final String[] NPM_VERSIONS = {
      "1.4.9"
  };
  private static final String[] NPM_TARGETS = {
      ".tgz",
  };

  @Rule
  public ResourceTestRule context = new ResourceTestRule.Builder()
  .addResource(new MavenProxyResource(TestContext.getConfiguration(), TestContext.getHttpClient()))
  .build();

  @Test
  public void testGetIndex() {
    String path = "/";
    Response response = context.client().target(path).request().get();
    assertResponse(path, response, MediaType.TEXT_HTML);
  }

  @Test
  public void testGetPomNodeJS() {
    for (String version : NODEJS_VERSIONS) {
      String path = "/org/nodejs/dist/nodejs-binaries/" + version + "/nodejs-binaries-" + version + ".pom";
      Response response = context.client().target(path).request().get();
      assertResponse(path, response, MediaType.APPLICATION_XML);
      assertTrue("Content length " + path, response.getLength() > 0);
      assertSHA1(path, response);
    }
  }

  @Test
  public void testGetPomNPM() {
    for (String version : NPM_VERSIONS) {
      String path = "/org/nodejs/dist/npm-binaries/" + version + "/npm-binaries-" + version + ".pom";
      Response response = context.client().target(path).request().get();
      assertResponse(path, response, MediaType.APPLICATION_XML);
      assertTrue("Content length " + path, response.getLength() > 0);
      assertSHA1(path, response);
    }
  }

  @Test
  public void testGetBinaryNodeJS() {
    for (String version : NODEJS_VERSIONS) {
      for (String target : NODEJS_TARGETS) {
        String path = "/org/nodejs/dist/nodejs-binaries/" + version + "/nodejs-binaries-" + version + target;
        Response response = context.client().target(path).request().get();
        assertResponse(path, response, MediaType.APPLICATION_OCTET_STREAM);
        assertSHA1(path, response);
      }
    }
  }

  @Test
  public void testGetBinaryNPM() {
    for (String version : NPM_VERSIONS) {
      for (String target : NPM_TARGETS) {
        String path = "/org/nodejs/dist/npm-binaries/" + version + "/npm-binaries-" + version + target;
        Response response = context.client().target(path).request().get();
        assertResponse(path, response, MediaType.APPLICATION_OCTET_STREAM);
        assertSHA1(path, response);
      }
    }
  }

  private void assertResponse(String path, Response response, String mediaType) {
    assertEquals("HTTP status " + path, HttpStatus.SC_OK, response.getStatus());
    assertEquals("Media type " + path, mediaType, response.getMediaType().toString());
    assertTrue(response.hasEntity());
  }

  private void assertSHA1(String path, Response dataResponse) {
    String sha1Path = path + ".sha1";
    Response sha1Response = context.client().target(sha1Path).request().get();
    assertResponse(sha1Path, sha1Response, MediaType.TEXT_PLAIN);

    try (InputStream is = dataResponse.readEntity(InputStream.class)) {
      byte[] data = IOUtils.toByteArray(is);
      String sha1 = sha1Response.readEntity(String.class);
      assertEquals(sha1, DigestUtils.sha1Hex(data));
    }
    catch (IOException ex) {
      throw new RuntimeException("Error checking SHA-1 of " + path, ex);
    }
  }

}
