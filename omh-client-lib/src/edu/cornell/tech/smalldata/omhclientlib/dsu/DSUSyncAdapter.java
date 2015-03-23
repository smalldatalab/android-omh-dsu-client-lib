package edu.cornell.tech.smalldata.omhclientlib.dsu;

import static edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper.COLUMN_NAME_DATPOINT_ID;
import static edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper.COLUMN_NAME_PAYLOAD;
import static edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper.TABLE_NAME_DATAPOINT;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibConsts;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibSQLiteOpenHelper;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibUtils;
import edu.cornell.tech.smalldata.omhclientlib.R;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.ConflictInWriteRequestException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.HttpPostRequestFailedException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoAccessTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.NoRefreshTokenException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnauthorizedWriteAttemptException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnsuccessfulDatapointUploadException;
import edu.cornell.tech.smalldata.omhclientlib.exceptions.UnsuccessfulWriteToDsuException;
import edu.cornell.tech.smalldata.omhclientlib.services.AuthorizationCodeService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class DSUSyncAdapter extends AbstractThreadedSyncAdapter {

	private Context mContext;
	private LocalBroadcastManager mLocalBroadcastManager;
	private BroadcastReceiver mAuthorizationCodeServiceFinishedBR;
	
	final static String LOG_TAG = "DSUSyncAdapter";
	
	public DSUSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.mContext = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		// TODO Auto-generated method stub
		try{
			uploadDatapoints();
		} catch (Exception e){
			e.printStackTrace();
		}
		

	}

	private void uploadDatapoints() throws UnsuccessfulDatapointUploadException {

		OmhClientLibSQLiteOpenHelper databaseOpenHelper = new OmhClientLibSQLiteOpenHelper(mContext);
		SQLiteDatabase database = databaseOpenHelper.getWritableDatabase();

		String sql = "SELECT " + COLUMN_NAME_DATPOINT_ID + ", " + COLUMN_NAME_PAYLOAD + " FROM " + TABLE_NAME_DATAPOINT;
		Cursor cursor = database.rawQuery(sql, null);

		boolean exitPayloadsLoop = false;
		while (cursor.moveToNext() && !exitPayloadsLoop) {

			String payload = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_PAYLOAD));

			boolean retry;
			do {
				retry = false;

				try {
					writeDataPointRequest(payload, mContext);

					deleteDataPoint(database, cursor);

				} catch (NoAccessTokenException e) {
					exitPayloadsLoop = true;
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
	}

	private void handleUnauthorizedWriteAttempt() throws HttpPostRequestFailedException, NoRefreshTokenException {

		OmhClientLibUtils.refreshAaccessTokenRequest(mContext);
	}
	
	/**
	 * Sends HTTP POST with datapoint payload to DSU.
	 * @param entity
	 * @param context
	 * @throws NoAccessTokenException
	 * @throws HttpPostRequestFailedException
	 * @throws UnsuccessfulWriteToDsuException
	 * @throws IOException 
	 * @throws AuthenticatorException 
	 * @throws OperationCanceledException 
	 */
	public void writeDataPointRequest(String entity, Context context)
			throws NoAccessTokenException, HttpPostRequestFailedException, UnsuccessfulWriteToDsuException, UnauthorizedWriteAttemptException,
				   ConflictInWriteRequestException {
		
		StringBuilder urlStringBuilder = new StringBuilder();
		urlStringBuilder.append(context.getString(R.string.dsu_root_url)).append('/').append("dataPoints");
	
		final String urlString = urlStringBuilder.toString();
		Log.d(LOG_TAG, urlString);
	
		HttpClient httpClient = new DefaultHttpClient();
		
		HttpPost httpPost = new HttpPost(urlString);
		
		String accessToken = "";
		try {
			AccountManager mAccountManager = AccountManager.get(context);
			accessToken = mAccountManager.blockingGetAuthToken(DSUAuth.ACCOUNT, "access_token", true);
		} catch (Exception e){
			throw new NoAccessTokenException();
		}
		String authorization = "Bearer " + accessToken;
		httpPost.setHeader("Authorization", authorization);
		Log.d(LOG_TAG, "Header Authorization: " + authorization);
		
		httpPost.setHeader("Content-type", "application/json");
		
		HttpResponse httpResponse = null;
	
		InputStream responseContentInputStream = null;
		InputStreamReader isr = null;
		
		try {
			
			httpPost.setEntity(new StringEntity(entity));
			Log.d(LOG_TAG, entity.substring(0, 50) + "...");
	
			httpResponse = httpClient.execute(httpPost);
			StringBuilder sb = new StringBuilder();
	
			if (httpResponse != null) {
	
				responseContentInputStream = httpResponse.getEntity().getContent();
				isr = new InputStreamReader(responseContentInputStream);
				
				char[] c = new char[8];
				int len;
				while ((len = isr.read(c)) != -1) {
					sb.append(c, 0, len);
				}
	
			}
	
			Log.d(LOG_TAG, "Response: " + sb.toString());
			StatusLine statusLine = httpResponse.getStatusLine();
			Log.d(LOG_TAG, String.format("status code: %d reason: %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
			if (HttpStatus.SC_UNAUTHORIZED == statusLine.getStatusCode()) {
				UnauthorizedWriteAttemptException e = new UnauthorizedWriteAttemptException();
				throw e;
			} else if (HttpStatus.SC_CONFLICT == statusLine.getStatusCode()) {
				ConflictInWriteRequestException e = new ConflictInWriteRequestException();
				throw e;
			} else if (HttpStatus.SC_CREATED != statusLine.getStatusCode()) {
				UnsuccessfulWriteToDsuException e = new UnsuccessfulWriteToDsuException();
				throw e;
			}
	
		} catch (UnsuccessfulWriteToDsuException | UnauthorizedWriteAttemptException | ConflictInWriteRequestException e) {
			throw e;
		} catch (Throwable tr) {
			HttpPostRequestFailedException e = new HttpPostRequestFailedException();
			e.addSuppressed(tr);
			throw e;
		}
		finally {
			try { 
				isr.close();
				isr = null;
				responseContentInputStream.close();
				responseContentInputStream = null; 
			}
			catch (IOException e) {
				Log.e(LOG_TAG, "Exception at closing http response input stream and reader.", e);
			}
		}
	}

}
