package com.ibm.mobilefirstplatform.clientsdk.android.security.internal.certificate;

import android.util.Base64;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;

/**
 * Created by cirilla on 8/3/15.
 */
public class DefaultJSONSigner implements IJSONSigner{

    private static final String ALG = "alg";

    @Override
    public String sign(KeyPair keyPair, JSONObject json) throws Exception {

        if (keyPair == null || json == null){
            throw new IllegalArgumentException("parameter cannot be null");
        }

        RSAPublicKey publicKey = ((RSAPublicKey)keyPair.getPublic());
        PrivateKey privateKey = keyPair.getPrivate();

        // create CSR Header (based on public key)
        JSONObject jwsHeaderJson = new JSONObject();
        jwsHeaderJson.put(ALG, "RS256");

        JSONObject publicKeyDataJson = new JSONObject();
        publicKeyDataJson.put(ALG, "RSA");

        String mod = encodeUrlSafe(publicKey.getModulus().toByteArray());
        publicKeyDataJson.put("mod", mod);

        String exp = encodeUrlSafe(publicKey.getPublicExponent().toByteArray());
        publicKeyDataJson.put("exp", exp);

        jwsHeaderJson.put("jpk", publicKeyDataJson);

        String jwsHeader = jwsHeaderJson.toString();
        String payload = json.toString();

        // concatenate JWS Header and payload.
        String csrHeaderAndPayload = encodeUrlSafe(jwsHeader.getBytes()) + "." + encodeUrlSafe(payload.getBytes());

        // create CSR Signature
        String jwsSignature = encodeUrlSafe(signCsrData(csrHeaderAndPayload,  privateKey));

        // Concatenate them all, and return the result.
        String result = csrHeaderAndPayload + "." + jwsSignature;
        return result;
    }

    private byte[] signCsrData(String csrJSONData, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(csrJSONData.getBytes());
        return signature.sign();
    }

    private String encodeUrlSafe(byte[] data) throws UnsupportedEncodingException {
        return new String(Base64.encode(data, Base64.URL_SAFE | Base64.NO_WRAP),"UTF-8");
    }
}
