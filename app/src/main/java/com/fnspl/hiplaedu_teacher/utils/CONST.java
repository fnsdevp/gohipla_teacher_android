package com.fnspl.hiplaedu_teacher.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.service.ClassEndJobService;
import com.fnspl.hiplaedu_teacher.service.ClassEndService;
import com.fnspl.hiplaedu_teacher.service.MyNavigationJobService;
import com.fnspl.hiplaedu_teacher.service.RoutineFetchJobService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by FNSPL on 9/8/2017.
 */

public class CONST {
    public static final String TOKEN = "token";
    public static final String SESSION_ID = "session_id";
    public static final String PROFILE_INFO = "profileInfo";
    public static final String CLASS_START = "classStart";
    public static final String LOGIN_FOR_FIRST_TIME = "loginForFirstTime";
    public static final String CURRENT_PERIOD = "currentPeriod";
    public static final String CLASS_STARTED = "classStarted";
    public static final String CLASS_ENDED = "classEnded";
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String CLASS_RUNNING = "classRunning";
    public static final String UPDATE_PROFILE = "updateProfile";
    public static final String NEXT_PERIOD = "nextClass" ;
    public static final String ZONE_ID = "zoneId";
    public static final String POINTY = "pointY";
    public static final String POINTX = "pointX";
    public static final int ATTENDANCE_ROOM_ID = 2;
    public static String NOTIFICATION_TYPE = "notificationType";
    public static final String IS_IN_CLASS = "isInClass";
    public static final int ROUTINE_FETCH_ID = 100;
    public static final int CLASS_START_ID = 101;
    public static final int CLASS_END_ID = 102;

    public static DisplayImageOptions ErrorWithLoaderRoundedCorner = new DisplayImageOptions.Builder()
            .showImageOnFail(R.drawable.no_profile_image)
            .showImageOnLoading(R.drawable.no_profile_image)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .imageScaleType(ImageScaleType.EXACTLY)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .displayer(new RoundedBitmapDisplayer(1000))
            .postProcessor(new BitmapProcessor() {
                @Override
                public Bitmap process(Bitmap bmp) {

                    int dimension = getSquareCropDimensionForBitmap(bmp);
                    bmp = ThumbnailUtils.extractThumbnail(bmp, dimension, dimension);
                    return bmp;
                }
            })
            .build();

    public static DisplayImageOptions ErrorWithLoaderNormalCorner = new DisplayImageOptions.Builder()
            .resetViewBeforeLoading(true)
            .showImageOnFail(R.mipmap.ic_launcher_round)
            .showImageOnLoading(R.mipmap.ic_launcher_round)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .displayer(new RoundedBitmapDisplayer(2))
            .postProcessor(new BitmapProcessor() {
                @Override
                public Bitmap process(Bitmap bmp) {

                    int dimension = getSquareCropDimensionForBitmap(bmp);
                    bmp = ThumbnailUtils.extractThumbnail(bmp, dimension, dimension);
                    return bmp;
                }
            })
            .considerExifParams(true)
            .imageScaleType(ImageScaleType.EXACTLY)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build();
    public static boolean isNotified=false;
    public static boolean isNotified1=false;

    public static int getSquareCropDimensionForBitmap(Bitmap bitmap) {
        //use the smallest dimension of the image to crop to
        return Math.min(bitmap.getWidth(), bitmap.getHeight());
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void scheduleClassStartJob(Context context, long timer, int jobId) {

        ComponentName serviceComponent = new ComponentName(context, MyNavigationJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);
        builder.setMinimumLatency(timer); // wait at least
        //builder.setOverrideDeadline(timer); // maximum delay
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(jobId);
        int result = jobScheduler.schedule(builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void scheduleClassEndJob(Context context, Date periodDate, int jobId) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(periodDate);

        ComponentName serviceComponent = new ComponentName(context, ClassEndJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);
        builder.setMinimumLatency(calendar.getTimeInMillis()-System.currentTimeMillis()); // wait at least
        //builder.setOverrideDeadline(1*1000); // maximum delay
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(jobId);
        jobScheduler.schedule(builder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void scheduleRoutineFetchJob(Context context, long timer, int jobId) {

        ComponentName serviceComponent = new ComponentName(context, RoutineFetchJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);
        builder.setMinimumLatency(timer); // wait at least
        //builder.setOverrideDeadline(timer); // maximum delay
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(jobId);
        jobScheduler.schedule(builder.build());
    }

    public static boolean checkVersion(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN){
            return true;
        }else{
            return false;
        }
    }
}
