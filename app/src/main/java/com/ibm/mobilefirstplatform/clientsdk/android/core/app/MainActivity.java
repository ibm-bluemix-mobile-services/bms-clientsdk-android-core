package com.ibm.mobilefirstplatform.clientsdk.android.core.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.ibm.bluemix.ssoservice.authorizationmanager.SSOAuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.AuthenticationContext;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.AuthenticationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.mca.api.MCAAuthorizationManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

public class MainActivity extends Activity{

	private final static Logger logger = Logger.getLogger("Anton");

//	private final static String backendRoute = "http://abms.mybluemix.net";
//	private final static String backendGuid = "2fe35477-51b0-4c87-803d-aca59511433b";
//	private final static String customRealm = "AntonRealm";

	private final static String backendRoute = "http://sso-backend.mybluemix.net";
	private final static String backendGuid = "";

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		try {
			BMSClient.getInstance().initialize(getApplicationContext(), backendRoute, backendGuid, BMSClient.REGION_US_SOUTH);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

//		BMSClient.getInstance().setAuthorizationManager(SSOAuthorizationManager.getInstance());
//		SSOAuthorizationManager.getInstance().setLoginUrl(backendRoute + "/login");

		BMSClient.getInstance().setAuthorizationManager(MCAAuthorizationManager.createInstance(this.getApplicationContext()));

		MCAAuthorizationManager.getInstance().registerAuthenticationListener(customRealm, new AuthenticationListener() {
			@Override
			public void onAuthenticationChallengeReceived (AuthenticationContext authContext, JSONObject challenge, Context context) {
				logger.error("onAuthenticationChallengeReceived :: " + challenge.toString());

				JSONObject challengeAnswer = new JSONObject();
				try {
					challengeAnswer.put("username", "john.lennon");
					challengeAnswer.put("password", "12345");
				} catch(JSONException e){
					logger.fatal("SHOULD NEVER HAPPEN!");
				}
				authContext.submitAuthenticationChallengeAnswer(challengeAnswer);
			}

			@Override
			public void onAuthenticationSuccess (Context context, JSONObject info) {
				logger.error("onAuthenticationSuccess :: " + info.toString());
			}

			@Override
			public void onAuthenticationFailure (Context context, JSONObject info) {
				logger.error("onAuthenticationFailure :: " + info.toString());
			}
		});

		BMSClient.getInstance().getAuthorizationManager().clearAuthorizationData();
	}

	public void StartButtonClicked (View view) {
		logger.info("StartButtonClicked");

		Request request = new Request("/protected", Request.GET);

		request.send(this, new ResponseListener() {
			@Override
			public void onSuccess (Response response) {
				logger.info("ResponseListener Success ::" + response.getResponseText());
			}

			@Override
			public void onFailure (Response response, Throwable t, JSONObject extendedInfo) {
				if (null != response){
					logger.error("ResponseListener Failure :: response :: " + response.getResponseText());
				} else if (null != t){
					logger.error("ResponseListenerFailure :: t :: " + t.getMessage());
				} else {
					logger.error("ResponseListener Failure :: extendedInfo :: " + extendedInfo.toString());
				}
			}
		});
	}
}

