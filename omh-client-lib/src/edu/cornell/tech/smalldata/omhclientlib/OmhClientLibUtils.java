package edu.cornell.tech.smalldata.omhclientlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;
import android.util.Log;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.ExchangingAuthCodeForTokensException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HttpPostRequestFailedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoAccessTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoRefreshTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.TokensResponseNotValidJsonException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnauthorizedWriteAttemptException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnsuccessfulWriteToDsuException;

public class OmhClientLibUtils {

	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	/**
	 * Sends HTTP POST with datapoint payload to DSU.
	 * @param entity
	 * @param context
	 * @throws NoAccessTokenException
	 * @throws HttpPostRequestFailedException
	 * @throws UnsuccessfulWriteToDsuException
	 */
	public static void writeDataPointRequest(String entity, Context context)
			throws NoAccessTokenException, HttpPostRequestFailedException, UnsuccessfulWriteToDsuException, UnauthorizedWriteAttemptException {
		
		StringBuilder urlStringBuilder = new StringBuilder();
		urlStringBuilder.append(context.getString(R.string.dsu_root_url)).append('/').append("dataPoints");
	
		final String urlString = urlStringBuilder.toString();
		Log.d(LOG_TAG, urlString);
	
		HttpClient httpClient = new DefaultHttpClient();
		
		HttpPost httpPost = new HttpPost(urlString);
		String accessToken = readAccessToken(context);
		if (accessToken == null) {
			throw new NoAccessTokenException();
		}
		String authorization = "Bearer " + accessToken;
		httpPost.setHeader("Authorization", authorization);
		Log.d(LOG_TAG, "Header Authorization: " + authorization);
		
		httpPost.setHeader("Content-type", "application/json");
		
		HttpResponse httpResponse = null;
	
		InputStream responseContentInputStream = null;
		InputStreamReader isr = null;
		
		try {
			
			httpPost.setEntity(new StringEntity(entity));
			Log.d(LOG_TAG, entity.substring(0, 50) + "...");
	
			httpResponse = httpClient.execute(httpPost);
			StringBuilder sb = new StringBuilder();
	
			if (httpResponse != null) {
	
				responseContentInputStream = httpResponse.getEntity().getContent();
				isr = new InputStreamReader(responseContentInputStream);
				
				char[] c = new char[8];
				int len;
				while ((len = isr.read(c)) != -1) {
					sb.append(c, 0, len);
				}
	
			}
	
			Log.d(LOG_TAG, "Response: " + sb.toString());
			StatusLine statusLine = httpResponse.getStatusLine();
			Log.d(LOG_TAG, String.format("status code: %d reason: %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
			if (HttpStatus.SC_UNAUTHORIZED == statusLine.getStatusCode()) {
				UnauthorizedWriteAttemptException e = new UnauthorizedWriteAttemptException();
				throw e;
			} else if (HttpStatus.SC_CREATED != statusLine.getStatusCode()) {
				UnsuccessfulWriteToDsuException e = new UnsuccessfulWriteToDsuException();
				throw e;
			}
	
		} catch (UnsuccessfulWriteToDsuException | UnauthorizedWriteAttemptException e) {
			throw e;
		} catch (Throwable tr) {
			HttpPostRequestFailedException e = new HttpPostRequestFailedException();
			e.addSuppressed(tr);
			throw e;
		}
		finally {
			try { 
				isr.close();
				isr = null;
				responseContentInputStream.close();
				responseContentInputStream = null; 
			}
			catch (IOException e) {
				Log.e(LOG_TAG, "Exception at closing http response input stream and reader.", e);
			}
		}
	}

	private static String readAccessToken(Context context) {
		
		String accessToken = null;
		
		SharedPreferences dsuSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
	
		accessToken = dsuSharedPreferences.getString(OmhClientLibConsts.PREFERENCES_KEY_DSU_ACCESS_TOKEN, null);
	
		return accessToken;
	}

	private static String readRefreshToken(Context context) {
		
		String refreshToken = null;
		
		SharedPreferences dsuSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
	
		refreshToken = dsuSharedPreferences.getString(OmhClientLibConsts.PREFERENCES_KEY_DSU_REFRESH_TOKEN, null);
	
		return refreshToken;
	}

	public static synchronized String installationId(Context context) {
		String installationId = null;
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);

		installationId = libSharedPreferences.getString(OmhClientLibConsts.PREFERENCES_KEY_INSTALLATION_ID, null);
		if (installationId == null) {
			installationId = createInstallationId(context);
		}
		
		return installationId;
	}

	private static synchronized String createInstallationId(Context context) {
		String installationId = null;
		
		installationId = UUID.randomUUID().toString();
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
		Editor editor = libSharedPreferences.edit();
		
		editor.putString(OmhClientLibConsts.PREFERENCES_KEY_INSTALLATION_ID, installationId);
		
		editor.commit();
		
		return installationId;
	}
	
	public static synchronized String dataPointSequence(Context context) {
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);

		int dataPointSequence = libSharedPreferences.getInt(OmhClientLibConsts.PREFERENCES_KEY_DATA_POINT_SEQUENCE, 0);
		
		Editor editor = libSharedPreferences.edit();
		editor.putInt(OmhClientLibConsts.PREFERENCES_KEY_DATA_POINT_SEQUENCE, ++dataPointSequence);
		editor.commit();
		
		return String.format("%010d", dataPointSequence);
	}

	public static String nextDataPointId(Context context) {
		
		return installationId(context) + dataPointSequence(context);
		
	}

	public static void refreshAaccessTokenRequest(Context context)
			throws HttpPostRequestFailedException, NoRefreshTokenException {
		
		StringBuilder urlStringBuilder = new StringBuilder();
		urlStringBuilder.append(context.getString(R.string.dsu_root_url)).append('/').append("oauth/token");
	
		final String urlString = urlStringBuilder.toString();
		Log.d(LOG_TAG, urlString);
	
		HttpClient httpClient = new DefaultHttpClient();
		
		HttpPost httpPost = new HttpPost(urlString);
		
		List<NameValuePair> nameValuePairsList = new ArrayList<NameValuePair>(2);
		String refreshToken = readRefreshToken(context);
		if (refreshToken == null) {
			throw new NoRefreshTokenException();
		}
		nameValuePairsList.add(new BasicNameValuePair("refresh_token", refreshToken));
		
		nameValuePairsList.add(new BasicNameValuePair("grant_type", "refresh_token"));
		
		HttpResponse httpResponse = null;
	
		InputStream responseContentInputStream = null;
		InputStreamReader isr = null;
		String response = null; 
		
		try {
			
			String value = context.getString(R.string.apps_dsu_client_id) + ":" + context.getString(R.string.apps_dsu_client_secret);
			byte[] valueByteArray;
			valueByteArray = value.getBytes("UTF-8");
			String encodedValue = "Basic " + Base64.encodeToString(valueByteArray, Base64.URL_SAFE | Base64.NO_WRAP);
			httpPost.setHeader("Authorization", encodedValue);
			
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairsList));
	
			httpResponse = httpClient.execute(httpPost);
			StringBuilder sb = new StringBuilder();
	
			if (httpResponse != null) {
				responseContentInputStream = httpResponse.getEntity().getContent();
				isr = new InputStreamReader(responseContentInputStream);
				
				char[] c = new char[8];
				int len;
				while ((len = isr.read(c)) != -1) {
					sb.append(c, 0, len);
				}
			}
	
			response = sb.toString();
			Log.d(LOG_TAG, "Response: " + response);
			
			storeTokens(response, context);
	
		} catch (Throwable tr) {
			HttpPostRequestFailedException e = new HttpPostRequestFailedException();
			e.addSuppressed(tr);
			throw e;
		}
		finally {
			try { 
				isr.close();
				isr = null;
				responseContentInputStream.close();
				responseContentInputStream = null; 
			}
			catch (IOException e) {
				Log.e(LOG_TAG, "Exception at closing http response input stream and reader.", e);
			}
		}
	}

	public static String readAuthorizationCode(Context context) {
		String authorizationCode = null;
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
	
		authorizationCode = libSharedPreferences.getString(OmhClientLibConsts.PREFERENCES_KEY_AUTHORIZATION_CODE, null);
	
		return authorizationCode;
	}

	public static void exchangeAuthCodeForTokens (Context context) throws ExchangingAuthCodeForTokensException {
		
		StringBuilder urlStringBuilder = new StringBuilder();
		
		String authorizationCode = readAuthorizationCode(context);
		
		urlStringBuilder.append(context.getString(R.string.dsu_root_url))
						.append('/').append("google-signin")
						.append('?').append("code").append('=').append("fromApp_").append(authorizationCode)
						.append('&').append("client_id").append('=').append(context.getString(R.string.apps_dsu_client_id));
		
		String urlString = urlStringBuilder.toString();
		Log.d(LOG_TAG, urlString);
	
		InputStreamReader isr = null;
		String response = null;
		
		try {
	
			URI uri = new URI(urlString);
			HttpGet httpGet = new HttpGet(uri);
			
			String value = context.getString(R.string.apps_dsu_client_id) + ":" + context.getString(R.string.apps_dsu_client_secret);
			byte[] valueByteArray = value.getBytes("UTF-8");
			String encodedValue = "Basic " + Base64.encodeToString(valueByteArray, Base64.URL_SAFE | Base64.NO_WRAP);
			
			httpGet.setHeader("Authorization", encodedValue);
			
			HttpClient httpClient = new DefaultHttpClient();
			
			HttpResponse httpResponse = httpClient.execute(httpGet);
			
			BufferedReader bufferedReader = null;
			StringBuilder sb = null;
			
			isr = new InputStreamReader(httpResponse.getEntity().getContent());
			bufferedReader = new BufferedReader(isr);
			sb = new StringBuilder();
			
			char[] c = new char[8];
			int len;
			while ((len = isr.read(c)) != -1) {
				sb.append(c, 0, len);
			}
	
			response = sb.toString();
			Log.d(LOG_TAG, response);
	
			storeTokens(response, context);
			
		} catch (Throwable tr) {
			ExchangingAuthCodeForTokensException e = new ExchangingAuthCodeForTokensException();
			e.addSuppressed(tr);
			throw e;
		}
		finally {
			try { 
				isr.close();
				isr = null;
			}
			catch (IOException e) {
				Log.e(LOG_TAG, "Exception at closing http response input stream when getting tokens from DSU.", e);
			}
		}
			
	}

	public static synchronized void storeTokens(String signInResponse, Context context) throws TokensResponseNotValidJsonException {
	
		if (signInResponse == null) {
			throw new TokensResponseNotValidJsonException();
		}
		
		JSONObject signInResponsejJsonObject = null;
		String accessToken = null;
		String refreshToken = null;
		try {
			signInResponsejJsonObject = new JSONObject(signInResponse); 
			accessToken = signInResponsejJsonObject.getString("access_token");
			refreshToken = signInResponsejJsonObject.getString("refresh_token");
		}
		catch (JSONException e) {
			TokensResponseNotValidJsonException e1 = new TokensResponseNotValidJsonException();
			e1.addSuppressed(e);
			throw e1;
		}
		
		if (accessToken != null && refreshToken != null) {
			
			SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
			Editor editor = libSharedPreferences.edit();
			
			editor.putString(OmhClientLibConsts.PREFERENCES_KEY_DSU_ACCESS_TOKEN, accessToken);
			editor.putString(OmhClientLibConsts.PREFERENCES_KEY_DSU_REFRESH_TOKEN, refreshToken);
			
			editor.commit();
			
			Log.d(LOG_TAG, String.format("Access token %s and refresh token %s stored.", accessToken, refreshToken));
			
		}
		
	}
	
	public static void removeAuthorizationCode(Context context) {
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
		
		Editor editor = libSharedPreferences.edit();
		editor.remove(OmhClientLibConsts.PREFERENCES_KEY_AUTHORIZATION_CODE);
		editor.commit();
		
		Log.d(LOG_TAG, OmhClientLibConsts.PREFERENCES_KEY_AUTHORIZATION_CODE + " removed from preferences.");
		
	}
	
	public static void removeAccessTokens(Context context) {
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
		
		Editor editor = libSharedPreferences.edit();
		editor.remove(OmhClientLibConsts.PREFERENCES_KEY_DSU_ACCESS_TOKEN);
		editor.remove(OmhClientLibConsts.PREFERENCES_KEY_DSU_REFRESH_TOKEN);
		editor.commit();
		
		Log.d(LOG_TAG, String.format("%s and %s removed from preferences.", OmhClientLibConsts.PREFERENCES_KEY_DSU_ACCESS_TOKEN, OmhClientLibConsts.PREFERENCES_KEY_DSU_REFRESH_TOKEN));
		
	}

	public static void invalidateAccessToken(Context context) {
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
		
		Editor editor = libSharedPreferences.edit();
		editor.putString(OmhClientLibConsts.PREFERENCES_KEY_DSU_ACCESS_TOKEN, "0df1cbda-48e4-418e-8ce9-862e6e3e7ff5");
		editor.commit();
		
		Log.d(LOG_TAG, "Access token invalidated.");
		
	}
	
	public static Account createSyncAdapterAccount(Context context) {
		
		Account account = null;
		
        String accountName = OmhClientLibConsts.ACCOUNT;
		String accountType = OmhClientLibConsts.ACCOUNT_TYPE;
        
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        
        Account[] omhClientLibAccounts = accountManager.getAccountsByType(accountType);
        
        if (omhClientLibAccounts.length == 0) {
        	
        	Account newAccount = new Account(accountName, accountType);
        	
        	if (accountManager.addAccountExplicitly(newAccount, null, null)) {
        		Log.d(LOG_TAG, String.format("Account %s / %s created.", accountType, accountName) ); 
        		account = newAccount;
        	} else {
        		/* The account exists or some other error occurred. */
        	}
        	
		} else {
			account = omhClientLibAccounts[0];
		}
        
        return account;
    }
	
}
