package edu.cornell.tech.smalldata.omhclientlib.schema;

import java.util.Calendar;

import edu.cornell.tech.smalldata.omhclientlib.schema.TimeFrameSchema.DateTime;

public class PamSchema implements Schema {
	
	public String getSchemaName() {
		return "pam";
	}
	
	private AffectValence affectValence;
	private AffectArousal affectArousal;
	private PositiveAffect positiveAffect;
	private NegativeAffect negativeAffect;
	private Mood mood;
	private EffectiveTimeFrame effectiveTimeFrame;
	
	public PamSchema(int position, Calendar calendar) {
		
		int valence = valence(position);
		int arousal = arousal(position);
		int positiveAffect = positiveAffect(valence, arousal);
		int negativeAffect = negativeAffect(valence, arousal);
		MoodEnum moodEnum = determineMood(valence, arousal);
		
		UnitValueSchema.Unit propertyUnit = new UnitValueSchema.Unit("");
		UnitValueSchema.Value propertyValue = new UnitValueSchema.Value(valence);
		UnitValueSchema unitValueSchema = new UnitValueSchema(propertyUnit, propertyValue);
		AffectValence propertyAffectValence = new AffectValence(unitValueSchema);
		
		propertyUnit = new UnitValueSchema.Unit("");
		propertyValue = new UnitValueSchema.Value(arousal);
		unitValueSchema = new UnitValueSchema(propertyUnit, propertyValue);
		AffectArousal propertyAffectArousal = new AffectArousal(unitValueSchema);
		
		propertyUnit = new UnitValueSchema.Unit("");
		propertyValue = new UnitValueSchema.Value(positiveAffect);
		unitValueSchema = new UnitValueSchema(propertyUnit, propertyValue);
		PositiveAffect propertyPositiveAffect = new PositiveAffect(unitValueSchema);
		
		propertyUnit = new UnitValueSchema.Unit("");
		propertyValue = new UnitValueSchema.Value(negativeAffect);
		unitValueSchema = new UnitValueSchema(propertyUnit, propertyValue);
		NegativeAffect propertyNegativeAffect = new NegativeAffect(unitValueSchema);
		
		Mood propertyMood = new Mood(moodEnum);
		
		DateTime dateTime = new DateTime(calendar);
		TimeFrameSchema timeFrameSchema = new TimeFrameSchema(dateTime);
		EffectiveTimeFrame propertyEffectiveTimeFrame = new EffectiveTimeFrame(timeFrameSchema);
		
		this.affectValence = propertyAffectValence;
		this.affectArousal = propertyAffectArousal;
		this.positiveAffect = propertyPositiveAffect;
		this.negativeAffect = propertyNegativeAffect;
		this.mood = propertyMood;
		this.effectiveTimeFrame = propertyEffectiveTimeFrame;
		
	}
	
	public AffectValence getPropertyAffectValence() {
		return affectValence;
	}
	
	public AffectArousal getPropertyAffectArousal() {
		return affectArousal;
	}
	
	public PositiveAffect getPropertyPositiveAffect() {
		return positiveAffect;
	}
	
	public NegativeAffect getPropertyNegativeAffect() {
		return negativeAffect;
	}
	
	public Mood getPropertyMood() {
		return mood;
	}
	
	public EffectiveTimeFrame getPropertyEffectiveTimeFrame() {
		return effectiveTimeFrame;
	}

	private int arousal(int position) {
		
		switch (position) {
		case 1: case 2: case 3: case 4:
			return 4;
		case 5: case 6: case 7: case 8:
			return 3;
		case 9: case 10: case 11: case 12:
			return 2;
		case 13: case 14: case 15: case 16:
			return 1;
		default:
			return 0;
		}
		
	}

	private int valence(int position) {
		
		switch (position) {
		case 1: case 5: case 9: case 13:
			return 1;
		case 2: case 6: case 10: case 14:
			return 2;
		case 3: case 7: case 11: case 15:
			return 3;
		case 4: case 8: case 12: case 16:
			return 4;
		default:
			return 0;
		}

		
	}
	
	private int positiveAffect(int valence, int arousal) {
		
		if (valence > 0 && valence < 5 && arousal > 0 && arousal < 5) {
			return (4 * valence) + arousal - 4; 
		} else {
			return 0;
		}
		
	}

	private int negativeAffect(int valence, int arousal) {
		
		if (valence > 0 && valence < 5 && arousal > 0 && arousal < 5) {
			return 4 * (5 - valence) + arousal - 4; 
		} else {
			return 0;
		}
		
	}

	private MoodEnum determineMood(int valence, int arousal) {
		
		switch (arousal) {
		case 4:
			switch (valence) {
			case 1: return MoodEnum.AFRAID;
			case 2: return MoodEnum.TENSE;
			case 3: return MoodEnum.EXCITED;
			case 4: return MoodEnum.DELIGHTED;
			default: return null;
			}
		case 3:
			switch (valence) {
			case 1: return MoodEnum.FRUSTRATED;
			case 2: return MoodEnum.ANGRY;
			case 3: return MoodEnum.HAPPY;
			case 4: return MoodEnum.GLAD;
			default: return null;
			}
		case 2:
			switch (valence) {
			case 1: return MoodEnum.MISERABLE;
			case 2: return MoodEnum.SAD;
			case 3: return MoodEnum.CALM;
			case 4: return MoodEnum.SATISFIED;
			default: return null;
			}
		case 1:
			switch (valence) {
			case 1: return MoodEnum.GLOOMY;
			case 2: return MoodEnum.TIRED;
			case 3: return MoodEnum.SLEEPY;
			case 4: return MoodEnum.SERENE;
			default: return null;
			}
		default:
			return null;
		}
		
	}

	public enum MoodEnum {
		AFRAID,
        TENSE,
        EXCITED,
        DELIGHTED,
        FRUSTRATED,
        ANGRY,
        HAPPY,
        GLAD,
        MISERABLE,
        SAD,
        CALM,
        SATISFIED,
        GLOOMY,
        TIRED,
        SLEEPY,
        SERENE
	}
	
	public static class AffectValence implements Property {
		
		private UnitValueSchema unitValueSchema;
		
		public AffectValence(UnitValueSchema unitValueSchema) {
			this.unitValueSchema = unitValueSchema;
		}
		
		@Override
		public String getJsonName() {
			return "affect-valence";
		}
		
		@Override
		public UnitValueSchema getJsonValue() {
			return this.unitValueSchema;
		}
		
	}

	public static class AffectArousal implements Property {
		
		private UnitValueSchema unitValueSchema;
		
		public AffectArousal(UnitValueSchema unitValueSchema) {
			this.unitValueSchema = unitValueSchema;
		}
		
		@Override
		public String getJsonName() {
			return "affect-arousal";
		}
		
		@Override
		public UnitValueSchema getJsonValue() {
			return this.unitValueSchema;
		}
		
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
	
	public static class NegativeAffect implements Property {
		
		private UnitValueSchema unitValueSchema;
		
		public NegativeAffect(UnitValueSchema unitValueSchema) {
			this.unitValueSchema = unitValueSchema;
		}
		
		@Override
		public String getJsonName() {
			return "negative_affect";
		}
		
		@Override
		public UnitValueSchema getJsonValue() {
			return this.unitValueSchema;
		}
		
	}
	
	public static class Mood implements Property {

		private MoodEnum moodEnum;
		
		public Mood(MoodEnum moodEnum) {
			this.moodEnum = moodEnum;
		}

		@Override
		public String getJsonName() {
			return "mood";
		}

		@Override
		public String getJsonValue() {
			
			switch (this.moodEnum) {
			case AFRAID:
				return "afraid";
			case TENSE:
				return "tense";
			case EXCITED:
				return "excited";
			case DELIGHTED:
				return "delighted";
			case FRUSTRATED:
				return "frustrated";
			case ANGRY:
				return "angry";
			case HAPPY:
				return "happy";
			case GLAD:
				return "glad";
			case MISERABLE:
				return "miserable";
			case SAD:
				return "sad";
			case CALM:
				return "calm";
			case SATISFIED:
				return "satisfied";
			case GLOOMY:
				return "gloomy";
			case TIRED:
				return "tired";
			case SLEEPY:
				return "sleepy";
			case SERENE:
				return "serene";
			default:
				return "";
			}
			
		}
		
	}

	public static class EffectiveTimeFrame implements Property {

		private TimeFrameSchema timeFrameSchema;

		public EffectiveTimeFrame(TimeFrameSchema timeFrameSchema) {
			this.timeFrameSchema = timeFrameSchema;
		}
		
		@Override
		public String getJsonName() {
			return "effective_time_frame";
		}

		@Override
		public TimeFrameSchema getJsonValue() {
			return timeFrameSchema;
		}
		
	}
}
