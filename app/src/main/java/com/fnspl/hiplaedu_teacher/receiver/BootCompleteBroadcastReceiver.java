package com.fnspl.hiplaedu_teacher.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.model.RoutinePeriod;
import com.fnspl.hiplaedu_teacher.service.RoutineFetchService;
import com.fnspl.hiplaedu_teacher.utils.CONST;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.paperdb.Paper;

/**
 * Created by FNSPL on 10/6/2017.
 */

public class BootCompleteBroadcastReceiver extends BroadcastReceiver {

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
    
    @Override
    public void onReceive(Context context, Intent intent) {
        //Toast.makeText(context, "Boot Complete", Toast.LENGTH_LONG).show();

        if(Paper.book().read(CONST.LOGIN_FOR_FIRST_TIME)==null){
            context.startService(new Intent(context, RoutineFetchService.class));
        }else if(Paper.book().read(CONST.CLASS_RUNNING, false) && Paper.book().read(CONST.CURRENT_PERIOD)!=null){
            setUpClassEndTimer(context);
        }else{
            setUpNextClassTimer(context);
        }

        if(Paper.book().read(CONST.LOGIN_FOR_FIRST_TIME)!=null){
            setRoutineFetchService(context);
        }
    }

    private void setUpNextClassTimer(Context context) {
        try {
            Date currentDateTime = new Date();

            Db_helper db_helper = new Db_helper(context);

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
                    }
                }else{
                    Paper.book().write(CONST.CURRENT_PERIOD, routinePeriodList.get(i).getRoutine_history_id());

                    setClassStartService(context, classDateTime);

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
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, CONST.CLASS_START_ID,
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
                CONST.scheduleClassStartJob(mContext, calendar.getTimeInMillis()-System.currentTimeMillis(), CONST.CLASS_START_ID);
            }
        }
    }

    public void setClassEndService(Context mContext, Date periodDate) {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, CONST.CLASS_START_ID,
                new Intent().setAction("END_CLASS_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT);

        // reset previous pending intent
        alarmManager.cancel(pendingIntent);

        // Set the alarm to start at approximately 08:00 morning.
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(periodDate);

        // if the scheduler date is passed, move scheduler time to tomorrow
        if (System.currentTimeMillis() > calendar.getTimeInMillis()) {
            //calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),pendingIntent);
    }


    public void setUpClassEndTimer(Context mContext) {
        try {
            Date currentDateTime = new Date();

            Db_helper db_helper = new Db_helper(mContext);
            RoutinePeriod routine = db_helper.getRoutine((int)Paper.book().read(CONST.CURRENT_PERIOD));
            if(routine!=null) {
                Date classDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routine.getEndTime());

                setClassEndService(mContext, classDateTime);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setRoutineFetchService(Context mContext) {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, CONST.ROUTINE_FETCH_ID,
                new Intent().setAction("FETCH_DAILY_ROUTINE_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT);

        // reset previous pending intent
        alarmManager.cancel(pendingIntent);

        // Set the alarm to start at approximately 08:00 morning.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.DATE, 1);

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

}
