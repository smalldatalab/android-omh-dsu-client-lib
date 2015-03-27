package edu.cornell.tech.smalldata.omhclientlib;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class OmhClientLibSQLiteOpenHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "OmhClientLib";
	private static final int DATABASE_VERSION = 1;
	
    public static final String TABLE_NAME_DATAPOINT = "DATAPOINT";
    public static final String COLUMN_NAME_DATPOINT_ID = "datapoint_id";
	public static final String COLUMN_NAME_PAYLOAD = "payload";
    private static final String CREATE_TABLE_DATAPOINT =
                "CREATE TABLE " + TABLE_NAME_DATAPOINT + " (" +
                COLUMN_NAME_DATPOINT_ID + " INTEGER PRIMARY KEY, " +		
                COLUMN_NAME_PAYLOAD + " TEXT);";
	
	public OmhClientLibSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_DATAPOINT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

	@Override
	public SQLiteDatabase getWritableDatabase() {
		return super.getWritableDatabase();
	}

	@Override
	public SQLiteDatabase getReadableDatabase() {
		return super.getReadableDatabase();
	}
	
}
