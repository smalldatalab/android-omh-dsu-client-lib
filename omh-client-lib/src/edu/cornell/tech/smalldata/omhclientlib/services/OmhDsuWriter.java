package edu.cornell.tech.smalldata.omhclientlib.services;

import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibConsts;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper;
import edu.cornell.tech.smalldata.omhclientlib.schema.BodyWeightSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.Schema;
import edu.cornell.tech.smalldata.omhclientlib.schema.UnsupportedOmhSchemaException;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class OmhDsuWriter {
	
	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	private Context mContext;

	public static void writeDataPoint(Context context, Schema schema) {
		
		OmhDsuWriter omhDsuWriter = new OmhDsuWriter(context);
		omhDsuWriter.startWrite(schema);
	}
	
	public OmhDsuWriter(Context context) {
		this.mContext = context;
	}

	private void startWrite(final Schema schema) {
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				try {
					handleWrite(schema);
				} catch (UnsupportedOmhSchemaException e) {
					Log.e(LOG_TAG, "Unsuccessful OmH write", e);
				}
			}
		}).start();
		
	}

	private void handleWrite(Schema schema) throws UnsupportedOmhSchemaException {
		
		String payload = null;
		
		if (schema instanceof PamSchema) {
			payload = DataPointPayloadCreator.createPam(mContext, (PamSchema) schema);
		} else if (schema instanceof BodyWeightSchema) {
			payload = DataPointPayloadCreator.createBodyWeight(mContext, (BodyWeightSchema) schema);
		} else {
			throw new UnsupportedOmhSchemaException();
		}
		
		insertIntoDatabase(payload);
		
	}

	private void insertIntoDatabase(String payload) {
		
		OmhClientLibSQLiteOpenHelper databaseOpenHelper = new OmhClientLibSQLiteOpenHelper(mContext);
		SQLiteDatabase database = databaseOpenHelper.getWritableDatabase();
		
		String table = OmhClientLibSQLiteOpenHelper.TABLE_NAME_DATAPOINT;
		ContentValues contentValues = new ContentValues();
		contentValues.put(OmhClientLibSQLiteOpenHelper.COLUMN_NAME_PAYLOAD, payload);
		
		Log.d(LOG_TAG, "inserting " + payload.substring(0, 25) + "...");
		long rowId = database.insert(table, null, contentValues);
		Log.d(LOG_TAG, "row id: " + rowId);
		
	}

}
