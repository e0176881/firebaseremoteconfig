package com.mingxuan.firebaseremoteconfig;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.internal.service.Common;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    private HashMap<String, Object> firebaseDefaultMap;
    public static final String VERSION_CODE_KEY = "latest_app_version";
    private static final String TAG = "MainActivity";
    ProgressDialog bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//This is default Map
        //Setting the Default Map Value with the current version code
        firebaseDefaultMap = new HashMap<>();
        firebaseDefaultMap.put(VERSION_CODE_KEY, getCurrentVersionCode());
        mFirebaseRemoteConfig.setDefaults(firebaseDefaultMap);

        //Setting that default Map to Firebase Remote Config

        //Setting Developer Mode enabled to fast retrieve the values
        mFirebaseRemoteConfig.setConfigSettings(
                new FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(BuildConfig.DEBUG)
                        .build());

        //Fetching the values here
        mFirebaseRemoteConfig.fetch().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    mFirebaseRemoteConfig.activateFetched();
                    Log.d(TAG, "Fetched value: " + mFirebaseRemoteConfig.getString(VERSION_CODE_KEY));
                    //calling function to check if new version is available or not
                    checkForUpdate();
                } else {
                    Toast.makeText(MainActivity.this, "Something went wrong please try again",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        Log.d(TAG, "Default value: " + mFirebaseRemoteConfig.getString(VERSION_CODE_KEY));
    }

    private void checkForUpdate() {
        int latestAppVersion = (int) mFirebaseRemoteConfig.getDouble(VERSION_CODE_KEY);
        System.out.println(getCurrentVersionCode());
        if (latestAppVersion > getCurrentVersionCode()) {
            new AlertDialog.Builder(this).setTitle("Please Update the App")
                    .setMessage("A new version of this app is available. Please update it").setPositiveButton(
                    "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new DownloadNewVersion().execute();
                            dialog.dismiss();
                        }
                    }).setCancelable(false).show();
        } else {
            Toast.makeText(this, "This app is already up to date", Toast.LENGTH_SHORT).show();
        }
    }

    private int getCurrentVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }


    class DownloadNewVersion extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar = new ProgressDialog(MainActivity.this);
            bar.setCancelable(false);
            bar.setMessage("Downloading...");
            bar.setIndeterminate(true);
            bar.setCanceledOnTouchOutside(false);
            bar.show();
          //  stoptimertask();
        }

        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            bar.setIndeterminate(false);
            bar.setMax(100);
            bar.setProgress(progress[0]);
            String msg = "";
            if (progress[0] > 99) {
                msg = "Finishing... ";
            } else {
                msg = "Downloading... " + progress[0] + "%";
            }
            bar.setMessage(msg);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
           // startTimer();
            bar.dismiss();
            if (result) {
                Toast.makeText(getApplicationContext(), "Update Done",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Error: Try Again",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            Boolean flag = false;
            try {
                String PATH;
                Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
                if (isSDPresent) {
                    PATH = Environment.getExternalStorageDirectory() + "/Download/";
                } else {
                    PATH = Environment.getDataDirectory() + "/Download/";
                }
                File file = new File(PATH);
                file.mkdirs();
                File outputFile = new File(file, "app-debug.apk");
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                // Download File from url

                URL u = new URL("http://voidq.xyz/app-debug.apk");
                URLConnection conn = u.openConnection();
                int contentLength = conn.getContentLength();

                DataInputStream stream = new DataInputStream(u.openStream());

                byte[] buffer = new byte[contentLength];
                stream.readFully(buffer);
                stream.close();

                DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
                fos.write(buffer);
                fos.flush();
                fos.close();
                // Install dowloaded Apk file from Devive----------------
                OpenNewVersion(PATH);
                flag = true;
            } catch (MalformedURLException e) {
                Log.e(TAG, "Update Error: " + e.getMessage());
                flag = false;
            } catch (IOException e) {
                Log.e(TAG, "Update Error: " + e.getMessage());
                flag = false;
            } catch (Exception e) {
                Log.e(TAG, "Update Error: " + e.getMessage());
                flag = false;
            }
            return flag;
        }

    }

    void OpenNewVersion(String location) {


        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider" ,new File(location + "app-debug.apk"));
        intent.setDataAndType(data, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}