package io.smalldatalab.omhclient;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This activity starts the Google authentication steps immediately, rather
 * than waiting for the user to initiate with a button, because Google authentication
 * is the only option, currently.
 *
 * @author jaredsieling
 */
public class DSUAccountAuthActivity extends AccountAuthenticatorActivity  {

    /* Request code used to invoke sign in user interactions. */
    private static final int REQUEST_CODE_RESOLVE_ISSUE = 1;
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;

    private String dsuAuthorizationMethod;

    /* Response code used to communicate the sign in result */
    public static final int FAILED_TO_GET_AUTH_CODE = 2;
    public static final int FAILED_TO_SIGN_IN = 3;
    public static final int INVALID_ACCESS_TOKEN = 4;



    private boolean mIntentInProgress;
    private String scope;


    final static String TAG = DSUAccountAuthActivity.class.getSimpleName();
    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progress = new ProgressDialog(this);
        progress.setTitle(getString(io.smalldatalab.omhclient.R.string.signin_progress_dialog_title));
        progress.setMessage(getString(io.smalldatalab.omhclient.R.string.signin_progress_dialog_message));
        progress.show();

        scope = getString(R.string.google_signin_scope);

        // Check which signin method to use
        Bundle options = DSUAccountAuthActivity.this.getIntent().getBundleExtra("options");
        dsuAuthorizationMethod = options.getString(DSUClient.DSU_AUTHORIZATION_METHOD_KEY);

        if(dsuAuthorizationMethod.equals(DSUClient.AUTHORIZATION_METHOD_OMH)){
            if (!mIntentInProgress) {
                mIntentInProgress = true;
                new OmhSignIn().execute();
            }
        } else {
            dsuAuthorizationMethod = DSUClient.AUTHORIZATION_METHOD_GOOGLE;
            startAccountPicker();
        }
    }

    protected void onStart() {
        super.onStart();

    }

    protected void onStop() {
        super.onStop();
    }
    void startAccountPicker (){
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, true, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }
    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            if (dsuAuthorizationMethod.equals(DSUClient.AUTHORIZATION_METHOD_GOOGLE)) {
                // Receiving a result from the AccountPicker
                if (responseCode == RESULT_OK) {
                    Account googleAccount =
                            new Account(
                            intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),
                            intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
                    // With the account name acquired, go get the auth token
                    new GoogleSignIn().execute(googleAccount);
                } else if (responseCode == RESULT_CANCELED) {
                    // The account picker dialog closed without selecting an account.
                    // Notify users that they must pick an account to proceed.
                    Toast.makeText(this, R.string.pick_account, Toast.LENGTH_SHORT).show();
                    finish();
               }

            }
        } else if (requestCode == REQUEST_CODE_RESOLVE_ISSUE) {
            mIntentInProgress = true;
            if (responseCode != RESULT_OK) {
                onDsuAuthFailed(FAILED_TO_GET_AUTH_CODE);
            } else {
                startAccountPicker();
            }
        }
    }



    @Override
    public void finish() {
        progress.dismiss();
        super.finish();
    }

    public void onDsuAuthFailed(final int reason) {
        setResult(FAILED_TO_SIGN_IN);
        finish();
    }


    private class GoogleSignIn extends AsyncTask<Account, Void, Void> {

        @Override
        protected Void doInBackground(Account... params) {
            Account googleAccount = params[0];

            // ** Step 1. Obtain Google Access Token **
            Context cxt = DSUAccountAuthActivity.this;
            try {
                String googleAccessToken = GoogleAuthUtil.getToken(DSUAccountAuthActivity.this, googleAccount, scope);
                Bundle options = DSUAccountAuthActivity.this.getIntent().getBundleExtra("options");
                DSUClient client = DSUClient.getDSUClientFromOptions(options, DSUAccountAuthActivity.this);

                Response response = client.signinGoogle(googleAccessToken);
                // clear token and default account from cache immediately to avoid stable state in the future
                GoogleAuthUtil.clearToken(DSUAccountAuthActivity.this, googleAccessToken);


                String responseBody = "";
                if (response != null && response.isSuccessful()) {
                    try {
                        responseBody = response.body().string();
                        // use the google access token to sign in the dsu
                        JSONObject token = new JSONObject(responseBody);
                        final String accessToken = token.getString(DSUAuth.ACCESS_TOKEN_TYPE);
                        final String refreshToken = token.getString(DSUAuth.REFRESH_TOKEN_TYPE);
                        // Get an instance of the Android account manager
                        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                        final Account dsuAccount = DSUAuth.getDefaultAccount(cxt);
                        // set options bundle as the userdata
                        accountManager.addAccountExplicitly(dsuAccount, null, options);

                        // make the account syncable and automatically synced
                        // TODO JARED: this is only used to access the SyncAdapter. Do we want to make it generic?
                        // TODO ANDY: I make authorities to be string value in the xml configuration
                        String providerAuthorities = DSUAuth.getDSUProviderAuthorities(cxt);
                        ContentResolver.setIsSyncable(dsuAccount, providerAuthorities, 1);
                        ContentResolver.setSyncAutomatically(dsuAccount, providerAuthorities, true);
                        ContentResolver.setMasterSyncAutomatically(true);

                        accountManager.setAuthToken(dsuAccount, DSUAuth.ACCESS_TOKEN_TYPE, accessToken);
                        accountManager.setAuthToken(dsuAccount, DSUAuth.REFRESH_TOKEN_TYPE, refreshToken);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent();
                                i.putExtra(AccountManager.KEY_ACCOUNT_NAME, dsuAccount.name);
                                i.putExtra(AccountManager.KEY_ACCOUNT_TYPE, dsuAccount.type);
                                DSUAccountAuthActivity.this.setAccountAuthenticatorResult(i.getExtras());
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Fail to parse response from google-sign-in endpoint:" + responseBody, e);
                        onDsuAuthFailed(INVALID_ACCESS_TOKEN);
                    }
                } else {
                    Log.e(TAG, "Failed to sign in: " + responseBody);
                    onDsuAuthFailed(FAILED_TO_SIGN_IN);
                }

                // ** Step 3. Check Returned Access Tokens **

            } catch (UserRecoverableAuthException e) {
                // Requesting an authorization code will always throw
                // UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
                // because the user must consent to offline access to their data.  After
                // consent is granted control is returned to your activity in onActivityResult
                // and the second call to GoogleAuthUtil.getToken will succeed.
                DSUAccountAuthActivity.this.startActivityForResult(e.getIntent(), REQUEST_CODE_RESOLVE_ISSUE);

            } catch (Exception e) {
                Log.e(TAG, "Failed to sign in", e);
                onDsuAuthFailed(FAILED_TO_SIGN_IN);
            }
            return null;
        }


    }

    private class OmhSignIn extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... _) {

            Context cxt = DSUAccountAuthActivity.this;
            try {
                Bundle options = DSUAccountAuthActivity.this.getIntent().getBundleExtra("options");
                DSUClient client = DSUClient.getDSUClientFromOptions(options, DSUAccountAuthActivity.this);

                Response response = client.signinOmh(options.getString(DSUClient.TEMP_USERNAME_KEY), options.getString(DSUClient.TEMP_PW_KEY));
                String responseBody = "";
                if (response != null && response.isSuccessful()) {
                    try {
                        responseBody = response.body().string();
                        JSONObject responseJson = new JSONObject(responseBody);

                        // Get an instance of the Android account manager
                        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                        final Account account = DSUAuth.getDefaultAccount(cxt);
                        // set options bundle as the userdata
                        accountManager.addAccountExplicitly(account, null, options);

                        // make the account syncable and automatically synced
                        String providerAuthorities = DSUAuth.getDSUProviderAuthorities(cxt);
                        ContentResolver.setIsSyncable(account, providerAuthorities, 1);
                        ContentResolver.setSyncAutomatically(account, providerAuthorities, true);
                        ContentResolver.setMasterSyncAutomatically(true);

                        accountManager.setAuthToken(account, DSUAuth.ACCESS_TOKEN_TYPE, responseJson.getString("access_token"));
                        accountManager.setAuthToken(account, DSUAuth.REFRESH_TOKEN_TYPE, responseJson.getString("refresh_token"));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent();
                                i.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
                                i.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                                DSUAccountAuthActivity.this.setAccountAuthenticatorResult(i.getExtras());
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Fail to parse response from omh-sign-in endpoint:" + responseBody, e);
                        onDsuAuthFailed(INVALID_ACCESS_TOKEN);
                    }
                } else {
                    Log.e(TAG, "Failed to sign in: " + responseBody);
                    onDsuAuthFailed(FAILED_TO_SIGN_IN);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to sign in", e);
                onDsuAuthFailed(FAILED_TO_SIGN_IN);
            }
            return null;
        }


    }
}
