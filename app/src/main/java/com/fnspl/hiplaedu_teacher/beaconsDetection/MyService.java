package com.fnspl.hiplaedu_teacher.beaconsDetection;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.activity.LoginActivity;
import com.fnspl.hiplaedu_teacher.activity.NotificationHandleActivity;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.model.RoutinePeriod;
import com.fnspl.hiplaedu_teacher.utils.CONST;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.paperdb.Paper;
import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconType;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconUtils;
import uk.co.alt236.bluetoothlelib.device.beacon.ibeacon.IBeaconDevice;


/**
 * Created by FNSPL on 9/21/2017.
 */

public class MyService extends Service {

    private BluetoothLeDeviceStore mDeviceStore;
    private IBeaconDevice iBeaconDevice;
    private boolean pass1 = false, pass2 = false, pass3 = false;
    private String msg = "";
    private BluetoothLeScanner mScanner;
    private BluetoothUtils mBluetoothUtils;
    private boolean flag = false;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
    private TimerTask mTimerTask = null;
    private Timer mTimer = new Timer();
    private Handler mHandler = new Handler();
    private static final int UPDATE_TIMEOUT = 1 * 10 * 1000;
    private double failureCount = 0;
    private Runnable mRunnable =
            new Runnable() {
                public void run() {
                    if(isOnline()){
                        if(!MainApplication.isClassStarted) {
                            new ZoneDetection().execute();
                        }else{
                            mTimer.cancel();

                            if (mTimerTask != null)
                                mTimerTask.cancel();

                            stopSelf();
                        }
                    }else {
                        if(!MainApplication.isClassStarted) {
                            setBluetoothEnable(true, MyService.this);
                            startScan();
                        }else{
                            mTimer.cancel();

                            if (mTimerTask != null)
                                mTimerTask.cancel();

                            stopSelf();
                        }
                    }
                }
            };
    private String macAddress="";
    private static boolean isClassLaunched = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        isClassLaunched=false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        isClassLaunched=false;

        if (getApplicationContext() != null) {

            mDeviceStore = new BluetoothLeDeviceStore();
            mBluetoothUtils = new BluetoothUtils(getApplicationContext());
            mScanner = new BluetoothLeScanner(mLeScanCallback, mBluetoothUtils);

            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(mRunnable);
                }
            };
            mTimer.schedule(mTimerTask, UPDATE_TIMEOUT, 100);

            //mHandler.post(mRunnable);

        } else {
            stopSelf();
        }

        setUpNextClassTimer();
    }

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            final BluetoothLeDevice deviceLe = new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis());

            if (BeaconUtils.getBeaconType(deviceLe) == BeaconType.IBEACON) {
                iBeaconDevice = new IBeaconDevice(deviceLe);
                int minor = iBeaconDevice.getMinor();
                if (minor == 34739) {  //52227 yellow

                    if (iBeaconDevice.getAccuracy() < 2.7) { //3.55

                        msg = "entered";
                        flag = true;
                        Log.d("joseph", "entered: ");
                        mScanner.scanLeDevice(-1, false);
                        mTimer.cancel();
                        mTimerTask.cancel();
                        stopSelf();

                    } else {

                        msg = "exited";
                        flag = false;
                        Log.d("joseph", "exite  d: ");

                    }

                    if (flag) {

                        mScanner.scanLeDevice(-1, false);
                        mTimer.cancel();
                        if (mTimerTask != null)
                            mTimerTask.cancel();

                        stopSelf();

                        //setUpNextClassTimer();

                        if (!MainApplication.isNotificationScreen && !MainApplication.isClassStarted) {
                            launchWelcomeClass(NotificationHandleActivity.CLASS_START);
                        }else if (MainApplication.isNotificationScreen && !MainApplication.isClassStarted) {
                            finishActivity(NotificationHandleActivity.CLASS_START);
                        }

                    }

                }
                //sendNotification("You are in class? " + flag);
            }

        }
    };

    private void finishActivity(String notificationtype) {
        Intent zoneInfo = new Intent("android.intent.action.FINSIHACTIVITY");
        zoneInfo.putExtra(CONST.NOTIFICATION_TYPE, notificationtype);
        sendBroadcast(zoneInfo);
        Paper.book().write(CONST.CLASS_STARTED, true);

        isClassLaunched = true;
    }

    private void launchWelcomeClass(String notificationtype) {
        //sendBroadcast(new Intent(getResources().getString(R.string.class_start)));
        Intent notificationHandle = new Intent(MyService.this, NotificationHandleActivity.class);
        notificationHandle.putExtra(CONST.NOTIFICATION_TYPE, notificationtype);
        notificationHandle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(notificationHandle);
        Paper.book().write(CONST.CLASS_STARTED, true);

        isClassLaunched=true;
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
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), CONST.CLASS_END_ID,
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

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                24 * 60 * 60 * 1000, pendingIntent);
    }


    private void startScanPrepare() {
        //
        // The COARSE_LOCATION permission is only needed after API 23 to do a BTLE scan
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(Constants.activity,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, new PermissionsResultAction() {

                        @Override
                        public void onGranted() {
                            startScan();
                        }

                        @Override
                        public void onDenied(String permission) {
                            Toast.makeText(Constants.activity,
                                    R.string.permission_not_granted_coarse_location,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        } else {
            startScan();
        }
    }

    private void startScan() {

        final boolean isBluetoothOn = mBluetoothUtils.isBluetoothOn();
        final boolean isBluetoothLePresent = mBluetoothUtils.isBluetoothLeSupported();
        mDeviceStore.clear();

        mBluetoothUtils.askUserToEnableBluetoothIfNeeded();
        if (isBluetoothOn && isBluetoothLePresent) {
            mScanner.scanLeDevice(UPDATE_TIMEOUT, true);
            //sendNotification("Start location");
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        isClassLaunched=true;

        mTimer.cancel();

        if (mTimerTask != null)
            mTimerTask.cancel();

    }

    private boolean setBluetoothEnable(boolean enable, Context mContext) {
        BluetoothAdapter bluetoothAdapter = mBluetoothUtils.getBluetoothAdapter();
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

        }
    }

    private class ZoneDetection extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            HttpsURLConnection urlConnection=null;
            try {
                TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };

                SSLContext sc = null;

                try {
                    sc = SSLContext.getInstance("SSL");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                try {
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                // Create all-trusting host name verifier
                HostnameVerifier allHostsValid = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

                String authString ="admin" + ":" + "C1sco12345";
                byte[] authEncBytes = Base64.encode(authString.getBytes(), Base64.DEFAULT);
                String authStringEnc = new String(authEncBytes);

                Log.d("Tester","Before request");
                String restURL = "https://bgl-cmx.ebc.cisco.com/api/location/v1/clients/count/byzone/detail?zoneId=97";
                URL url = new URL(restURL);
                Log.d("Tester","Make URL");
                urlConnection = (HttpsURLConnection) url.openConnection();
                Log.d("Tester","Open Connection");
                urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
                urlConnection.setConnectTimeout(10000);
                Log.d("Tester","Set Property");
                int responseCode = urlConnection.getResponseCode();
                Log.d("Tester","Responce Code: "+responseCode);

                if(responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = urlConnection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);

                    int numCharsRead;
                    char[] charArray = new char[1024];
                    StringBuffer sb = new StringBuffer();
                    while ((numCharsRead = isr.read(charArray)) > 0) {
                        sb.append(charArray, 0, numCharsRead);
                    }
                    result = sb.toString();
                }

            }catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                return result;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            macAddress = getMacAddr();
            //macAddress = "d0:37:42:d4:b6:f1";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {JSONObject cmxResponse = new JSONObject(s);

                if(cmxResponse.getJSONArray("MacAddress")!=null){
                    JSONArray macAddressArray = cmxResponse.getJSONArray("MacAddress");
                    boolean flag = false;
                    for (int i = 0; i < macAddressArray.length(); i++) {
                        if(macAddress.equalsIgnoreCase(macAddressArray.get(i).toString())){
                            flag = true;
                            break;
                        }
                    }

                    if (flag) {

                        mScanner.scanLeDevice(-1, false);
                        mTimer.cancel();
                        mTimerTask.cancel();

                        stopSelf();

                        if (!MainApplication.isNotificationScreen && !MainApplication.isClassStarted) {
                            launchWelcomeClass(NotificationHandleActivity.CLASS_START);
                        }else if (MainApplication.isNotificationScreen && !MainApplication.isClassStarted) {
                            finishActivity(NotificationHandleActivity.CLASS_START);
                        }

                    }else{
                        /*if(failureCount<3) {
                            failureCount++;
                        }else{
                            if(failureCount>2 && failureCount<5) {
                                failureCount++;
                                setBluetoothEnable(true, MyService.this);
                                startScan();
                            }else{
                                failureCount = 0;
                            }
                        }*/
                    }

                }else{
                    setBluetoothEnable(true, MyService.this);
                    startScan();
                }

            } catch (JSONException e) {
                e.printStackTrace();

                setBluetoothEnable(true, MyService.this);
                startScan();
            }
        }
    }

    public String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
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

}
