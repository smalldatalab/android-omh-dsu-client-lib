package edu.cornell.tech.smalldata.omhclientlib.dsu;

import android.accounts.Account;

public class DSUAuth {
	public static final String ACCOUNT_TYPE = "io.smalldata.dsu.pam";
    public static final String ACCOUNT_NAME = "Context";
    public static final String ACCESS_TOKEN_TYPE = "access_token";
    public static final String REFRESH_TOKEN_TYPE = "refresh_token";
    public  static final Account ACCOUNT = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
}
