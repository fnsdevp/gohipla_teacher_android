package com.fnspl.hiplaedu_teacher.firebase;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.activity.LoginActivity;
import com.fnspl.hiplaedu_teacher.activity.NotificationHandleActivity;
import com.fnspl.hiplaedu_teacher.activity.SplashActivity;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.service.RoutineFetchService;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.paperdb.Paper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private String TAG = "Firebase";
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage != null && remoteMessage.getData() != null) {

            Log.d(TAG, "From: " + remoteMessage.getFrom());
            //Log.d(TAG, "Notification TripMessageData Body: " + remoteMessage.getNotification().getBody());
            Log.d(TAG, "Notification TripMessageData Data: " + remoteMessage.getData().toString());
            //DBG.d(TAG, "TripMessageData Notification Body: " + remoteMessage.getNotification().getBody());
            Map<String, String> data = remoteMessage.getData();

            ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            // Get info from the currently active task
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            String activityName = taskInfo.get(0).topActivity.getClassName();

            String action = "", studentName = "Avishek Mishra", actionType = "";

            try {
                //JSONObject jsonObject = new JSONObject(body);

                if (data.containsKey("pushType") && data.get("pushType").equalsIgnoreCase("start_attendance")) {

                    try {


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (data.containsKey("pushType") && data.get("pushType").equalsIgnoreCase("stop_attendance")) {

                    try {


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (data.containsKey("pushType") && data.get("pushType").equalsIgnoreCase("Request_attendance")) {

                    try {

                        if (!MainApplication.isNotificationScreen)
                            launchWelcomeClass(NotificationHandleActivity.MANUAL_ATTENDANCE);
                        else
                            finishActivity(NotificationHandleActivity.MANUAL_ATTENDANCE);

                        sendNotification(data.get("studentname") + " has requested for manual attendance");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (data.containsKey("action") && data.get("action").equalsIgnoreCase("student_sneakout")) {

                    try {

                        if (!MainApplication.isNotificationScreen)
                            launchWelcomeClass(NotificationHandleActivity.SNEAK_OUT);
                        else
                            finishActivity(NotificationHandleActivity.SNEAK_OUT);

                        sendNotificationSneakOut(data.get("student_name") + " is going out of class");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (data.containsKey("action") && data.get("action").equalsIgnoreCase("routine_updated")) {

                    //startService(new Intent(getApplicationContext(), RoutineFetchService.class));
                    setRoutineFetchService(getApplicationContext());

                } else if (data.containsKey("action") && data.get("action").equalsIgnoreCase("user_modification")) {

                    Paper.book().write(CONST.UPDATE_PROFILE, true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private void launchWelcomeClass(String notificationtype) {
        //sendBroadcast(new Intent(getResources().getString(R.string.class_start)));
        Intent notificationHandle = new Intent(MyFirebaseMessagingService.this, NotificationHandleActivity.class);
        notificationHandle.putExtra(CONST.NOTIFICATION_TYPE, notificationtype);
        notificationHandle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(notificationHandle);
    }

    private void finishActivity(String notificationtype) {
        Intent zoneInfo = new Intent("android.intent.action.FINSIHACTIVITY");
        zoneInfo.putExtra(CONST.NOTIFICATION_TYPE, notificationtype);
        sendBroadcast(zoneInfo);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();

    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
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
        Intent resultIntent = new Intent(this, NotificationHandleActivity.class);
        resultIntent.putExtra(CONST.NOTIFICATION_TYPE, NotificationHandleActivity.MANUAL_ATTENDANCE);

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

    private void sendNotificationSneakOut(String message) {
        Intent intent = new Intent(this, NotificationHandleActivity.class);
        intent.putExtra(CONST.NOTIFICATION_TYPE, NotificationHandleActivity.SNEAK_OUT);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(LoginActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent dismissIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(getNotificationIcon())
                        //.addAction(R.drawable.ic_close, "Mark Absent", dismissIntent)
                        .setColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary))
                        .setVibrate(new long[]{1000, 1000, 1000})
                        .setLights(Color.RED, 3000, 3000)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText("" + message)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message));
        // Creates an explicit intent for an Activity in your app
        mBuilder.setContentIntent(dismissIntent);
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

    private boolean setBluetoothEnable(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = false;

        if (bluetoothAdapter != null) {
            isEnabled = bluetoothAdapter.isEnabled();
        } else {
            return false;
        }

        if (enable && !isEnabled) {
            return bluetoothAdapter.enable();
        } else if (!enable && isEnabled) {
            return bluetoothAdapter.disable();
        }
        // No need to change bluetooth state
        enableLocation();
        return true;
    }

    private void enableLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsProviderEnabled, isNetworkProviderEnabled;
            isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsProviderEnabled && !isNetworkProviderEnabled) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }

        }

    }

    public void setRoutineFetchService(Context mContext) {
        if (CONST.checkVersion()) {
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), CONST.ROUTINE_FETCH_ID,
                    new Intent().setAction("FETCH_DAILY_ROUTINE_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT);

            // reset previous pending intent
            alarmManager.cancel(pendingIntent);

            alarmManager.set(AlarmManager.RTC_WAKEUP, 1000, pendingIntent);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CONST.scheduleRoutineFetchJob(mContext, 1000, CONST.ROUTINE_FETCH_ID);
            }
        }
    }

}
