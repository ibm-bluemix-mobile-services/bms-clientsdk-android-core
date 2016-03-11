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

package com.ibm.mobilefirstplatform.clientsdk.android.core.api;

import com.ibm.mobilefirstplatform.clientsdk.android.core.internal.AbstractClient;

public class MFPClient extends AbstractClient {
    protected String protocol;
    protected String hostName;
    protected String port;

    protected String deviceMetadataHeader;

    protected boolean initialized = false;

    protected static AbstractClient instance = null;

    /**
     * Should be called to obtain the instance of MFPClient.
     * @return the instance of MFPClient.
     */
    public static MFPClient getInstance() {
        if (instance == null) {
            instance = new MFPClient();
        }

        return (MFPClient)instance;
    }

    private MFPClient() {
    }

    public void initialize(String protocol, String hostName, String port, String deviceMetadataHeader){
        this.protocol = protocol;
        this.hostName = hostName;
        this.port = port;

        this.deviceMetadataHeader = deviceMetadataHeader;

        initialized = true;
    }

    public String getURL(){
        if(protocol == null || hostName == null) {
            return null;
        }

        String url = protocol + "://" + hostName;

        if(port != null){
            url += ":" + port;
        }

        return url;
    }

    public String getDeviceMetadataHeader(){
        return deviceMetadataHeader;
    }

    public boolean isInitialized(){
        return initialized;
    }
}
