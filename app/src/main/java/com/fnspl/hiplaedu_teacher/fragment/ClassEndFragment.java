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
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.databinding.FragmentClassEndBinding;
import com.fnspl.hiplaedu_teacher.model.ProfileInfo;
import com.fnspl.hiplaedu_teacher.model.RoutinePeriod;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.fnspl.hiplaedu_teacher.utils.NetworkUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
import java.util.List;

import io.paperdb.Paper;

/**
 * A simple {@link Fragment} subclass.
 */
public class ClassEndFragment extends Fragment {

    private FragmentClassEndBinding binding_classEnd;
    private View mView;
    private ProgressDialog pDialog;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

    public ClassEndFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding_classEnd = DataBindingUtil.inflate(inflater, R.layout.fragment_class_end, container, false);
        mView = binding_classEnd.getRoot();
        init(mView);
        return mView;
    }

    private void init(View mView) {
        binding_classEnd.btnClassStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Paper.book().read(CONST.PROFILE_INFO) != null) {
                    doStopClass();
                } else {

                }
            }
        });
    }

    private void doStopClass() {
        ProfileInfo profileInfo = Paper.book().read(CONST.PROFILE_INFO);

        String urlParameters = "teacher_id=" + profileInfo.getId() +
                "&routine_history_id=" + Paper.book().read(CONST.CURRENT_PERIOD, 0) +
                "&flag=stop";

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

            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage(getString(R.string.dialog_msg));
            pDialog.setCancelable(false);
            pDialog.show();
            //macAddress = "d0:37:42:d4:b6:f1";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }

            Log.d("Tester", "Result: " + s);
            try {
                JSONObject cmxResponse = new JSONObject(s);

                if (cmxResponse.getString("status").equalsIgnoreCase("success")) {

                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();
                    Gson gson = builder.create();

                    //Toast.makeText(getActivity(), cmxResponse.optString("message"), Toast.LENGTH_SHORT).show();

                    Paper.book().delete(CONST.CLASS_STARTED);
                    Paper.book().delete(CONST.CLASS_RUNNING);
                    Paper.book().delete(CONST.CLASS_ENDED);

                    setUpNextClassTimer();

                    if (getActivity() != null) {
                        MainApplication.isNotificationScreen=false;
                        getActivity().startActivity(new Intent(getActivity(), DashboardActivity.class));
                        getActivity().finish();
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
            Date currentDateTime = new Date();

            if (routinePeriod != null) {
                Date classStartDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routinePeriod.getStartTime());
                Date classEndDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routinePeriod.getEndTime());

                long diff = classEndDateTime.getTime() - classStartDateTime.getTime();
                int seconds = (int) (diff / 1000) % 60;
                int minutes = (int) ((diff / (1000 * 60)) % 60);

                binding_classEnd.tvPeriod.setText(minutes + ":00" + " min");

                binding_classEnd.tvClassName.setText("" + routinePeriod.getClassname() + "-" + routinePeriod.getSection_name());
                binding_classEnd.tvSubject.setText("" + routinePeriod.getSubject_name());
            }else{
                setUpNextClassTimer();

                Paper.book().delete(CONST.CLASS_ENDED);

                Intent intent = new Intent(getActivity(), DashboardActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }

            if(!Paper.book().read(CONST.CLASS_ENDED, false) && getActivity()!=null){
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

            List<RoutinePeriod> routinePeriodList = db_helper.getRoutine(new SimpleDateFormat("EEEE").
                    format(currentDateTime).toLowerCase());

            for (int i=0; i<routinePeriodList.size(); i++) {

                Date classDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime)+" "+routinePeriodList.get(i).getStartTime());
                Calendar cal = Calendar.getInstance();
                cal.setTime(classDateTime);
                cal.set(Calendar.MINUTE, -2);

                if(Paper.book().read(CONST.CURRENT_PERIOD, 0) >= routinePeriodList.get(i).getRoutine_history_id()){
                    if(i==(routinePeriodList.size()-1)){
                        Paper.book().delete(CONST.LOGIN_FOR_FIRST_TIME);
                        Paper.book().delete(CONST.CURRENT_PERIOD);
                    }
                }else{
                    Paper.book().write(CONST.CURRENT_PERIOD, routinePeriodList.get(i).getRoutine_history_id());

                    setClassStartService(getActivity(), classDateTime);

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
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), CONST.CLASS_START_ID,
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

}
