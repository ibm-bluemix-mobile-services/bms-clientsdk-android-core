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

import static android.test.MoreAsserts.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ResponseTest {

    @Test
    public void testDefaultValues(){
        Response response = new Response(null);

        assertTrue(response.getStatus() == 0);
        assertTrue(response.getResponseBytes() == null);
        assertTrue(response.getResponseJSON() == null);
        assertTrue(response.getResponseText() == null);
        assertTrue(response.getResponseHeaders() == null);
        assertTrue(response.getResponseHeadersNames() == null);
        assertTrue(response.getResponseHeader("nonExistentHeader") == null);
        assertFalse(response.isRedirect());
        assertFalse(response.isSuccessful());
    }

}