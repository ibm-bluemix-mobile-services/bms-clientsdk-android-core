package com.ibm.mobilefirstplatform.clientsdk.android.core.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.FailResponse;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.MFPRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResourceRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

public class MainActivity extends Activity implements ResponseListener{

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		try {
			BMSClient.getInstance().initialize("http://9.148.225.106:9080", "vit1");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

        BMSClient.getInstance().registerAuthenticationListener("customAuthRealm_1", new CustomChallengeHandler());

		AuthorizationManager.createInstance(this.getApplicationContext());
		AuthorizationManager.getInstance().obtainAuthorizationHeader(this, this);

        try {
            ResourceRequest r = new ResourceRequest(this, "http://9.148.225.106:3000/v1/apps/vit1/service", MFPRequest.GET);
            r.send(this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

	@Override
	public void onSuccess(Response response) {

	}

	@Override
	public void onFailure(FailResponse response, Throwable t) {

	}
}

class CustomChallengeHandler implements AuthenticationListener {

    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {
        try {
            JSONObject answer = new JSONObject("{\"userName\":\"asaf\",\"password\":\"123\"}");
            authContext.submitAuthenticationChallengeAnswer(answer);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAuthenticationSuccess(JSONObject info) {

    }

    @Override
    public void onAuthenticationFailure(JSONObject info) {

    }
}
