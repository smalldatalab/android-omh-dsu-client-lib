package edu.cornell.tech.smalldata.omhclientlib;

import java.util.UUID;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;

public class AuthorizationCodeService extends Service implements ConnectionCallbacks, OnConnectionFailedListener {

	private static String LOG_TAG = AppConsts.APP_LOG_TAG;

	public enum State {
		INIT, 
		GOOGLE_API_CLIENT_BUILT, 
		GOOGLE_API_CLIENT_CONNECTING,
		GOOGLE_API_CLIENT_BUILD_FAILED,
		ENQUIRING_GOOGLE_AUTHORIZATION_CODE,
		USER_INTERVENTION_ON_GETTING_AUTHORIZATION_CODE,
		GOOGLE_AUTHORIZATION_CODE_RECEIVED,
		USER_INTERVENTION_ON_CONNECTION_FAILED,
		GOOGLE_AUTHORIZATION_CODE_STORED
	};

	private State mState;
	private Context mContext;
	private GoogleApiClient mGoogleApiClient;
	private BroadcastReceiver mUserInterventionScreenFinishedBR;
	private String mUuidString;
	private LocalBroadcastManager mLocalBroadcastManager;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		init();
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "finishing " + AuthorizationCodeService.class.getSimpleName());
	}

	public void init() {

		mUuidString = UUID.randomUUID().toString();

		setState(State.INIT);

		mContext = getApplicationContext();

		mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);

		mUserInterventionScreenFinishedBR = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				onReceiveUserInterventionScreenFinished(context, intent);
			}
		};

		mGoogleApiClient = buildGoogleApiClient();

		if (mGoogleApiClient != null) {
			setState(State.GOOGLE_API_CLIENT_BUILT);

			setState(State.GOOGLE_API_CLIENT_CONNECTING);
			mGoogleApiClient.connect();

		} else {
			setState(State.GOOGLE_API_CLIENT_BUILD_FAILED);
			stopSelf();
		}

	}

	private void setState(State state) {
		mState = state;
		Log.d(LOG_TAG, "State: " + state.toString());
	}

	private void enquireAuthorizationCodeInBackground() {

		Object[] asyncTaskParams = new Object[2];
		asyncTaskParams[0] = mContext;
		asyncTaskParams[1] = mGoogleApiClient;

		setState(State.ENQUIRING_GOOGLE_AUTHORIZATION_CODE);

		new AsyncTask<Object, Void, Void>() {
			@Override
			protected Void doInBackground(Object... params) {

				return enquireAuthorizationCode(params);
			}
		}.execute(asyncTaskParams);

	}

	public Void enquireAuthorizationCode(Object... params) {

		Object object = params[0];
		Context context = (Context) object;
		object = params[1];
		GoogleApiClient googleApiClient = (GoogleApiClient) object;

		Bundle appActivitiesBundle = new Bundle();
		appActivitiesBundle = null;

		String dsuGoogleClientId = context.getString(R.string.dsu_google_client_id);
		String authScopes = context.getString(R.string.dsu_google_auth_scope);
		String scopes = "oauth2:server:client_id:" + dsuGoogleClientId + ":api_scope:" + authScopes;
		String accountName = Plus.AccountApi.getAccountName(googleApiClient);

		try {
			Log.d(LOG_TAG, "accountName: " + accountName);
			Log.d(LOG_TAG, "authScopes: " + authScopes);
			Log.d(LOG_TAG, "scopes: " + scopes);

			String authorizationCode = GoogleAuthUtil.getToken(context, accountName, scopes, appActivitiesBundle);

			setState(State.GOOGLE_AUTHORIZATION_CODE_RECEIVED);
			Log.d(LOG_TAG, "authorizationCode:" + authorizationCode);
			
			storeAuthorizationCode(authorizationCode);

			GoogleAuthUtil.clearToken(context, authorizationCode);
			
			stopSelf();

		} catch (UserRecoverableAuthException e) {
			// Requesting an authorization code will always throw
			// UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
			// because the user must consent to offline access to their data.  After
			// consent is granted control is returned to your activity in onActivityResult
			// and the second call to GoogleAuthUtil.getToken will succeed.

			Intent userInterventionIntent = e.getIntent();

			Intent startUserInterventionActivityIntent = new Intent(context, StartUserInterventionActivity.class);
			
			startUserInterventionActivityIntent.putExtra(AppConsts.EXTRA_USER_INTERVENTION_INTENT, userInterventionIntent);
			startUserInterventionActivityIntent.putExtra(AppConsts.EXTRA_DSU_INSTANCE_IDENTIFIER, mUuidString );
			
			startUserInterventionActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			context.startActivity(startUserInterventionActivityIntent);

			setState(State.USER_INTERVENTION_ON_GETTING_AUTHORIZATION_CODE);

			IntentFilter intentFilter = new IntentFilter(AppConsts.ACTION_USER_INTERVENTION_SCREEN_FINISHED);
			mLocalBroadcastManager.registerReceiver(mUserInterventionScreenFinishedBR, intentFilter);

		} catch(Throwable tr){
			Log.e(LOG_TAG, "Error at requiring authorization code", tr);
		}

		return null;

	}

	private void storeAuthorizationCode(String authorizationCode) {

		SharedPreferences dsuSharedPreferences = getSharedPreferences(AppConsts.SHARED_PREFERENCES_DSU, Context.MODE_PRIVATE);
		Editor editor = dsuSharedPreferences.edit();

		if (authorizationCode != null) {
			editor.putString(AppConsts.PREFERENCES_KEY_AUTHORIZATION_CODE, authorizationCode);
			setState(State.GOOGLE_AUTHORIZATION_CODE_STORED);
		}
		
		editor.commit();
		
	}

	private void onReceiveUserInterventionScreenFinished(Context context, Intent intent) {

		boolean ok = false;
		if (intent.hasExtra(AppConsts.EXTRA_DSU_INSTANCE_IDENTIFIER)) {
			String dsuInstanceIdentifier = intent.getStringExtra(AppConsts.EXTRA_DSU_INSTANCE_IDENTIFIER);
			if (dsuInstanceIdentifier.equals(mUuidString)) {
				ok = true;
			}
		}
		if (!ok) return;

		switch (mState) {
		case USER_INTERVENTION_ON_GETTING_AUTHORIZATION_CODE:

			enquireAuthorizationCodeInBackground();

			break;
		case USER_INTERVENTION_ON_CONNECTION_FAILED:

			setState(State.GOOGLE_API_CLIENT_CONNECTING);
			mGoogleApiClient.connect();

			break;
		default:
			break;
		}

		mLocalBroadcastManager.unregisterReceiver(mUserInterventionScreenFinishedBR);

	}

	private GoogleApiClient buildGoogleApiClient() {
		GoogleApiClient googleApiClient = null;

		try {

			Builder googleApiClientBuilder = new GoogleApiClient.Builder(mContext);
			googleApiClientBuilder.useDefaultAccount()
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.addApi(Plus.API)
			.addScope(Plus.SCOPE_PLUS_LOGIN);

			googleApiClient = googleApiClientBuilder.build();

		} catch (Throwable tr) {
			Log.e(LOG_TAG, "Error at building Google api client", tr);
		}

		return googleApiClient;
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		
		if (connectionResult.hasResolution()) {

			PendingIntent resolutionPendingIntent = connectionResult.getResolution();

			Intent startUserInterventionActivityIntent = new Intent(mContext, StartUserInterventionActivity.class);
			
			startUserInterventionActivityIntent.putExtra(AppConsts.EXTRA_USER_INTERVENTION_PENDING_INTENT, resolutionPendingIntent);
			startUserInterventionActivityIntent.putExtra(AppConsts.EXTRA_DSU_INSTANCE_IDENTIFIER, mUuidString );
			
			startUserInterventionActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			mContext.startActivity(startUserInterventionActivityIntent);

			setState(State.USER_INTERVENTION_ON_CONNECTION_FAILED);

			IntentFilter intentFilter = new IntentFilter(AppConsts.ACTION_USER_INTERVENTION_SCREEN_FINISHED);
			mLocalBroadcastManager.registerReceiver(mUserInterventionScreenFinishedBR, intentFilter);
			
		} else {
			stopSelf();
		}
			

	}

	@Override
	public void onConnected(Bundle arg0) {

		enquireAuthorizationCodeInBackground();

	}

	@Override
	public void onConnectionSuspended(int arg0) {
		
		stopSelf();

	}

}
