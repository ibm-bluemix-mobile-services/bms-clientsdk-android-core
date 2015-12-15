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

package com.ibm.mobilefirstplatform.clientsdk.android.analytics.api;

import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public class NetworkLoggingInterceptor implements Interceptor{
    @Override public com.squareup.okhttp.Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        Logger logger = MFPAnalytics.logger;

        logger.analytics("BaseRequest outbound", null);

        long t1 = System.currentTimeMillis();

        String trackingid = UUID.randomUUID().toString();

        Request requestWithHeaders = request.newBuilder()
                .header("x-wl-analytics-tracking-id", trackingid)
                .build();

        com.squareup.okhttp.Response response = chain.proceed(requestWithHeaders);

        long t2 = System.currentTimeMillis();

        try {
            JSONObject metadata = new JSONObject();
            metadata.put("$url", request.urlString());
            metadata.put("$category", "network");
            metadata.put("$trackingid", trackingid);
            metadata.put("$outboundTimestamp", t1);
            metadata.put("$inboundTimestamp", t2);
            metadata.put("$duration", t2- t1);

            if(response != null){
                metadata.put("$statusCode", response.code());
            }

            if(response != null && response.body() != null && response.body().contentLength() >= 0){
                metadata.put("$bytesReceived", response.body().contentLength());
            }

            logger.analytics("BaseRequest inbound", metadata);
        } catch (JSONException e) {
            //Do nothing, since it is just for analytics.
        }

        return response;
    }
}
