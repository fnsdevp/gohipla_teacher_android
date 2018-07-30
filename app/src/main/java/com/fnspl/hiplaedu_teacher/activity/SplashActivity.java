package com.fnspl.hiplaedu_teacher.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.beaconsDetection.Constants;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.model.ZoneInfo;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.fnspl.hiplaedu_teacher.utils.MarshmallowPermissionHelper;
import com.navigine.naviginesdk.NavigineSDK;

import io.paperdb.Paper;

public class SplashActivity extends BaseActivity {

    private static final int REQUEST_ALL_PERMISSION = 100;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Constants.activity = SplashActivity.this;

        setContentView(R.layout.activity_splash);

        setUpZoneData();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                nextPage();
            }
        }, 3000);

    }

    private void nextPage() {
        if (Paper.book().read(CONST.PROFILE_INFO) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(R.anim.slideinfromright, R.anim.slideouttoleft);
            supportFinishAfterTransition();
        } else {
            if (haveNetworkConnection()) {
                checkNavinginePermissions();
            } else {
                showNetConnectionDialog();
            }
        }
    }

    private void doCalculation() {
        if (Paper.book().read(CONST.CLASS_STARTED, false)) {
            startActivity(new Intent(this, NotificationHandleActivity.class).putExtra(CONST.NOTIFICATION_TYPE, NotificationHandleActivity.CLASS_START));
            overridePendingTransition(R.anim.slideinfromright, R.anim.slideouttoleft);
            supportFinishAfterTransition();
        } else if (Paper.book().read(CONST.CLASS_ENDED, false)) {
            startActivity(new Intent(this, NotificationHandleActivity.class).putExtra(CONST.NOTIFICATION_TYPE, NotificationHandleActivity.CLASS_END));
            overridePendingTransition(R.anim.slideinfromright, R.anim.slideouttoleft);
            supportFinishAfterTransition();
        } else {
            startActivity(new Intent(this, DashboardActivity.class));
            overridePendingTransition(R.anim.slideinfromright, R.anim.slideouttoleft);
            supportFinishAfterTransition();
        }
    }

    private void showNetConnectionDialog() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(SplashActivity.this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(SplashActivity.this);
        }
        builder.setTitle("No Internet Connection")
                .setMessage("Please connect to local WIFI and then proceed.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        if (haveNetworkConnection()) {
                            checkNavinginePermissions();
                        } else {
                            showNetConnectionDialog();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void checkNavinginePermissions() {
        if (Build.VERSION.SDK_INT > 22) {
            if (MarshmallowPermissionHelper.getAllNaviginePermission(null
                    , this, REQUEST_ALL_PERMISSION)) {
                if (!MainApplication.isNavigineInitialized) {
                    (new InitTask(this)).execute();
                } else {
                    if (Paper.book().read(CONST.PROFILE_INFO, null) != null) {
                        doCalculation();
                    }
                }
            }
        } else {
            if (!MainApplication.isNavigineInitialized) {
                (new InitTask(this)).execute();
            } else {
                if (Paper.book().read(CONST.PROFILE_INFO, null) != null) {
                    doCalculation();
                }
            }
        }
    }

    // Callback with the request from calling requestPermissions(...)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        switch (requestCode) {
            case REQUEST_ALL_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[3] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[4] == PackageManager.PERMISSION_GRANTED) {
                    if (!MainApplication.isNavigineInitialized) {
                        (new InitTask(this)).execute();
                    }
                }
                return;
            }

            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    class InitTask extends AsyncTask<Void, Void, Boolean> {
        private Context mContext = null;
        private String mErrorMsg = null;

        public InitTask(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (!MainApplication.initialize(getApplicationContext())) {
                mErrorMsg = "Error downloading location information! Please, try again later or contact technical support";
                return Boolean.FALSE;
            }
            Log.d("AAAAAA", "Initialized!");
            if (!NavigineSDK.loadLocation(MainApplication.LOCATION_ID, 30)) {
                mErrorMsg = "Error downloading location information! Please, try again later or contact technical support";
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result.booleanValue()) {
                // Starting main activity

                doCalculation();
            } else {
                Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }
    }

    private void setUpZoneData() {
        Db_helper db_helper = new Db_helper(getApplicationContext());

        //For FNSPL Zone
        db_helper.insert_zone(new ZoneInfo(1, "824,1067","21,23.5","25,23.5","25,17.8","21,17.8"));
        db_helper.insert_zone(new ZoneInfo(2, "824,1067","21,23.5","25,23.5","25,17.8","21,17.8"));
        db_helper.insert_zone(new ZoneInfo(3, "824,1067","21,23.5","25,23.5","25,17.8","21,17.8"));

    }

}
