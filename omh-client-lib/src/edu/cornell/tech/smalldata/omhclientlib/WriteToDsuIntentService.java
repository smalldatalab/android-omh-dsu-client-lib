package edu.cornell.tech.smalldata.omhclientlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import edu.cornell.tech.smalldata.omhclientlib.enums.AcquisitionProvenanceModality;
import edu.cornell.tech.smalldata.omhclientlib.enums.MassUnit;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HeaderNotInsertedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HttpPostRequestFailedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoAccessTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.RequestBodyNotCreatedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.WritingToDsuFailedException;
import edu.cornell.tech.smalldata.omhclientlib.schema.BodyWeight;
import edu.cornell.tech.smalldata.omhclientlib.schema.Schema;

public class WriteToDsuIntentService extends IntentService {
	
	private static String LOG_TAG = AppConsts.APP_LOG_TAG;
	private static final String ACTION_WRITE_BODY_WEIGHT = "edu.cornell.tech.smalldata.omhclientlib.action.WRITE_BODY_WEIGHT";

	private static final String EXTRA_PARAM_BODY_WEIGHT_UNIT = "edu.cornell.tech.smalldata.omhclientlib.extra.PARAM_BODY_WEIGHT_UNIT";
	private static final String EXTRA_PARAM_BODY_WEIGHT_UNIT_VALUE = "edu.cornell.tech.smalldata.omhclientlib.extra.PARAM_BODY_WEIGHT_UNIT_VALUE";

	/**
	 * Starts this service to perform writing body weight data to DSU
	 */
	public static void startActionWriteBodyWeight(Context context, String bodyWeightUnit, double bodyWeightUnitValue) {
		
		Intent intent = new Intent(context, WriteToDsuIntentService.class);
		intent.setAction(ACTION_WRITE_BODY_WEIGHT);
		intent.putExtra(EXTRA_PARAM_BODY_WEIGHT_UNIT, bodyWeightUnit);
		intent.putExtra(EXTRA_PARAM_BODY_WEIGHT_UNIT_VALUE, bodyWeightUnitValue);
		context.startService(intent);
	}

	private Context mContext;

	public WriteToDsuIntentService() {
		super("WriteToDsuIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		mContext = getApplicationContext();
		
		if (intent != null) {
			final String action = intent.getAction();
			
			switch (action) {
			case ACTION_WRITE_BODY_WEIGHT:
				
				final String bodyWeightUnit = intent.getStringExtra(EXTRA_PARAM_BODY_WEIGHT_UNIT);
				final double bodyWeightUnitValue = intent.getDoubleExtra(EXTRA_PARAM_BODY_WEIGHT_UNIT_VALUE, 0d);
				try {
					handleActionWriteBodyWeight(new BodyWeight(bodyWeightUnitValue, MassUnit.KG));
				} catch (NoAccessTokenException | RequestBodyNotCreatedException | HttpPostRequestFailedException e) {
					WritingToDsuFailedException e1 = new WritingToDsuFailedException();
					e1.addSuppressed(e);
					Log.e(LOG_TAG, "ACTION_WRITE_BODY_WEIGHT", e1);
				}
				break;
			default:
				break;
			}
			
		}
	}

	private void handleActionWriteBodyWeight(BodyWeight bodyWeight)
			throws NoAccessTokenException, RequestBodyNotCreatedException, HttpPostRequestFailedException {
		
		String entity;
		try { 
			entity = formPayloadBodyWeight(bodyWeight); 
		} catch (HeaderNotInsertedException e) {
			RequestBodyNotCreatedException e1 = new RequestBodyNotCreatedException();
			e1.addSuppressed(e);
			throw e1;
		}
		
		request(entity);

	}

	private String formPayloadBodyWeight(BodyWeight bodyWeight) throws HeaderNotInsertedException {
		
		JSONObject payloadJsonObject = new JSONObject();
		
		insertPayloadHeader(payloadJsonObject, "001", null, AcquisitionProvenanceModality.SENSED);
		
		try {
			JSONObject bodyJsonObject = new JSONObject();
			
			JSONObject bodyWeightJsonObject = new JSONObject();
			bodyWeightJsonObject.put("value", bodyWeight.bodyWeightValue);
			bodyWeightJsonObject.put("unit", bodyWeight.bodyWeightMassUnit.inJson());
			
			bodyJsonObject.put(BodyWeight.PROPERTY_BODY_WEIGHT, bodyWeightJsonObject);
			
			payloadJsonObject.put("body", bodyJsonObject); 
		} 
		catch (JSONException e) {
		}
		
		try { Log.d(LOG_TAG, payloadJsonObject.toString(2)); } catch (JSONException e) {}
		return payloadJsonObject.toString();
	}

	private void insertPayloadHeader(JSONObject jsonObject, String id, Schema schema, AcquisitionProvenanceModality acquisitionProvenanceModality) throws HeaderNotInsertedException {
		
		JSONObject headerJsonObject = new JSONObject();
		
		try {
			
			headerJsonObject.put("id", id);
			
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			String creationDateTime = String.format("%tFT%<tTZ", cal);
			
			headerJsonObject.put("creation_date_time", creationDateTime);
			
			JSONObject schemaIdJsonObject = new JSONObject();
			schemaIdJsonObject.put("namespace", Schema.NAMESPACE);
			schemaIdJsonObject.put("name", Schema.NAME);
			schemaIdJsonObject.put("version", Schema.VERSION);
			
			headerJsonObject.put("schema_id", schemaIdJsonObject);
			
			JSONObject acquisitionProvenancejJsonObject = new JSONObject();
			acquisitionProvenancejJsonObject.put("source_name", mContext.getString(R.string.acquisition_provenance_source_name));
			acquisitionProvenancejJsonObject.put("modality", acquisitionProvenanceModality.inJson());
			
			headerJsonObject.put("acquisition_provenance", acquisitionProvenancejJsonObject);
			
			jsonObject.put("header", headerJsonObject); 
		} 
		catch (JSONException e) {
			throw new HeaderNotInsertedException();
		}
		
	}

	private void request(String entity) throws NoAccessTokenException, HttpPostRequestFailedException {
		
		StringBuilder urlStringBuilder = new StringBuilder();
		urlStringBuilder.append(mContext.getString(R.string.dsu_root_url)).append('/').append("dataPoints");
	
		final String urlString = urlStringBuilder.toString();
		Log.d(LOG_TAG, urlString);
	
		HttpClient httpClient = new DefaultHttpClient();
		
		HttpPost httpPost = new HttpPost(urlString);
		String accessToken = readAccessToken();
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

	private String readAccessToken() {
		
		String accessToken = null;
		
		SharedPreferences dsuSharedPreferences = mContext.getSharedPreferences(AppConsts.SHARED_PREFERENCES_OMHCLIENTLIB, Context.MODE_PRIVATE);

		accessToken = dsuSharedPreferences.getString(AppConsts.PREFERENCES_KEY_DSU_ACCESS_TOKEN, null);

		return accessToken;
	}

	
}
