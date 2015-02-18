package edu.cornell.tech.smalldata.omhclientlib.services;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibConsts;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibUtils;
import edu.cornell.tech.smalldata.omhclientlib.R;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnsupportedOmhSchemaException;
import edu.cornell.tech.smalldata.omhclientlib.schema.BodyWeightSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.LocationSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.MobilitySchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.OhmageResponseSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.PamSchema;
import edu.cornell.tech.smalldata.omhclientlib.schema.Schema;

public class OmhDsuWriter {
	
	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	private Context mContext;

	private Account mAccount;

	public static void writeDataPoint(Context context, Schema schema) {
		
		OmhDsuWriter omhDsuWriter = new OmhDsuWriter(context);
		omhDsuWriter.startWrite(schema);
	}
	
	public OmhDsuWriter(Context context) {
		this.mContext = context;
		mAccount = OmhClientLibUtils.createSyncAdapterAccount(context);
	}

	private void startWrite(final Schema schema) {
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				try {
					handleWrite(schema);
					
					if ("yes".equals(mContext.getString(R.string.omhclientlib_use_sync_adapter).toLowerCase())) {
						
						Bundle settingsBundle = new Bundle();
						settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
						settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
						
						String authority = mContext.getString(R.string.omhclientlib_syncadapter_provider_authority);
						
						ContentResolver.requestSync(mAccount, authority, settingsBundle);
						
					} else {
						
						Intent serviceIntent = new Intent(mContext, UploadToDsuService.class);
						mContext.startService(serviceIntent);
						
					}
					
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
		} else if (schema instanceof LocationSchema) {
			payload = DataPointPayloadCreator.createLocation(mContext, (LocationSchema) schema);
		} else if (schema instanceof MobilitySchema) {
			payload = DataPointPayloadCreator.createMobility(mContext, (MobilitySchema) schema);
		} else if (schema instanceof OhmageResponseSchema) {
			payload = DataPointPayloadCreator.createOhmageResponse(mContext, (OhmageResponseSchema) schema);
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
		
		if (mContext.getString(R.string.omhclientlib_display_datapoint_submitted_message).toLowerCase().equals("yes")) {
			
			Handler handler = new Handler(mContext.getMainLooper());
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					String text = "Your response has been submitted.";
					Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
				}
			};
			handler.post(runnable);
		}
		
		database.close();
		
	}

}
