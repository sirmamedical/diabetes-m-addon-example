# Diabetes:M Add-on example

##### Not ready yet!!! This API is under development and production version of Diabetes:M doesn't support it yet.

## Introduction
This is an example app which demonstrates how to exchange data with Diabetes:M application for Android.
 
## How to start
The easiest way is to clone this repository and use it as a seed project.

To include the api library you have to add the following lines to your app gradle file:

```
dependencies {
    //other dependencies
    
    compile 'com.google.code.gson:gson:2.7'
    compile 'com.diabetesm.addons.api:diabetes_m_addon_api@aar'
}
```

Create instance of DiabetesAppConnection and request access from Diabetes:M app as shown below:
```
DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
diaConnection.requestAccess(this, requestPushDataAccess, ACCESS_REQUEST_CODE);
```

Handle the result in onActivityForResult:
```
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode==ACCESS_REQUEST_CODE) {
        DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
        int res = diaConnection.onActivityResult(resultCode, data);
        if (res==0){
            //do stuff if access is granted
            mResult.setText("Access granted!");
        } else if (res==1){
            //do stuff if access is rejected
            mResult.setText("Access rejected!");
        } else {
            //do stuff if request is canceled
            mResult.setText("Canceled!");
        }
    }
}
```

If you are granted access permission then the configuration data can be read with:  
```
final DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
diaConnection.requestSimpleData(new DiabetesAppConnection.IResultListener() {
    @Override
    public void onResult(Bundle resultData) {
        if (resultData.getString("result", "").equals("Unauthorized")) {
            mResult.setText("Unauthorized");
            return;
        }

        final Configuration configuration = DiabetesAppConnection.getConfiguration(resultData);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //update UI 
                String result =
                        "DateFormat: "+configuration.getDateFormat()+"\n" +
                        "TimeFormat: "+configuration.getTimeFormat()+"\n" +
                        "Glucose Unit: "+configuration.getGlucoseUnit()+"\n" +
                        "Calibration glucose time: "+configuration.getmCalibrationGlucoseTime()+"\n" +
                        "Calibration glucose: "+configuration.getmCalibrationGlucose()+"\n"
                        ;
                mResult.setText(result);
            }
        });
    }
});
```
