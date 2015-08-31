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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal.encryption;

import android.util.Base64;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encryption and decryption based on AES algorithm
 */
public class AESStringEncryption implements StringEncryption {

    final static String Algorithm = "AES";

    Key key;

    public AESStringEncryption(String password) {
        key = new SecretKeySpec(password.getBytes(), Algorithm);
    }

    @Override
    public String encrypt(String str) {
        byte[] bytes = doFinalWithMode(Cipher.ENCRYPT_MODE, str.getBytes());
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    @Override
    public String decrypt(String str) {
        byte[] decode = Base64.decode(str.getBytes(), Base64.NO_WRAP);
        byte[] bytes = doFinalWithMode(Cipher.DECRYPT_MODE, decode);
        return new String(bytes);
    }

    private byte[] doFinalWithMode(int mode, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(Algorithm);
            cipher.init(mode, key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new byte[0];
    }
}
