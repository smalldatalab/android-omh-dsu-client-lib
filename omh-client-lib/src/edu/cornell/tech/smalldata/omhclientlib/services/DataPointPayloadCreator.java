package edu.cornell.tech.smalldata.omhclientlib.services;

import java.util.Calendar;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibConsts;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibUtils;
import edu.cornell.tech.smalldata.omhclientlib.R;
import edu.cornell.tech.smalldata.omhclientlib.enums.AcquisitionProvenanceModality;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.DatapointCreationFailedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HeaderNotInsertedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.PayloadBodyNotCreatedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.RequestBodyNotCreatedException;
import edu.cornell.tech.smalldata.omhclientlib.schema.BodyWeightSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.BodyWeightSchema.BodyWeight;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema.Unit;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema.Value;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema.PositiveAffect;
import edu.cornell.tech.smalldata.omhclientlib.schema.Schema;
import edu.cornell.tech.smalldata.omhclientlib.schema.UnitValueSchema;

public class DataPointPayloadCreator {
	
	private static final int DATAPOINT_BODY_WEIGHT = 1;
	private static final int DATAPOINT_PAM = 2;
	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	private Context mContext;
	
	public static String createBodyWeight(final Context context, final BodyWeightSchema bodyWeightSchema) {
		String payload = null;
		
		DataPointPayloadCreator dataPointCreator = new DataPointPayloadCreator(context);
		payload = dataPointCreator.startCreation(DATAPOINT_BODY_WEIGHT, bodyWeightSchema);
		
		return payload;
	}

	public static String createPam(final Context context, final PamSchema pamSchema) {
		String payload = null;
		
		DataPointPayloadCreator dataPointCreator = new DataPointPayloadCreator(context);
		payload = dataPointCreator.startCreation(DATAPOINT_PAM, pamSchema);
		
		return payload;
	}
	
	public DataPointPayloadCreator(Context context) {
		this.mContext = context;
	}

	private String startCreation(final int dataPointType, final Schema schema) {
		
		return handleCreation(dataPointType, schema);
		
	}

	private String handleCreation(int dataPointType, Schema schema) {
		String payload = null;
		
		switch (dataPointType) {
		case DATAPOINT_BODY_WEIGHT:
			
			try {
				payload = handleCreationBodyWeight((BodyWeightSchema) schema);
			} catch (RequestBodyNotCreatedException e) {
				DatapointCreationFailedException e1 = new DatapointCreationFailedException();
				e1.addSuppressed(e);
			}
			
			break;
		case DATAPOINT_PAM:
			
			try {
				payload = handleCreationPam((PamSchema) schema);
			} catch (RequestBodyNotCreatedException e) {
				DatapointCreationFailedException e1 = new DatapointCreationFailedException();
				e1.addSuppressed(e);
			}
			
			break;
		default:
			break;
		}
		
		return payload;
	}
	
	private String handleCreationBodyWeight(BodyWeightSchema bodyWeightSchema) throws RequestBodyNotCreatedException {
		
		String payload;
		try { 
			payload = formPayloadBodyWeight(bodyWeightSchema); 
		} catch (HeaderNotInsertedException | PayloadBodyNotCreatedException e) {
			RequestBodyNotCreatedException e1 = new RequestBodyNotCreatedException();
			e1.addSuppressed(e);
			throw e1;
		}
		
		return payload;
		
	}

	private String handleCreationPam(PamSchema pamSchema) throws RequestBodyNotCreatedException {
		
		String payload;
		try { 
			payload = formPayloadPam(pamSchema); 
		} catch (HeaderNotInsertedException | PayloadBodyNotCreatedException e) {
			RequestBodyNotCreatedException e1 = new RequestBodyNotCreatedException();
			e1.addSuppressed(e);
			throw e1;
		}
		
		return payload;
	
	}
	
	private String formPayloadBodyWeight(BodyWeightSchema bodyWeightSchema) throws HeaderNotInsertedException, PayloadBodyNotCreatedException {
		
		JSONObject payloadJsonObject = new JSONObject();
		
		String id = OmhClientLibUtils.nextDataPointId(mContext);
		insertPayloadHeader(payloadJsonObject, id, bodyWeightSchema, AcquisitionProvenanceModality.SENSED);
		
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

	private String formPayloadPam(PamSchema pamSchema) throws HeaderNotInsertedException, PayloadBodyNotCreatedException {
		
		JSONObject payloadJsonObject = new JSONObject();
		
		String id = OmhClientLibUtils.nextDataPointId(mContext);
		insertPayloadHeader(payloadJsonObject, id, pamSchema, AcquisitionProvenanceModality.SENSED);
		
		try {
			JSONObject bodyJsonObject = new JSONObject();
			
			PositiveAffect propertyPositiveAffect = pamSchema.getPropertyPositiveAffect();
			
			JSONObject positiveAffectJsonObject = new JSONObject();
			
			UnitValueSchema unitValueSchema = propertyPositiveAffect.getJsonValue();
			UnitValueSchema.Unit propertyUnit = unitValueSchema.getPropertyUnit();
			UnitValueSchema.Value propertyValue = unitValueSchema.getPropertyValue();
			
			positiveAffectJsonObject.put(propertyUnit.getJsonName(), propertyUnit.getJsonValue());
			positiveAffectJsonObject.put(propertyValue.getJsonName(), propertyValue.getJsonValue());
			
			bodyJsonObject.put(propertyPositiveAffect.getJsonName(), positiveAffectJsonObject);
			
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
			schemaIdJsonObject.put("name", schema.getSchemaName());
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


}
