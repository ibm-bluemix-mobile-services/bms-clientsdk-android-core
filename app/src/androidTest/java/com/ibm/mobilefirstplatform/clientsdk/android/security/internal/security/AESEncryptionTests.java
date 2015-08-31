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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal.security;


import android.test.InstrumentationTestCase;

import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.encryption.AESStringEncryption;

public class AESEncryptionTests extends InstrumentationTestCase {


    final static String testStr = "Hello this is Test String";

    AESStringEncryption encryption;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        encryption = new AESStringEncryption("Bar12345Bar12345");
    }

    public void testStaticEncyptDecrypt() throws Exception {
        String encrypt = encryption.encrypt(testStr);
        String decrypted = encryption.decrypt(encrypt);
        assertEquals(testStr, decrypted);
    }
}