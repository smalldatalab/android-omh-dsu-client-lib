package edu.cornell.tech.smalldata.omhclientlib.services;

import java.util.Calendar;
import java.util.TimeZone;

import org.json.JSONArray;
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
import edu.cornell.tech.smalldata.omhclientlib.schema.LocationSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.LocationSchema.Accuracy;
import edu.cornell.tech.smalldata.omhclientlib.schema.LocationSchema.Altitude;
import edu.cornell.tech.smalldata.omhclientlib.schema.LocationSchema.Bearing;
import edu.cornell.tech.smalldata.omhclientlib.schema.LocationSchema.Latitude;
import edu.cornell.tech.smalldata.omhclientlib.schema.LocationSchema.Longitude;
import edu.cornell.tech.smalldata.omhclientlib.schema.LocationSchema.Speed;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema.Unit;
import edu.cornell.tech.smalldata.omhclientlib.schema.MassUnitValueSchema.Value;
import edu.cornell.tech.smalldata.omhclientlib.schema.ProbableActivitySchema.Activity;
import edu.cornell.tech.smalldata.omhclientlib.schema.ProbableActivitySchema.Confidence;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema.AffectArousal;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema.AffectValence;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema.EffectiveTimeFrame;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema.Mood;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema.NegativeAffect;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema.PositiveAffect;
import edu.cornell.tech.smalldata.omhclientlib.schema.MobilitySchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.ProbableActivitySchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.Schema;
import edu.cornell.tech.smalldata.omhclientlib.schema.TimeFrameSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.TimeFrameSchema.DateTime;
import edu.cornell.tech.smalldata.omhclientlib.schema.UnitValueSchema;

public class DataPointPayloadCreator {
	
	private static final int DATAPOINT_BODY_WEIGHT = 1;
	private static final int DATAPOINT_PAM = 2;
	private static final int DATAPOINT_LOCATION = 3;
	private static final int DATAPOINT_MOBILITY = 4;
	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	private Context mContext;
	
	public static String createBodyWeight(final Context context, final BodyWeightSchema bodyWeightSchema) {
		String payload = null;
		
		DataPointPayloadCreator dataPointPayloadCreator = new DataPointPayloadCreator(context);
		payload = dataPointPayloadCreator.startCreation(DATAPOINT_BODY_WEIGHT, bodyWeightSchema);
		
		return payload;
	}
	
	public static String createPam(final Context context, final PamSchema pamSchema) {
		String payload = null;
		
		DataPointPayloadCreator dataPointPayloadCreator = new DataPointPayloadCreator(context);
		payload = dataPointPayloadCreator.startCreation(DATAPOINT_PAM, pamSchema);
		
		return payload;
	}
	
	public static String createLocation(final Context context, final LocationSchema locationSchema) {
		String payload = null;
		
		DataPointPayloadCreator dataPointPayloadCreator = new DataPointPayloadCreator(context);
		payload = dataPointPayloadCreator.startCreation(DATAPOINT_LOCATION, locationSchema);
		
		return payload;
	}

	public static String createMobility(final Context context, final MobilitySchema mobilitySchema) {
		String payload = null;
		
		DataPointPayloadCreator dataPointPayloadCreator = new DataPointPayloadCreator(context);
		payload = dataPointPayloadCreator.startCreation(DATAPOINT_MOBILITY, mobilitySchema);
		
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
		case DATAPOINT_LOCATION:
			
			try {
				payload = handleCreationLocation((LocationSchema) schema);
			} catch (RequestBodyNotCreatedException e) {
				DatapointCreationFailedException e1 = new DatapointCreationFailedException();
				e1.addSuppressed(e);
			}
			
			break;
		case DATAPOINT_MOBILITY:
			
			try {
				payload = handleCreationMobility((MobilitySchema) schema);
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
	
	private String handleCreationLocation(LocationSchema locationSchema) throws RequestBodyNotCreatedException {
		
		String payload;
		try { 
			payload = formPayloadLocation(locationSchema); 
		} catch (HeaderNotInsertedException | PayloadBodyNotCreatedException e) {
			RequestBodyNotCreatedException e1 = new RequestBodyNotCreatedException();
			e1.addSuppressed(e);
			throw e1;
		}
		
		return payload;
	
	}

	private String handleCreationMobility(MobilitySchema mobilitySchema) throws RequestBodyNotCreatedException {
		
		String payload;
		try { 
			payload = formPayloadMobility(mobilitySchema); 
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
			
			AffectValence propertyAffectValence = pamSchema.getPropertyAffectValence();
			if (propertyAffectValence != null) {
				JSONObject affectValenceJsonObject = new JSONObject();
				
				UnitValueSchema unitValueSchema = propertyAffectValence.getJsonValue();
				UnitValueSchema.Unit propertyUnit = unitValueSchema.getPropertyUnit();
				UnitValueSchema.Value propertyValue = unitValueSchema.getPropertyValue();
				
				affectValenceJsonObject.put(propertyUnit.getJsonName(), propertyUnit.getJsonValue());
				affectValenceJsonObject.put(propertyValue.getJsonName(), propertyValue.getJsonValue());
				
				bodyJsonObject.put(propertyAffectValence.getJsonName(), affectValenceJsonObject);
			}
			
			AffectArousal propertyAffectArousal = pamSchema.getPropertyAffectArousal();
			if (propertyAffectArousal != null) {
				JSONObject affectArousalJsonObject = new JSONObject();
				
				UnitValueSchema unitValueSchema = propertyAffectArousal.getJsonValue();
				UnitValueSchema.Unit propertyUnit = unitValueSchema.getPropertyUnit();
				UnitValueSchema.Value propertyValue = unitValueSchema.getPropertyValue();
				
				affectArousalJsonObject.put(propertyUnit.getJsonName(), propertyUnit.getJsonValue());
				affectArousalJsonObject.put(propertyValue.getJsonName(), propertyValue.getJsonValue());
				
				bodyJsonObject.put(propertyAffectArousal.getJsonName(), affectArousalJsonObject);
			}
			
			PositiveAffect propertyPositiveAffect = pamSchema.getPropertyPositiveAffect();
			if (propertyPositiveAffect != null) {
				JSONObject positiveAffectJsonObject = new JSONObject();
				
				UnitValueSchema unitValueSchema = propertyPositiveAffect.getJsonValue();
				UnitValueSchema.Unit propertyUnit = unitValueSchema.getPropertyUnit();
				UnitValueSchema.Value propertyValue = unitValueSchema.getPropertyValue();
				
				positiveAffectJsonObject.put(propertyUnit.getJsonName(), propertyUnit.getJsonValue());
				positiveAffectJsonObject.put(propertyValue.getJsonName(), propertyValue.getJsonValue());
				
				bodyJsonObject.put(propertyPositiveAffect.getJsonName(), positiveAffectJsonObject);
			}
			
			NegativeAffect propertyNegativeAffect = pamSchema.getPropertyNegativeAffect();
			if (propertyNegativeAffect != null) {
				JSONObject negativeAffectJsonObject = new JSONObject();
				
				UnitValueSchema unitValueSchema = propertyNegativeAffect.getJsonValue();
				UnitValueSchema.Unit propertyUnit = unitValueSchema.getPropertyUnit();
				UnitValueSchema.Value propertyValue = unitValueSchema.getPropertyValue();
				
				negativeAffectJsonObject.put(propertyUnit.getJsonName(), propertyUnit.getJsonValue());
				negativeAffectJsonObject.put(propertyValue.getJsonName(), propertyValue.getJsonValue());
				
				bodyJsonObject.put(propertyNegativeAffect.getJsonName(), negativeAffectJsonObject);
			}
			
			Mood propertyMood = pamSchema.getPropertyMood();
			if (propertyMood != null) {
				bodyJsonObject.put(propertyMood.getJsonName(), propertyMood.getJsonValue());
			}
			
			EffectiveTimeFrame propertyEffectiveTimeFrame = pamSchema.getPropertyEffectiveTimeFrame();
			if (propertyEffectiveTimeFrame != null) {
				JSONObject effectiveTimeFrameJsonObject = new JSONObject();
				
				TimeFrameSchema timeFrameSchema = propertyEffectiveTimeFrame.getJsonValue();
				DateTime propertyDateTime = timeFrameSchema.getPropertyDateTime();
				
				effectiveTimeFrameJsonObject.put(propertyDateTime.getJsonName(), propertyDateTime.getJsonValue());
				
				bodyJsonObject.put(propertyEffectiveTimeFrame.getJsonName(), effectiveTimeFrameJsonObject);
			}
			
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
	
	private String formPayloadLocation(LocationSchema locationSchema) throws HeaderNotInsertedException, PayloadBodyNotCreatedException {
		
		JSONObject payloadJsonObject = new JSONObject();
		
		String id = OmhClientLibUtils.nextDataPointId(mContext);
		insertPayloadHeader(payloadJsonObject, id, locationSchema, AcquisitionProvenanceModality.SENSED);
		
		try {
			JSONObject bodyJsonObject = new JSONObject();
			
			Latitude propertyLatitude = locationSchema.getPropertyLatitude();
			if (propertyLatitude != null) {
				bodyJsonObject.put(propertyLatitude.getJsonName(), propertyLatitude.getJsonValue());
			}
			
			Longitude propertyLongitude = locationSchema.getPropertyLongitude();
			if (propertyLongitude != null) {
				bodyJsonObject.put(propertyLongitude.getJsonName(), propertyLongitude.getJsonValue());
			}
			
			Accuracy propertyAccuracy = locationSchema.getPropertyAccuracy();
			if (propertyAccuracy != null) {
				bodyJsonObject.put(propertyAccuracy.getJsonName(), propertyAccuracy.getJsonValue());
			}
			
			Altitude propertyAltitude = locationSchema.getPropertyAltitude();
			if (propertyAltitude != null) {
				bodyJsonObject.put(propertyAltitude.getJsonName(), propertyAltitude.getJsonValue());
			}
			
			Bearing propertyBearing = locationSchema.getPropertyBearing();
			if (propertyBearing != null) {
				bodyJsonObject.put(propertyBearing.getJsonName(), propertyBearing.getJsonValue());
			}
			
			Speed propertySpeed = locationSchema.getPropertySpeed();
			if (propertySpeed != null) {
				bodyJsonObject.put(propertySpeed.getJsonName(), propertySpeed.getJsonValue());
			}
			
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

	private String formPayloadMobility(MobilitySchema mobilitySchema) throws HeaderNotInsertedException, PayloadBodyNotCreatedException {
		
		JSONObject payloadJsonObject = new JSONObject();
		
		String id = OmhClientLibUtils.nextDataPointId(mContext);
		insertPayloadHeader(payloadJsonObject, id, mobilitySchema, AcquisitionProvenanceModality.SENSED);
		
		try {
			JSONObject bodyJsonObject = new JSONObject();
			
			JSONArray probableActivitiesJsonArray = new JSONArray();
			
			ProbableActivitySchema[] probableActivitySchemas = mobilitySchema.getPropertyProbableActivities();
			
			for (int i = 0; i < probableActivitySchemas.length; i++) {
				
				ProbableActivitySchema probableActivitySchema = probableActivitySchemas[i];
				
				JSONObject probableActivityJsonObject = new JSONObject();
				
				Activity propertyActivity = probableActivitySchema.getPropertyActivity();
				if (propertyActivity != null) {
					probableActivityJsonObject.put(propertyActivity.getJsonName(), propertyActivity.getJsonValue());
				}

				Confidence propertyConfidence = probableActivitySchema.getPropertyConfidence();
				if (propertyConfidence != null) {
					probableActivityJsonObject.put(propertyConfidence.getJsonName(), propertyConfidence.getJsonValue());
				}
				
				probableActivitiesJsonArray.put(probableActivityJsonObject);
			}
			
			bodyJsonObject.put("", probableActivitiesJsonArray);
			
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
