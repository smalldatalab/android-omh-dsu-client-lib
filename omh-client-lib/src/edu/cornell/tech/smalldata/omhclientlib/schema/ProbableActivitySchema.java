package edu.cornell.tech.smalldata.omhclientlib.schema;

public class ProbableActivitySchema implements Schema {
	
	public String getSchemaName() {
		return "probable-activity";
	}

	private Activity mActivity;
	private Confidence mConfidence;
	
	public ProbableActivitySchema(String activity, double confidence) {
		this.mActivity = new Activity(activity);
		this.mConfidence = new Confidence(confidence);
	}

	public Activity getPropertyActivity() {
		return mActivity;
	}
	
	public Confidence getPropertyConfidence() {
		return mConfidence;
	}
	
	public static class Activity implements Property {
		
		private String activity;
		
		public Activity(String activity) {
			this.activity = activity;
		}

		@Override
		public String getJsonName() {
			return "activity";
		}
		
		@Override
		public String getJsonValue() {
			return activity;
		}
		
	}
	
	public static class Confidence implements Property {
		
		private double confidence;
		
		public Confidence(double confidence) {
			this.confidence = confidence;
		}

		@Override
		public String getJsonName() {
			return "confidence";
		}
		
		@Override
		public Double getJsonValue() {
			return confidence;
		}
		
	}
	
}
