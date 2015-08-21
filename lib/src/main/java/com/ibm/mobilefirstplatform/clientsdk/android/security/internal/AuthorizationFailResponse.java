package com.ibm.mobilefirstplatform.clientsdk.android.security.internal;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.FailResponse;

/**
 * Created by vitalym on 8/11/15.
 */
public class AuthorizationFailResponse extends FailResponse{
    protected AuthorizationFailResponse(ErrorCode errorCode) {
        super(errorCode, null);
    }
}
