package com.ibm.mobilefirstplatform.clientsdk.android.core.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.internal.BaseRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

public class MainActivity extends Activity implements ResponseListener{

    //private final static String backendURL = "http://9.148.225.153:9080"; // your BM application URL
    //private final static String backendURL = "http://bmscord.mybluemix.net";
    private final static String backendURL = "http://bmscord.mybluemix.net";
    private final static String backendGUID = "86c972e0-0d74-4651-b2bd-f8de74c10e32"; // the GUID you get from the dashboard
    private final static String customResourceURL = "http://bmscord.mybluemix.net/protected"; // any protected resource
    private final static String customRealm = "customAuthRealm_1"; // auth realm


	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



		try {
			BMSClient.getInstance().initialize(getApplicationContext(), backendURL, backendGUID);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

        /*
        BMSClient.getInstance().registerAuthenticationListener(customRealm, new AuthenticationListener() {
            @Override
            public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {

            }

            @Override
            public void onAuthenticationSuccess(Context context, JSONObject info) {

            }

            @Override
            public void onAuthenticationFailure(Context context, JSONObject info) {

            }
        });
        */

        // to make the authorization happen next time
        AuthorizationManager.getInstance().clearAuthorizationData();
        
        Request r = new Request(customResourceURL, Request.GET);
        r.send(this, this);
        /*
        AuthorizationManager.getInstance().obtainAuthorizationHeader(this, new ResponseListener(){

            @Override
            public void onSuccess(Response response) {

            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {

            }
        });
        */
    }

	@Override
	public void onSuccess(Response response) {
        // here we handle authentication success
        if (response.getStatus() == 303) {
            Request r = new Request(customResourceURL, Request.GET);
            r.send(this, this);
        }

	}

	@Override
	public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
        // handle auth failure
	}
}

class CustomChallengeHandler implements AuthenticationListener {

    @Override
    public void onAuthenticationChallengeReceived(AuthenticationContext authContext, JSONObject challenge, Context context) {
        try {
            // provide your custom credentials here
            // you can display a dialog here to obtain user name and password
            JSONObject answer = new JSONObject("{\"userName\":\"asaf\",\"password\":\"123\"}");

            // submit the credentials obtained from the user
            authContext.submitAuthenticationChallengeAnswer(answer);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAuthenticationSuccess(Context context, JSONObject info) {

    }

    @Override
    public void onAuthenticationFailure(Context context, JSONObject info) {

    }
}
