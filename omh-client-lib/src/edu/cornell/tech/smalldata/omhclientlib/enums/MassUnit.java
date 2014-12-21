package edu.cornell.tech.smalldata.omhclientlib.enums;

public enum MassUnit {
	
	 FG,
	 PG,
	 NG,
	 MICROGRAM,
	 MG,
	 G,
	 KG,
	 METRIC_TON,
	 GR,
	 OZ,
	 LB,
	 TON;
	 
	 public String inJson() {
		String returnValue = null;
		
		switch (this) {
		case FG: returnValue = "fg"; break;
		case PG: returnValue = "pg"; break;
		case NG: returnValue = "ng"; break;
		case MICROGRAM: returnValue = "ug"; break;
		case MG: returnValue = "mg"; break;
		case G: returnValue = "g"; break;
		case KG: returnValue = "kg"; break;
		case METRIC_TON: returnValue = "Metric Ton"; break;
		case GR: returnValue = "gr"; break;
		case OZ: returnValue = "oz"; break;
		case LB: returnValue = "lb"; break;
		case TON: returnValue = "Ton"; break;
		default: returnValue = ""; break;
		}
		
		return returnValue;
	}
	 
}
