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

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DSUAuthenticator extends AbstractAccountAuthenticator {

    private Context mContext;

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
        final AccountManager am = AccountManager.get(mContext);
        String authToken = am.peekAuthToken(account, DSUAuth.ACCESS_TOKEN_TYPE);
        String refreshToken = am.peekAuthToken(account, DSUAuth.REFRESH_TOKEN_TYPE);


        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            if (refreshToken != null) {
                try {
                    DSUClient client = DSUClient.getDSUClientFromUserData(account, mContext);
                    Response response = client.refreshToken(refreshToken);
                    JSONObject token = new JSONObject(response.body().string());
                    authToken = token.getString(DSUAuth.ACCESS_TOKEN_TYPE);
                    refreshToken = token.getString(DSUAuth.REFRESH_TOKEN_TYPE);
                    am.setAuthToken(account, DSUAuth.ACCESS_TOKEN_TYPE, authToken);
                    am.setAuthToken(account, DSUAuth.REFRESH_TOKEN_TYPE, refreshToken);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't refresh the access token - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(mContext, DSUAccountAuthActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, res);
        final Bundle b = new Bundle();
        b.putParcelable(AccountManager.KEY_INTENT, intent);
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
