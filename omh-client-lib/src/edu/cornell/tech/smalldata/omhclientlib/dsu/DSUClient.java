package edu.cornell.tech.smalldata.omhclientlib.dsu;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;

import java.io.IOException;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.plus.Plus;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class DSUClient {
	private static final String BASE_URL = "https://ohmage-omh.smalldata.io/dsu";
    public static final String CLIENT_ID = "io.smalldata.android.pam";
    public static final String CLIENT_SECRET = "5Eo43jkLD7z76c";
    public static final String CLIENT_AUTHORIZATION =
            Base64.encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(), Base64.DEFAULT);

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final static OkHttpClient client = new OkHttpClient();

    public static Response postData(String accessToken, String json) throws IOException {
		return null;
//        RequestBody body = RequestBody.create(JSON, json);
//        Request request = new Request.Builder()
//                .url(BASE_URL + "/dataPoints")
//                .header("Authorization", "Bearer " + accessToken)
//                .post(body)
//                .build();
//        return client.newCall(request).execute();
    }

    public static Response signin(String googleToken) throws IOException {
		RequestBody body = new FormEncodingBuilder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("access_token", googleToken)
                .build();
        Request request = new Request.Builder()
                .url(getUrl() + "/social-signin/google")
                .post(body)
                .build();
        return client.newCall(request).execute();
    }
    
    public static Response refreshToken(String refreshToken) throws IOException {
		return null;
//        RequestBody body = new FormEncodingBuilder()
//                .add("grant_type", "refresh_token")
//                .add("refresh_token", refreshToken)
//                .build();
//        Request request = new Request.Builder()
//                .header("Authorization", "Basic " + CLIENT_AUTHORIZATION)
//                .url(BASE_URL + "/oauth/token")
//                .post(body)
//                .build();
//        return client.newCall(request).execute();
    }
    
    public static boolean isSignedIn(Context context, String accountType){
    	AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] omhClientLibAccounts = accountManager.getAccountsByType(accountType);
        
        if (omhClientLibAccounts.length == 0) {
        	return false;
        } else {
        	return true;
        }
    }
    
    private static GoogleApiClient mGoogleApiClient;
    public static void logout(final Context context, String accountType) {
    	// Remove account
    	AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] omhClientLibAccounts = accountManager.getAccountsByType(accountType);
        
        if (omhClientLibAccounts.length != 0) {
        	final Account account = omhClientLibAccounts[0];
        	
        	accountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
				@Override
				public void run(AccountManagerFuture<Boolean> future) {
					try {
						if(future.getResult()) {
							// TODO JARED: We need to revoke access and clear token for Google+ user. Would use the following
							// lines, but need to create GoogleApiClient and do a bunch of callbacks. Cached token
							// is cleared in DSUAuthActivity if request fails.
							//
							//  Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient); // don't need this one
							//  Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
							//  GoogleAuthUtil.clearToken(context, googleAccessToken);
						}else{
							throw new Exception("Logout failed.");
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}, null);
        }
    }
    
    public static String getUrl(){
    	return BASE_URL;
    }

}
