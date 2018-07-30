package com.fnspl.hiplaedu_teacher.fragment;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.activity.DashboardActivity;
import com.fnspl.hiplaedu_teacher.activity.NotificationHandleActivity;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.databinding.FragmentClassStartBinding;
import com.fnspl.hiplaedu_teacher.beaconsDetection.MyService;
import com.fnspl.hiplaedu_teacher.model.ProfileInfo;
import com.fnspl.hiplaedu_teacher.model.RoutinePeriod;
import com.fnspl.hiplaedu_teacher.service.MyNavigationService;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.fnspl.hiplaedu_teacher.utils.NetworkUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.paperdb.Paper;

/**
 * A simple {@link Fragment} subclass.
 */
public class ClassStartFragment extends Fragment {

    private FragmentClassStartBinding binding_classStart;
    private View mView;
    private ProgressDialog pDialog;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

    public ClassStartFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        MainApplication.isClassStarted = true;

        // Inflate the layout for this fragment
        binding_classStart = DataBindingUtil.inflate(inflater, R.layout.fragment_class_start, container, false);
        mView = binding_classStart.getRoot();
        init(mView);
        return mView;
    }

    private void init(View mView) {

        if(getActivity()!=null) {
            //getActivity().stopService(new Intent(getActivity(), MyService.class));
            //getActivity().stopService(new Intent(getActivity(), MyNavigationService.class));
        }

        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage(getString(R.string.dialog_msg));
        pDialog.setCancelable(false);

        binding_classStart.btnClassStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Paper.book().read(CONST.PROFILE_INFO) != null) {
                    doStartClass();
                }
            }
        });

        setUpNextClassTimer();
    }

    private void doStartClass() {
        ProfileInfo profileInfo = Paper.book().read(CONST.PROFILE_INFO);

        String urlParameters = "teacher_id=" + profileInfo.getId() +
                "&routine_history_id=" + Paper.book().read(CONST.CURRENT_PERIOD, 0) +
                "&flag=start";

        new APIRequest().execute(urlParameters);
    }

    private class APIRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            HttpURLConnection urlConnection = null;
            try {
                Log.d("Tester", "Before request");
                URL url = new URL(NetworkUtility.BASEURL + NetworkUtility.CLASS_START_END);
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

            pDialog.show();
            //macAddress = "d0:37:42:d4:b6:f1";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }

            try {
                JSONObject cmxResponse = new JSONObject(s);

                if (cmxResponse.getString("status").equalsIgnoreCase("success")) {

                    Paper.book().delete(CONST.CLASS_STARTED);

                    if (getActivity() != null) {
                        Paper.book().write(CONST.CLASS_RUNNING, true);

                        getActivity().startActivity(new Intent((NotificationHandleActivity) getActivity(), DashboardActivity.class));
                        getActivity().finish();

                        MainApplication.openAttendanceReport = true;

                        Toast.makeText(getActivity(), getResources().getString(R.string.class_start_successfully), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (getActivity() != null)
                        Toast.makeText(getActivity(), cmxResponse.optString("message"), Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();
                //Toast.makeText(getActivity(), "JSON Exception", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            Db_helper db_helper = new Db_helper(getActivity());
            RoutinePeriod routinePeriod = db_helper.getRoutine(Paper.book().read(CONST.CURRENT_PERIOD, 0));
            if (routinePeriod != null) {
                Date currentDateTime = new Date();
                Date classStartDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routinePeriod.getStartTime());
                Date classEndDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routinePeriod.getEndTime());

                long diff = classEndDateTime.getTime() - classStartDateTime.getTime();
                int seconds = (int) (diff / 1000) % 60;
                int minutes = (int) ((diff / (1000 * 60)) % 60);

                binding_classStart.tvPeriod.setText(minutes + ":00" + " min");

                binding_classStart.tvClassName.setText("" + routinePeriod.getClassname() + "-" + routinePeriod.getSection_name());
                binding_classStart.tvSubject.setText("" + routinePeriod.getSubject_name());
            }else{
                Paper.book().delete(CONST.CLASS_STARTED);

                Intent intent = new Intent(getActivity(), DashboardActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }

            if (!Paper.book().read(CONST.CLASS_STARTED, false) && getActivity() != null) {
                Intent intent = new Intent(getActivity(), DashboardActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            } else if (Paper.book().read(CONST.CLASS_RUNNING, false) && getActivity() != null) {
                Paper.book().delete(CONST.CLASS_STARTED);

                Intent intent = new Intent(getActivity(), DashboardActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }

        } catch (Exception e) {

        }
    }

    private void setUpNextClassTimer() {
        try {
            Date currentDateTime = new Date();

            Db_helper db_helper = new Db_helper(getActivity());

            RoutinePeriod routinePeriod = db_helper.getRoutine((int) Paper.book().read(CONST.CURRENT_PERIOD));

            Date classDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routinePeriod.getEndTime());

            setClassEndService(getActivity(), classDateTime);

        } catch (Exception ex) {

        }
    }

    public void setClassEndService(Context mContext, Date periodDate) {
        // Set the alarm to start at approximately 08:00 morning.
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(periodDate);

        if(CONST.checkVersion()) {
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), CONST.CLASS_END_ID,
                    new Intent().setAction("END_CLASS_SERVICE"), PendingIntent.FLAG_UPDATE_CURRENT);
            // reset previous pending intent
            alarmManager.cancel(pendingIntent);
            // if the scheduler date is passed, move scheduler time to tomorrow
            if (System.currentTimeMillis() > calendar.getTimeInMillis()) {
                //calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),pendingIntent);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CONST.scheduleClassEndJob(mContext, periodDate, CONST.CLASS_END_ID);
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MainApplication.isClassStarted=false;
    }
}
