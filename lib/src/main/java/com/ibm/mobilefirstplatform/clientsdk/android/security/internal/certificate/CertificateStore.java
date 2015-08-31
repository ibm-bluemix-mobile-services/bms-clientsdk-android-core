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

package com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Stores certificate and his key pair on local storage
 * Created by cirilla on 8/6/15.
 */
public class CertificateStore {

    private static final String alias = "registration";
    File keyStoreFile;
    private char[] password;
    private KeyStore keyStore;

    public CertificateStore(File keyStoreFile, String password) {
        this.keyStoreFile = keyStoreFile;
        this.password = password.toCharArray();
    }

    public void saveCertificate(KeyPair keyPair, X509Certificate certificate) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        loadKeyStore();
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), password, new X509Certificate[]{certificate});

        //save the keystore
        FileOutputStream fos = new FileOutputStream(keyStoreFile);
        keyStore.store(fos,password);
        fos.close();
    }

    private void loadKeyStore() throws IOException, KeyStoreException, CertificateException {

        try {
            if (keyStore == null){
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

                //load existing keystore
                if (keyStoreFile.exists() && keyStoreFile.length() > 0){
                    FileInputStream fileInputStream = new FileInputStream(keyStoreFile);
                    keyStore.load(fileInputStream,password);
                    fileInputStream.close();
                }

                //load empty keystore
                else{
                    keyStore.load(null, password);
                }
            }

        }  catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public KeyPair getStoredKeyPair() throws CertificateException, KeyStoreException, IOException, UnrecoverableEntryException, NoSuchAlgorithmException {

        loadKeyStore();
        KeyPair keyPair = null;

        if (keyStore.containsAlias(alias)){
            KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, new KeyStore.PasswordProtection(password));
            Certificate cert = keyStore.getCertificate(alias);
            keyPair = new KeyPair(cert.getPublicKey(), pke.getPrivateKey());
        }

        return keyPair;
    }

    public X509Certificate getCertificate() throws CertificateException, KeyStoreException, IOException {
        loadKeyStore();
        X509Certificate certificate = null;

        if (keyStore.containsAlias(alias)){
            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain==null || chain.length==0){
                throw new IOException("No certificate found");
            }

            certificate = (X509Certificate) chain[0];
        }

        return certificate;
    }


    public boolean isCertificateStored(){
        try {
            loadKeyStore();

            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain!=null && chain.length > 0){
                return true;
            }

        } catch (Exception e) {
            return false;
        }

        return false;
    }



}
