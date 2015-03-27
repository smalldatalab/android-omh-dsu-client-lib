package edu.cornell.tech.smalldata.omhclientlib.services;

import static edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper.COLUMN_NAME_DATPOINT_ID;
import static edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper.COLUMN_NAME_PAYLOAD;
import static edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper.TABLE_NAME_DATAPOINT;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibConsts;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibUtils;
import edu.cornell.tech.smalldata.omhclientlib.R;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.ConflictInWriteRequestException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.ExchangingAuthCodeForTokensException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HttpPostRequestFailedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoAccessTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoRefreshTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnauthorizedWriteAttemptException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnsuccessfulDatapointUploadException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnsuccessfulWriteToDsuException;

/**
 * Checks SQLite DATAPOINT table and tries to upload to DSU all datapoints stored in it.
 */
public class UploadToDsuService extends Service {
	
	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	private enum State {
		UPLOADING_DATAPOINTS,
		WAITING_FOR_AUTHORIZATION_CODE_SERVICE,
		EXCHANGING_AUTHORIZATION_CODE_FOR_TOKENS
	};

	private State mState;
	
	private Context mContext;

	private LocalBroadcastManager mLocalBroadcastManager;
	private BroadcastReceiver mAuthorizationCodeServiceFinishedBR;

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

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mLocalBroadcastManager != null && mAuthorizationCodeServiceFinishedBR != null) {
			mLocalBroadcastManager.unregisterReceiver(mAuthorizationCodeServiceFinishedBR);
		}
		
		Log.d(LOG_TAG, UploadToDsuService.class.getSimpleName() + " finished."  );
	}
	
	private void setState(State state) {
		mState = state;
		Log.d(LOG_TAG, "UploadToDsuService state: " + state.toString());
	}

	private void handleUpload() {
		
		setState(State.UPLOADING_DATAPOINTS);
		mContext = getApplicationContext();
		
		boolean ok = true;
		
		if (checkConnectivity() == false) {
			ok = false;
		}
		
		if (ok) {
			try {
				uploadDatapoints();
			} catch (UnsuccessfulDatapointUploadException e) {
				Log.e(LOG_TAG, "Exception at uploading datapoints.", e);
			} catch (Throwable tr) {
				Log.e(LOG_TAG, "Throwable at uploading datapoints.", tr);
			}
		}
		
		if (mState != State.WAITING_FOR_AUTHORIZATION_CODE_SERVICE) {
			stopSelf();
		}
		
	}

	private boolean checkConnectivity() {
		
		boolean ok = false;
		boolean exception = false;
		int status = -1;
		try {
			status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
		} catch (Exception e) {
			exception = true;
		}
		if ((status != ConnectionResult.SUCCESS) || exception) {
			
			Handler handler = new Handler(Looper.getMainLooper());
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					
					Toast.makeText(mContext, R.string.google_play_services_not_available_message, Toast.LENGTH_LONG).show();
				}
			};
			handler.post(runnable);
			
		} else {
			ok = true;
		}
		
		return ok;
	}

	private void uploadDatapoints() throws UnsuccessfulDatapointUploadException {
		
		OmhClientLibSQLiteOpenHelper databaseOpenHelper = new OmhClientLibSQLiteOpenHelper(mContext);
		SQLiteDatabase database = databaseOpenHelper.getWritableDatabase();
		
		String sql = "SELECT " + COLUMN_NAME_DATPOINT_ID + ", " + COLUMN_NAME_PAYLOAD + " FROM " + TABLE_NAME_DATAPOINT;
		Log.d(LOG_TAG, sql);
		Cursor cursor = database.rawQuery(sql, null);
		Log.d(LOG_TAG, "Returned " + cursor.getCount() + " records.");
		
		boolean exitPayloadsLoop = false;
		while (cursor.moveToNext() && !exitPayloadsLoop) {
			
			String payload = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_PAYLOAD));
			
			boolean retry;
			do {
				retry = false;
				
				try {
					OmhClientLibUtils.writeDataPointRequest(payload, mContext);
					
					deleteDataPoint(database, cursor);
					
				} catch (NoAccessTokenException e) {
					exitPayloadsLoop = true;
					handleAccessTokenAbsence();
				} catch (UnauthorizedWriteAttemptException e) {
					try {
						handleUnauthorizedWriteAttempt();
						retry = true;
					} catch (HttpPostRequestFailedException | NoRefreshTokenException e1) {
						UnsuccessfulDatapointUploadException e2 = new UnsuccessfulDatapointUploadException();
						e2.addSuppressed(e1);
						throw e2;
					}
				} catch (ConflictInWriteRequestException e) {
					deleteDataPoint(database, cursor);
				} catch (HttpPostRequestFailedException | UnsuccessfulWriteToDsuException e) {
					UnsuccessfulDatapointUploadException e1 = new UnsuccessfulDatapointUploadException();
					e1.addSuppressed(e);
					throw e1;
				}
				
			} while (retry);
			
		}
		
		cursor.close();
		database.close();
		
	}

	private void deleteDataPoint(final SQLiteDatabase database, final Cursor cursor) {
		
		String[] dataPointId = {Integer.toString(cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_DATPOINT_ID)))};
		int rowsDeleted = database.delete(TABLE_NAME_DATAPOINT, COLUMN_NAME_DATPOINT_ID + " = ?", dataPointId);
		Log.d(LOG_TAG, "deleting datapoint_id " + dataPointId[0] + ", rows deleted: " + rowsDeleted);
		
	}

	private void handleAccessTokenAbsence() {

		if (mLocalBroadcastManager == null) {
			mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
		}
		if (mAuthorizationCodeServiceFinishedBR == null) {
			mAuthorizationCodeServiceFinishedBR = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					onReceiveAuthorizationCodeServiceFinished(context, intent);
				}
			};
		}
		
		IntentFilter intentFilter = new IntentFilter(OmhClientLibConsts.ACTION_AUTHORIZATION_CODE_SERVICE_FINISHED);
		mLocalBroadcastManager.registerReceiver(mAuthorizationCodeServiceFinishedBR, intentFilter);
		
		setState(State.WAITING_FOR_AUTHORIZATION_CODE_SERVICE);
		
		Intent intent = new Intent(this, AuthorizationCodeService.class);
		startService(intent);
	}

	protected void onReceiveAuthorizationCodeServiceFinished(Context context, Intent intent) {

		if (mLocalBroadcastManager != null && mAuthorizationCodeServiceFinishedBR != null) {
			mLocalBroadcastManager.unregisterReceiver(mAuthorizationCodeServiceFinishedBR);
		}
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					setState(State.EXCHANGING_AUTHORIZATION_CODE_FOR_TOKENS);
					OmhClientLibUtils.exchangeAuthCodeForTokens(mContext);
				} catch (ExchangingAuthCodeForTokensException e) {
					Log.e(LOG_TAG, "Exception at exchanging auth code for tokens.", e);
					stopSelf();
				}
				
				handleUpload();
			}
		}).start();
	}

	private void handleUnauthorizedWriteAttempt() throws HttpPostRequestFailedException, NoRefreshTokenException {
		
		OmhClientLibUtils.refreshAaccessTokenRequest(mContext);
	}
	
}
