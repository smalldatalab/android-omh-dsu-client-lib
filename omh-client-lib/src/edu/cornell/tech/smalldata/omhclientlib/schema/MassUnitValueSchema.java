package edu.cornell.tech.smalldata.omhclientlib.schema;

public class MassUnitValueSchema implements Schema {

	public static final String NAME = "mass-unit-value";
	
	private Unit unit;
	private Value value;
	
	public MassUnitValueSchema(Unit propertyUnit, Value propertyValue) {
		this.unit = propertyUnit;
		this.value = propertyValue;
	}

	public Unit getPropertyUnit() {
		return unit;
	}

	public Value getPropertyValue() {
		return value;
	}

	public static class Unit implements Property {

		private MassUnit massUnit;
		
		public Unit(MassUnit massUnit) {
			this.massUnit = massUnit;
		}

		@Override
		public String getJsonName() {
			return "unit";
		}

		@Override
		public String getJsonValue() {
			return massUnit.getJsonValue();
		}
		
	}
	
	public static class Value implements Property {

		private double value;
		
		public Value(double value) {
			this.value = value;
		}

		@Override
		public String getJsonName() {
			return "value";
		}

		@Override
		public Double getJsonValue() {
			return this.value;
		}
		
	}
	
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
		 
		 public String getJsonValue() {
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

}
