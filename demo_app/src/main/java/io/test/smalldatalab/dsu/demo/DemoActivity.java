package io.test.smalldatalab.dsu.demo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.json.JSONException;

import java.io.IOException;

import io.smalldatalab.omhclient.DSUAuth;
import io.smalldatalab.omhclient.DSUClient;
import io.smalldatalab.omhclient.DSUDataPoint;
import io.smalldatalab.omhclient.DSUDataPointBuilder;

import static android.view.View.OnClickListener;

public class DemoActivity extends ActionBarActivity {
    final static String TAG = DemoActivity.class.getSimpleName();


    private TextView textView;
    private Handler mHandler = new Handler();

    private void message(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DemoActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        this.textView = (TextView) this.findViewById(R.id.textView);
        final Activity activity = this;
        final DSUClient dsuClient =
                new DSUClient(
                        "https://lifestreams.smalldata.io/dsu", // dsu url
                        "io.smalldatalab.dummy", // dus client id
                        "xEUJgIdS2f12jmYomzEH", // dsu secret
                        this // Context
                );


        // sign in
        ((Button) this.findViewById(R.id.sign_in_button)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread() {

                    @Override
                    public void run() {
                        try {
                            if (dsuClient.blockingSignIn(activity) != null) {
                                message("Account created");
                            } else {
                                message("Account creation failed");
                            }
                            return;
                        } catch (AuthenticatorException e) {
                            e.printStackTrace();
                        } catch (OperationCanceledException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        message("Account creation failed");
                    }
                }.start();

            }
        });

        // sign out
        ((Button) this.findViewById(R.id.sign_out_button)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread() {

                    @Override
                    public void run() {
                        try {
                            if (dsuClient.blockingSignOut()) {
                                message("SignOut succeeded");
                            } else {
                                message("SignOut failed");
                            }
                            return;
                        } catch (AuthenticatorException e) {
                            e.printStackTrace();
                        } catch (OperationCanceledException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        message("SignOut failed");
                    }
                }.start();

            }
        });

        // submit data
        ((Button) this.findViewById(R.id.submit)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    DSUDataPoint datapoint =
                            new DSUDataPointBuilder()
                                    .setSchemaNamespace("io.smalldatalab")
                                    .setSchemaName("dummy")
                                    .setSchemaVersion("1.0")
                                    .setBody("{\"dummy\":0}")
                                    .setCreationDateTime(new DateTime()) // optional
                                    .setAcquisitionSource("dummy") // optional
                                    .setAcquisitionModality("sensed") // optional
                                    .createDSUDataPoint();
                    datapoint.save();
                    message("Saved:" + datapoint.toJson().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        // force upload
        ((Button) this.findViewById(R.id.forceUpload)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dsuClient.forceSync();
            }
        });
        // test refresh token
        ((Button) this.findViewById(R.id.refreshTokenTest)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        AccountManager accountManager = (AccountManager) activity.getSystemService(Context.ACCOUNT_SERVICE);
                        Account account = DSUAuth.getDefaultAccount(activity);
                        String accessToken = null;
                        try {
                            accessToken = accountManager.blockingGetAuthToken(account, DSUAuth.ACCESS_TOKEN_TYPE, false);
                            accountManager.invalidateAuthToken(account.type, accessToken);
                            String newAccessToken = accountManager.blockingGetAuthToken(account, DSUAuth.ACCESS_TOKEN_TYPE, false);
                            if (newAccessToken != null) {
                                message("Refresh Token Succeed");
                            } else {
                                message("Refresh Token Failed");
                            }
                        } catch (Exception e) {
                            message("Refresh Token Failed");
                            e.printStackTrace();
                        }
                        Looper.loop();
                    }
                }.start();

            }
        });
        /**
         * Periodically check the status of sign-in and number of data points at local
         */
        Runnable mStatusChecker = new Runnable() {
            @Override
            public void run() {
                String status = dsuClient.isSignedIn() ? "Signed in" : "Not Signed In";
                long numDatapoint = DSUDataPoint.count(DSUDataPoint.class, null, null);
                status += "\nData Points:" + numDatapoint;
                textView.setText(status);
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.postDelayed(mStatusChecker, 500);

    }


}
