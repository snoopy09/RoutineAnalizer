package jp.ac.titech.itpro.sdl.simplemap;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;

public class MyLocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private final static String TAG = "MyLocationService";

    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private enum UpdatingState {STOPPED, REQUESTING, STARTED}
    private UpdatingState state = UpdatingState.STOPPED;

    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private final static int REQCODE_PERMISSIONS = 1234;


    private Context context;
    private InternalFileReadWrite fileReadWrite;

    public MyLocationService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        context = getApplicationContext();
        // 内部ストレージにログを保存
        fileReadWrite = new InternalFileReadWrite(context);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // to do something
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5 * 60 * 1000L);
        locationRequest.setFastestInterval(0L);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "onLocationResult");
                StringBuilder strBuf = new StringBuilder();
//                strBuf.append("getLocation\n");
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    strBuf.append(Calendar.getInstance().get(Calendar.DATE) + ",");
                    strBuf.append(latLng.latitude + ",");
                    strBuf.append(latLng.longitude);
                    strBuf.append("\n");
                }
                fileReadWrite.writeLogFile(strBuf.toString());
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        startLocationUpdate(true);
          return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopLocationUpdate();
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


//    @Override
//    public void onMapReady(GoogleMap map) {
//        Log.d(TAG, "onMapReady");
////        map.moveCamera(CameraUpdateFactory.zoomTo(15f));
//    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
        if (state == UpdatingState.REQUESTING)
            startLocationUpdate(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }

    private void startLocationUpdate(boolean reqPermission) {
        Log.d(TAG, "startLocationUpdate");
//        for (String permission : PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(this, permission)
//                    != PackageManager.PERMISSION_GRANTED) {
//                if (reqPermission)
//                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQCODE_PERMISSIONS);
//                else
//                    Toast.makeText(this,
//                            getString(R.string.toast_requires_permission_format, permission),
//                            Toast.LENGTH_SHORT).show();
//                return;
//            }
//        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        state = UpdatingState.STARTED;
    }

//    @Override
//    public void onRequestPermissionsResult(int reqCode,
//                                           @NonNull String[] permissions, @NonNull int[] grants) {
//        Log.d(TAG, "onRequestPermissionsResult");
//        switch (reqCode) {
//            case REQCODE_PERMISSIONS:
//                startLocationUpdate(false);
//                break;
//        }
//    }

    private void stopLocationUpdate() {
        Log.d(TAG, "stopLocationUpdate");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        state = UpdatingState.STOPPED;
    }

 }
