package edu.cornell.tech.smalldata.omhclientlib.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibConsts;
import edu.cornell.tech.smalldata.omhclientlib.services.UploadToDsuService;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	
	private static final String LOG_TAG = OmhClientLibConsts.APP_LOG_TAG;

	private Context mContext;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		
		mContext = context;
	}

	public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);

		mContext = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		Log.d(LOG_TAG, "onPerformSync called.");
		
		Intent serviceIntent = new Intent(mContext, UploadToDsuService.class);
		mContext.startService(serviceIntent);

	}

}
