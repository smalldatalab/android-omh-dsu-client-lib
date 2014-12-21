package edu.cornell.tech.smalldata.omhclientlib.schema;

import edu.cornell.tech.smalldata.omhclientlib.enums.MassUnit;


public class BodyWeight implements Schema {
	
	public static final String NAME = "body-weight";
	
	public static final String PROPERTY_BODY_WEIGHT = "body_weight";
	public static final String PROPERTY_EFFECTIVE_TIME_FRAME = "effective_time_frame";
	public static final String PROPERTY_DESCRIPTIVE_STATISTIC = "descriptive_statistic";
	
	public double bodyWeightValue;
	public MassUnit bodyWeightMassUnit;
	
	public BodyWeight(double bodyWeightValue, MassUnit bodyWeightMassUnit) {
		this.bodyWeightValue = bodyWeightValue;
		this.bodyWeightMassUnit = bodyWeightMassUnit;
	}

}
