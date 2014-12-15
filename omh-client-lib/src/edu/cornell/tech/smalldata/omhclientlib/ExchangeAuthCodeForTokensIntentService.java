package edu.cornell.tech.smalldata.omhclientlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

public class ExchangeAuthCodeForTokensIntentService extends IntentService {
	
	private static String LOG_TAG = AppConsts.APP_LOG_TAG;
	private Context mContext;
	
	public ExchangeAuthCodeForTokensIntentService() {
		super("ExchangeAuthCodeForTokensIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(LOG_TAG, "onHandleIntent");
		
		mContext = getApplicationContext();
		
		String authorizationCode = readAuthorizationCode();
		if (authorizationCode == null) {
			return;
		}
		
		requestTokens(authorizationCode);
		
	}

	private String readAuthorizationCode() {
		String authorizationCode = null;
		
		SharedPreferences dsuSharedPreferences = mContext.getSharedPreferences(AppConsts.SHARED_PREFERENCES_DSU, Context.MODE_PRIVATE);

		authorizationCode = dsuSharedPreferences.getString(AppConsts.PREFERENCES_KEY_AUTHORIZATION_CODE, null);

		return authorizationCode;
	}
	
	private void requestTokens (String authorizationCode) {
		String response = null;
		
		StringBuilder urlStringBuilder = new StringBuilder();
		
		urlStringBuilder.append(mContext.getString(R.string.dsu_root_url))
						.append('/').append("google-signin")
						.append('?').append("code").append('=').append("fromApp_").append(authorizationCode)
						.append('&').append("client_id").append('=').append(mContext.getString(R.string.apps_dsu_client_id));
		
		String urlString = urlStringBuilder.toString();
		
		Log.d(LOG_TAG, urlString);

		InputStreamReader isr = null;
		
		try {

			URI uri = new URI(urlString);
			HttpGet httpGet = new HttpGet(uri);
			
			String value = mContext.getString(R.string.apps_dsu_client_id) + ":" + mContext.getString(R.string.apps_dsu_client_secret);
			byte[] valueByteArray = value.getBytes("UTF-8");
			String encodedValue = "Basic " + Base64.encodeToString(valueByteArray, Base64.DEFAULT);
			
			httpGet.setHeader("Authorization", encodedValue);
			Log.d(LOG_TAG, "Authorization: " + value + " (encoded: " + encodedValue + ")");
			
			HttpClient httpClient = new DefaultHttpClient();
			
			HttpResponse httpResponse = httpClient.execute(httpGet);
			
			BufferedReader bufferedReader = null;
			StringBuilder sb = null;
			
			isr = new InputStreamReader(httpResponse.getEntity().getContent());
			bufferedReader = new BufferedReader(isr);
			String line = "";
			sb = new StringBuilder();
			while ((line = bufferedReader.readLine()) != null) {
				sb.append(line).append("\n");
			}

			response = sb.toString();
			Log.d(LOG_TAG, response);

//			return response;

		} catch (Throwable tr) {
			Log.e(LOG_TAG, "Throwable at getting tokens from DSU.", tr);
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

	/**
	 * Sign in to DSU with the Google One-Time Auth Code
	 */
	private void requestTokensHarderStep2 (String authorizationCode) {
			Log.d(LOG_TAG, "requestTokensHarderStep2");
			String response = null;
			
			StringBuilder urlStringBuilder = new StringBuilder();
			
			urlStringBuilder.append(mContext.getString(R.string.dsu_root_url))
			.append('/').append("auth/google")
			.append('?').append("code").append('=').append("fromApp_").append(authorizationCode);
			
			String urlString = urlStringBuilder.toString();;
			Log.d(LOG_TAG, urlString);
			
			InputStreamReader isr = null;
			
			try {
				
				URI uri = new URI(urlString);
				HttpGet httpGet = new HttpGet(uri);
				
				CookieManager cookieManager = new CookieManager();
                CookieHandler.setDefault(cookieManager);
				HttpClient httpClient = new DefaultHttpClient();
				
				HttpResponse httpResponse = httpClient.execute(httpGet);
				
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				Log.d(LOG_TAG, "statusCode: " + statusCode);
				
				Header[] headersArray = httpResponse.getAllHeaders();
				for (int i = 0; i < headersArray.length; i++) {
					Log.d(LOG_TAG, "header " + headersArray[i].getName() + ": " + headersArray[i].getValue());
				}
				
				BufferedReader bufferedReader = null;
				StringBuilder sb = null;
				
				isr = new InputStreamReader(httpResponse.getEntity().getContent());
				bufferedReader = new BufferedReader(isr);
				String line = "";
				sb = new StringBuilder();
				while ((line = bufferedReader.readLine()) != null) {
					sb.append(line).append("\n");
				}
	
				response = sb.toString();
				Log.d(LOG_TAG, response);
	
			} catch (Throwable tr) {
				Log.e(LOG_TAG, "Throwable at getting tokens from DSU.", tr);
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
	
}
