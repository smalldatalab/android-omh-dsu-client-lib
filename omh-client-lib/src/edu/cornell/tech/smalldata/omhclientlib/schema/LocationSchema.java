package edu.cornell.tech.smalldata.omhclientlib.schema;



public class LocationSchema implements Schema {
	
	public String getSchemaName() {
		return "location";
	}

	private Latitude mLatitude;
	private Longitude mLongitude;
	private Accuracy mAccuracy;
	private Altitude mAltitude;
	private Bearing mBearing;
	private Speed mSpeed;
	
	public LocationSchema(double latitude, double longitude, double accuracy, double altitude, double bearing, double speed) {
		this.mLatitude = new Latitude(latitude);
		this.mLongitude = new Longitude(longitude);
		this.mAccuracy = new Accuracy(accuracy);
		this.mAltitude = new Altitude(altitude);
		this.mBearing = new Bearing(bearing);
		this.mSpeed = new Speed(speed);
	}
	
	public Latitude getPropertyLatitude() {
		return mLatitude;
	}
	
	public Longitude getPropertyLongitude() {
		return mLongitude;
	}
	
	public Accuracy getPropertyAccuracy() {
		return mAccuracy;
	}
	
	public Altitude getPropertyAltitude() {
		return mAltitude;
	}
	
	public Bearing getPropertyBearing() {
		return mBearing;
	}

	public Speed getPropertySpeed() {
		return mSpeed;
	}
	
	public static class Latitude implements Property {
		
		private double latitude;
		
		public Latitude(double latitude) {
			this.latitude = latitude;
		}
		
		@Override
		public String getJsonName() {
			return "latitude";
		}
		
		@Override
		public Double getJsonValue() {
			return latitude;
		}
		
	}
	
	public static class Longitude implements Property {
		
		private double longitude;
		
		public Longitude(double longitude) {
			this.longitude = longitude;
		}
		
		@Override
		public String getJsonName() {
			return "longitude";
		}
		
		@Override
		public Double getJsonValue() {
			return longitude;
		}
		
	}
	
	public static class Accuracy implements Property {
		
		private double accuracy;
		
		public Accuracy(double accuracy) {
			this.accuracy = accuracy;
		}
		
		@Override
		public String getJsonName() {
			return "accuracy";
		}
		
		@Override
		public Double getJsonValue() {
			return accuracy;
		}
		
	}
	
	public static class Altitude implements Property {
		
		private double altitude;
		
		public Altitude(double altitude) {
			this.altitude = altitude;
		}
		
		@Override
		public String getJsonName() {
			return "altitude";
		}
		
		@Override
		public Double getJsonValue() {
			return altitude;
		}
		
	}
	
	public static class Bearing implements Property {
		
		private double bearing;
		
		public Bearing(double bearing) {
			this.bearing = bearing;
		}
		
		@Override
		public String getJsonName() {
			return "bearing";
		}
		
		@Override
		public Double getJsonValue() {
			return bearing;
		}
		
	}
	
	public static class Speed implements Property {
		
		private double speed;
		
		public Speed(double speed) {
			this.speed = speed;
		}

		@Override
		public String getJsonName() {
			return "speed";
		}
		
		@Override
		public Double getJsonValue() {
			return speed;
		}
		
	}
	
}
