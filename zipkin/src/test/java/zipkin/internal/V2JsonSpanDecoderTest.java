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
package zipkin.internal;

import org.junit.Test;
import zipkin.SpanDecoder;
import zipkin.internal.v2.Span;
import zipkin.internal.v2.codec.BytesEncoder;
import zipkin.internal.v2.codec.BytesMessageEncoder;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static zipkin.TestObjects.LOTS_OF_SPANS;

public class V2JsonSpanDecoderTest {
  zipkin.Span span1 = ApplyTimestampAndDuration.apply(LOTS_OF_SPANS[0]);
  zipkin.Span span2 = ApplyTimestampAndDuration.apply(LOTS_OF_SPANS[1]);
  Span span2_1 = V2SpanConverter.fromSpan(span1).get(0);
  Span span2_2 = V2SpanConverter.fromSpan(span2).get(0);

  SpanDecoder decoder = new V2JsonSpanDecoder();

  @Test(expected = UnsupportedOperationException.class) public void readSpan() {
    decoder.readSpan(BytesEncoder.JSON.encode(span2_1));
  }

  @Test public void readSpans() {
    byte[] message = BytesMessageEncoder.JSON_TO_BYTES.encode(asList(
      BytesEncoder.JSON.encode(span2_1),
      BytesEncoder.JSON.encode(span2_2)
    ));

    assertThat(decoder.readSpans(message))
      .containsExactly(span1, span2);
  }
}
