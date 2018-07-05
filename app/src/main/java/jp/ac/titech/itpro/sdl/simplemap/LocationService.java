package jp.ac.titech.itpro.sdl.simplemap;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class LocationService extends Service implements LocationListener{

    private LocationManager locationManager;
    private Context context;

    private static final int MinTime = 1000;
    private static final float MinDistance = 50;

    private InternalFileReadWrite fileReadWrite;


    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
        // 内部ストレージにログを保存
        fileReadWrite = new InternalFileReadWrite(context);

        // LocationManager インスタンス生成
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int requestCode = 0;
        String channelId = "default";
        String title = context.getString(R.string.app_name);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, requestCode,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // ForegroundにするためNotificationが必要、Contextを設定
        NotificationManager notificationManager =
                (NotificationManager)context.
                        getSystemService(Context.NOTIFICATION_SERVICE);

        // Notification　Channel 設定
        NotificationChannel channel = new NotificationChannel(
                channelId, title , NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Silent Notification");
        // 通知音を消さないと毎回通知音が出てしまう
        // この辺りの設定はcleanにしてから変更
        channel.setSound(null,null);
        // 通知ランプを消す
        channel.enableLights(false);
        channel.setLightColor(Color.BLUE);
        // 通知バイブレーション無し
        channel.enableVibration(false);

        if(notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(context, channelId)
                    .setContentTitle(title)
                    // 本来なら衛星のアイコンですがandroid標準アイコンを設定
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setContentText("GPS")
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .build();

            // startForeground
            startForeground(1, notification);
        }

        startGPS();

        return START_NOT_STICKY;
    }


    protected void startGPS() {
        StringBuilder strBuf = new StringBuilder();
        strBuf.append("startGPS\n");

        Log.d("debug", "startGPS");

        final boolean gpsEnabled
                = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            // GPSを設定するように促す
            enableLocationSettings();
        }

        if (locationManager != null) {
            Log.d("debug", "locationManager.requestLocationUpdates");
            try {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)!=
                        PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MinTime, MinDistance, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            strBuf.append("locationManager=null\n");
        }

    }


    @Override
    public void onLocationChanged(Location location) {

        StringBuilder strBuf = new StringBuilder();

        strBuf.append("----------\n");

        String str = "Latitude = " +String.valueOf(location.getLatitude()) + "\n";
        strBuf.append(str);

        str = "Longitude = " + String.valueOf(location.getLongitude()) + "\n";
        strBuf.append(str);

        str = "Accuracy = " + String.valueOf(location.getAccuracy()) + "\n";
        strBuf.append(str);

        str = "Altitude = " + String.valueOf(location.getAltitude()) + "\n";
        strBuf.append(str);

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        String currentTime = sdf.format(location.getTime());

        str = "Time = " + currentTime + "\n";
        strBuf.append(str);

        str = "Speed = " + String.valueOf(location.getSpeed()) + "\n";
        strBuf.append(str);

        str = "Bearing = " + String.valueOf(location.getBearing()) + "\n";
        strBuf.append(str);

        strBuf.append("----------\n");

        Log.d("debug", strBuf.toString());

        fileReadWrite.writeFile(strBuf.toString());
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        StringBuilder strBuf = new StringBuilder();

        switch (status) {
            case LocationProvider.AVAILABLE:
                //strBuf.append("LocationProvider.AVAILABLE\n");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                strBuf.append("LocationProvider.OUT_OF_SERVICE\n");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                strBuf.append("LocationProvider.TEMPORARILY_UNAVAILABLE\n");
                break;
        }

        fileReadWrite.writeFile(strBuf.toString());

    }

    private void enableLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(settingsIntent);
    }

    private void stopGPS(){
        if (locationManager != null) {
            // update を止める
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(this);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        stopGPS();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}