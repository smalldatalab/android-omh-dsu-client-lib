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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import edu.cornell.tech.smalldata.omhclientlib.enums.AcquisitionProvenanceModality;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HeaderNotInsertedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HttpPostRequestFailedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoAccessTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.PayloadBodyNotCreatedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.RequestBodyNotCreatedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.WritingToDsuFailedException;
import edu.cornell.tech.smalldata.omhclientlib.schema.BodyWeightSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.BodyWeightSchema.BodyWeight;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema.Unit;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema.Value;
import edu.cornell.tech.smalldata.omhclientlib.schema.Schema;

public class DsuWriter {
	
	private static final int ACTION_WRITE_BODY_WEIGHT = 1;

	public static void writeBodyWeight(Context context, BodyWeightSchema bodyWeightSchema) {
		
		DsuWriter dsuWriter = new DsuWriter(context);
		dsuWriter.startWrite(ACTION_WRITE_BODY_WEIGHT, bodyWeightSchema);
		
	}
	
	public DsuWriter(Context context) {
		this.mContext = context;
	}

	private static final String LOG_TAG = AppConsts.APP_LOG_TAG;
	private Context mContext;

	private void startWrite(final int writeAction, final Schema schema) {
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				handleWrite(writeAction, schema);
			}
		}).start();
		
	}

	protected void handleWrite(int writeAction, Schema schema) {
		
		switch (writeAction) {
		case ACTION_WRITE_BODY_WEIGHT:
			
			try {
				handleActionWriteBodyWeight((BodyWeightSchema) schema);
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


	private void handleActionWriteBodyWeight(BodyWeightSchema bodyWeightSchema)
			throws NoAccessTokenException, RequestBodyNotCreatedException, HttpPostRequestFailedException {
		
		String entity;
		try { 
			entity = formPayloadBodyWeight(bodyWeightSchema); 
		} catch (HeaderNotInsertedException | PayloadBodyNotCreatedException e) {
			RequestBodyNotCreatedException e1 = new RequestBodyNotCreatedException();
			e1.addSuppressed(e);
			throw e1;
		}
		
		request(entity);
	
	}


	private String formPayloadBodyWeight(BodyWeightSchema bodyWeightSchema) throws HeaderNotInsertedException, PayloadBodyNotCreatedException {
		
		JSONObject payloadJsonObject = new JSONObject();
		
		insertPayloadHeader(payloadJsonObject, "002", null, AcquisitionProvenanceModality.SENSED);
		
		try {
			JSONObject bodyJsonObject = new JSONObject();
			
			BodyWeight propertyBodyWeight = bodyWeightSchema.getPropertyBodyWeight();
			
			JSONObject bodyWeightJsonObject = new JSONObject();
			
			MassUnitValueSchema massUnitValueSchema = propertyBodyWeight.getJsonValue();
			Unit propertyUnit = massUnitValueSchema.getPropertyUnit();
			Value propertyValue = massUnitValueSchema.getPropertyValue();
			
			bodyWeightJsonObject.put(propertyUnit.getJsonName(), propertyUnit.getJsonValue());
			bodyWeightJsonObject.put(propertyValue.getJsonName(), propertyValue.getJsonValue());
			
			bodyJsonObject.put(propertyBodyWeight.getJsonName(), bodyWeightJsonObject);
			
			payloadJsonObject.put("body", bodyJsonObject); 
		} 
		catch (JSONException e) {
			PayloadBodyNotCreatedException e1 = new PayloadBodyNotCreatedException();
			e1.addSuppressed(e);
			throw e1;
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
