/*
 *     Copyright 2017 IBM Corp.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.ibm.mobilefirstplatform.clientsdk.android.core.internal;


import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ProgressListener;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProgressRequestBodyTests {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CountDownLatch latch = null;

    @Test
    public void testGetContentLength() {
        long expectedContentLength = 42;

        RequestBody mockedRequestBody = mock(RequestBody.class);
        try {
            when(mockedRequestBody.contentLength()).thenReturn(expectedContentLength);
        }
        catch (IOException e) {
            fail("No idea how this could even happen");
        }

        ProgressRequestBody progressBody = new ProgressRequestBody(null, mockedRequestBody, null);

        assertEquals(expectedContentLength, progressBody.contentLength());
    }

    @Test
    public void testGetContentLengthUnknown() {
        long defaultContentLength = -1;

        RequestBody mockedRequestBody = mock(RequestBody.class);
        try {
            when(mockedRequestBody.contentLength()).thenThrow(new IOException());
        }
        catch (IOException e) {
            fail("No idea how this could even happen");
        }

        ProgressRequestBody progressBody = new ProgressRequestBody(null, mockedRequestBody, null);

        assertEquals(defaultContentLength, progressBody.contentLength());
    }

    @Test
    public void testContentType() {
        MediaType expectedContentType = MediaType.parse("text/plain; charset=utf-8");

        RequestBody mockedRequestBody = mock(RequestBody.class);
        when(mockedRequestBody.contentType()).thenReturn(expectedContentType);

        ProgressRequestBody progressBody = new ProgressRequestBody(null, mockedRequestBody, null);

        assertEquals(expectedContentType, progressBody.contentType());
    }

    @Test
    public void testWriteTo() throws Exception {
        final byte[] testPayload = new byte[100000];
        new Random().nextBytes(testPayload);

        RequestBody mockedRequestBody = mock(RequestBody.class);
        try {
            when(mockedRequestBody.contentLength()).thenReturn((long)testPayload.length);
        }
        catch (IOException e) {
            fail("Failed to get the length of the test payload");
        }

        int numberOfOnProgressCalls = testPayload.length / ProgressRequestBody.SEGMENT_SIZE + 1;
        latch = new CountDownLatch(numberOfOnProgressCalls);

        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(long bytesSoFar, long totalBytesExpected) {
                latch.countDown();
            }
        };

        ProgressRequestBody progressRequestBody = new ProgressRequestBody(null, mockedRequestBody, listener) {
            protected Source getSourceFromPayload(Object payload) throws IOException {
                ByteArrayInputStream byteStream = new ByteArrayInputStream(testPayload);
                return Okio.source(byteStream);
            }
        };

        BufferedSink mockedSink = mock(BufferedSink.class, Mockito.CALLS_REAL_METHODS);
        Buffer sink = new Buffer();
        when(mockedSink.buffer()).thenReturn(sink);

        progressRequestBody.writeTo(mockedSink);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testWriteToWithSmallSource() throws Exception {
        final byte[] testPayload = new byte[10];
        new Random().nextBytes(testPayload);

        RequestBody mockedRequestBody = mock(RequestBody.class);
        try {
            when(mockedRequestBody.contentLength()).thenReturn((long)testPayload.length);
        }
        catch (IOException e) {
            fail("Failed to get the length of the test payload");
        }

        // If the upload size is very small (smaller than the ProgressRequestBody.SEGMENT_SIZE),
        // then onProgress() should only be called once.
        int numberOfOnProgressCalls = 1;
        latch = new CountDownLatch(numberOfOnProgressCalls);

        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(long bytesSoFar, long totalBytesExpected) {
                latch.countDown();
            }
        };

        ProgressRequestBody progressRequestBody = new ProgressRequestBody(null, mockedRequestBody, listener) {
            protected Source getSourceFromPayload(Object payload) throws IOException {
                ByteArrayInputStream byteStream = new ByteArrayInputStream(testPayload);
                return Okio.source(byteStream);
            }
        };

        BufferedSink mockedSink = mock(BufferedSink.class, Mockito.CALLS_REAL_METHODS);
        Buffer sink = new Buffer();
        when(mockedSink.buffer()).thenReturn(sink);

        progressRequestBody.writeTo(mockedSink);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testWriteToWithNullSource() throws Exception {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void onProgress(long bytesSoFar, long totalBytesExpected) {
                fail("Should not have reached onProgress when reading from a null source");
            }
        };

        ProgressRequestBody progressRequestBody = new ProgressRequestBody(null, null, listener) {
            protected Source getSourceFromPayload(Object payload) throws IOException {
                return null;
            }
        };

        BufferedSink mockedSink = mock(BufferedSink.class, Mockito.CALLS_REAL_METHODS);
        Buffer sink = new Buffer();
        when(mockedSink.buffer()).thenReturn(sink);

        progressRequestBody.writeTo(mockedSink);
    }

    @Test
    public void testGetSourceFromPayloadWithString() {
        String payloadString = "Upload text";

        MediaType mockedMediaType = mock(MediaType.class);
        when(mockedMediaType.charset()).thenReturn(Charset.defaultCharset());

        RequestBody mockedRequestBody = mock(RequestBody.class);
        when(mockedRequestBody.contentType()).thenReturn(mockedMediaType);

        ProgressRequestBody progressRequestBody = new ProgressRequestBody(null, mockedRequestBody, null);
        Source result = null;
        try {
            result = progressRequestBody.getSourceFromPayload(payloadString);
        }
        catch (IOException e) {
            fail("Should have been able to turn the string into a Source");
        }

        assertNotNull(result);
    }

    @Test
    public void testGetSourceFromPayloadWithFile() {
        File payloadFile = null;
        try {
            payloadFile = temporaryFolder.newFile();
        }
        catch (IOException e) {
            fail();
        }

        ProgressRequestBody progressRequestBody = new ProgressRequestBody(null, null, null);
        Source result = null;
        try {
            result = progressRequestBody.getSourceFromPayload(payloadFile);
        }
        catch (IOException e) {
            fail("Should have been able to turn the string into a Source");
        }

        assertNotNull(result);
    }

    @Test
    public void testGetSourceFromPayloadWithInputStream() {
        byte[] bytePayload = new byte[10];
        new Random().nextBytes(bytePayload);
        InputStream payloadStream = new ByteArrayInputStream(bytePayload);

        ProgressRequestBody progressRequestBody = new ProgressRequestBody(null, null, null);
        Source result = null;
        try {
            result = progressRequestBody.getSourceFromPayload(payloadStream);
        }
        catch (IOException e) {
            fail("Should have been able to turn the InputStream into a Source");
        }

        assertNotNull(result);
    }

    @Test
    public void testGetSourceFromPayloadWithByteArray() {
        byte[] bytePayload = new byte[10];
        new Random().nextBytes(bytePayload);

        ProgressRequestBody progressRequestBody = new ProgressRequestBody(null, null, null);
        Source result = null;
        try {
            result = progressRequestBody.getSourceFromPayload(bytePayload);
        }
        catch (IOException e) {
            fail("Should have been able to turn the InputStream into a Source");
        }

        assertNotNull(result);
    }

    @Test
    public void testGetSourceFromPayloadWithInvalidSource() {
        int invalidPayload = 42;

        ProgressRequestBody progressRequestBody = new ProgressRequestBody(null, null, null);
        Source result = null;
        try {
            result = progressRequestBody.getSourceFromPayload(invalidPayload);
        }
        catch (IOException e) {
            fail("Should have been able to turn the InputStream into a Source");
        }

        assertNull(result);
    }
}
