package io.smalldatalab.omhclient;

import android.accounts.Account;
import android.content.Context;

public class DSUAuth {
    public static String TAG = DSUAuth.class.getSimpleName();


    public static final String ACCESS_TOKEN_TYPE = "access_token";
    public static final String REFRESH_TOKEN_TYPE = "refresh_token";

    public static String getDSUProviderAuthorities(Context cxt) {
        String authorities = cxt.getString(R.string.dsu_provider_authorities);
        if (authorities.equals("REPLACE_ME_IN_YOUR_APP")) throw new AssertionError();
        return authorities;
    }

    public static Account getDefaultAccount(Context cxt) {
        String appName = String.valueOf(cxt.getApplicationInfo().loadLabel(cxt.getPackageManager()));
        return new Account(appName, appName);
    }


}
