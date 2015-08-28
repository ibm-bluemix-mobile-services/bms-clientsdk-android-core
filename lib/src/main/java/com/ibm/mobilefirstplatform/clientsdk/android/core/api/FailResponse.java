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

/**
 * This class adds additional methods to Response for details regarding the failure of the request.
 */
public class FailResponse extends Response {

    /**
     * Error codes explaining why the request failed.
     */
    public enum ErrorCode {
        /**
         * The client failed to connect to the server. Possible reasons include connection timeout,
         * DNS failures, secure connection problems, etc.
         */
        UNABLE_TO_CONNECT,
        /**
         * The server responded with a failure code.
         */
        SERVER_ERROR
    }

    private ErrorCode errorCode;


    protected FailResponse(ErrorCode errorCode) {
        this(errorCode, (com.squareup.okhttp.Response)null);
    }

    protected FailResponse(ErrorCode errorCode, com.squareup.okhttp.Response response) {
        super(response);

        this.errorCode = errorCode;
    }

    public FailResponse(ErrorCode errorCode, Response response) {
        this(errorCode, response != null ? response.getInternalResponse() : null);
    }

    /**
     * Get the error code for the cause of the failure.
     * @return The error cause for the cause of the failure
     */
    public ErrorCode getErrorCode(){
        return errorCode;
    }
}
