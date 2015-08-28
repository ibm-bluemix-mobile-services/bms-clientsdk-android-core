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

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * General help methods for handling certificates
 * Created by cirilla on 8/6/15.
 */
public class CertificatesUtility {

    public static X509Certificate base64StringToCertificate(String certificateString) throws CertificateException, IOException {

        if (certificateString == null){
            throw new IllegalArgumentException("certificateString cannot be null");
        }

        byte[] encodedCert = Base64.decode(certificateString,Base64.DEFAULT);
        InputStream inStream = new ByteArrayInputStream(encodedCert);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
        inStream.close();

        return cert;
    }

    public static void checkValidityWithPublicKey(X509Certificate certificate, PublicKey publicKey) throws CertificateNotYetValidException, CertificateExpiredException {

        Date now = new Date();
        long nowTime = now.getTime();
        Date afterAddingOneMinute = new Date(nowTime + 60000);

        //we are checking the certificate against current time plus one minute to prevent false failure because of sync problems
        certificate.checkValidity(afterAddingOneMinute);
        if (!certificate.getPublicKey().equals(publicKey)) {
            throw new RuntimeException("Failed to validate public key");
        }
    }

    public static String getClientIdFromCertificate(X509Certificate certificate){

        if (certificate == null){
            throw new IllegalArgumentException("Certificate cannot be null");
        }

        //subjectDN is of the form: "UID=<clientId>, DC=<some other value>" or "DC=<some other value>, UID=<clientId>"
        String clientId = null;

        String subjectDN = certificate.getSubjectDN().getName();
        String[] parts = subjectDN.split(Pattern.quote(","));
        for (String part: parts){
            if (part.contains("UID=")){
                String uid=part.substring(part.indexOf("UID="));
                clientId = uid.split(Pattern.quote("="))[1];
            }
        }

        return clientId;
    }
}
