package edu.cornell.tech.smalldata.omhclientlib;

import android.content.Intent;


public class OmhClientLibConsts {
	
	public static final String APP_LOG_TAG = "OmhClientLib";

	public static final String EXTRA_USER_INTERVENTION_INTENT = "edu.cornell.tech.smalldata.omhclientlib.USER_INTERVENTION_INTENT";
	public static final String EXTRA_USER_INTERVENTION_PENDING_INTENT = "edu.cornell.tech.smalldata.omhclientlib.USER_INTERVENTION_PENDING_INTENT";
	public static final String EXTRA_DSU_INSTANCE_IDENTIFIER = "edu.cornell.tech.smalldata.omhclientlib.DSU_INSTANCE_IDENTIFIER";

	public static final String ACTION_USER_INTERVENTION_SCREEN_FINISHED = "edu.cornell.tech.smalldata.omhclientlib.USER_INTERVENTION_SCREEN_FINISHED";
	public static final String ACTION_AUTHORIZATION_CODE_SERVICE_FINISHED = "edu.cornell.tech.smalldata.omhclientlib.AUTHORIZATION_CODE_SERVICE_FINISHED";

	public static final String SHARED_PREFERENCES_OMHCLIENTLIB = "shared-preferences-omhclientlib";

	public static final String PREFERENCES_KEY_AUTHORIZATION_CODE = "AUTHORIZATION_CODE";
	public static final String PREFERENCES_KEY_DSU_ACCESS_TOKEN = "DSU_ACCESS_TOKEN";
	public static final String PREFERENCES_KEY_DSU_REFRESH_TOKEN = "DSU_REFRESH_TOKEN";
	public static final String PREFERENCES_KEY_INSTALLATION_ID = "INSTALLATION_ID";
	public static final String PREFERENCES_KEY_DATA_POINT_SEQUENCE = "DATA_POINT_SEQUENCE";

	public static final String SCHEMA_NAMESPACE = "omh";
	public static final String SCHEMA_VERSION = "1.0";

	public static final String ACCOUNT = "dummyaccount";
	public static final String ACCOUNT_TYPE = "edu.cornell.tech.smalldata.omhclientlib";

	public static String sInstallationId;

}
