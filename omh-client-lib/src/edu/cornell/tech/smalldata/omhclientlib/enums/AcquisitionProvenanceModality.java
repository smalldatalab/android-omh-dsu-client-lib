package edu.cornell.tech.smalldata.omhclientlib.enums;

public enum AcquisitionProvenanceModality {
	
	SENSED,
	SELF_REPORTED;

	public String inJson() {
		String returnValue = null;
		
		switch (this) {
		case SENSED:
			returnValue = "sensed";
			break;
		case SELF_REPORTED:
			returnValue = "self reported";
			break;
		default:
			break;
		}
		
		return returnValue;
	}
}
