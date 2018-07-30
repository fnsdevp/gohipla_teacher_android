package com.fnspl.hiplaedu_teacher.fragment;


import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.databinding.FragmentProfileInfoBinding;
import com.fnspl.hiplaedu_teacher.model.ProfileInfo;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.fnspl.hiplaedu_teacher.utils.NetworkUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import io.paperdb.Paper;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileInfoFragment extends Fragment {

    private FragmentProfileInfoBinding binding_profileInfo;
    private View mView;

    public ProfileInfoFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding_profileInfo =  DataBindingUtil.inflate(inflater, R.layout.fragment_profile_info, container, false);
        binding_profileInfo.setProfileInfo(ProfileInfoFragment.this);
        mView = binding_profileInfo.getRoot();
        init(mView);
        return mView;
    }

    private void init(View mView) {
        profileInfo();
    }

    private void profileInfo() {
        try {
            if (Paper.book().read(CONST.PROFILE_INFO) != null) {
                ProfileInfo profileInfo = Paper.book().read(CONST.PROFILE_INFO);
                ImageLoader.getInstance().displayImage(NetworkUtility.IMAGE_TEACHER_BASEURL+""+ profileInfo.getPhoto(),
                        binding_profileInfo.ivProfilePic, CONST.ErrorWithLoaderRoundedCorner);
                binding_profileInfo.tvName.setText(String.format("%s", profileInfo.getName()));
                binding_profileInfo.tvSubject.setText(String.format("%s", profileInfo.getDesignation()));
                binding_profileInfo.tvDepartment.setText(String.format("%s", profileInfo.getDepartment()));
                binding_profileInfo.tvPhone.setText(String.format("%s", profileInfo.getPhone()));
                binding_profileInfo.tvEmail.setText(String.format("%s", profileInfo.getEmail()));
                binding_profileInfo.tvAddress.setText(String.format("%s", profileInfo.getAddress()));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Paper.book().read(CONST.UPDATE_PROFILE, false)) {
            updateInfo();
        }

    }

    private void updateInfo() {
        ProfileInfo profileInfo = Paper.book().read(CONST.PROFILE_INFO);
        if (profileInfo != null) {
            String urlParameters = "user_id=" + profileInfo.getId() +
                    "&user_type=teacher";

            new APIRequest().execute(urlParameters);
        }
    }

    private class APIRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            HttpURLConnection urlConnection = null;

            try {
                Log.d("Tester", "Before request");
                URL url = new URL(NetworkUtility.BASEURL + NetworkUtility.PROFILE_UPDATE);
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


        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {
                JSONObject cmxResponse = new JSONObject(s);

                if (cmxResponse.getString("status").equalsIgnoreCase("success")) {

                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();
                    Gson gson = builder.create();

                    ProfileInfo profile = gson.fromJson(cmxResponse.getJSONArray("userDetails").getJSONObject(0).toString() , ProfileInfo.class) ;

                    Paper.book().write(CONST.PROFILE_INFO, profile);

                    Paper.book().delete(CONST.UPDATE_PROFILE);

                    profileInfo();

                }

            } catch (Exception ex) {

            }
        }

    }

}
