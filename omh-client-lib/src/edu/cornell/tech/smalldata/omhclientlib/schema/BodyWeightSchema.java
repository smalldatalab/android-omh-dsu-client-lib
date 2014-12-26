package edu.cornell.tech.smalldata.omhclientlib.schema;



public class BodyWeightSchema implements Schema {

	public static final String NAME = "body-weight";
	
	private BodyWeight bodyWeight;
	
	public BodyWeightSchema(BodyWeight propertyBodyWeight) {
		this.bodyWeight = propertyBodyWeight;
	}

	public BodyWeight getPropertyBodyWeight() {
		return bodyWeight;
	}

	public static class BodyWeight implements Property {
		
		private MassUnitValueSchema massUnitValueSchema;
		
		public BodyWeight(MassUnitValueSchema massUnitValueSchema) {
			this.massUnitValueSchema = massUnitValueSchema;
		}

		@Override
		public String getJsonName() {
			return "body_weight";
		}
		
		@Override
		public MassUnitValueSchema getJsonValue() {
			return massUnitValueSchema;
		}
		
	}
	
}
