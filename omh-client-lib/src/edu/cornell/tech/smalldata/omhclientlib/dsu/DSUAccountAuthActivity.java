package edu.cornell.tech.smalldata.omhclientlib.dsu;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.squareup.okhttp.Response;

import edu.cornell.tech.smalldata.omhclientlib.R;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This activity starts the Google authentication steps immediately, rather
 * than waiting for the user to initiate with a button, because Google authentication
 * is the only option, currently.
 * 
 * @author jaredsieling
 *
 */
public class DSUAccountAuthActivity extends AccountAuthenticatorActivity implements
	GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

	/* Request code used to invoke sign in user interactions. */
	private static final int RC_SIGN_IN = 0;
	private static final int AUTH_CODE_REQUEST_CODE = 1;
	private static final int REQUEST_RESOLVE_ERROR = 2;

	/* Response code used to communicate the sign in result */
	public static final int FAILED_TO_GET_AUTH_CODE = 2;
	public static final int FAILED_TO_SIGN_IN = 3;
	public static final int INVALID_ACCESS_TOKEN = 4;

	/* Client used to interact with Google APIs. */
	private GoogleApiClient mGoogleApiClient;
	private boolean mIntentInProgress;

	final static String TAG = DSUAccountAuthActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.transparent);
		
		// Init google plus integration
		mGoogleApiClient = new GoogleApiClient.Builder(this)
		.addApi(Plus.API)
		.addScope(Plus.SCOPE_PLUS_LOGIN)
		.addConnectionCallbacks(this)
		.addOnConnectionFailedListener(this)
		.build();

	}

	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	protected void onStop() {
		super.onStop();

		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
		if (requestCode == RC_SIGN_IN) {
			mIntentInProgress = false;

			if (!mGoogleApiClient.isConnecting()) {
				mGoogleApiClient.connect();
			}
		} else if (requestCode == AUTH_CODE_REQUEST_CODE) {
			mIntentInProgress = true;
			if (responseCode != RESULT_OK) {
                onDsuAuthFailed(FAILED_TO_GET_AUTH_CODE);
            } else {
                new SignIn().execute();
            }
		}
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (!mIntentInProgress && result.hasResolution()) {
		    try {
		      mIntentInProgress = true;
		      result.startResolutionForResult(this, RC_SIGN_IN);
		    } catch (SendIntentException e) {
		      // The intent was canceled before it was sent.  Return to the default
		      // state and attempt to connect to get an updated ConnectionResult.
		      mIntentInProgress = false;
		      mGoogleApiClient.connect();
		    }
		  }
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		if (!mIntentInProgress){
			mIntentInProgress = true;
			new SignIn().execute();
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		mGoogleApiClient.connect();
	}
	
	public void onDsuAuthFailed(final int reason){
		setResult(this.FAILED_TO_SIGN_IN);
		finish();
	}
	
	
	private class SignIn extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... _) {

			// ** Step 1. Obtain Google Access Token **
			Bundle appActivities = new Bundle();
			String scopes = "oauth2:email profile";

			try {
				String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
				String googleAccessToken = GoogleAuthUtil.getToken(
						DSUAccountAuthActivity.this,                             // Context context
						accountName,  										// String accountName
						scopes,                                            // String scope
						appActivities                                      // Bundle bundle
						);
				Response response = DSUClient.signin(googleAccessToken);
				if(response != null){
					try {
						JSONObject token = new JSONObject(response.body().string());
						final String accessToken = token.getString(DSUAuth.ACCESS_TOKEN_TYPE);
						final String refreshToken = token.getString(DSUAuth.REFRESH_TOKEN_TYPE);
						// Get an instance of the Android account manager
						AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE); 

						accountManager.addAccountExplicitly(DSUAuth.ACCOUNT, null, null);

						// make the account syncable and automatically synced
						// TODO JARED: this is only used to access the SyncAdapter. Do we want to make it generic?
						ContentResolver.setIsSyncable(DSUAuth.ACCOUNT, DSUContentProvider.AUTHORITY, 1);
						ContentResolver.setSyncAutomatically(DSUAuth.ACCOUNT, DSUContentProvider.AUTHORITY, true);
						ContentResolver.setMasterSyncAutomatically(true);

						accountManager.setAuthToken(DSUAuth.ACCOUNT, "access_token", accessToken);
						accountManager.setAuthToken(DSUAuth.ACCOUNT, "refresh_token", refreshToken);

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Intent i = new Intent();
								i.putExtra(AccountManager.KEY_ACCOUNT_NAME, DSUAuth.ACCOUNT.name);
								i.putExtra(AccountManager.KEY_ACCOUNT_TYPE, DSUAuth.ACCOUNT.type);
								DSUAccountAuthActivity.this.setAccountAuthenticatorResult(i.getExtras());
								setResult(RESULT_OK);
								finish();
							}
						});
					} catch (JSONException e) {
						Log.e(TAG, "Fail to parse response from google-sign-in endpoint", e);
						GoogleAuthUtil.clearToken(DSUAccountAuthActivity.this, googleAccessToken); // clear from cache is it failed
						onDsuAuthFailed(INVALID_ACCESS_TOKEN);
					}
				}else{
					Log.e(TAG, response.body().string());
					onDsuAuthFailed(INVALID_ACCESS_TOKEN);
				}

				// ** Step 3. Check Returned Access Tokens **

			} catch (UserRecoverableAuthException e) {
				// Requesting an authorization code will always throw
				// UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
				// because the user must consent to offline access to their data.  After
				// consent is granted control is returned to your activity in onActivityResult
				// and the second call to GoogleAuthUtil.getToken will succeed.
				DSUAccountAuthActivity.this.startActivityForResult(e.getIntent(), AUTH_CODE_REQUEST_CODE);

			} catch (Exception e) {
				onDsuAuthFailed(FAILED_TO_SIGN_IN);
			}
			return null;
		}


	}

}
