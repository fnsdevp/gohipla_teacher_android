package com.fnspl.hiplaedu_teacher.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.databinding.ActivityNotificationHandleBinding;
import com.fnspl.hiplaedu_teacher.fragment.ClassEndFragment;
import com.fnspl.hiplaedu_teacher.fragment.ClassStartFragment;
import com.fnspl.hiplaedu_teacher.fragment.ManualAttendanceFragment;
import com.fnspl.hiplaedu_teacher.fragment.RoutineFragment;
import com.fnspl.hiplaedu_teacher.fragment.StudentsOutOfClassFragment;
import com.fnspl.hiplaedu_teacher.beaconsDetection.Constants;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.navigine.naviginesdk.NavigineSDK;

import io.paperdb.Paper;


public class NotificationHandleActivity extends BaseActivity {

    public static final String SNEAK_OUT = "sneakOut";
    private ActivityNotificationHandleBinding binding_welcome_class;
    private PowerManager.WakeLock wakeLock;
    public static final String CLASS_START = "ClassStart";
    public static final String CLASS_END = "ClassEnd";
    public static final String MANUAL_ATTENDANCE = "manualAttendanceFragment";
    public static final String ROUTINE = "routineFragment";
    public static final String FINISH = "finish";
    private ClassFinishBroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Constants.activity = NotificationHandleActivity.this;

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        binding_welcome_class = DataBindingUtil.setContentView(NotificationHandleActivity.this, R.layout.activity_notification_handle);
        binding_welcome_class.setWelcomeClass(NotificationHandleActivity.this);

        binding_welcome_class.imgDrawerToggel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotificationHandleActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        if (getIntent() != null && getIntent().getStringExtra(CONST.NOTIFICATION_TYPE) != null) {
            switch (getIntent().getStringExtra(CONST.NOTIFICATION_TYPE)) {

                case CLASS_START:
                    Paper.book().write(CONST.CLASS_STARTED, true);
                    Paper.book().delete(CONST.CLASS_ENDED);

                    setFragment(new ClassStartFragment(), CLASS_START);
                    break;

                case CLASS_END:
                    Paper.book().write(CONST.CLASS_ENDED, true);
                    Paper.book().delete(CONST.CLASS_STARTED);

                    setFragment(new ClassEndFragment(), CLASS_END);
                    break;

                case MANUAL_ATTENDANCE:

                    setFragment(new ManualAttendanceFragment(), MANUAL_ATTENDANCE);
                    break;

                case ROUTINE:

                    setFragment(new ManualAttendanceFragment(), ROUTINE);
                    break;

                case SNEAK_OUT:

                    setFragment(new StudentsOutOfClassFragment(), SNEAK_OUT);
                    break;
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE, "wakeLock");
            wakeLock.acquire();
            wakeLock.release();

            MainApplication.isNotificationScreen=true;

            IntentFilter intentFilter = new IntentFilter(
                    "android.intent.action.FINSIHACTIVITY");

            mReceiver = new ClassFinishBroadcastReceiver();
            registerReceiver(mReceiver, intentFilter);

            if(!MainApplication.isNavigineInitialized) {
                new InitTask(this).execute();
            }

        } catch (Throwable th) {
            // ignoring this exception, probably wakeLock was already released
        }

    }

    public void setFragment(Fragment fragment, String fragmentName) {

        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE, "wakeLock");
            wakeLock.acquire();
            wakeLock.release();
        } catch (Throwable th) {
            // ignoring this exception, probably wakeLock was already released
        }

        switch (getIntent().getStringExtra(CONST.NOTIFICATION_TYPE)) {

            case CLASS_START:
                Paper.book().write(CONST.CLASS_STARTED, true);
                Paper.book().delete(CONST.CLASS_ENDED);

                break;

            case CLASS_END:
                Paper.book().write(CONST.CLASS_ENDED, true);
                Paper.book().delete(CONST.CLASS_STARTED);

                break;
        }

        android.support.v4.app.FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        /*Fragment oldfragment = getSupportFragmentManager().findFragmentByTag(fragmentName);
        if(oldfragment==null) {
            t.replace(R.id.fragment_container, fragment, fragmentName);
            t.addToBackStack(null);
        }else{*/
        t.replace(R.id.fragment_container, fragment, fragmentName);
        //}
        t.commit();
    }

    @Override
    public void onBackPressed() {

        Intent intent = new Intent(NotificationHandleActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }

    public class ClassFinishBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getStringExtra(CONST.NOTIFICATION_TYPE)) {

                case CLASS_START:
                    Paper.book().write(CONST.CLASS_STARTED, true);
                    Paper.book().delete(CONST.CLASS_ENDED);

                    setFragment(new ClassStartFragment(), CLASS_START);
                    break;

                case CLASS_END:
                    Paper.book().write(CONST.CLASS_ENDED, true);
                    Paper.book().delete(CONST.CLASS_STARTED);

                    setFragment(new ClassEndFragment(), CLASS_END);
                    break;

                case MANUAL_ATTENDANCE:

                    setFragment(new ManualAttendanceFragment(), MANUAL_ATTENDANCE);
                    break;

                case ROUTINE:

                    setFragment(new RoutineFragment(), ROUTINE);
                    break;

                case SNEAK_OUT:

                    setFragment(new StudentsOutOfClassFragment(), SNEAK_OUT);
                    break;

                case FINISH:
                    MainApplication.isNotificationScreen=false;

                    Intent nextPage = new Intent(NotificationHandleActivity.this, DashboardActivity.class);
                    nextPage.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(nextPage);
                    finish();
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mReceiver!=null){
            unregisterReceiver(mReceiver);
        }

        MainApplication.isNotificationScreen=false;

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

            } else {
                Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }
    }

}
