package com.ibm.bms.clientsdk.android.security.internal.certificate;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Created by cirilla on 8/6/15.
 */
public class CertificateAuxiliary {

    private static final int RSA_KEY_SIZE = 512;

    //X509Certificate certificate;

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

    public static boolean checkValidityWithPublicKey(X509Certificate certificate, PublicKey publicKey){
        try {
            Date now = new Date();
            long nowTime = now.getTime();
            Date afterAddingOneMinute=new Date(nowTime + 60000);

            //we are checking the certificate against current time plus one minute to prevent false failure because of sync problems
            certificate.checkValidity(afterAddingOneMinute);
            if (certificate.getPublicKey().equals(publicKey)){
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String getClientIdFromCertificate(X509Certificate certificate){

        if (certificate == null){
            throw new IllegalArgumentException("certificate cannot be null");
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
