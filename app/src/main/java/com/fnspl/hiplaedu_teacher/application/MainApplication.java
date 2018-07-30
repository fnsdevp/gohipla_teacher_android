package com.fnspl.hiplaedu_teacher.application;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.StrictMode;
import android.support.multidex.MultiDex;
import android.util.DisplayMetrics;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.receiver.StartServiceBroadcastReceiver;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.navigine.naviginesdk.NavigationThread;
import com.navigine.naviginesdk.NavigineSDK;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import io.fabric.sdk.android.Fabric;
import java.util.Locale;
import java.util.UUID;

import io.paperdb.Paper;

/**
 * Created by FNSPL on 9/15/2017.
 */

public class MainApplication extends Application {

    public static FaceServiceClient getFaceServiceClient() {
        return sFaceServiceClient;
    }

    private static FaceServiceClient sFaceServiceClient;

    public static UUID faceId = null;

    public static boolean isOTPScreen= false;
    public static boolean isNotificationScreen= false;
    public static boolean isClassStarted= false;
    public static boolean openAttendanceReport=false;

    public static final String TAG           = "Navigine.Demo";
    public static final String SERVER_URL    = "https://api.navigine.com";
    //public static final String USER_HASH     = "C213-85E7-2265-3F4B";
    public static final String USER_HASH     = "0F17-DAE1-4D0A-1778";
    //public static final int LOCATION_ID   = 2409;
    public static final int LOCATION_ID   = 1894;

    public static NavigationThread Navigation    = null;
    public static boolean isNavigineInitialized = false;

    // Screen parameters
    public static float DisplayWidthPx            = 0.0f;
    public static float DisplayHeightPx           = 0.0f;
    public static float DisplayWidthDp            = 0.0f;
    public static float DisplayHeightDp           = 0.0f;
    public static float DisplayDensity            = 0.0f;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        sFaceServiceClient = new FaceServiceRestClient(getString(R.string.endpoint), getString(R.string.subscription_key));

        Paper.init(getApplicationContext());
        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        MultiDex.install(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().registerReceiver(new StartServiceBroadcastReceiver(), new IntentFilter("STOP_WIFI_ZONE_SERVICE"));
            getApplicationContext().registerReceiver(new StartServiceBroadcastReceiver(), new IntentFilter("START_WIFI_ZONE_SERVICE"));
            getApplicationContext().registerReceiver(new StartServiceBroadcastReceiver(), new IntentFilter("END_CLASS_SERVICE"));
            getApplicationContext().registerReceiver(new StartServiceBroadcastReceiver(), new IntentFilter("FETCH_DAILY_ROUTINE_SERVICE"));
        }

        /*Intent intent = new Intent("STOP_WIFI_ZONE_SERVICE");
        intent.setClass(this, StartServiceBroadcastReceiver.class);

        Intent intent1 = new Intent("START_WIFI_ZONE_SERVICE");
        intent1.setClass(this, StartServiceBroadcastReceiver.class);

        Intent intent2 = new Intent("END_CLASS_SERVICE");
        intent2.setClass(this, StartServiceBroadcastReceiver.class);

        Intent intent3 = new Intent("FETCH_DAILY_ROUTINE_SERVICE");
        intent3.setClass(this, StartServiceBroadcastReceiver.class);*/

    }

    public static boolean initialize(Context context)
    {
        NavigineSDK.setParameter(context, "debug_level", 2);
        NavigineSDK.setParameter(context, "apply_server_config_enabled",  false);
        NavigineSDK.setParameter(context, "actions_updates_enabled",      false);
        NavigineSDK.setParameter(context, "location_updates_enabled",     true);
        NavigineSDK.setParameter(context, "location_update_timeout",      30);
        NavigineSDK.setParameter(context, "post_beacons_enabled",         true);
        NavigineSDK.setParameter(context, "post_messages_enabled",        true);
        if (!NavigineSDK.initialize(context, USER_HASH, SERVER_URL)) {
            return false;
        }else{
            isNavigineInitialized=true;
        }

        Navigation = NavigineSDK.getNavigation();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        DisplayWidthPx  = displayMetrics.widthPixels;
        DisplayHeightPx = displayMetrics.heightPixels;
        DisplayDensity  = displayMetrics.density;
        DisplayWidthDp  = DisplayWidthPx  / DisplayDensity;
        DisplayHeightDp = DisplayHeightPx / DisplayDensity;

        Log.d(TAG, String.format(Locale.ENGLISH, "Display size: %.1fpx x %.1fpx (%.1fdp x %.1fdp, density=%.2f)",
                DisplayWidthPx, DisplayHeightPx,
                DisplayWidthDp, DisplayHeightDp,
                DisplayDensity));

        return true;
    }

    public static void finish()
    {
        isNavigineInitialized=false;
        if (Navigation == null)
            return;

        NavigineSDK.finish();
        Navigation = null;
    }

}
