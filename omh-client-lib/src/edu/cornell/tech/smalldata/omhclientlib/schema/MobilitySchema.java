package edu.cornell.tech.smalldata.omhclientlib.schema;

public class MobilitySchema implements Schema {
	
	public String getSchemaName() {
		return "mobility";
	}

	private ProbableActivitySchema[] mProbableActivitySchemas;

	public MobilitySchema(ProbableActivitySchema[] probableActivitySchemas) {
		this.mProbableActivitySchemas = probableActivitySchemas;
	}

	public ProbableActivitySchema[] getPropertyProbableActivities() {
		return mProbableActivitySchemas;
	}
	
}
