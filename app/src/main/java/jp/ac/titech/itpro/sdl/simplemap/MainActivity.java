package jp.ac.titech.itpro.sdl.simplemap;




import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MULTI_PERMISSIONS = 101;

    public boolean isAllowedLocation = false;
    public boolean isAllowedExternalWrite = false;

    private TextView textView;
    private InternalFileReadWrite fileReadWrite;
    private PieChart mPieChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        fileReadWrite = new InternalFileReadWrite(context);

        // API 23以上でパーミッシンの確認
        checkMultiPermissions();
    }

    // 位置情報許可の確認、外部ストレージのPermissionにも対応できるようにしておく
    private  void checkMultiPermissions(){
        int permissionLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        //int permissionExtStorage = ContextCompat.checkSelfPermission(this,
        //        Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> reqPermissions = new ArrayList<>();

        // permission が許可されているか確認
        if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
            isAllowedLocation = true;
            Log.d("debug","permissionLocation:GRANTED");
        }
        else{
            reqPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        //if (permissionExtStorage == PackageManager.PERMISSION_GRANTED) {
        //    isAllowedExternalWrite = true;
        //    Log.d("debug","permissionExtStorage:GRANTED");
        //}
        //else{
        //    reqPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //}
        if (!reqPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    reqPermissions.toArray(new String[reqPermissions.size()]),
                    REQUEST_MULTI_PERMISSIONS);
        }
        else{
            startLocationService();
        }
    }

    // 結果の受け取り
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_MULTI_PERMISSIONS) {
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].
                            equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            isAllowedLocation = true;
                        } else {
                            // それでも拒否された時の対応
                            Toast toast = Toast.makeText(this,
                                    "位置情報の許可がないので計測できません", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                    // 今回は使いません
                    else if (permissions[i].
                            equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            isAllowedExternalWrite = true;
                        } else {
                            // それでも拒否された時の対応
                            Toast toast = Toast.makeText(this,
                                    "外部書込の許可がないので書き込みできません", Toast.LENGTH_SHORT);
                            toast.show();

                        }
                    }
                }

                startLocationService();

            }
        }
    }

    private void startLocationService() {
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.log_text);

        Button buttonStart = findViewById(R.id.button_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), LocationService.class);

                // API 26 以降
                startForegroundService(intent);

                // Activityを終了させる
                finish();
            }
        });

        Button buttonLog = findViewById(R.id.button_log);
        buttonLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                textView.setText(fileReadWrite.readFile());
            }
        });

        Button buttonReset = findViewById(R.id.button_reset);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Serviceの停止
                Intent intent = new Intent(getApplication(), LocationService.class);
                stopService(intent);

                fileReadWrite.clearFile();
                textView.setText("");
            }
        });
    }

    private void setupPieChartView() {
        mPieChart = (PieChart) findViewById(R.id.pie_chart);
        mPieChart.setUsePercentValues(true);
//        mPieChart.setDescription("チャートの説明");

        Legend legend = mPieChart.getLegend();
        legend.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);

        // 円グラフに表示するデータ
        List<Float> values = Arrays.asList(40f, 30f, 20f, 10f);
        List<String> labels = Arrays.asList("A", "B", "C", "D");
        List<PieEntry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            entries.add(new PieEntry(values.get(i), labels.get(i)));
        }

        PieDataSet dataSet = new PieDataSet(entries, "昨日");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setDrawValues(true);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(12f);
        pieData.setValueTextColor(Color.WHITE);

        mPieChart.setData(pieData);
    }


}

//
//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.location.Location;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.MapFragment;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.model.LatLng;
//
//import android.graphics.Color;
//import android.graphics.DashPathEffect;
//import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//
//import com.github.mikephil.charting.charts.PieChart;
//import com.github.mikephil.charting.components.Legend;
//import com.github.mikephil.charting.data.PieEntry;
//import com.github.mikephil.charting.data.PieData;
//import com.github.mikephil.charting.data.PieDataSet;
//import com.github.mikephil.charting.formatter.PercentFormatter;
//import com.github.mikephil.charting.utils.ColorTemplate;
//
//import java.util.List;
//import java.util.Arrays;
//import java.util.ArrayList;
//import java.util.Calendar;
//
//
//public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
//        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
//    private final static String TAG = "MainActivity";
//
//    private Button replaceButton;
//    private PieChart mPieChart;
//
//    private enum UpdatingState {STOPPED, REQUESTING, STARTED}
//
//    private UpdatingState state = UpdatingState.STOPPED;
//
//    private final static String[] PERMISSIONS = {
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_FINE_LOCATION
//    };
//    private final static int REQCODE_PERMISSIONS = 1234;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.d(TAG, "onCreate");
//        setContentView(R.layout.activity_main);
//
////        MapFragment mapFragment =
////                (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
////        mapFragment.getMapAsync(this);
//
//
//        LocationTrackingService();
//        replaceButton = findViewById(R.id.replace_button);
//        mPieChart = (PieChart) findViewById(R.id.pie_chart);
//        setupPieChartView();
//     }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        Log.d(TAG, "onStart");
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        Log.d(TAG, "onResume");
//        if (state != UpdatingState.STARTED && googleApiClient.isConnected())
//            startLocationUpdate(true);
//        else
//            state = UpdatingState.REQUESTING;
//    }
//
//    @Override
//    protected void onPause() {
//        Log.d(TAG, "onPause");
//        if (state == UpdatingState.STARTED)
//            stopLocationUpdate();
//        super.onPause();
//    }
//
//    @Override
//    protected void onStop() {
//        Log.d(TAG, "onStop");
//        googleApiClient.disconnect();
//        super.onStop();
//    }
//
//    @Override
//    public void onMapReady(GoogleMap map) {
//        Log.d(TAG, "onMapReady");
//        map.moveCamera(CameraUpdateFactory.zoomTo(15f));
//        googleMap = map;
//    }
//
//    @Override
//    public void onConnected(@Nullable Bundle bundle) {
//        Log.d(TAG, "onConnected");
//        if (state == UpdatingState.REQUESTING)
//            startLocationUpdate(true);
//    }
//
//    @Override
//    public void onConnectionSuspended(int i) {
//        Log.d(TAG, "onConnectionSuspended");
//    }
//
//    @Override
//    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
//        Log.d(TAG, "onConnectionFailed");
//    }
//
//    private void startLocationUpdate(boolean reqPermission) {
//        Log.d(TAG, "startLocationUpdate");
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
//        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
//        state = UpdatingState.STARTED;
//    }
//
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
//
//    private void stopLocationUpdate() {
//        Log.d(TAG, "stopLocationUpdate");
//        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
//        state = UpdatingState.STOPPED;
//    }
//
//    public void onClickReplaceButton(View v) {
////        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
//
//        Log.d(TAG, "onClickReplaceButton");
//        Intent intent = new Intent(this, LocationTrackingService.class);
//        stopService(intent);
//
////        Log.d(TAG, String.valueOf(state));
////        if (googleApiClient.isConnected())
////            startLocationUpdate(true);
//    }
//
//    private void setupPieChartView() {
//        mPieChart.setUsePercentValues(true);
////        mPieChart.setDescription("チャートの説明");
//
//        Legend legend = mPieChart.getLegend();
//        legend.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
//
//        // 円グラフに表示するデータ
//        List<Float> values = Arrays.asList(40f, 30f, 20f, 10f);
//        List<String> labels = Arrays.asList("A", "B", "C", "D");
//        List<PieEntry> entries = new ArrayList<>();
//        for (int i = 0; i < 4; i++) {
//            entries.add(new PieEntry(values.get(i), labels.get(i)));
//        }
//
//        PieDataSet dataSet = new PieDataSet(entries, "昨日");
//        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
//        dataSet.setDrawValues(true);
//
//        PieData pieData = new PieData(dataSet);
//        pieData.setValueFormatter(new PercentFormatter());
//        pieData.setValueTextSize(12f);
//        pieData.setValueTextColor(Color.WHITE);
//
//        mPieChart.setData(pieData);
//    }
//
// //    private void createPieChart() {
////        PieChart pieChart = (PieChart) findViewById(R.id.pie_chart);
////
////        pieChart.setDrawHoleEnabled(true); // 真ん中に穴を空けるかどうか
////        pieChart.setHoleRadius(50f);       // 真ん中の穴の大きさ(%指定)
//////        pieChart.setHoleColorTransparent(true);
////        pieChart.setTransparentCircleRadius(55f);
////        pieChart.setRotationAngle(270);          // 開始位置の調整
////        pieChart.setRotationEnabled(true);       // 回転可能かどうか
////        pieChart.getLegend().setEnabled(true);   //
//////        pieChart.setDescription("PieChart 説明");
////        pieChart.setData(createPieChartData());
////
//////        // 更新
//////        pieChart.invalidate();
//////        // アニメーション
//////        pieChart.animateXY(2000, 2000); // 表示アニメーション
////    }
////
////    // pieChartのデータ設定
////    private PieData createPieChartData() {
////        ArrayList<Entry> yVals = new ArrayList<>();
////        ArrayList<String> xVals = new ArrayList<>();
////        ArrayList<Integer> colors = new ArrayList<>();
////
////        xVals.add("A");
////        xVals.add("B");
////        xVals.add("C");
////
////        yVals.add(new Entry(20, 0));
////        yVals.add(new Entry(30, 1));
////        yVals.add(new Entry(50, 2));
////
////        PieDataSet dataSet = new PieDataSet((List)yVals, "Data");
////        dataSet.setSliceSpace(5f);
////        dataSet.setSelectionShift(1f);
////
////        // 色の設定
//////        colors.add(ColorTemplate.COLORFUL_COLORS[0]);
//////        colors.add(ColorTemplate.COLORFUL_COLORS[1]);
//////        colors.add(ColorTemplate.COLORFUL_COLORS[2]);
////        colors.add(-2535286);
////        colors.add(-92921);
////        colors.add(-67720);
//////        colors.add(-9787514);
//////        colors.add(-13253935);
////        dataSet.setColors(colors);
////        dataSet.setDrawValues(true);
////
////        PieData data = new PieData(dataSet);
//////        data.setValueFormatter(new PercentFormatter());
////
////        // テキストの設定
////        data.setValueTextSize(12f);
////        data.setValueTextColor(Color.WHITE);
////        return data;
////    }
// private void LocationTrackingService() {
//     Log.d(TAG, "Service in " + Thread.currentThread());
//     Intent intent = new Intent(this, LocationTrackingService.class);
//     intent.putExtra(LocationTrackingService.EXTRA_MYARG, "Hello, Service");
//     startService(intent);
// }
//
//}
//
