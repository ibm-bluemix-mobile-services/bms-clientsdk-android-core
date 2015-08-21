package com.ibm.bms.clientsdk.android.security.internal;

import com.ibm.bms.clientsdk.android.core.api.FailResponse;

/**
 * Created by vitalym on 8/11/15.
 */
public class AuthorizationFailResponse extends FailResponse{
    protected AuthorizationFailResponse(ErrorCode errorCode) {
        super(errorCode, null);
    }
}
