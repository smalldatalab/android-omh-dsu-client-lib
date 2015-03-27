package io.smalldatalab.omhclient;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Upload data points to the DSU. The id of data point is randomly assign by with UUID. When data
 * point id conflict occurs (409), the data point will be skipped and uploaded later with  a new id.
 * The data point that cannot be parsed as JSON will be deleted.
 * <p/>
 * When 401 is returned, the access token will be invalidated, and the AccountAuthenticator will
 * refresh the access token in the next sync iteration.
 */
public class DSUSyncAdapter extends AbstractThreadedSyncAdapter {

    private Context mContext;
    final static String TAG = DSUSyncAdapter.class.getSimpleName();

    public DSUSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.mContext = context;
    }

    class TokenExpiredError extends Exception {
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.v(TAG, "************* Start Syncing ************************");
        try {
            AccountManager accountManager = (AccountManager) getContext().getSystemService(Context.ACCOUNT_SERVICE);
            List<DSUDataPoint> datapoints = DSUDataPoint.listAll(DSUDataPoint.class);

            String token = accountManager.blockingGetAuthToken(DSUAuth.getDefaultAccount(mContext), DSUAuth.ACCESS_TOKEN_TYPE, true);
            DSUClient client = DSUClient.getDSUClientFromUserData(account, getContext());
            for (DSUDataPoint datapoint : datapoints) {
                try {
                    JSONObject json = datapoint.toJson();
                    Log.v(TAG, "Try to Upload " + json.toString());

                    Response response = client.postData(token, json.toString());
                    if (response.isSuccessful()) { // success or conflict(409)
                        // Remove the data point from the database
                        datapoint.delete();
                        syncResult.stats.numEntries++;
                        syncResult.stats.numDeletes++;
                    } else if (response.code() == 409) {
                        Log.i(TAG, "Conflict datapoint id. Try again with a new id later.");
                        syncResult.stats.numSkippedEntries++;
                    } else if (response.code() == 401) {// the token is invalid
                        // invalidate the token
                        accountManager.invalidateAuthToken(DSUAuth.getDefaultAccount(mContext).type, token);
                        throw new TokenExpiredError();
                    } else {
                        // unknown response
                        Log.e(TAG, "Unknown response code(" + response.code() + ") from DSU" + response.body() + "for data:" + json);
                        syncResult.stats.numSkippedEntries++;
                    }
                } catch (JSONException e) {
                    syncResult.stats.numSkippedEntries++;
                    datapoint.delete();
                    Log.e(TAG, "Delete datapoint that cannot be parsed:" + datapoint, e);
                }
            }
        } catch (AuthenticatorException e) {
            syncResult.stats.numAuthExceptions++;
            Log.e(TAG, "Sync error", e);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "Sync error", e);
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            Log.e(TAG, "Sync error", e);
        } catch (TokenExpiredError tokenExpiredError) {
            syncResult.stats.numIoExceptions++;
            Log.i(TAG, "Token expired");
        }
        Log.v(TAG, "************* End Syncing ************************");

    }
}
