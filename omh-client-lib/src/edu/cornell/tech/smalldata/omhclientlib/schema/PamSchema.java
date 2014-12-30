package edu.cornell.tech.smalldata.omhclientlib.schema;

public class PamSchema implements Schema {
	
	public String getSchemaName() {
		return "pam";
	}
	
	private PositiveAffect positiveAffect;
	
	public PamSchema(PositiveAffect propertyPositiveAffect) {
		this.positiveAffect = propertyPositiveAffect;
	}
	
	public PositiveAffect getPropertyPositiveAffect() {
		return positiveAffect;
	}

	public static class PositiveAffect implements Property {

		private UnitValueSchema unitValueSchema;
		
		public PositiveAffect(UnitValueSchema unitValueSchema) {
			this.unitValueSchema = unitValueSchema;
		}

		@Override
		public String getJsonName() {
			return "positive_affect";
		}

		@Override
		public UnitValueSchema getJsonValue() {
			return this.unitValueSchema;
		}
		
	}

}
