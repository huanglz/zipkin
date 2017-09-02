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
package zipkin.collector;

import org.junit.Before;
import org.junit.Test;
import zipkin.SpanDecoder;
import zipkin.internal.ApplyTimestampAndDuration;
import zipkin.internal.DetectingSpanDecoder;
import zipkin.internal.Util;
import zipkin.internal.V2SpanConverter;
import zipkin.internal.V2StorageComponent;
import zipkin.internal.v2.Span;
import zipkin.internal.v2.codec.BytesEncoder;
import zipkin.internal.v2.codec.BytesMessageEncoder;
import zipkin.internal.v2.storage.SpanConsumer;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static zipkin.TestObjects.LOTS_OF_SPANS;
import static zipkin.storage.Callback.NOOP;

public class CollectorTest {
  zipkin.storage.StorageComponent storage = mock(zipkin.storage.StorageComponent.class);
  Collector collector;
  zipkin.Span span1 = ApplyTimestampAndDuration.apply(LOTS_OF_SPANS[0]);
  Span span2_1 = V2SpanConverter.fromSpan(span1).get(0);

  @Before public void setup() throws Exception {
    collector = spy(Collector.builder(Collector.class)
      .storage(storage).build());
    when(collector.idString(span1)).thenReturn("1"); // to make expectations easier to read
  }

  @Test public void unsampledSpansArentStored() {
    when(storage.asyncSpanConsumer()).thenThrow(new AssertionError());

    collector = Collector.builder(Collector.class)
      .sampler(CollectorSampler.create(0.0f))
      .storage(storage).build();

    collector.accept(asList(span1), NOOP);
  }

  @Test public void doesntCallDeprecatedSampleMethod() {
    CollectorSampler sampler = mock(CollectorSampler.class);
    when(sampler.isSampled(span1)).thenThrow(new AssertionError());

    collector = Collector.builder(Collector.class)
      .sampler(sampler)
      .storage(storage).build();

    collector.accept(asList(span1), NOOP);

    verify(sampler).isSampled(span1.traceId, span1.debug);
  }

  @Test public void errorDetectingFormat() {
    CollectorMetrics metrics = mock(CollectorMetrics.class);

    collector = Collector.builder(Collector.class)
      .metrics(metrics)
      .storage(storage).build();

    collector.acceptSpans("foo".getBytes(Util.UTF_8), new DetectingSpanDecoder(), NOOP);

    verify(metrics).incrementMessagesDropped();
  }

  @Test public void convertsSpan2Format() {
    byte[] bytes = BytesMessageEncoder.JSON_TO_BYTES.encode(asList(BytesEncoder.JSON.encode(span2_1)));
    collector.acceptSpans(bytes, SpanDecoder.DETECTING_DECODER, NOOP);

    verify(collector).acceptSpans(bytes, SpanDecoder.DETECTING_DECODER, NOOP);
    verify(collector).accept(asList(span1), NOOP);
  }

  /**
   * When a version 2 storage component is in use, route directly to it as opposed to
   * double-conversion.
   */
  @Test public void routesToSpan2Collector() {
    abstract class WithSpan2 extends V2StorageComponent implements zipkin.storage.StorageComponent {
      @Override public abstract SpanConsumer v2SpanConsumer();
    }
    WithSpan2 storage = mock(WithSpan2.class);
    SpanConsumer span2Consumer = mock(SpanConsumer.class);
    when(storage.v2SpanConsumer()).thenReturn(span2Consumer);

    collector = spy(Collector.builder(Collector.class)
      .storage(storage).build());

    byte[] bytes = BytesMessageEncoder.JSON_TO_BYTES.encode(asList(BytesEncoder.JSON.encode(span2_1)));
    collector.acceptSpans(bytes, SpanDecoder.DETECTING_DECODER, NOOP);

    verify(collector, never()).isSampled(any(zipkin.Span.class)); // skips v1 processing
    verify(span2Consumer).accept(eq(asList(span2_1))); // goes to v2 instead
  }
}
