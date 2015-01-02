package edu.cornell.tech.smalldata.omhclientlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HttpPostRequestFailedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoAccessTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnauthorizedWriteAttemptException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnsuccessfulWriteException;

public class OmhClientLibUtils {

	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	/**
	 * Sends HTTP POST with datapoint payload to DSU.
	 * @param entity
	 * @param context
	 * @throws NoAccessTokenException
	 * @throws HttpPostRequestFailedException
	 * @throws UnsuccessfulWriteException
	 */
	public static void writeDataPointRequest(String entity, Context context)
			throws NoAccessTokenException, HttpPostRequestFailedException, UnsuccessfulWriteException, UnauthorizedWriteAttemptException {
		
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
				while (isr.read(c) != -1) {
					sb.append(c);
				}
	
			}
	
			Log.d(LOG_TAG, "Response: " + sb.toString());
			StatusLine statusLine = httpResponse.getStatusLine();
			Log.d(LOG_TAG, String.format("status code: %d reason: %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
			if (HttpStatus.SC_UNAUTHORIZED == statusLine.getStatusCode()) {
				UnauthorizedWriteAttemptException e = new UnauthorizedWriteAttemptException();
				throw e;
			} else if (HttpStatus.SC_CREATED != statusLine.getStatusCode()) {
				UnsuccessfulWriteException e = new UnsuccessfulWriteException();
				throw e;
			}
	
		} catch (UnsuccessfulWriteException | UnauthorizedWriteAttemptException e) {
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
				Log.e(LOG_TAG, "Exception at closing http response input stream and reader when posting order note.", e);
			}
		}
	}

	private static String readAccessToken(Context context) {
		
		String accessToken = null;
		
		SharedPreferences dsuSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
	
		accessToken = dsuSharedPreferences.getString(OmhClientLibConsts.PREFERENCES_KEY_DSU_ACCESS_TOKEN, null);
	
		return accessToken;
	}


	public static String installationId(Context context) {
		String installationId = null;
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);

		installationId = libSharedPreferences.getString(OmhClientLibConsts.PREFERENCES_KEY_INSTALLATION_ID, null);
		if (installationId == null) {
			installationId = createInstallationId(context);
		}
		
		return installationId;
	}

	private static String createInstallationId(Context context) {
		String installationId = null;
		
		installationId = UUID.randomUUID().toString();
		
		SharedPreferences libSharedPreferences = context.getSharedPreferences(OmhClientLibConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);
		Editor editor = libSharedPreferences.edit();
		
		editor.putString(OmhClientLibConsts.PREFERENCES_KEY_INSTALLATION_ID, installationId);
		
		editor.commit();
		
		return installationId;
	}
	
	public static String dataPointSequence(Context context) {
		
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
	
}
