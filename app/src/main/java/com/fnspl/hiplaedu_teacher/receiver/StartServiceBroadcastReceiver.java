package com.fnspl.hiplaedu_teacher.receiver;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.fnspl.hiplaedu_teacher.beaconsDetection.MyService;
import com.fnspl.hiplaedu_teacher.service.ClassEndService;
import com.fnspl.hiplaedu_teacher.service.MyNavigationService;
import com.fnspl.hiplaedu_teacher.service.RoutineFetchService;
import com.fnspl.hiplaedu_teacher.utils.CONST;

import io.paperdb.Paper;

/*
 * Created by FNSPL on 9/5/2017.
 */

public class StartServiceBroadcastReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equalsIgnoreCase("STOP_WIFI_ZONE_SERVICE")) {
            //if (isMyServiceRunning(context, MyService.class)) {
            if (isMyServiceRunning(context, MyNavigationService.class)) {
                Log.d("Testing", "Service is running!! Stopping...");
                //context.stopService(new Intent(context, MyService.class));

                //Using Navigine
                context.stopService(new Intent(context, MyNavigationService.class));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, ClassEndService.class));
                } else {
                    context.startService(new Intent(context, ClassEndService.class));
                }
            } else {
                Log.d("Testing", "Service not running");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, ClassEndService.class));
                } else {
                    context.startService(new Intent(context, ClassEndService.class));
                }
            }
            Paper.book().write(CONST.CLASS_ENDED, true);
        }else if (intent.getAction().equalsIgnoreCase("START_WIFI_ZONE_SERVICE")) {
            setBluetoothEnable(true, context);

            //if (!isMyServiceRunning(context, MyService.class)) {
            if (!isMyServiceRunning(context, MyNavigationService.class)) {
                Log.d("Testing", "Service not running!! Starting...");
                //Using beacons
                //context.startService(new Intent(context, MyService.class));

                //Using Navigine
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, MyNavigationService.class));
                } else {
                    context.startService(new Intent(context, MyNavigationService.class));
                }
            } else {
                Log.d("Testing", "Service is running");
            }
        }else if (intent.getAction().equalsIgnoreCase("END_CLASS_SERVICE")) {
            setBluetoothEnable(true, context);

            if (!isMyServiceRunning(context, ClassEndService.class)) {
                Log.d("Testing", "Service not running!! Starting...");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, ClassEndService.class));
                } else {
                    context.startService(new Intent(context, ClassEndService.class));
                }
            } else {
                Log.d("Testing", "Service is running");
            }
        }else if (intent.getAction().equalsIgnoreCase("FETCH_DAILY_ROUTINE_SERVICE")) {
            if (!isMyServiceRunning(context, RoutineFetchService.class)) {
                Log.d("Testing", "Service not running!! Starting...");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, RoutineFetchService.class));
                } else {
                    context.startService(new Intent(context, RoutineFetchService.class));
                }
            } else {
                Log.d("Testing", "Service is running");
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

    private boolean setBluetoothEnable(boolean enable, Context mContext) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = false;

        if(bluetoothAdapter != null) {
            isEnabled = bluetoothAdapter.isEnabled();
        }else{
            return false ;
        }

        if (enable && !isEnabled) {
            return bluetoothAdapter.enable();
        } else if (!enable && isEnabled) {
            return bluetoothAdapter.disable();
        }
        // No need to change bluetooth state
        enableLocation(mContext);
        return true;
    }

    private void enableLocation(Context mContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsProviderEnabled, isNetworkProviderEnabled;
            isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsProviderEnabled && !isNetworkProviderEnabled) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }

        } /*else {
            LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsProviderEnabled, isNetworkProviderEnabled;
            isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGpsProviderEnabled && !isNetworkProviderEnabled) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        }*/
    }

}