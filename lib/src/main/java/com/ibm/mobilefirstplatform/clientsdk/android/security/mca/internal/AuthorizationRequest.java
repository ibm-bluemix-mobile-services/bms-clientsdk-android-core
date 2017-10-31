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

package com.ibm.mobilefirstplatform.clientsdk.android.security.mca.internal;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.internal.BaseRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.internal.TLSEnabledSSLSocketFactory;
import okhttp3.OkHttpClient;

import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * AuthorizationRequest is used internally to send authorization requests.
 */
public class AuthorizationRequest extends BaseRequest {

    private static OkHttpClient httpClient = new OkHttpClient();
    private static final OkHttpClient.Builder httpClientB = new OkHttpClient.Builder();

    static {
        SSLSocketFactory tlsEnabledSSLSocketFactory;
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };
            tlsEnabledSSLSocketFactory = new TLSEnabledSSLSocketFactory();
            httpClientB.sslSocketFactory(tlsEnabledSSLSocketFactory, (X509TrustManager)trustAllCerts[0]);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructs the authorization request
     * @param url request url
     * @param method request method
     * @throws MalformedURLException if url is not valid
     */
    public AuthorizationRequest(String url, String method) throws MalformedURLException {
        super(url, method);

        // we want to handle redirects in-place
        httpClientB.followRedirects(false);
    }

    /**
     * Override the base getter to return authrization http client
     * @return internal http client
     */
    protected OkHttpClient getHttpClient() {
        return httpClientB.build();
    }

    /**
     * Setup network interceptor.
     */
    public static void setup(){
        httpClientB.followRedirects(false);
    }

    @Override
    public void send(final ResponseListener listener) {
        super.send(listener);
    }

    @Override
    public void send(Map<String, String> formParameters, ResponseListener listener) {
        super.send(formParameters, listener);
    }


}
