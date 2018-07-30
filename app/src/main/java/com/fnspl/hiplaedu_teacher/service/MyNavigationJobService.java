package com.fnspl.hiplaedu_teacher.service;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.activity.DashboardActivity;
import com.fnspl.hiplaedu_teacher.activity.LoginActivity;
import com.fnspl.hiplaedu_teacher.activity.NotificationHandleActivity;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.model.ProfileInfo;
import com.fnspl.hiplaedu_teacher.model.RoutinePeriod;
import com.fnspl.hiplaedu_teacher.model.ZoneInfo;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.navigine.naviginesdk.DeviceInfo;
import com.navigine.naviginesdk.Location;
import com.navigine.naviginesdk.NavigationThread;
import com.navigine.naviginesdk.NavigineSDK;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import io.paperdb.Paper;

/**
 * Created by FNSPL on 3/20/2018.
 */

public class MyNavigationJobService extends JobService {

    private static final String TAG = "NAVIGINE.Demo";
    public static final String ERROR = "error";
    public static final String DEVICE_LOCATION = "deviceLocation";
    private static final int UPDATE_TIMEOUT = 3000;  // milliseconds
    private static final int ADJUST_TIMEOUT = 5000; // milliseconds
    private static final int ERROR_MESSAGE_TIMEOUT = 5000; // milliseconds
    private static final boolean ORIENTATION_ENABLED = true; // Show device orientation?

    private TimerTask mTimerTask = null;
    private Timer mTimer = new Timer();
    private Handler mHandler = new Handler();
    private boolean mAdjustMode = false;
    private long mErrorMessageTime = 0;
    private Location mLocation = null;
    private DeviceInfo mDeviceInfo = null; // Current device
    private Intent locatonFetch;
    private Intent errorMessage;
    private Db_helper db_helper;
    private RoutinePeriod routinePeriod;
    private ArrayList<ZoneInfo> zoneInfos = new ArrayList<>();
    private ArrayList<PointF[]> zoneInfoPoint = new ArrayList<>();
    private int currentZone = 0;
    private ProfileInfo profile;
    private boolean isNotified = true;
    private boolean isMarkedAbsent = false;
    private boolean isClassLaunched = false;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
    private JobParameters parameters;

    @Override
    public boolean onStartJob(JobParameters params) {

        this.parameters = params;

        isClassLaunched = false;

        Paper.book().delete(DEVICE_LOCATION);

        if (MainApplication.isNavigineInitialized) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(mRunnable);
                }
            };
            mTimer.schedule(mTimerTask, UPDATE_TIMEOUT, UPDATE_TIMEOUT);
        } else {
            new InitTask(getApplicationContext()).execute();
        }

        db_helper = new Db_helper(getApplicationContext());
        if (db_helper != null) {
            //zoneInfos = db_helper.getAllZoneInfo();
            routinePeriod = db_helper.getRoutine(Paper.book().read(CONST.CURRENT_PERIOD, 0));
            if (routinePeriod != null) {
                ZoneInfo zoneInfo1 = db_helper.getZoneInfo("" + routinePeriod.getRoom_id());
                zoneInfos.add(zoneInfo1);
            } else {
                zoneInfos = db_helper.getAllZoneInfo();
            }

            profile = Paper.book().read(CONST.PROFILE_INFO);

            for (ZoneInfo zoneInfo :
                    zoneInfos) {
                zoneInfoPoint.add(convertToPoints(zoneInfo));
            }
        }

        setUpNextClassTimer();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {

        if (mTimerTask != null && mTimer != null) {
            mTimerTask.cancel();
            mTimer.cancel();
            mTimerTask = null;
            mTimer = null;
        }

        return false;
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
                mErrorMsg = "Error downloading location 'Navigine Demo'! Please, try again later or contact technical support";
                return Boolean.FALSE;
            }
            Log.d(TAG, "Initialized!");
            if (!NavigineSDK.loadLocation(MainApplication.LOCATION_ID, 30)) {
                mErrorMsg = "Error downloading location 'Navigine Demo'! Please, try again later or contact technical support";
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result.booleanValue()) {
                // Starting main activity

                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.post(mRunnable);
                    }
                };
                mTimer.schedule(mTimerTask, UPDATE_TIMEOUT, UPDATE_TIMEOUT);

            } else {
                Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }
    }

    final Runnable mRunnable =
            new Runnable() {
                public void run() {
                    navigationCalculation();
                }
            };

    private void navigationCalculation() {
        if (MainApplication.Navigation == null) {
            Log.d(TAG, "Sorry, navigation is not supported on your device!");
            return;
        }

        final long timeNow = NavigineSDK.currentTimeMillis();

        if (mErrorMessageTime > 0 && timeNow > mErrorMessageTime + ERROR_MESSAGE_TIMEOUT) {
            mErrorMessageTime = 0;
        }

        // Start navigation if necessary
        if (MainApplication.Navigation.getMode() == NavigationThread.MODE_IDLE)
            MainApplication.Navigation.setMode(NavigationThread.MODE_NORMAL);

        // Get device info from NavigationThread
        mDeviceInfo = MainApplication.Navigation.getDeviceInfo();
        //Paper.book().write(DEVICE_LOCATION, mDeviceInfo);
        Log.e("time", Long.toString(System.currentTimeMillis()));

        if (mDeviceInfo.errorCode == 0) {
            mErrorMessageTime = 0;

            if (!MainApplication.isClassStarted && routinePeriod != null) {
                calculateZone(mDeviceInfo);
            } else {
                mTimer.cancel();

                if (mTimerTask != null)
                    mTimerTask.cancel();

                jobFinished(parameters, false);
            }

        }
    }

    private void calculateZone(DeviceInfo mDeviceInfo) {
        Log.d(TAG, "X : " + mDeviceInfo.x + " Y: " + mDeviceInfo.y);
        for (int index = 0; index < zoneInfoPoint.size(); index++) {
            boolean inZone = contains(zoneInfoPoint.get(index), new PointF(mDeviceInfo.x, mDeviceInfo.y));
            if (inZone && zoneInfos.get(index).getId() == routinePeriod.getRoom_id()) {
                //currentZone = zoneInfos.get(index).getId();

                if (mTimer != null && mTimerTask != null) {
                    mTimer.cancel();
                    mTimerTask.cancel();
                }

                jobFinished(parameters, false);

                if (!MainApplication.isNotificationScreen && !MainApplication.isClassStarted) {
                    launchWelcomeClass(NotificationHandleActivity.CLASS_START);
                } else if (MainApplication.isNotificationScreen && !MainApplication.isClassStarted) {
                    finishActivity(NotificationHandleActivity.CLASS_START);
                }

                isNotified = true;

            } else if (inZone && zoneInfos.get(index).getId() != routinePeriod.getRoom_id()) {
                //currentZone = zoneInfos.get(index).getId();

                isNotified = false;
            } else {
                //currentZone = zoneInfos.get(index).getId();

                isNotified = false;
            }

        }
    }

    private PointF[] convertToPoints(ZoneInfo zoneInfo) {
        PointF[] pointFs = new PointF[4];

        String[] pointsA = zoneInfo.getPointA().split(",");
        PointF pointA = new PointF(Float.parseFloat(pointsA[0]), Float.parseFloat(pointsA[1]));
        pointFs[0] = pointA;

        String[] pointsB = zoneInfo.getPointB().split(",");
        PointF pointB = new PointF(Float.parseFloat(pointsB[0]), Float.parseFloat(pointsB[1]));
        pointFs[1] = pointB;

        String[] pointsC = zoneInfo.getPointC().split(",");
        PointF pointC = new PointF(Float.parseFloat(pointsC[0]), Float.parseFloat(pointsC[1]));
        pointFs[2] = pointC;

        String[] pointsD = zoneInfo.getPointD().split(",");
        PointF pointD = new PointF(Float.parseFloat(pointsD[0]), Float.parseFloat(pointsD[1]));
        pointFs[3] = pointD;

        return pointFs;
    }

    public boolean contains(PointF[] points, PointF test) {
        int i;
        int j;
        boolean result = false;
        /*for (i = 0, j = points.length - 1; i < points.length; j = i++) {
            if ((points[i].y > test.y) != (points[j].y > test.y) &&
                    (test.x < (points[j].x - points[i].x) * (test.y - points[i].y) / (points[j].y - points[i].y) + points[i].x)) {
                result = !result;
            }
        }*/
        if (((points[0].x < test.x) && (points[0].y > test.y))) {
            if (((points[3].x < test.x) && (points[3].y < test.y))) {
                if (((points[1].x > test.x) && (points[1].y > test.y))) {
                    if (((points[2].x > test.x) && (points[2].y < test.y))) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    private void sendNotification(String message, int zoneId) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(getNotificationIcon())
                        .setColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary))
                        .setVibrate(new long[]{1000, 1000, 1000})
                        .setLights(Color.RED, 3000, 3000)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText("" + message)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message));
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, DashboardActivity.class);
        resultIntent.putExtra(CONST.ZONE_ID, zoneId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(LoginActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }

    private void sendWelcomeNotification(String message) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(getNotificationIcon())
                        .setColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary))
                        .setVibrate(new long[]{1000, 1000, 1000})
                        .setLights(Color.RED, 3000, 3000)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText("" + message)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message));
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, LoginActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(LoginActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }

    private int getNotificationIcon() {
        boolean useWhiteIcon = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        return useWhiteIcon ? R.mipmap.ic_launcher : R.mipmap.ic_launcher;
    }

    private void finishActivity(String notificationtype) {
        Intent zoneInfo = new Intent("android.intent.action.FINSIHACTIVITY");
        zoneInfo.putExtra(CONST.NOTIFICATION_TYPE, notificationtype);
        sendBroadcast(zoneInfo);
        Paper.book().write(CONST.CLASS_STARTED, true);

        isClassLaunched = true;
    }

    private void launchWelcomeClass(String notificationtype) {
        //sendBroadcast(new Intent(getResources().getString(R.string.class_start)));
        Intent notificationHandle = new Intent(MyNavigationJobService.this, NotificationHandleActivity.class);
        notificationHandle.putExtra(CONST.NOTIFICATION_TYPE, notificationtype);
        notificationHandle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(notificationHandle);
        Paper.book().write(CONST.CLASS_STARTED, true);

        isClassLaunched = true;
    }

    private void setUpNextClassTimer() {
        try {

            Date currentDateTime = new Date();

            Db_helper db_helper = new Db_helper(getApplicationContext());

            RoutinePeriod routinePeriod = db_helper.getRoutine((int) Paper.book().read(CONST.CURRENT_PERIOD));

            Date classDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routinePeriod.getEndTime());

            setClassEndService(getApplicationContext(), classDateTime);

        } catch (Exception ex) {

        }
    }

    public void setClassEndService(Context mContext, Date periodDate) {
        // Set the alarm to start at approximately 08:00 morning.
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(periodDate);

        if (CONST.checkVersion()) {
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), CONST.CLASS_END_ID,
                    new Intent().setAction("END_CLASS_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT);
            // reset previous pending intent
            alarmManager.cancel(pendingIntent);
            // if the scheduler date is passed, move scheduler time to tomorrow
            if (System.currentTimeMillis() > calendar.getTimeInMillis()) {
                //calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CONST.scheduleClassEndJob(mContext, periodDate, CONST.CLASS_END_ID);
            }
        }

    }

}
