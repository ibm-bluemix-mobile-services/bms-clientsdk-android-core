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

import android.content.Context;
import android.test.mock.MockContext;

import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AppIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.DeviceIdentity;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.UserIdentity;

import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
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


public class RequestTest {

    CountDownLatch latch = null;

    private void setup() {

        class MockAuthorizationManager implements AuthorizationManager {
            private final DeviceIdentity deviceIdentity = null;
            private final AppIdentity appIdentity = null;
            public MockAuthorizationManager(Context context){ }
            @Override
            public boolean isAuthorizationRequired (int statusCode, Map<String, List<String>> headers) { return false; }
            @Override
            public boolean isAuthorizationRequired (HttpURLConnection urlConnection) throws IOException { return false; }
            @Override
            public void obtainAuthorization (Context context, ResponseListener listener, Object... params) { }
            @Override
            public String getCachedAuthorizationHeader () { return null; }
            @Override
            public void clearAuthorizationData () {}
            @Override
            public UserIdentity getUserIdentity () { return null; }
            @Override
            public DeviceIdentity getDeviceIdentity () { return null; }
            @Override
            public AppIdentity getAppIdentity () { return null; }
            @Override
            public void logout(Context context, ResponseListener listener) { }
        }

        BMSClient.getInstance().setAuthorizationManager(new MockAuthorizationManager(new MockContext()));
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
    public void testAutoRetriesWithTimeout() throws Exception {
        setup();
        latch = new CountDownLatch(1);

        MockWebServer mockServer = new MockWebServer();
        mockServer.start();

        Request request = new Request(mockServer.url("").toString(), Request.GET, 10, 3);
        ResponseListener listener = new ResponseListener() {
            @Override
            public void onSuccess(Response response) { }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if (response == null && t != null) {
                    if (t.getMessage().contains("timeout") || t.getMessage().contains("timed out")) {
                        latch.countDown();
                    }
                }
            }
        };

        request.send(null, listener);

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, request.getNumberOfRetries()); // Make sure all request retries have been exhausted

        mockServer.shutdown();
    }

    @Test
    public void testAutoRetriesWith504Response() throws Exception {
        setup();
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
                latch.countDown();
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
}