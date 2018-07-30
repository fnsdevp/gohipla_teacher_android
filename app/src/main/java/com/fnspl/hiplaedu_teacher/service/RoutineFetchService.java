package com.fnspl.hiplaedu_teacher.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.activity.DashboardActivity;
import com.fnspl.hiplaedu_teacher.activity.LoginActivity;
import com.fnspl.hiplaedu_teacher.activity.NotificationHandleActivity;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.model.ProfileInfo;
import com.fnspl.hiplaedu_teacher.model.RoutinePeriod;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.fnspl.hiplaedu_teacher.utils.NetworkUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.paperdb.Paper;

public class RoutineFetchService extends Service {

    private TimerTask mTimerTask = null;
    private Timer mTimer = new Timer();
    private Handler mHandler = new Handler();
    private static final int UPDATE_TIMEOUT = 1*60*1000;
    private String macAddress="";
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
    private int i=0;

    public RoutineFetchService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //
        Log.d("Tester", "Service started");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForeground(1002, new Notification());

        i=0;
        if(Paper.book().read(CONST.PROFILE_INFO)!=null){
            if(isOnline()) {
                fetchRoutine();
            }else{
                setRoutineFetchService(getApplicationContext(), 180000);
            }
        }

        //sendNotification("Routine Updated Successfully");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //
        Log.d("Tester", "Service destroyed");
    }

    private void fetchRoutine(){
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        ProfileInfo profileInfo = Paper.book().read(CONST.PROFILE_INFO);

        String urlParameters = "userid="+profileInfo.getId()+
                "&usertype=teacher"+
                "&date="+date+
                "&device_type=Android";

        new RoutineFetch().execute(urlParameters);
    }

    private class RoutineFetch extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            HttpURLConnection urlConnection = null;
            try {
                Log.d("Tester", "Before request");
                URL url = new URL(NetworkUtility.BASEURL+ NetworkUtility.ROUTINE_FETCH);
                String urlParameters = params[0];

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("USER-AGENT", "Mozilla/5.0");
                connection.setRequestProperty("ACCEPT-LANGUAGE", "en-US,en;0.5");
                connection.setDoOutput(true);

                DataOutputStream dStream = new DataOutputStream(connection.getOutputStream());
                dStream.writeBytes(urlParameters);
                dStream.flush();
                dStream.close();
                int responseCode = connection.getResponseCode();

                final StringBuilder output = new StringBuilder("Request URL " + url);
                output.append(System.getProperty("line.separator") + "Request Parameters " + urlParameters);
                output.append(System.getProperty("line.separator") + "Response Code " + responseCode);
                output.append(System.getProperty("line.separator") + "Type " + "POST");
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = "";
                StringBuilder responseOutput = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    responseOutput.append(line);
                }
                br.close();

                result = responseOutput.toString();

            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                return result;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //macAddress = getMacAddr();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            List<RoutinePeriod> routinePeriodsList = new ArrayList<>();

            setRoutineFetchService(getApplicationContext());

            try {
                JSONObject response = new JSONObject(s);

                if(response.getString("status").equalsIgnoreCase("success")){
                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();
                    Gson gson = builder.create();

                    JSONObject user_routine = response.getJSONArray("user_routine").getJSONObject(0);
                    JSONArray routineArray = user_routine.getJSONArray("routine");

                    for (int i = 0; i < routineArray.length(); i++) {
                        RoutinePeriod routinePeriod = gson.fromJson(routineArray.getJSONObject(i).toString() ,
                                RoutinePeriod.class) ;

                        routinePeriodsList.add(routinePeriod);
                    }

                    Db_helper db_helper = new Db_helper(getApplicationContext());
                    db_helper.deleteRoutine();

                    if (routinePeriodsList.size() > 0)
                        Paper.book().write(CONST.LOGIN_FOR_FIRST_TIME, false);
                    else
                        Paper.book().delete(CONST.LOGIN_FOR_FIRST_TIME);

                    db_helper.insertAllRoutines(routinePeriodsList);
                    db_helper.getWritableDatabase().close();

                    if(!Paper.book().read(CONST.CLASS_STARTED,false) && !Paper.book().read(CONST.CLASS_ENDED,false)
                            && !Paper.book().read(CONST.CLASS_RUNNING, false)) {

                        setUpNextClassTimer();
                    }

                    stopSelf();

                }else{
                    if(i<4) {
                        i++;
                        fetchRoutine();
                    }else{
                        stopSelf();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
                //Toast.makeText(getApplicationContext(), "JSON Exception", Toast.LENGTH_SHORT).show();
                if(i<4) {
                    i++;
                    fetchRoutine();
                }else{
                    stopSelf();
                }
            }
        }
    }

    private void setUpNextClassTimer() {
        try {
            Date currentDateTime = new Date();

            Db_helper db_helper = new Db_helper(getApplicationContext());

            List<RoutinePeriod> routinePeriodList = db_helper.getRoutine(new SimpleDateFormat("EEEE").
                    format(currentDateTime).toLowerCase());

            for (int i=0; i<routinePeriodList.size(); i++) {

                Date classDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime)+" "+routinePeriodList.get(i).getStartTime());
                Calendar cal = Calendar.getInstance();
                cal.setTime(classDateTime);
                cal.set(Calendar.MINUTE, -2);

                if(currentDateTime.compareTo(classDateTime)>0){
                    if(i==(routinePeriodList.size()-1)){
                        Paper.book().delete(CONST.LOGIN_FOR_FIRST_TIME);
                        //Paper.book().delete(CONST.CURRENT_PERIOD);
                    }
                }else{
                    Paper.book().write(CONST.CURRENT_PERIOD, routinePeriodList.get(i).getRoutine_history_id());

                    setClassStartService(getApplicationContext(), classDateTime);

                    break;
                }

            }

        }catch (Exception ex){

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
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CONST.scheduleClassStartJob(mContext, calendar.getTimeInMillis()-System.currentTimeMillis(),
                        CONST.CLASS_START_ID);
            }
        }
    }

    public void setRoutineFetchService(Context mContext) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (CONST.checkVersion()) {
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), CONST.ROUTINE_FETCH_ID,
                    new Intent().setAction("FETCH_DAILY_ROUTINE_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT);

            // reset previous pending intent
            alarmManager.cancel(pendingIntent);

            // Set the alarm to start at approximately 08:00 morning.
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CONST.scheduleRoutineFetchJob(mContext, calendar.getTimeInMillis()-System.currentTimeMillis(), CONST.ROUTINE_FETCH_ID);
            }
        }
    }

    public void setRoutineFetchService(Context mContext, long time) {
        if (CONST.checkVersion()) {
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), CONST.ROUTINE_FETCH_ID,
                    new Intent().setAction("FETCH_DAILY_ROUTINE_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT);

            // reset previous pending intent
            alarmManager.cancel(pendingIntent);

            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CONST.scheduleRoutineFetchJob(mContext, time, CONST.ROUTINE_FETCH_ID);
            }
        }
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    private int getNotificationIcon() {
        boolean useWhiteIcon = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        return useWhiteIcon ? R.mipmap.ic_launcher : R.mipmap.ic_launcher;
    }

    private void sendNotification(String message) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(getNotificationIcon())
                        .setColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary))
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

}
