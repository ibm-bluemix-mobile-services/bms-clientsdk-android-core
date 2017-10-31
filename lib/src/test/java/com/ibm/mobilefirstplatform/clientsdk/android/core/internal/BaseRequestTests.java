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
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import org.json.JSONObject;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseRequestTests {

    private CountDownLatch latch = null;

    @Test
    public void testSend() throws Exception {
        latch = new CountDownLatch(1);

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void send(final String requestBody, ResponseListener listener) {
                assertEquals("", requestBody);
                latch.countDown();
            }
        };

        request.send(null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendText() throws Exception {
        latch = new CountDownLatch(1);

        final String requestBodyText = "Request body text";
        final ResponseListener expectedListener = new DummyResponseListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertEquals(expectedListener, responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.send(requestBodyText, expectedListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendTextWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNull(requestBody);
                latch.countDown();
            }
        };

        request.send("", null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendFormParameters() throws Exception {
        latch = new CountDownLatch(1);

        final Map<String, String> requestBodyFormParameters = new HashMap<>();
        requestBodyFormParameters.put("parameter", "value");
        final ResponseListener expectedListener = new DummyResponseListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertEquals(expectedListener, responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.send(requestBodyFormParameters, expectedListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendFormParametersWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        final Map<String, String> requestBodyFormParameters = new HashMap<>();
        requestBodyFormParameters.put("parameter", "value");

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.send(requestBodyFormParameters, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendJSON() throws Exception {
        latch = new CountDownLatch(1);

        final JSONObject requestBodyJSON = new JSONObject();
        requestBodyJSON.put("key", "value");
        final ResponseListener expectedListener = new DummyResponseListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertEquals(expectedListener, responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.send(requestBodyJSON, expectedListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendJSONWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        final JSONObject requestBodyJSON = new JSONObject();
        requestBodyJSON.put("key", "value");

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.send(requestBodyJSON, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendData() throws Exception {
        latch = new CountDownLatch(1);

        final byte[] requestBodyData = new byte[10];
        new Random().nextBytes(requestBodyData);
        final ResponseListener expectedListener = new DummyResponseListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertEquals(expectedListener, responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.send(requestBodyData, expectedListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendDataWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        final byte[] requestBodyData = new byte[10];
        new Random().nextBytes(requestBodyData);

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.send(requestBodyData, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownload() throws Exception {
        latch = new CountDownLatch(1);

        final ResponseListener expectedResponseListener = new DummyResponseListener();
        final ProgressListener expectedProgressListener = new DummyProgressListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertEquals(expectedProgressListener, progressListener);
                assertEquals(expectedResponseListener, responseListener);
                assertNull(requestBody);
                latch.countDown();
            }
        };

        request.download(expectedProgressListener, expectedResponseListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNull(requestBody);
                latch.countDown();
            }
        };

        request.download(null, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadText() throws Exception {
        latch = new CountDownLatch(1);

        final String requestBodyText = "Request body text";
        final ResponseListener expectedResponseListener = new DummyResponseListener();
        final ProgressListener expectedProgressListener = new DummyProgressListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertEquals(expectedProgressListener, progressListener);
                assertEquals(expectedResponseListener, responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.download(requestBodyText, expectedProgressListener, expectedResponseListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadTextWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNull(requestBody);
                latch.countDown();
            }
        };

        request.download("", null, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadFormParameters() throws Exception {
        latch = new CountDownLatch(1);

        final Map<String, String> requestBodyFormParameters = new HashMap<>();
        requestBodyFormParameters.put("parameter", "value");
        final ResponseListener expectedResponseListener = new DummyResponseListener();
        final ProgressListener expectedProgressListener = new DummyProgressListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertEquals(expectedProgressListener, progressListener);
                assertEquals(expectedResponseListener, responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.download(requestBodyFormParameters, expectedProgressListener, expectedResponseListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadFormParametersWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        final Map<String, String> requestBodyFormParameters = new HashMap<>();
        requestBodyFormParameters.put("parameter", "value");

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.download(requestBodyFormParameters, null, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadJSON() throws Exception {
        latch = new CountDownLatch(1);

        final JSONObject requestBodyJSON = new JSONObject();
        requestBodyJSON.put("key", "value");
        final ResponseListener expectedResponseListener = new DummyResponseListener();
        final ProgressListener expectedProgressListener = new DummyProgressListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertEquals(expectedProgressListener, progressListener);
                assertEquals(expectedResponseListener, responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.download(requestBodyJSON, expectedProgressListener, expectedResponseListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadJSONWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        final JSONObject requestBodyJSON = new JSONObject();
        requestBodyJSON.put("key", "value");

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.download(requestBodyJSON, null, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadData() throws Exception {
        latch = new CountDownLatch(1);

        final byte[] requestBodyData = new byte[10];
        new Random().nextBytes(requestBodyData);
        final ResponseListener expectedResponseListener = new DummyResponseListener();
        final ProgressListener expectedProgressListener = new DummyProgressListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertEquals(expectedProgressListener, progressListener);
                assertEquals(expectedResponseListener, responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.download(requestBodyData, expectedProgressListener, expectedResponseListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testDownloadDataWithNullParameters() throws Exception {
        latch = new CountDownLatch(1);

        final byte[] requestBodyData = new byte[10];
        new Random().nextBytes(requestBodyData);

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.download(requestBodyData, null, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUploadText() throws Exception {
        latch = new CountDownLatch(1);

        final String requestBodyText = "Request body text";
        final ResponseListener expectedResponseListener = new DummyResponseListener();
        final ProgressListener progressListener = new DummyProgressListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertEquals(expectedResponseListener, responseListener);
                assertTrue(requestBody instanceof ProgressRequestBody);
                latch.countDown();
            }
        };

        request.upload(requestBodyText, progressListener, expectedResponseListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUploadTextWithNullListeners() throws Exception {
        latch = new CountDownLatch(1);

        final String requestBodyText = "Request body text";

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.upload(requestBodyText, null, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUploadTextWithEmptyText() throws Exception {
        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                fail("The upload method should have returned early and not called sendRequest()");
            }
        };

        request.upload("", null, null);
    }

    @Test
    public void testUploadData() throws Exception {
        latch = new CountDownLatch(1);

        final byte[] requestBodyData = new byte[10];
        new Random().nextBytes(requestBodyData);
        final ResponseListener expectedResponseListener = new DummyResponseListener();
        final ProgressListener progressListener = new DummyProgressListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertEquals(expectedResponseListener, responseListener);
                assertTrue(requestBody instanceof ProgressRequestBody);
                latch.countDown();
            }
        };

        request.upload(requestBodyData, progressListener, expectedResponseListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUploadDataWithNullListeners() throws Exception {
        latch = new CountDownLatch(1);

        final byte[] requestBodyData = new byte[10];
        new Random().nextBytes(requestBodyData);

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.upload(requestBodyData, null, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUploadDataWithEmptyText() throws Exception {

        final byte[] emptyRequestBodyData = new byte[0];

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                fail("The upload method should have returned early and not called sendRequest()");
            }
        };

        request.upload(emptyRequestBodyData, null, null);
    }

    @Test
    public void testUploadFile() throws Exception {
        latch = new CountDownLatch(1);

        final File requestBodyFile = mock(File.class);
        final ResponseListener expectedResponseListener = new DummyResponseListener();
        final ProgressListener progressListener = new DummyProgressListener();

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertEquals(expectedResponseListener, responseListener);
                assertTrue(requestBody instanceof ProgressRequestBody);
                latch.countDown();
            }
        };

        request.upload(requestBodyFile, progressListener, expectedResponseListener);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUploadFileWithNullListeners() throws Exception {
        latch = new CountDownLatch(1);

        final File requestBodyFile = mock(File.class);

        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void sendRequest(final ProgressListener progressListener, final ResponseListener responseListener, final RequestBody requestBody) {
                assertNull(progressListener);
                assertNull(responseListener);
                assertNotNull(requestBody);
                latch.countDown();
            }
        };

        request.upload(requestBodyFile, null, null);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetCallbackTriggersProgressListener() throws Exception {
        latch = new CountDownLatch(1);

        okhttp3.Response mockedOkHttpResponse = mock(okhttp3.Response.class);
        okhttp3.Call call = mock(okhttp3.Call.class);

        when(mockedOkHttpResponse.isSuccessful()).thenReturn(true);
        when(mockedOkHttpResponse.body()).thenReturn(mock(ResponseBody.class));
        when(mockedOkHttpResponse.request()).thenReturn(mock(Request.class));

        ProgressListener progressListener = new DummyProgressListener();
        ResponseListener responseListener = new DummyResponseListener();
        BaseRequest request = new BaseRequest("", "") {
            @Override
            protected void updateProgressListener(ProgressListener progressListener, Response response) {
                latch.countDown();
            }
        };
        Callback callback = request.getCallback(progressListener, responseListener);
        callback.onResponse(call, mockedOkHttpResponse);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUpdateProgressListenerSetsResponseBytes() {

        byte[] expectedResponseBytes = new byte[10000];
        new Random().nextBytes(expectedResponseBytes);
        InputStream responseStream = new ByteArrayInputStream(expectedResponseBytes);

        ResponseImpl mockedResponse = mock(ResponseImpl.class);
        when(mockedResponse.getResponseByteStream()).thenReturn(responseStream);
        when(mockedResponse.getResponseBytes()).thenCallRealMethod();
        when(mockedResponse.getContentLength()).thenReturn((long)expectedResponseBytes.length);
        doCallRealMethod().when(mockedResponse).setResponseBytes(expectedResponseBytes);

        ProgressListener progressListener = new DummyProgressListener();
        BaseRequest request = new BaseRequest("", "");
        request.updateProgressListener(progressListener, mockedResponse);

        assertTrue(Arrays.equals(expectedResponseBytes, mockedResponse.getResponseBytes()));
    }

    @Test
    public void testUpdateProgressListenerCallsOnProgress() throws Exception {

        byte[] expectedResponseBytes = new byte[10000];
        new Random().nextBytes(expectedResponseBytes);
        InputStream responseStream = new ByteArrayInputStream(expectedResponseBytes);

        ResponseImpl mockedResponse = mock(ResponseImpl.class);
        when(mockedResponse.getResponseByteStream()).thenReturn(responseStream);
        when(mockedResponse.getContentLength()).thenReturn((long)expectedResponseBytes.length);

        int expectedNumberOfOnProgressCalls = expectedResponseBytes.length / 2048;
        latch = new CountDownLatch(expectedNumberOfOnProgressCalls + 1);

        ProgressListener progressListener = new ProgressListener() {
            @Override
            public void onProgress(long bytesSoFar, long totalBytesExpected) {
                latch.countDown();
            }
        };
        BaseRequest request = new BaseRequest("", "");
        request.updateProgressListener(progressListener, mockedResponse);

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    }


    class DummyResponseListener implements ResponseListener {
        public void onSuccess(Response response) {
            // Do nothing
        }

        public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
            // Do nothing
        }
    }

    class DummyProgressListener implements ProgressListener {
        public void onProgress(long bytesSoFar, long totalBytesExpected) {
            // Do nothing
        }
    }
}
