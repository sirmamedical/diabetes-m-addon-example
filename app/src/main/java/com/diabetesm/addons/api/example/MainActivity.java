package com.diabetesm.addons.api.example;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.diabetesm.addons.api.DiabetesAppConnection;
import com.diabetesm.addons.api.dto.Configuration;
import com.diabetesm.addons.api.dto.LogEntry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    public static final int ACCESS_REQUEST_CODE = 999;

    private TextView mResult;
    private Button mPushData;
    private Button mReadData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button readAccess = (Button)findViewById(R.id.request_read_access);
        readAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestAccess(false);
            }
        });

        Button pushAccess = (Button)findViewById(R.id.request_push_data_access);
        pushAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestAccess(true);
            }
        });

        mReadData = (Button)findViewById(R.id.read_data);
        mReadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readData();
            }
        });

        mPushData = (Button)findViewById(R.id.push_data);
        mPushData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pushData();
            }
        });

        DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
        boolean accessAlreadyGranted = diaConnection.isAuthenticated();
        mReadData.setEnabled(accessAlreadyGranted);
        mPushData.setEnabled(accessAlreadyGranted);

        Button mSaveData = (Button) findViewById(R.id.save_data);
        mSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveData();
            }
        });

        mResult = (TextView) findViewById(R.id.diabetes_m_result);
    }

    public void onResume(){
        super.onResume();
        checkStatus();
    }

    private void checkStatus() {
        final DiabetesAppConnection diaConnection = new DiabetesAppConnection(MainActivity.this);

        DiabetesAppConnection.DiabetesMCheck checkStatus = diaConnection.checkDiabetesMApp();
        if (checkStatus != DiabetesAppConnection.DiabetesMCheck.OK) {
            //update UI id Diabetes:M doesn't support this API or is not installed
            mReadData.setEnabled(false);
            mPushData.setEnabled(false);
            String message = checkStatus == DiabetesAppConnection.DiabetesMCheck.NOT_FOUND ?
                    "Missing Diabetes:M app!" :
                    "Incompatible Diabetes:M version. Must be 5.0.5 or above!";
            mResult.setText(message);
            return;
        }

        readData();
    }

    private void saveData() {
        Intent intent = new Intent("com.mydiabetes.action.SAVE_LOG_ENTRY");
        intent.putExtra("DATETIME", System.currentTimeMillis());
        intent.putExtra("GLUCOSE", 8.5f);
        //intent.putExtra("ESTIMATED", true);//Use ESTIMATED parameter if this glucose check must not be used for calibration

        intent.putExtra("CARBS", 100f);//float
        intent.putExtra("PROTEIN", 50f);//float
        intent.putExtra("FAT", 10f);//float
        intent.putExtra("CALORIES", 150f);//float

        intent.putExtra("EXERCISE_INDEX", 10);//int
        intent.putExtra("EXERCISE_DURATION", 60);//int
        intent.putExtra("EXERCISE_COMMENT", "API EXERCISE");

        intent.putExtra("NOTES", "HELLO FROM API  EXAMPLE");
        startActivity(intent);
    }

    private void readData() {
        final DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
        diaConnection.requestSimpleData(new DiabetesAppConnection.IResultListener() {
            @Override
            public void onResult(Bundle resultData) {
                if (resultData.getString(DiabetesAppConnection.RESULT_KEY, "").equals(DiabetesAppConnection.RESULT_UNAUTHORIZED)) {
                    mResult.setText("Unauthorized");
                    mReadData.setEnabled(false);
                    mPushData.setEnabled(false);
                    return;
                }

                final Configuration configuration = DiabetesAppConnection.getConfiguration(resultData);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        @SuppressLint("SimpleDateFormat")
                        DateFormat df = new SimpleDateFormat(configuration.getDateFormat() + " " + configuration.getTimeFormat());

                        String result =
                                "DateFormat: "+configuration.getDateFormat()+"\n" +
                                "TimeFormat: "+configuration.getTimeFormat()+"\n" +
                                "Glucose Unit: "+configuration.getGlucoseUnit()+"\n" +
                                "Calibration glucose time: "+df.format(new Date(configuration.getCalibrationGlucoseTime()))+"\n" +
                                "Calibration glucose: "+configuration.getCalibrationGlucose()+"\n"
                                ;
                        mResult.setText(result);
                    }
                });
            }
        });
    }

    private void pushData() {
        List<LogEntry> pushData = new ArrayList<>();

        //Prepare the first entry at current time
        Calendar cal = Calendar.getInstance();
        LogEntry entry1 = new LogEntry();
        entry1.setDateTime(cal.getTimeInMillis());
        entry1.setGlucose(5.6f);
        entry1.setNote("API Example: Pushed normal glucose!");
        pushData.add(entry1);

        //Prepare the second entry at one hour ago. This entry is set as sensor entry!
        cal.add(Calendar.HOUR, -1);
        LogEntry entry2 = new LogEntry();
        entry2.setDateTime(cal.getTimeInMillis());
        entry2.setGlucose(8.9f);
        entry2.setSensor(true);
        entry2.setNote("API Example: Pushed sensor glucose!");
        pushData.add(entry2);

        //Instantiate DiabetesAppConnection and call pushData
        DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
        diaConnection.pushData(pushData, new DiabetesAppConnection.IResultListener() {
            @Override
            public void onResult(Bundle bundle) {
                //In callback check the result
                final String result = bundle.getString(DiabetesAppConnection.RESULT_KEY, "");

                if (result.equals(DiabetesAppConnection.RESULT_UNAUTHORIZED)) {
                    mResult.setText("Unauthorized");
                    mReadData.setEnabled(false);
                    mPushData.setEnabled(false);
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mResult.setText("PushData result = " + result);
                    }
                });
            }
        });
    }

    void requestAccess(boolean requestPushDataAccess) {
        DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
        diaConnection.requestAccess(this, requestPushDataAccess, ACCESS_REQUEST_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Check for the access request code
        if (requestCode==ACCESS_REQUEST_CODE) {
            //Create instance of DiabetesAppConnection so we can check the response from Diabetes:M
            DiabetesAppConnection diaConnection = new DiabetesAppConnection(MainActivity.this);
            //Call onActivityResult to decode for us the access status
            DiabetesAppConnection.AccessPermission res = diaConnection.onActivityResult(resultCode, data);
            if (res == DiabetesAppConnection.AccessPermission.GRANTED){
                //We must check for "pushDataPermission" extra so we can know if the push data permission is granted
                boolean isPushEnabled = data.getExtras().getBoolean("pushDataPermission", false);

                //initialize all the stuff related to Diabetes:M communication ...

                mResult.setText("Access granted! (" + (isPushEnabled ? "FULL ACCESS" : "READ ONLY") + ")");

                mReadData.setEnabled(true);
                mPushData.setEnabled(isPushEnabled);
            } else if (res == DiabetesAppConnection.AccessPermission.REJECTED){
                mResult.setText("Access rejected!");
            } else {
                mResult.setText("Canceled!");
            }
        }
    }
}
