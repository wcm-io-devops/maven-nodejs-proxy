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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

/**
 * Spool binary data from HTTP response to JAX-RS output.
 */
class SpoolStreamingOutput implements StreamingOutput {

  private final HttpEntity httpEntity;

  public SpoolStreamingOutput(HttpEntity httpEntity) {
    this.httpEntity = httpEntity;
  }

  @Override
  public void write(OutputStream os) throws IOException, WebApplicationException {
    try (InputStream is = httpEntity.getContent()) {
      IOUtils.copyLarge(is, os);
    }
    catch (IOException ex) {
      // ignore
    }
    finally {
      EntityUtils.consumeQuietly(httpEntity);
    }
  }

}
