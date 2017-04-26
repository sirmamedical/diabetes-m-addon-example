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

        mResult = (TextView) findViewById(R.id.diabetes_m_result);
    }

    private void readData() {
        final DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
        diaConnection.requestSimpleData(new DiabetesAppConnection.IResultListener() {
            @Override
            public void onResult(Bundle resultData) {
                if (resultData.getString(DiabetesAppConnection.RESULT_KEY, "").equals(DiabetesAppConnection.RESULT_UNAUTHORIZED)) {
                    mResult.setText("Unauthorized");
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

        Calendar cal = Calendar.getInstance();
        LogEntry entry1 = new LogEntry();
        entry1.setDateTime(cal.getTimeInMillis());
        entry1.setGlucose(5.6f);
        entry1.setNote("API Example: Pushed normal glucose!");
        pushData.add(entry1);

        cal.add(Calendar.HOUR, -1);
        LogEntry entry2 = new LogEntry();
        entry2.setDateTime(cal.getTimeInMillis());
        entry2.setGlucose(8.9f);
        entry2.setSensor(true);
        entry2.setNote("API Example: Pushed sensor glucose!");
        pushData.add(entry2);

        DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
        diaConnection.pushData(pushData, new DiabetesAppConnection.IResultListener() {
            @Override
            public void onResult(Bundle bundle) {
                final String result = bundle.getString("result", "");
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

        if (requestCode==ACCESS_REQUEST_CODE) {
            DiabetesAppConnection diaConnection = new DiabetesAppConnection(MainActivity.this);
            DiabetesAppConnection.AccessPermission res = diaConnection.onActivityResult(resultCode, data);
            if (res == DiabetesAppConnection.AccessPermission.GRANTED){
                mResult.setText("Access granted!");
                mPushData.setEnabled(true);
                mReadData.setEnabled(true);
            } else if (res == DiabetesAppConnection.AccessPermission.REJECTED){
                mResult.setText("Access rejected!");
            } else {
                mResult.setText("Canceled!");
            }
        }
    }
}
