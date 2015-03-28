package io.smalldatalab.omhclient;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DSUAuthenticator extends AbstractAccountAuthenticator {

    private Context mContext;
    private final static String TAG = DSUAuthenticator.class.getSimpleName();
    public DSUAuthenticator(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
                                 String accountType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType, String authTokenType,
                             String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {

        final Intent intent = new Intent(mContext, DSUAccountAuthActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra("options", options);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                     Account account, Bundle options) throws NetworkErrorException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse res,
                               Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {
        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        Log.v(TAG, "******* Get access token start ******** ");
        final AccountManager am = AccountManager.get(mContext);
        String authToken = am.peekAuthToken(account, DSUAuth.ACCESS_TOKEN_TYPE);
        String refreshToken = am.peekAuthToken(account, DSUAuth.REFRESH_TOKEN_TYPE);


        if (TextUtils.isEmpty(authToken)) {
            // access token not found
            if (refreshToken != null) {
                // Lets try to refresh the token
                String responseBody = "";
                try {
                    DSUClient client = DSUClient.getDSUClientFromUserData(account, mContext);
                    Response response = client.refreshToken(refreshToken);
                    responseBody = response.body().string();
                    JSONObject token = new JSONObject(responseBody);
                    authToken = token.getString(DSUAuth.ACCESS_TOKEN_TYPE);
                    refreshToken = token.getString(DSUAuth.REFRESH_TOKEN_TYPE);
                    am.setAuthToken(account, DSUAuth.ACCESS_TOKEN_TYPE, authToken);
                    am.setAuthToken(account, DSUAuth.REFRESH_TOKEN_TYPE, refreshToken);
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Cannot refresh token with refresh token: " + refreshToken +
                            " Response is:" + responseBody, e);
                }
            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);

            Log.v(TAG, "******* Get access token succeeded ********");
            return result;
        }

        // If we get here, then we couldn't refresh the access token - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(mContext, DSUAccountAuthActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, res);
        final Bundle b = new Bundle();
        b.putParcelable(AccountManager.KEY_INTENT, intent);
        Log.e(TAG, "******* Get access token failed! ********");
        return b;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
                              Account account, String[] features) throws NetworkErrorException {
        // TODO Auto-generated method stub
        return null;
    }

}
