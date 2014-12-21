package edu.cornell.tech.smalldata.omhclientlib.enums;

public enum AcquisitionProvenanceModality {
	
	SENSED;

	public String inJson() {
		String returnValue = null;
		
		switch (this) {
		case SENSED:
			returnValue = "sensed";
			break;
		default:
			break;
		}
		
		return returnValue;
	}
}
