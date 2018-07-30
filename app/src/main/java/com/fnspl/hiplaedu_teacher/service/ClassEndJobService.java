package com.fnspl.hiplaedu_teacher.service;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.fnspl.hiplaedu_teacher.activity.NotificationHandleActivity;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.model.RoutinePeriod;
import com.fnspl.hiplaedu_teacher.utils.CONST;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.paperdb.Paper;

/**
 * Created by FNSPL on 3/21/2018.
 */

public class ClassEndJobService extends JobService {

    private TimerTask mTimerTask = null;
    private Timer mTimer = new Timer();
    private Handler mHandler = new Handler();
    private static final int UPDATE_TIMEOUT = 1 * 60 * 1000;
    private String macAddress = "";
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.d("Tester", "Service started");

        if (Paper.book().read(CONST.CLASS_RUNNING, false)) {

            Paper.book().write(CONST.CLASS_ENDED, true);
            Paper.book().delete(CONST.CLASS_START);

            if (!MainApplication.isNotificationScreen)
                launchWelcomeClass(NotificationHandleActivity.CLASS_END);
            else
                finishActivity(NotificationHandleActivity.CLASS_END);

        } else {
            MainApplication.isClassStarted = false;

            Paper.book().delete(CONST.CLASS_ENDED);
            Paper.book().delete(CONST.CLASS_STARTED);
            Paper.book().delete(CONST.CLASS_RUNNING);

            /*if (isMyServiceRunning(getApplicationContext(), MyService.class))
                stopService(new Intent(this, MyService.class));*/

            //Using Navigine
            if (CONST.checkVersion()) {
                if (isMyServiceRunning(getApplicationContext(), MyNavigationService.class))
                    stopService(new Intent(this, MyNavigationService.class));
            } else {
                //if (isMyServiceRunning(getApplicationContext(), MyNavigationJobService.class))
                stopService(new Intent(this, MyNavigationJobService.class));
            }

            setUpNextClassTimer();

            if (MainApplication.isNotificationScreen)
                finishActivity(NotificationHandleActivity.FINISH);

        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d("Tester", "Service destroyed");

        return false;
    }

    private void launchWelcomeClass(String notificationtype) {
        Paper.book().delete(CONST.CLASS_STARTED);
        //sendBroadcast(new Intent(getResources().getString(R.string.class_start)));
        Intent notificationHandle = new Intent(ClassEndJobService.this, NotificationHandleActivity.class);
        notificationHandle.putExtra(CONST.NOTIFICATION_TYPE, notificationtype);
        notificationHandle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(notificationHandle);

        stopSelf();
    }

    private void finishActivity(String notificationtype) {
        Intent zoneInfo = new Intent("android.intent.action.FINSIHACTIVITY");
        zoneInfo.putExtra(CONST.NOTIFICATION_TYPE, notificationtype);
        sendBroadcast(zoneInfo);
    }

    private void setUpNextClassTimer() {
        try {
            Date currentDateTime = new Date();

            Db_helper db_helper = new Db_helper(getApplicationContext());

            List<RoutinePeriod> routinePeriodList = db_helper.getRoutine(new SimpleDateFormat("EEEE").
                    format(currentDateTime).toLowerCase());

            for (int i = 0; i < routinePeriodList.size(); i++) {

                Date classDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routinePeriodList.get(i).getStartTime());
                Calendar cal = Calendar.getInstance();
                cal.setTime(classDateTime);
                cal.set(Calendar.MINUTE, -2);

                if (Paper.book().read(CONST.CURRENT_PERIOD, 0) >= routinePeriodList.get(i).getRoutine_history_id()) {
                    if (i == (routinePeriodList.size() - 1)) {
                        Paper.book().delete(CONST.LOGIN_FOR_FIRST_TIME);
                        Paper.book().delete(CONST.CURRENT_PERIOD);
                    }
                } else {
                    Paper.book().write(CONST.CURRENT_PERIOD, routinePeriodList.get(i).getRoutine_history_id());

                    setClassStartService(getApplicationContext(), classDateTime);

                    break;
                }

            }


        } catch (Exception ex) {

        }

    }

    public void setClassStartService(Context mContext, Date periodDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(periodDate);

        if (CONST.checkVersion()) {

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), CONST.CLASS_START_ID,
                    new Intent().setAction("START_WIFI_ZONE_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT);
            // reset previous pending intent
            alarmManager.cancel(pendingIntent);

            // if the scheduler date is passed, move scheduler time to tomorrow
            if (System.currentTimeMillis() > calendar.getTimeInMillis()) {
                //calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CONST.scheduleClassStartJob(mContext, calendar.getTimeInMillis() - System.currentTimeMillis(), CONST.CLASS_START_ID);
            }
        }
    }

    private boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
