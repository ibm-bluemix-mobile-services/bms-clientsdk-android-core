/*
 *     Copyright 2015 IBM Corp.
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

package com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.security;

import android.test.InstrumentationTestCase;
import android.util.Base64;

import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal.certificate.DefaultJSONSigner;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Created by cirilla on 8/11/15.
 */
public class DefaultJSONSignerTests extends InstrumentationTestCase {

    DefaultJSONSigner jsonSigner = new DefaultJSONSigner();

    public void testParamsForNull(){
        testForNull(null,new JSONObject());
        testForNull(generateRandomKeyPair(),null);
    }


    private void testForNull(KeyPair keyPair, JSONObject json){

        boolean isThrownIlligalArgumentException = false;
        try {
            jsonSigner.sign(keyPair, json);
        } catch (IllegalArgumentException e) {
            isThrownIlligalArgumentException = true;
        }
        catch (Exception e) {
        }

        assertTrue(isThrownIlligalArgumentException);
    }

    public void testSigning() throws JSONException {

        JSONObject testPayload = new JSONObject();
        testPayload.put("testName", "testValue");

        KeyPair keyPair = generateRandomKeyPair();

        String result = "";

        try {
            result = jsonSigner.sign(keyPair,testPayload);
        } catch (Exception e) {
            fail();
            e.printStackTrace();
        }

        String[] splitedValues = result.split("\\.");


        //check for the correct structure
        assertEquals(3, splitedValues.length);

        //check for the correct structure of the first part
        JSONObject firstPart = new JSONObject(new String(Base64.decode(splitedValues[0], Base64.DEFAULT)));

        assertEquals("RS256", firstPart.get("alg"));
        assertNotNull(firstPart.get("jpk"));

        JSONObject jpkJSONObject = new JSONObject(firstPart.getString("jpk"));

        //test jpk JSON
        assertEquals("RSA", jpkJSONObject.getString("alg"));
        assertNotNull(jpkJSONObject.getString("mod"));
        assertNotNull(jpkJSONObject.getString("exp"));

        //check for the correct structure of the second parts
        JSONObject secondPart = new JSONObject(new String(Base64.decode(splitedValues[1], Base64.DEFAULT)));

        assertEquals("testValue", secondPart.getString("testName"));
    }


    public KeyPair generateRandomKeyPair() {
        KeyPair keyPair = null;

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(512);
            keyPair = kpg.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return keyPair;
    }



}
