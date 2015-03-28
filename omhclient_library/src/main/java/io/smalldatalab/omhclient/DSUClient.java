package io.smalldatalab.omhclient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * The main class of OmhClient library that provide simple methods for sign-in, sign-out,
 * and force-upload. Create a DSUClient with your DSU settings (url, dsu_client_id, secret).
 */
public class DSUClient {


    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final static OkHttpClient client = new OkHttpClient();

    public static final String DSU_URL_KEY = "dsu_url";
    public static final String DSU_CLIENT_ID_KEY = "dsu_client_id";
    public static final String DSU_CLIENT_SECRET_KEY = "dsu_client_secret";

    final private String dsu_url;
    final private String dsu_client_id;
    final private String dsu_client_secret;
    final private String dsu_client_auth;
    final private Account defaultAccount;
    final private Context cxt;
    final private AccountManager accountManager;

    private String getClientId() {
        return dsu_client_id;
    }

    private String getClientSecret() {
        return dsu_client_secret;
    }

    private String getClientAuthorization() {
        return dsu_client_auth;
    }

    /**
     * Create a DSU Client with a Bundle containing DSU settings. This method is called by
     * Authenticator which receives a Bundle when SignIn process
     *
     * @param options Bundle contains DSU settings
     * @param cxt     Context object
     * @return DSUClient with settings
     */
    protected static DSUClient getDSUClientFromOptions(Bundle options, Context cxt) {
        return new DSUClient(
                options.getString(DSU_URL_KEY),
                options.getString(DSU_CLIENT_ID_KEY),
                options.getString(DSU_CLIENT_SECRET_KEY),
                cxt);
    }

    /**
     * Create a DSU Client with options stored in an account's UserData.
     * These data are stored by the AccountAuthActivity when creating an account.
     * This method is called by SyncAdapter or Authenticator when upload data or refresh token.
     *
     * @param account user account
     * @param cxt     Context object
     * @return DSUClient with settings
     */
    protected static DSUClient getDSUClientFromUserData(Account account, Context cxt) {
        AccountManager accountManager = (AccountManager) cxt.getSystemService(Context.ACCOUNT_SERVICE);
        return new DSUClient(
                accountManager.getUserData(account, DSUClient.DSU_URL_KEY),
                accountManager.getUserData(account, DSUClient.DSU_CLIENT_ID_KEY),
                accountManager.getUserData(account, DSUClient.DSU_CLIENT_SECRET_KEY),
                cxt);
    }

    public DSUClient(String dsu_url, String dsu_client_id, String dsu_client_secret, Context cxt) {
        if (dsu_url == null || dsu_client_id == null || dsu_client_secret == null) {
            throw new RuntimeException("Missing dsu client settings!");
        }
        this.dsu_url = dsu_url;
        this.dsu_client_id = dsu_client_id;
        this.dsu_client_secret = dsu_client_secret;
        // USE NO_WRAP to avoid additional newline at the end.
        this.dsu_client_auth = Base64.encodeToString((dsu_client_id + ":" + dsu_client_secret).getBytes(), Base64.NO_WRAP);
        this.cxt = cxt;
        this.defaultAccount = DSUAuth.getDefaultAccount(cxt);
        this.accountManager = (AccountManager) cxt.getSystemService(Context.ACCOUNT_SERVICE);
    }

    protected Response postData(String accessToken, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(dsu_url + "/dataPoints")
                .header("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        return client.newCall(request).execute();
    }

    protected Response signin(String googleToken) throws IOException {
        RequestBody body = new FormEncodingBuilder()
                .add("client_id", getClientId())
                .add("client_secret", getClientSecret())
                .add("access_token", googleToken)
                .build();
        Request request = new Request.Builder()
                .url(dsu_url + "/social-signin/google")
                .post(body)
                .build();
        return client.newCall(request).execute();
    }

    protected Response refreshToken(String refreshToken) throws IOException {

        RequestBody body = new FormEncodingBuilder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();
        Request request = new Request.Builder()
                .header("Authorization", "Basic " + getClientAuthorization())
                .url(dsu_url + "/oauth/token")
                .post(body)
                .build();
        return client.newCall(request).execute();
    }


    /**
     * Sign in the DSU using Google Sign-in. Don't run this in the main UI thread.
     *
     * @param activity an Activity object that will be used to start the SignIn Activity
     * @return true if sign in succeeded otherwise false
     * @throws AuthenticatorException     error in authenticating the user using google sign in
     * @throws OperationCanceledException user cancel the sign in process
     * @throws IOException                network error
     */
    public Account blockingGoogleSignIn(final Activity activity) throws AuthenticatorException, OperationCanceledException, IOException {
        Bundle accountOptions = new Bundle();
        accountOptions.putString(DSU_URL_KEY, dsu_url);
        accountOptions.putString(DSU_CLIENT_ID_KEY, dsu_client_id);
        accountOptions.putString(DSU_CLIENT_SECRET_KEY, dsu_client_secret);

        AccountManagerFuture<Bundle> future = accountManager.addAccount(
                defaultAccount.type,
                DSUAuth.ACCESS_TOKEN_TYPE,
                null,
                accountOptions,
                activity, null, null);
        Bundle result = future.getResult();
        if (result.getString(AccountManager.KEY_ACCOUNT_NAME).equals(defaultAccount.name) &&
                result.getString(AccountManager.KEY_ACCOUNT_TYPE).equals(defaultAccount.type)) {
            return defaultAccount;
        } else {
            throw new AuthenticatorException("Wrong account name/type.");
        }

    }

    /**
     * Sign out user from the DSU.
     * @return true if sign out succeeded otherwise false
     * @throws AuthenticatorException error in authenticating the user using google sign in
     * @throws OperationCanceledException user cancel the sign out process
     * @throws IOException network error
     */
    public boolean blockingSignOut() throws AuthenticatorException, OperationCanceledException, IOException {

        // Remove account
        Account[] omhClientLibAccounts = accountManager.getAccountsByType(defaultAccount.type);
        if (omhClientLibAccounts.length != 0) {
            final Account account = omhClientLibAccounts[0];
            AccountManagerFuture<Boolean> future = accountManager.removeAccount(account, null, null);

            // TODO JARED: We need to revoke access and clear token for Google+ user. Would use the following
            // TODO ANDY: I did it in the DSUAccountAuthActivity immediately after receiving an token
            // lines, but need to create GoogleApiClient and do a bunch of callbacks. Cached token
            // is cleared in DSUAuthActivity if request fails.
            //
            //  Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient); // don't need this one
            //  Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            //  GoogleAuthUtil.clearToken(context, googleAccessToken);
            return future.getResult();
        }
        return true;
    }

    /**
     * Check if the user already sign in the DSU
     * @return true if the user already sign in
     */
    public boolean isSignedIn() {
        Account[] omhClientLibAccounts = accountManager.getAccountsByType(defaultAccount.type);
        if (omhClientLibAccounts.length == 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Start uploading the data to DSU immediately. do nothing if the user hasn't sign in.
     */
    public void forceSync() {
        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        /*
         * Request the sync for the default account, authority, and
         * manual sync settings
         */
        ContentResolver.requestSync(defaultAccount, DSUAuth.getDSUProviderAuthorities(cxt), settingsBundle);
    }


    @Override
    public String toString() {
        return "DSUClient{" +
                "dsu_url='" + dsu_url + '\'' +
                ", dsu_client_id='" + dsu_client_id + '\'' +
                ", dsu_client_secret='" + dsu_client_secret + '\'' +
                ", dsu_client_auth='" + dsu_client_auth + '\'' +
                ", defaultAccount=" + defaultAccount +
                '}';
    }
}
