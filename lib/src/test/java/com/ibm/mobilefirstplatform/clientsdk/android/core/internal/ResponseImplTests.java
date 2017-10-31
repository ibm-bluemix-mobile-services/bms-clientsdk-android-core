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


import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ResponseImplTests {

    private Response mockedOkHttpResponse;

    @Before
    public void setupOkHttpResponse() {
        mockedOkHttpResponse = mock(Response.class);
        when(mockedOkHttpResponse.body()).thenReturn(mock(ResponseBody.class));
        when(mockedOkHttpResponse.request()).thenReturn(mock(Request.class));
    }


    @Test
    public void testGetRequestUrl() {
        String expectedUrl = "http://httpbin.org";
        Request mockedRequest = mock(Request.class);
        when(mockedRequest.toString()).thenReturn(expectedUrl);
        when(mockedOkHttpResponse.request()).thenReturn(mockedRequest);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertEquals(expectedUrl, response.getRequestURL());
    }

    @Test
    public void testGetStatus() {
        int expectedStatus = 404;
        when(mockedOkHttpResponse.code()).thenReturn(expectedStatus);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertEquals(expectedStatus, response.getStatus());
    }

    @Test
    public void testGetStatusNull() {
        ResponseImpl response = new ResponseImpl(null);

        assertEquals(0, response.getStatus());
    }

    @Test
    public void testGetContentLength() {
        long expectedContentLength = 42;

        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        try {
            when(mockedResponseBody.contentLength()).thenReturn(expectedContentLength);
        }
        catch (Exception e) {
            fail("Should have been able to get the mocked content length.");
        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertEquals(expectedContentLength, response.getContentLength());
    }

    @Test
    public void testGetContentLengthThrows() {
        ResponseBody mockedResponseBody = mock(ResponseBody.class);
//        try {
//            when(mockedResponseBody.contentLength()).thenThrow(new IOException());
//        }
//        catch (Exception e) {
//            fail("Should have been able to get the mocked content length.");
//        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertEquals(0, response.getContentLength());
    }

    @Test
    public void testGetResponseText() throws JSONException {
        byte[] expectedByteStream = null;
        String expectedText = "Expected text";

        try {
            InputStream in = IOUtils.toInputStream(expectedText, "UTF-8");
            expectedByteStream = IOUtils.toByteArray(in);
                    //toInputStream(expectedText, "UTF-8");
        }
        catch (Exception e) {
            fail("Should have been able to convert json to input stream");
        }

        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        try {
            when(mockedResponseBody.bytes()).thenReturn(expectedByteStream);
        }
        catch (Exception e) {
            fail("Should have been able to get the mocked byte stream");
        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertTrue(expectedText.equals(response.getResponseText()));
    }

    @Test
    public void testGetResponseTextWithInvalidContentType() throws JSONException {
        InputStream byteStreamWithUnsupportedEncoding = null;
        String expectedText = "asdf";
        try {
            byteStreamWithUnsupportedEncoding = IOUtils.toInputStream(expectedText, "UTF-32");
        }
        catch (Exception e) {
            fail("Should have been able to convert json to input stream");
        }

        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        try {
            when(mockedResponseBody.byteStream()).thenReturn(byteStreamWithUnsupportedEncoding);
            when(mockedResponseBody.contentType()).thenReturn(MediaType.parse("text/plain"));
        }
        catch (Exception e) {
            fail("Should have been able to get the mocked byte stream");
        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertFalse(expectedText.equals(response.getResponseText()));
    }

    @Test
    public void testGetResponseTextNull() throws JSONException {
        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        try {
            when(mockedResponseBody.byteStream()).thenReturn(null);
        }
        catch (Exception e) {
            fail("Should have been able to get the mocked byte stream");
        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertEquals("", response.getResponseText());
    }

    @Test
    public void testGetResponseJSON() throws JSONException {
        byte[] expectedByteStream = null;
        JSONObject expectedJSON = new JSONObject();
        expectedJSON.put("key0", "value0");
        expectedJSON.put("key1", "value1");
        try {
            InputStream in = IOUtils.toInputStream(expectedJSON.toString(), "UTF-8");
            expectedByteStream = IOUtils.toByteArray(in);
            //expectedByteStream = IOUtils.toInputStream(expectedJSON.toString(), "UTF-8");
        }
        catch (Exception e) {
            fail("Should have been able to convert json to input stream");
        }

        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        try {
            when(mockedResponseBody.bytes()).thenReturn(expectedByteStream);
        }
        catch (Exception e) {
            fail("Should have been able to get the mocked byte stream");
        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertTrue(expectedJSON.toString().equals(response.getResponseJSON().toString()));
    }

    @Test
    public void testGetResponseJSONWithInvalidJSON() throws JSONException {
        InputStream expectedByteStream = null;
        String invalidJSON = "xxx";
        try {
            expectedByteStream = IOUtils.toInputStream(invalidJSON, "UTF-8");
        }
        catch (Exception e) {
            fail("Should have been able to convert json to input stream");
        }

        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        try {
            when(mockedResponseBody.byteStream()).thenReturn(expectedByteStream);
        }
        catch (Exception e) {
            fail("Should have been able to get the mocked byte stream");
        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertNull(response.getResponseJSON());
    }

    @Test
    public void testGetResponseJSONNull() throws JSONException {
        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        try {
            when(mockedResponseBody.byteStream()).thenReturn(null);
        }
        catch (Exception e) {
            fail("Should have been able to get the mocked byte stream");
        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertNull(response.getResponseJSON());
    }

    @Test
    public void testGetResponseBytes() {
        String testString = "Test bytes";
        InputStream expectedByteStream = null;
        byte[] expectedBytes = null;
        try {
            expectedByteStream = IOUtils.toInputStream(testString, "UTF-8");
            expectedBytes = IOUtils.toByteArray(IOUtils.toInputStream(testString, "UTF-8"));
        }
        catch (IOException e) {
            fail("Should have been able to convert string to input stream and then to byte array");
        }

        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        try {
            when(mockedResponseBody.byteStream()).thenReturn(expectedByteStream);
        }
        catch (Exception e) {
            fail("Should have been able to get the mocked byte stream.");
        }
        when(mockedOkHttpResponse.body()).thenReturn(mockedResponseBody);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertTrue(Arrays.equals(expectedBytes, response.getResponseBytes()));
    }

    @Test
    public void testSetResponseBytes() {
        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);
        byte[] expectedBytes = "Test bytes".getBytes();
        response.setResponseBytes(expectedBytes);

        assertEquals(expectedBytes, response.getResponseBytes());
    }

    @Test
    public void testIsRedirect() {
        when(mockedOkHttpResponse.isRedirect()).thenReturn(true);
        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);
        assertTrue(response.isRedirect());

        when(mockedOkHttpResponse.isRedirect()).thenReturn(false);
        response = new ResponseImpl(mockedOkHttpResponse);
        assertFalse(response.isRedirect());
    }

    @Test
    public void testIsSuccessful() {
        when(mockedOkHttpResponse.isSuccessful()).thenReturn(true);
        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);
        assertTrue(response.isSuccessful());

        when(mockedOkHttpResponse.isSuccessful()).thenReturn(false);
        response = new ResponseImpl(mockedOkHttpResponse);
        assertFalse(response.isSuccessful());
    }

    @Test
    public void testGetHeaders() {
        String[] headers = {"header0", "header1"};
        List<String> values0 = Arrays.asList("value0", "value1", "value2");
        List<String> values1 = Arrays.asList("valueA", "valueB");

        Headers testHeaders = new Headers.Builder()
                .add(headers[0], values0.get(0))
                .add(headers[0], values0.get(1))
                .add(headers[0], values0.get(2))
                .add(headers[1], values1.get(0))
                .add(headers[1], values1.get(1))
                .build();
        when(mockedOkHttpResponse.headers()).thenReturn(testHeaders);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        Map<String, List<String>> expectedHeaders = new HashMap<String, List<String>>();
        expectedHeaders.put(headers[0], values0);
        expectedHeaders.put(headers[1], values1);
        assertEquals(expectedHeaders, response.getHeaders());
    }

    @Test
    public void testGetHeadersNull() {
        when(mockedOkHttpResponse.headers()).thenReturn(null);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertNull(response.getHeaders());
    }

    @Test
    public void testGetHeader() {
        String expectedHeader = "header";
        List<String> expectedValues = Arrays.asList("value0", "value1", "value2");

        Headers testHeaders = new Headers.Builder()
                .add(expectedHeader, expectedValues.get(0))
                .add(expectedHeader, expectedValues.get(1))
                .add(expectedHeader, expectedValues.get(2))
                .build();
        when(mockedOkHttpResponse.headers()).thenReturn(testHeaders);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertEquals(expectedValues, response.getHeader(expectedHeader));
    }

    @Test
    public void testGetHeaderNull() {
        when(mockedOkHttpResponse.headers()).thenReturn(null);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertNull(response.getHeader("header"));
    }

    @Test
    public void testGetFirstHeader() {
        String expectedHeader = "header0";
        String expectedValue = "value0";
        Headers testHeaders = Headers.of(expectedHeader, expectedValue, "header1", "value1", "header2", "value2");
        when(mockedOkHttpResponse.headers()).thenReturn(testHeaders);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertEquals(expectedValue, response.getFirstHeader(expectedHeader));
    }

    @Test
    public void testGetFirstHeaderNull() {
        when(mockedOkHttpResponse.headers()).thenReturn(null);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertNull(response.getFirstHeader("header"));
    }

    @Test
    public void testGetResponseHeaderNames() {
        String[] headerNames = {"header0", "header1"};
        Set<String> expectedHeaderNames = new HashSet<>(Arrays.asList(headerNames));

        Headers testHeaders = Headers.of(headerNames[0], "value0", headerNames[1], "value1");
        when(mockedOkHttpResponse.headers()).thenReturn(testHeaders);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertEquals(expectedHeaderNames, response.getResponseHeadersNames());
    }

    @Test
    public void testGetResponseHeaderNamesNull() {
        when(mockedOkHttpResponse.headers()).thenReturn(null);

        ResponseImpl response = new ResponseImpl(mockedOkHttpResponse);

        assertNull(response.getResponseHeadersNames());
    }
}
