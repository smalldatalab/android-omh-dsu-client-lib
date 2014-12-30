package edu.cornell.tech.smalldata.omhclientlib;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class StartUserInterventionActivity extends Activity {

	private static String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;
	
	private static final int REQUEST_CODE_USER_INTERVENTION_SCREEN = 0;
	private Intent mIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		
		mIntent = getIntent();
		
		if (mIntent.hasExtra(OmhClientLibConsts.EXTRA_USER_INTERVENTION_INTENT)) {
			
			Intent userInterventionIntent = mIntent.getParcelableExtra(OmhClientLibConsts.EXTRA_USER_INTERVENTION_INTENT);
			if (userInterventionIntent != null) {
				startActivityForResult(userInterventionIntent, REQUEST_CODE_USER_INTERVENTION_SCREEN);
			} else {
				Log.d(LOG_TAG, "user intervention intent is null");
			}
			
		} else if (mIntent.hasExtra(OmhClientLibConsts.EXTRA_USER_INTERVENTION_PENDING_INTENT)) {
			
			PendingIntent userInterventionPendingIntent = mIntent.getParcelableExtra(OmhClientLibConsts.EXTRA_USER_INTERVENTION_PENDING_INTENT);
			if (userInterventionPendingIntent != null) {
				
				IntentSender intentSender = userInterventionPendingIntent.getIntentSender();
				try { startIntentSenderForResult(intentSender, REQUEST_CODE_USER_INTERVENTION_SCREEN, null, 0, 0, 0);}
				catch (SendIntentException e) {
					Log.e(LOG_TAG, "Error at startIntentSenderForResult", e);
				}
				
			} else {
				Log.d(LOG_TAG, "user intervention pending intent is null");
			}
			
		} else {
			Log.d(LOG_TAG, "missing both extras: EXTRA_NAME_USER_INTERVENTION_INTENT EXTRA_USER_INTERVENTION_PENDING_INTENT");
		}
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		switch (requestCode) {
		case REQUEST_CODE_USER_INTERVENTION_SCREEN:
			
			if (resultCode == RESULT_OK) {
				Log.d(LOG_TAG, "REQUEST_CODE_USER_INTERVENTION_SCREEN RESULT_OK");
			}
			
			LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
			
			Intent intent = new Intent(OmhClientLibConsts.ACTION_USER_INTERVENTION_SCREEN_FINISHED);
			if (mIntent.hasExtra(OmhClientLibConsts.EXTRA_DSU_INSTANCE_IDENTIFIER)) {
				intent.putExtra(OmhClientLibConsts.EXTRA_DSU_INSTANCE_IDENTIFIER, mIntent.getStringExtra(OmhClientLibConsts.EXTRA_DSU_INSTANCE_IDENTIFIER));
			}
			
			localBroadcastManager.sendBroadcast(intent);
			
			finish();
			
			break;
		default:
			break;
		}
	}
	
}
