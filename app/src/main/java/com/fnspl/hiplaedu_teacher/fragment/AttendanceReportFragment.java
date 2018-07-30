package com.fnspl.hiplaedu_teacher.fragment;


import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.activity.DashboardActivity;
import com.fnspl.hiplaedu_teacher.activity.NotificationHandleActivity;
import com.fnspl.hiplaedu_teacher.adapter.CurrentAttendanceAdapter;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.databinding.FragmentAttendanceReportBinding;
import com.fnspl.hiplaedu_teacher.model.CurrentAttendanceData;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.paperdb.Paper;

/**
 * A simple {@link Fragment} subclass.
 */
public class AttendanceReportFragment extends Fragment {

    private FragmentAttendanceReportBinding binding_attendance_report;
    private View mView;
    private CurrentAttendanceAdapter mAdapter;
    private ProgressDialog pDialog;

    public AttendanceReportFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding_attendance_report = DataBindingUtil.inflate(inflater, R.layout.fragment_attendance_report, container, false);
        binding_attendance_report.setAttendanceReport(AttendanceReportFragment.this);
        mView = binding_attendance_report.getRoot();
        init(mView);
        return mView;
    }

    private void init(View mView) {

        binding_attendance_report.rvAttendanceReport.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new CurrentAttendanceAdapter(getActivity(), new ArrayList<CurrentAttendanceData>());
        binding_attendance_report.rvAttendanceReport.setAdapter(mAdapter);

        binding_attendance_report.btnRequestReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DashboardActivity) getActivity()).setFragment(new ManualAttendanceFragment(),
                        NotificationHandleActivity.MANUAL_ATTENDANCE);
            }
        });

        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage("Please wait...");

        binding_attendance_report.pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    populateData();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        try {

            if (Paper.book().read(CONST.CLASS_STARTED, false) && MainApplication.isNotificationScreen) {
                ((NotificationHandleActivity)getActivity()).setFragment(new ClassStartFragment(), NotificationHandleActivity.CLASS_START);
            } else if (Paper.book().read(CONST.CLASS_ENDED, false) && MainApplication.isNotificationScreen) {
                ((NotificationHandleActivity)getActivity()).setFragment(new ClassEndFragment(), NotificationHandleActivity.CLASS_END);
            } else {
                populateData();
            }

        } catch (Exception e) {

        }

    }

    private void populateData() throws ParseException {
        Date currentDateTime = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

        Db_helper db_helper = new Db_helper(getActivity());

        if (Paper.book().read(CONST.CURRENT_PERIOD) != null) {
            RoutinePeriod routinePeriod = db_helper.getRoutine((int) Paper.book().read(CONST.CURRENT_PERIOD));
            binding_attendance_report.tvClassName.setText("" + routinePeriod.getClassname() + "-" + routinePeriod.getSection_name()+
                    "("+routinePeriod.getSubject_name()+")");
        } else {
            List<RoutinePeriod> routinePeriodList = db_helper.getRoutine(new SimpleDateFormat("EEEE").
                    format(currentDateTime).toLowerCase());

            for (RoutinePeriod routinePeriod :
                    routinePeriodList) {

                Date classDateTime = dateFormat.parse(new SimpleDateFormat("yyyy-MM-dd").format(currentDateTime) + " " + routinePeriod.getEndTime());
                if (currentDateTime.compareTo(classDateTime) > 0) {

                } else {
                    binding_attendance_report.tvClassName.setText("" + routinePeriod.getClassname() + "-" + routinePeriod.getSection_name()+
                            "("+routinePeriod.getSubject_name()+")");
                    break;
                }

            }
        }

        fetchRecord();
    }

    private void fetchRecord() {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        ProfileInfo profileInfo = Paper.book().read(CONST.PROFILE_INFO);

        String urlParameters = "teacher_id=" + profileInfo.getId() +
                "&usertype=teacher" +
                "&routine_history_id=" + Paper.book().read(CONST.CURRENT_PERIOD, 0) +
                "&device_type=Android";

        new RecordFetch().execute(urlParameters);
    }

    private class RecordFetch extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            HttpURLConnection urlConnection = null;
            try {
                Log.d("Tester", "Before request");
                URL url = new URL(NetworkUtility.BASEURL + NetworkUtility.CURRENT_STUDENT_LIST);
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
            //macAddress = getMacAddr();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            pDialog.dismiss();

            binding_attendance_report.pullToRefresh.setRefreshing(false);

            try {
                JSONObject response = new JSONObject(s);

                if (response.getString("status").equalsIgnoreCase("success")) {
                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();
                    Gson gson = builder.create();

                    CurrentAttendanceData[] currentAttendanceData = gson.fromJson(response.getJSONArray("studentlist").toString(),
                            CurrentAttendanceData[].class);

                    List<CurrentAttendanceData> currentAttendanceDatas = Arrays.asList(currentAttendanceData);

                    mAdapter.notifyDataChange(currentAttendanceDatas);

                    setAttendanceData(currentAttendanceDatas);

                } else {
                    if (getActivity() != null)
                        Toast.makeText(getActivity(), "No record available", Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();

                if (getActivity() != null)
                    Toast.makeText(getActivity(), "No record available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setAttendanceData(List<CurrentAttendanceData> currentAttendanceDatas) {
        int present = 0, absent = 0, remains = 0;

        for (CurrentAttendanceData currentData :
                currentAttendanceDatas) {

            if (currentData != null && currentData.getStatus() != null) {
                if (currentData.getStatus().equalsIgnoreCase("present")) {
                    present++;
                } else if (currentData.getStatus().equalsIgnoreCase("absent")) {
                    absent++;
                } else {
                    remains++;
                }
            } else {
                remains++;
            }
        }

        binding_attendance_report.btnPresent.setText("" + present);
        binding_attendance_report.btnAbsent.setText("" + absent);
        binding_attendance_report.btnRemains.setText("" + remains);
    }

}
