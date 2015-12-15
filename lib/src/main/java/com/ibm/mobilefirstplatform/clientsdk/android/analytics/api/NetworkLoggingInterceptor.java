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

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public class NetworkLoggingInterceptor implements Interceptor{
    @Override public com.squareup.okhttp.Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        long startTime = System.currentTimeMillis();

        String trackingID = UUID.randomUUID().toString();

        Request requestWithHeaders = request.newBuilder()
                .header("x-wl-analytics-tracking-id", trackingID)
                .build();

        com.squareup.okhttp.Response response = chain.proceed(requestWithHeaders);


        if(MFPAnalytics.isRecordingNetworkEvents){
            JSONObject metadata = generateRoundTripRequestAnalyticsMetadata(request, startTime, trackingID, response);

            if(metadata != null){
                MFPAnalytics.log(metadata);
            }
        }

        return response;
    }

    protected JSONObject generateRoundTripRequestAnalyticsMetadata(Request request, long startTime, String trackingID, Response response) throws IOException {
        JSONObject metadata = new JSONObject();

        long endTime = System.currentTimeMillis();

        try {
            metadata.put("$path", request.urlString());
            metadata.put(MFPAnalytics.CATEGORY, "network");
            metadata.put("$trackingid", trackingID);
            metadata.put("$outboundTimestamp", startTime);
            metadata.put("$inboundTimestamp", endTime);
            metadata.put("$roundTripTime", endTime - startTime);

            if(response != null){
                metadata.put("$responseCode", response.code());
            }

            if(response != null && response.body() != null && response.body().contentLength() >= 0){
                metadata.put("$bytesReceived", response.body().contentLength());
            }

            return metadata;
        } catch (JSONException e) {
            //Do nothing, since it is just for analytics.
            return null;
        }
    }
}
