package edu.cornell.tech.smalldata.omhclientlib.schema;

import java.util.Calendar;

public class TimeFrameSchema implements Schema {
	
	public String getSchemaName() {
		return "time-frame";
	}

	private DateTime dateTime;
	
	public TimeFrameSchema(DateTime dateTime) {
		this.dateTime = dateTime;
	}

	public DateTime getPropertyDateTime() {
		return dateTime;
	}

	public static class DateTime implements Property {

		private Calendar calendar;

		public DateTime(Calendar calendar) {
			this.calendar = calendar;
		}

		@Override
		public String getJsonName() {
			return "date_time";
		}

		@Override
		public String getJsonValue() {
			
			if (calendar != null) {
				return String.format("%tFT%<tT.%<tLZ", calendar);
			} else {
				return "";
			}
			
		}
		
	}

}
