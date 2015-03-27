# omh-android-client-lib
Android library project that enables authentication and upload of  data-points to Ohmage OMH DSU. 
# See [demo_app](https://github.com/smalldatalab/android-omh-dsu-client-lib/tree/master/demo_app/) for usage example.
# Getting Started
* Before you start, make sure you have obtain a pair of DSU client_id and secret from your DSU admin.
* Register your app's Application ID and (debug and release) SHA1 key in [Google Developer Console](https://console.developers.google.com/project). (See Step 1. in this [tutalrial](https://developers.google.com/+/mobile/android/getting-started))
* Add **omhClientLib-release-x.x.aar** to your */your_app/libs* folder.
* In *build.gradle* of your app, add the following dependencies:
```gradle
dependencies {
 
    /** For omh client library **/
    compile(name:'omhclient_library-release-1.0', ext:'aar')
    
    // for google sign-in (4.4 or higher)
    compile 'com.google.android.gms:play-services-identity:7.0.0'
    compile 'com.google.android.gms:play-services-plus:7.0.0'
    // for http client
    compile 'com.squareup.okhttp:okhttp:2.3.0'
    // for db
    compile 'com.github.satyan:sugar:1.3'
    // for datetime
    compile 'joda-time:joda-time:2.3'
}
```
* In your *AndroidManifes.xml*, add attribute ```android:name="com.orm.SugarApp" ``` to the application tag. This will replace the default Application class with the SurgarApp to enble the [Surgar ORM](http://satyan.github.io/sugar/). For example:

```xml
<!-- The Application class is set to com.orm.SugarApp for the ORM to work.-->
<application
    android:name="com.orm.SugarApp"
    android:allowBackup="true"
    android:icon="@drawable/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/AppTheme">
    
    .....
</application>
```
** You can also extend the SurgarApp class if you need to further customize the Aplplication class.
  
* In your *strings.xml* file, add a string value named **dsu_provider_authorities**. This value is used to uniquely identify an internal Content Provider and so it must be *unique* for each app to prevent conflict.
```xml
<!--IMPORTANT: dsu_provider_authorities should be unique for your app! -->
<string name="dsu_provider_authorities">io.smalldatalab.dsu.demo</string>

<!-- Optionally replace these to customize the dialog -->
<string name="signin_progress_dialog_title">Sign in (Customized Message)</string>
<string name="signin_progress_dialog_message">We are signing you in…  (Customized Message)</string>
```
* Create DSUCLient object in your app with the DSU's url/client_id/secret configurated:
```java
final DSUClient dsuClient =
        new DSUClient(
                "https://lifestreams.smalldata.io/dsu", // dsu url
                "io.smalldatalab.dummy", // dus client id
                "xEUJgIdS2f12jmYomzEH", // dsu secret
                context // Context
        );
```
* SignIn/SignOut the user with the following methods (They involve blocking network call. Don't run them in the main UI thread.):
```java
// sign in (run this in an Activity)
boolean success = dsuClient.blockingGoogleSignIn(activity);

// sign out
boolean success = dsuClient.blockingSignOut();
```
* Check if the user is signed in
```java
dsuClient.isSignedIn() ? "Signed in" : "Not Signed In";
```
* Create and save a data point using DSUDataPointBuilder:
```java
DSUDataPoint datapoint =
        new DSUDataPointBuilder()
                .setSchemaNamespace("io.smalldatalab")
                .setSchemaName("dummy")
                .setSchemaVersion("1.0")
                .setBody("{\"dummy\":0}") // or .setBody(new JSONObject("......"))
                .setCreationDateTime(new DateTime()) // optional
                .setAcquisitionSource("dummy") // optional
                .setAcquisitionModality("sensed") // or "self_reported" , also optional
                .createDSUDataPoint();
datapoint.save(); // save the data point to the local database
```
* Data points saved in the local database will be automatically uploaded to the DSU. You can also force-upload them by:
```java
dsuClient.forceSync()
```
