package edu.cornell.tech.smalldata.omhclientlib.schema;

import org.json.JSONObject;

public class OhmageResponseSchema implements Schema {
	
	public String getSchemaName() {
		return "ohmage";
	}
	
	private OhmageData mOhmageData;
	
	public OhmageResponseSchema(JSONObject respoonseDataJsonObject) {
		
		mOhmageData = new OhmageData(respoonseDataJsonObject);
	}
	
	public OhmageData getPropertyOhmageData() {
		
		return mOhmageData;
	}
	
	public static class OhmageData implements Property {
		
		private JSONObject dataJsonObject;
		
		public OhmageData(JSONObject dataJsonObject) {
			this.dataJsonObject = dataJsonObject;
		}
		
		@Override
		public String getJsonName() {
			return "data";
		}
		
		@Override
		public JSONObject getJsonValue() {
			return this.dataJsonObject;
		}
		
	}

}
