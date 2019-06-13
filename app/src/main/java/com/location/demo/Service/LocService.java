package com.location.demo.Service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.location.demo.LocationModel;
import com.location.demo.Retrofit.APIClient;
import com.location.demo.Retrofit.APIInterface;
import java.util.Timer;
import java.util.TimerTask;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final long MIN_TIME_LOCATION_UPDATES = 30 * 1000;   // 30 sec
    public static final long OBD_TIME_LOCATION_UPDATES = 30 * 1000;   // 30 sec
    public static final long IDLE_TIME_LOCATION_UPDATES = 3600 * 1000;   // 1 hour
    public static APIInterface apiInterface;
    public static LocationRequest locationRequest;
    public static GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("tag", "---------onCreate Service");
        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                .permitDiskWrites()
                .build());
        StrictMode.setThreadPolicy(old);

        mTimer = new Timer();
        mTimer.schedule(timerTask, OBD_TIME_LOCATION_UPDATES, OBD_TIME_LOCATION_UPDATES );

        createLocationRequest(MIN_TIME_LOCATION_UPDATES);

        // check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Make it stick to the notification panel so it is less prone to get cancelled by the Operating System.
        return START_STICKY;
    }

    @SuppressLint("RestrictedApi")
    protected void createLocationRequest(long time) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(time);
        locationRequest.setFastestInterval(time);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    void StopService(){
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("onBind","onBind");
        return null;
    }

    public void onDestroy() {

        Log.i("tag", "---------onDestroy Service ");

        String packageName   = "com.location.demo"; // add project package name here..

      //  if(!isStopService) {  //
            Intent intent = new Intent(packageName);
            intent.putExtra("location", "torestore");
            sendBroadcast(intent);

      /*  }else{
            try {
                StopLocationUpdates();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/

    }

    protected void StopLocationUpdates() {
        try {
            if (mGoogleApiClient.isConnected()) {
                stopForeground(true);
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
                mGoogleApiClient.disconnect();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();
    }


    /* *
     * Method to verify google play services on the device
     * */

    public boolean checkPlayServices() {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                Log.d("GooglePlayServices", "UserResolvableError"  );
            } else {
                Log.d("GooglePlayServices", "This device is not supported." );
            }
            return false;
        }
        return true;
    }

    public void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "onConnected");
        try {
            requestLocationUpdates();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("onConnectionSuspended", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("onConnectionFailed", "onConnectionFailed");
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        try {
            String sLat = String.valueOf(latitude);
            String sLnt = String.valueOf(longitude);

            if(isNetworkAvailable(getApplicationContext())) {
                sendLocation(getApplicationContext(),sLat,sLnt);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        int GpsVehicleSpeed = (int) location.getSpeed() * 18 / 5;
        Log.d("Location speed", "------Current speed: " + GpsVehicleSpeed );
    }

    public Timer mTimer;

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {

            Log.e("Log", "--Running Location Service");
            createLocationRequest(IDLE_TIME_LOCATION_UPDATES);
        }
    };

    public static void showToast(Context context,String msg){
        Toast toast = Toast.makeText(context,msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public static void sendLocation(final Context context, String lat, String lng){
        apiInterface = APIClient.getClient().create(APIInterface.class);
        Call call = apiInterface.sendLocation("+919877627171", lat, lng);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if(response.isSuccessful()){
                    LocationModel model = (LocationModel) response.body();
                    if(model.isStatus()) {
                     //   showToast(context,model.getMessage());
                    }else {
                      //  showToast(context,model.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                call.cancel();
            }
        });
    }

}
