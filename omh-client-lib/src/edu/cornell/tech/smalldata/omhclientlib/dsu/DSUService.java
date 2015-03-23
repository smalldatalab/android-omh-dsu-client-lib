package edu.cornell.tech.smalldata.omhclientlib.dsu;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * A service that is called by the OS to access the AccountAuthenticator and SyncAdapter for the DSU.
 * 
 * In order for your app to properly use these components (assuming you are not building with 
 * Gradle), you need to complete the following steps:
 * 
 * 1. Copy the <service ... /> item from the manifest in this library, to the manifest of your app. If
 * 	you do not do this, the OS will not recognize the service.
 * 
 * 2. Copy the file res/xml/dsu_authenticator.xml into your app, and at minimum update the 'accountType'.
 * 
 * The accountType string will be referenced within your app when using the AccountManager, and should be
 * unique to your app. If you don't copy this file over, the service will register with the OS with the
 * default accountType string.
 * 
 *  IMPORTANT: If another app has specified an AccountAuthenticator of the same accounType string, the OS will call 
 *  that service instead of this one, if it was installed first.  To avoid, make sure to unique your accountType
 *  string for each app that uses this library.  
 * 
 * 3. Copy the file res/xml/dsu_syncadapter.xml into you app.
 
 * 
 * @author jaredsieling
 *
 */
public class DSUService extends Service {
	private static String TAG = DSUService.class.getSimpleName();
    private DSUSyncAdapter sSyncAdapter;
    private static final Object sSyncAdapterLock = new Object();

    private DSUAuthenticator sAuth;
    private static final Object sAuthLock = new Object();

    @Override
    public void onCreate() {
    	// For thread-safe locking
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new DSUSyncAdapter(this, true);
            }
        }
        synchronized (sAuthLock) {
            if (sAuth == null) {
                sAuth = new DSUAuthenticator(this);
            }
        }
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		if(intent.getAction().equals("android.accounts.AccountAuthenticator")){
            return sAuth.getIBinder();
        }else if(intent.getAction().equals("android.content.SyncAdapter")) {
            return sSyncAdapter.getSyncAdapterBinder();
        }
		return null;
	}

}
