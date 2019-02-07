/*
    Copyright 2015 IBM Corp.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.ibm.mobilefirstplatform.clientsdk.android.core.api;

import com.ibm.mobilefirstplatform.clientsdk.android.security.DummyAuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.ResponseBody;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class RequestTests {

    private CountDownLatch latch = null;

    private void setupBMSClient() {

        AuthorizationManager mockAuthorizationManager = mock(DummyAuthorizationManager.class);
        BMSClient.getInstance().setAuthorizationManager(mockAuthorizationManager);
    }

    @Test
    public void testDefaultValues() throws Exception{
        String testUrl = "http://httpbin.org";
        Request request = new Request(testUrl, Request.POST);

        assertTrue(request.getAllHeaders() == null || request.getAllHeaders().size() == 0);
        assertTrue(request.getHeaders("invalidHeaderName").size() == 0);
        assertTrue(request.getMethod().equalsIgnoreCase(Request.POST));
        assertTrue(request.getQueryParameters() == null || request.getQueryParameters().size() == 0);
        assertTrue(request.getTimeout() == Request.DEFAULT_TIMEOUT);
        assertTrue(request.getUrl().toString().equalsIgnoreCase(testUrl));
    }

    @Test
    public void testConstructorWithAutoRetries() {
        String testUrl = "http://httpbin.org";
        int numberOfRetries = 3;
        Request request = new Request(testUrl, Request.GET, Request.DEFAULT_TIMEOUT, numberOfRetries);
        assertEquals(numberOfRetries, request.getNumberOfRetries());
    }

    @Test
    public void testAutoRetriesWithTimeout() throws Exception {
        setupBMSClient();
        latch = new CountDownLatch(1);

        MockWebServer mockServer = new MockWebServer();
        mockServer.start();

        int numberOfRetries = 3;
        Request request = new Request(mockServer.url("").toString(), Request.GET, 10, numberOfRetries);
        ResponseListener listener = new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                // Do nothing
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if (response == null && t != null && t.getClass().getName().toLowerCase().contains("timeout")) {
                    latch.countDown();
                }
            }
        };

        // This will timeout because the MockServer has no responses to give back
        request.send(null, listener);

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, request.getNumberOfRetries()); // Make sure all request retries have been exhausted

        mockServer.shutdown();
    }

    @Test
    public void testAutoRetriesWith504Response() throws Exception {
        setupBMSClient();
        latch = new CountDownLatch(1);

        int numberOfRetries = 5;

        MockWebServer mockServer = new MockWebServer();
        MockResponse response504 = new MockResponse().setResponseCode(504);

        for (int i = 0; i <= numberOfRetries; i++) {
            mockServer.enqueue(response504);
        }
        mockServer.start();

        Request request = new Request(mockServer.url("").toString(), Request.GET, 10, numberOfRetries);
        ResponseListener listener = new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                // Do nothing
            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if (response != null && response.getStatus() == 504) {
                    latch.countDown();
                }
            }
        };

        request.send(null, listener);

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, request.getNumberOfRetries()); // Make sure all request retries have been exhausted

        mockServer.shutdown();
    }

    @Test
    public void timeoutShouldBeChangeable() throws Exception{
        String testUrl = "http://httpbin.org";
        Request request = new Request(testUrl, Request.POST, 60);

        assertEquals(60, request.getTimeout());

        request.setTimeout(42);

        assertEquals(42, request.getTimeout());
    }

    @Test
    public void shouldBeAbleToAddQueryParameter() throws Exception {
        String testUrl = "http://httpbin.org";
        Request request = new Request(testUrl, Request.POST, 60);

        String testQueryName = "test";
        String testQueryValue = "testValue";

        request.setQueryParameter(testQueryName, testQueryValue);

        Map<String, String> queryParams = request.getQueryParameters();

        assertTrue(queryParams != null && queryParams.size() > 0);
        assertEquals(testQueryValue, queryParams.get(testQueryName));

        Map<String, String> testParams = new HashMap<>();
        String testName2 = "test2";
        String testValue2 = "testValue2";
        testParams.put(testName2, testValue2);

        request.setQueryParameters(testParams);

        assertEquals(testValue2, request.getQueryParameters().get(testName2));
    }

    @Test
    public void shouldBeAbleToAddAndRemoveHeaders() throws Exception {
        String testUrl = "http://httpbin.org";
        Request request = new Request(testUrl, Request.POST, 60);

        String testHeaderName = "testHeader";
        String testHeaderValue = "testValue";

        request.addHeader(testHeaderName, testHeaderValue);

        List<String> headers = request.getHeaders(testHeaderName);

        assertTrue(headers != null && headers.size() > 0);
        assertTrue(headers.get(0).equalsIgnoreCase(testHeaderValue));

        headers = request.getAllHeaders().get(testHeaderName);

        assertTrue(headers != null && headers.size() > 0);
        assertTrue(headers.get(0).equalsIgnoreCase(testHeaderValue));

        request.removeHeaders(testHeaderName);

        headers = request.getHeaders(testHeaderName);

        assertTrue(headers == null || headers.size() == 0);
    }

    @Test
    public void shouldBeAbleToSetHeaders() throws Exception {
        String testUrl = "http://httpbin.org";
        Request request = new Request(testUrl, Request.POST, 60);

        String testHeaderName = "testHeader";
        String testHeaderValue = "testValue";

        List<String> testHeaderValues = new ArrayList<>();
        Map<String, List<String>> testHeaders = new HashMap<>();

        testHeaderValues.add(testHeaderValue);
        testHeaders.put(testHeaderName, testHeaderValues);

        request.setHeaders(testHeaders);

        List<String> headers = request.getHeaders(testHeaderName);

        assertTrue(headers != null && headers.size() > 0);
        assertTrue(headers.get(0).equalsIgnoreCase(testHeaderValue));
    }

    @Test
    public void testGetCallbackTriggersProgressListener() throws Exception {
        latch = new CountDownLatch(1);

        setupBMSClient();

        okhttp3.Response mockedOkHttpResponse = mock(okhttp3.Response.class);
        okhttp3.Call call = mock(okhttp3.Call.class);

        when(mockedOkHttpResponse.isSuccessful()).thenReturn(true);
        when(mockedOkHttpResponse.body()).thenReturn(mock(ResponseBody.class));
        when(mockedOkHttpResponse.request()).thenReturn(mock(okhttp3.Request.class));
        okhttp3.Headers headers = new Headers.Builder().build();
        when(mockedOkHttpResponse.headers()).thenReturn(headers);

        ProgressListener progressListener = new DummyProgressListener();
        ResponseListener responseListener = new DummyResponseListener();
        Request request = new Request("", "") {
            @Override
            protected void updateProgressListener(ProgressListener progressListener, Response response) {
                latch.countDown();
            }
        };
        Callback callback = request.getCallback(progressListener, responseListener);
        callback.onResponse(call, mockedOkHttpResponse);

        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }


    class DummyResponseListener implements ResponseListener {
        public void onSuccess(com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response response) {
            // Do nothing
        }

        public void onFailure(com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response response, Throwable t, JSONObject extendedInfo) {
            // Do nothing
        }
    }

    class DummyProgressListener implements ProgressListener {
        public void onProgress(long bytesSoFar, long totalBytesExpected) {
            // Do nothing
        }
    }
}