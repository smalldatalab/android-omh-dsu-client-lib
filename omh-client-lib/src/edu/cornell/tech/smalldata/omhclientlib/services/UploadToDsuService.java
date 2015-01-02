package edu.cornell.tech.smalldata.omhclientlib.services;

import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibConsts;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibUtils;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HttpPostRequestFailedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoAccessTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnauthorizedWriteAttemptException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnsuccessfulWriteException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;
import static edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper.*;

/**
 * Checks SQLite DATAPOINT table and tries to upload to DSU all datapoints stored in it.
 */
public class UploadToDsuService extends Service {
	
	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	private Context mContext;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				handleUpload();
			}
		}).start();
		
		return Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void handleUpload() {
		
		mContext = getApplicationContext();
		
		boolean ok = true;
		
		if (checkConnectivity() == false) {
			ok = false;
		}
		
		if (ok) {
			uploadDatapoints();
		}
		
		stopSelf();
		
	}

	private boolean checkConnectivity() {
		return true;
	}

	private void uploadDatapoints() {
		
		OmhClientLibSQLiteOpenHelper databaseOpenHelper = new OmhClientLibSQLiteOpenHelper(mContext);
		SQLiteDatabase database = databaseOpenHelper.getWritableDatabase();
		
		String sql = "SELECT " + COLUMN_NAME_DATPOINT_ID + ", " + COLUMN_NAME_PAYLOAD + " FROM " + TABLE_NAME_DATAPOINT;
		Log.d(LOG_TAG, sql);
		Cursor cursor = database.rawQuery(sql, null);
		Log.d(LOG_TAG, "Returned " + cursor.getCount() + " records.");
		
		while (cursor.moveToNext()) {
			
			String payload = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_PAYLOAD));
			
			try {
				OmhClientLibUtils.writeDataPointRequest(payload, mContext);
				
				String[] dataPointId = {Integer.toString(cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_DATPOINT_ID)))};
				int rowsDeleted = database.delete(TABLE_NAME_DATAPOINT, COLUMN_NAME_DATPOINT_ID + " = ?", dataPointId);
				Log.d(LOG_TAG, "deleting datapoint_id " + dataPointId[0] + ", rows deleted: " + rowsDeleted);
				
			} catch (NoAccessTokenException e) {
				// TODO request authorization code and exchange it for tokens
			} catch (UnauthorizedWriteAttemptException e) {
				// TODO refresh token
			} catch (HttpPostRequestFailedException | UnsuccessfulWriteException e) {
				Log.e("proba", "Exception at writing data point DSU", e);
			}
			
		}
		
		cursor.close();
		
	}
}
