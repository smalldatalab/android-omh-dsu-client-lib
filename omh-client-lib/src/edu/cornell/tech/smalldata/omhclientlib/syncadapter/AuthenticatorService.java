package edu.cornell.tech.smalldata.omhclientlib.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service {
	
	private AccountAuthenticator mAccountAuthenticator;
	
    @Override
    public void onCreate() {
    	
        mAccountAuthenticator = new AccountAuthenticator(this);
        
    }

	@Override
	public IBinder onBind(Intent intent) {
		
		return mAccountAuthenticator.getIBinder();
		
	}

}
