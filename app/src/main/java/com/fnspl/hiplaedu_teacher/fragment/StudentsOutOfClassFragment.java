package com.fnspl.hiplaedu_teacher.fragment;


import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.activity.DashboardActivity;
import com.fnspl.hiplaedu_teacher.activity.NotificationHandleActivity;
import com.fnspl.hiplaedu_teacher.adapter.StudentOutOfClassAdapter;
import com.fnspl.hiplaedu_teacher.application.MainApplication;
import com.fnspl.hiplaedu_teacher.database.Db_helper;
import com.fnspl.hiplaedu_teacher.databinding.FragmentStudentOutOfClassBinding;
import com.fnspl.hiplaedu_teacher.model.ProfileInfo;
import com.fnspl.hiplaedu_teacher.model.RoutinePeriod;
import com.fnspl.hiplaedu_teacher.model.StudentOutOfClassData;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.paperdb.Paper;

/**
 * A simple {@link Fragment} subclass.
 */
public class StudentsOutOfClassFragment extends Fragment implements StudentOutOfClassAdapter.OnMarkAbsentClickListener {

    private FragmentStudentOutOfClassBinding binding_attendance_report;
    private View mView;
    private StudentOutOfClassAdapter mAdapter;
    private List<StudentOutOfClassData> currentAttendanceDatas;

    public StudentsOutOfClassFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding_attendance_report = DataBindingUtil.inflate(inflater, R.layout.fragment_student_out_of_class, container, false);
        binding_attendance_report.setStudentOutOfClass(StudentsOutOfClassFragment.this);
        mView = binding_attendance_report.getRoot();
        init(mView);
        return mView;
    }

    private void init(View mView) {

        binding_attendance_report.rvAttendanceReport.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new StudentOutOfClassAdapter(getActivity(), new ArrayList<StudentOutOfClassData>());
        mAdapter.setOnAttendanceClickListener(this);
        binding_attendance_report.rvAttendanceReport.setAdapter(mAdapter);

        fetchRecord();
    }

    private void fetchRecord(){

        ProfileInfo profileInfo = Paper.book().read(CONST.PROFILE_INFO);

        String urlParameters = "teacher_id="+profileInfo.getId()+
                "&routine_history_id="+Paper.book().read(CONST.CURRENT_PERIOD,0);

        new RecordFetch().execute(urlParameters);
    }

    @Override
    public void onMarkAbsen(int position, StudentOutOfClassData data) {
        goingOutOfClass(data);
    }

    private class RecordFetch extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            HttpURLConnection urlConnection = null;
            try {
                Log.d("Tester", "Before request");
                URL url = new URL(NetworkUtility.BASEURL+ NetworkUtility.SNEAKOUT_STUDENT_LIST);
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

            try {
                JSONObject response = new JSONObject(s);

                if(response.getString("status").equalsIgnoreCase("success")){
                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();
                    Gson gson = builder.create();

                    StudentOutOfClassData[] currentAttendanceData = gson.fromJson(response.getJSONArray("sneakstudentlist").toString() ,
                            StudentOutOfClassData[].class) ;

                    currentAttendanceDatas = Arrays.asList(currentAttendanceData);
                    if (currentAttendanceDatas.size() > 0) {
                        mAdapter.notifyDataChange(currentAttendanceDatas);
                    }else{
                        /*if(MainApplication.isNotificationScreen) {
                            MainApplication.isNotificationScreen = false;

                            Intent nextPage = new Intent(getActivity(), DashboardActivity.class);
                            nextPage.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getActivity().startActivity(nextPage);
                            getActivity().finish();
                        }else{
                            ((DashboardActivity)getActivity()).setFragment(new ProfileInfoFragment(), DashboardActivity.PROFILE);
                        }*/
                    }

                    mAdapter.notifyDataChange(currentAttendanceDatas);

                }else{
                    if(getActivity()!=null)
                    Toast.makeText(getActivity(), getResources().getString(R.string.no_record_available), Toast.LENGTH_SHORT).show();

                    currentAttendanceDatas.clear();

                    mAdapter.notifyDataChange(currentAttendanceDatas);

                    /*if(MainApplication.isNotificationScreen) {
                        MainApplication.isNotificationScreen = false;

                        Intent nextPage = new Intent(getActivity(), DashboardActivity.class);
                        nextPage.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getActivity().startActivity(nextPage);
                        getActivity().finish();
                    }else{
                        ((DashboardActivity)getActivity()).setFragment(new ProfileInfoFragment(), DashboardActivity.PROFILE);
                    }*/

                }

            } catch (JSONException e) {
                e.printStackTrace();
                if(getActivity()!=null)
                Toast.makeText(getActivity(), getResources().getString(R.string.no_record_available), Toast.LENGTH_SHORT).show();

                currentAttendanceDatas= new ArrayList<>();
                mAdapter.notifyDataChange(currentAttendanceDatas);

                /*if(MainApplication.isNotificationScreen) {
                    MainApplication.isNotificationScreen = false;

                    Intent nextPage = new Intent(getActivity(), DashboardActivity.class);
                    nextPage.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(nextPage);
                    getActivity().finish();
                }else{
                    ((DashboardActivity)getActivity()).setFragment(new ProfileInfoFragment(), DashboardActivity.PROFILE);
                }*/

            }
        }
    }

    private void goingOutOfClass(StudentOutOfClassData studentOutOfClassData){
        String time = new SimpleDateFormat("hh:mm a").format(new Date());

        ProfileInfo profileInfo = Paper.book().read(CONST.PROFILE_INFO);
        int routine_id = Paper.book().read(CONST.CURRENT_PERIOD,0);

        String urlParameters = "student_id="+studentOutOfClassData.getStudent_id()+
                "&teacher_id="+profileInfo.getId()+
                "&out_time="+time+
                "&remark=Marked Absent"+
                "&routine_history_id="+routine_id;

        new SneakRegister().execute(urlParameters);
    }

    private class SneakRegister extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            HttpURLConnection urlConnection = null;
            try {
                Log.d("Tester", "Before request");
                URL url = new URL(NetworkUtility.BASEURL+ NetworkUtility.MARK_ABSENT);
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

            try {
                JSONObject response = new JSONObject(s);

                if(response.getString("status").equalsIgnoreCase("success")){
                    //isNotified = true;
                    fetchRecord();
                }


            } catch (JSONException e) {
                e.printStackTrace();
                // Toast.makeText(getApplicationContext(), "JSON Exception", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Paper.book().read(CONST.CLASS_STARTED, false) && MainApplication.isNotificationScreen) {
            ((NotificationHandleActivity)getActivity()).setFragment(new ClassStartFragment(), NotificationHandleActivity.CLASS_START);
        } else if (Paper.book().read(CONST.CLASS_ENDED, false) && MainApplication.isNotificationScreen) {
            ((NotificationHandleActivity)getActivity()).setFragment(new ClassEndFragment(), NotificationHandleActivity.CLASS_END);
        } else {
            populateData();
        }

    }

    private void populateData() {
        Db_helper db_helper = new Db_helper(getActivity());

        if(Paper.book().read(CONST.CURRENT_PERIOD)!=null){
            RoutinePeriod routinePeriod = db_helper.getRoutine((int)Paper.book().read(CONST.CURRENT_PERIOD));
            binding_attendance_report.tvClassName.setText("" + routinePeriod.getClassname() + "-" + routinePeriod.getSection_name());
        }
    }
}
