/**
 * Copyright 2015-2017 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin.internal.v2.codec;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import zipkin.internal.Util;

import static org.assertj.core.api.Assertions.assertThat;

public class BytesMessageEncoderTest {

  @Test public void emptyList_jsonBytes() throws IOException {
    List<byte[]> encoded = Arrays.asList();
    assertThat(BytesMessageEncoder.JSON_TO_BYTES.encode(encoded))
      .isEqualTo("[]".getBytes(Util.UTF_8));
  }

  @Test public void singletonList_jsonBytes() throws IOException {
    List<byte[]> encoded = Arrays.asList(new byte[] {'1'});
    assertThat(BytesMessageEncoder.JSON_TO_BYTES.encode(encoded))
      .isEqualTo("[1]".getBytes(Util.UTF_8));
  }

  @Test public void multiItemList_jsonBytes() throws IOException {
    List<byte[]> encoded = Arrays.asList(new byte[] {'3'}, new byte[] {'4'}, new byte[] {'5'});
    assertThat(BytesMessageEncoder.JSON_TO_BYTES.encode(encoded))
      .isEqualTo("[3,4,5]".getBytes(Util.UTF_8));
  }
}
