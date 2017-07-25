# Diabetes:M Add-on example
## Introduction
This is an example app which demonstrates how to exchange data with Diabetes:M application for Android.

If you need to open the Log Entry form and pass glucose value to save, you don't have to request for any permissions and use the API at all. You simply can pass the intent as shown below:
```java
Intent intent = new Intent("com.mydiabetes.action.SAVE_LOG_ENTRY");
intent.putExtra("DATETIME", System.currentTimeMillis());
intent.putExtra("GLUCOSE", 8.5f);
//intent.putExtra("ESTIMATED", true);//Use ESTIMATED parameter if this glucose check must not be used for calibration
startActivity(intent);
```
If need to read the configuration and last glucose value and/or push some data silently, then you must use the provided API. 
## How to start
The easiest way is to clone this repository and use it as a seed project. 

If you already have an existing project then you must copy the library module located at app/libs/diabetes_m_addon_api_1.0.aar to your project app/libs folder.

To include the api library you have to add the following lines to your app gradle file:
```groovy
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
 
    //other dependencies
     
    compile 'com.google.code.gson:gson:2.7'
    compile 'com.diabetesm.addons.api:diabetes_m_addon_api_1.0@aar'
}
```
Add next lines in your proguard-rules file:
```
-keep class com.diabetesm.addons.api.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
```

Create instance of DiabetesAppConnection and check for Diabetes:M package status:
```java
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
```
Then request access from Diabetes:M app as shown below:
```java
DiabetesAppConnection diaConnection = new DiabetesAppConnection(this);
diaConnection.requestAccess(this, requestPushDataAccess, ACCESS_REQUEST_CODE);
```
Handle the result in onActivityForResult:
```java
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
```
## Read configuration data and last glucose value
If you are granted access permission then the configuration data can be read with:  
```java
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
```
## Push data silently
To push data silently you must prepare the list with log entries to save. You may save only limited data to Diabetes:M application which is:
* entry date/time
* glucose reading in mmol/l
* bolus insulin units 
* basal insulin units
* carbs taken
* fat taken
* protein taken
* calories taken
* note

If your LogEntry contains sensor glucose reading then use setSensor(isSensor) to specify this.

In the following code 2 entries are created - one normal glucose check and one sensor glucose check 
```java
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
```
To push this data we call pushData method from the api instance:
```java
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
```
