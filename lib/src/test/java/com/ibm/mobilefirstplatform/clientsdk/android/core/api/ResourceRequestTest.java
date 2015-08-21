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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ResourceRequestTest {

    @Test
    public void testDefaultValues() throws Exception{
        String testUrl = "http://test.com";
        MFPRequest request = new MFPRequest(testUrl, ResourceRequest.POST);

        assertTrue(request.getAllHeaders() == null || request.getAllHeaders().size() == 0);
        assertTrue(request.getHeaders("invalidHeaderName").size() == 0);
        assertTrue(request.getMethod().equalsIgnoreCase(ResourceRequest.POST));
        assertTrue(request.getQueryParameters() == null || request.getQueryParameters().size() == 0);
        assertTrue(request.getTimeout() == ResourceRequest.DEFAULT_TIMEOUT);
        assertTrue(request.getUrl().toString().equalsIgnoreCase(testUrl));
    }

    @Test
    public void timeoutShouldBeChangeable() throws Exception{
        String testUrl = "http://test.com";
        ResourceRequest request = new ResourceRequest(testUrl, ResourceRequest.POST, 60);

        assertEquals(60, request.getTimeout());

        request.setTimeout(42);

        assertEquals(42, request.getTimeout());
    }

    @Test
    public void shouldBeAbleToAddQueryParameter() throws Exception {
        String testUrl = "http://test.com";
        ResourceRequest request = new ResourceRequest(testUrl, ResourceRequest.POST, 60);

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
        String testUrl = "http://test.com";
        ResourceRequest request = new ResourceRequest(testUrl, ResourceRequest.POST, 60);

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
        String testUrl = "http://test.com";
        ResourceRequest request = new ResourceRequest(testUrl, ResourceRequest.POST, 60);

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