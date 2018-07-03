package jp.ac.titech.itpro.sdl.simplemap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final static String TAG = "MainActivity";

    private TextView infoView;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
//    private Button replaceButton;
    private PieChart mPieChart;

    private enum UpdatingState {STOPPED, REQUESTING, STARTED}

    private UpdatingState state = UpdatingState.STOPPED;

    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private final static int REQCODE_PERMISSIONS = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        infoView = findViewById(R.id.info_view);
//        MapFragment mapFragment =
//                (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
//        mapFragment.getMapAsync(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest();
//        locationRequest.setInterval(10000L);
        locationRequest.setFastestInterval(5000L);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "onLocationResult");
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    infoView.setText(getString(R.string.latlng_format,
                            latLng.latitude, latLng.longitude));
                    if (googleMap != null)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }
            }
        };

//        replaceButton = findViewById(R.id.replace_button);
        mPieChart = (PieChart) findViewById(R.id.pie_chart);
        setupPieChartView();
     }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (state != UpdatingState.STARTED && googleApiClient.isConnected())
            startLocationUpdate(true);
        else
            state = UpdatingState.REQUESTING;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        if (state == UpdatingState.STARTED)
            stopLocationUpdate();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");
        map.moveCamera(CameraUpdateFactory.zoomTo(15f));
        googleMap = map;
    }

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
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (reqPermission)
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQCODE_PERMISSIONS);
                else
                    Toast.makeText(this,
                            getString(R.string.toast_requires_permission_format, permission),
                            Toast.LENGTH_SHORT).show();
                return;
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        state = UpdatingState.STARTED;
    }

    @Override
    public void onRequestPermissionsResult(int reqCode,
                                           @NonNull String[] permissions, @NonNull int[] grants) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (reqCode) {
            case REQCODE_PERMISSIONS:
                startLocationUpdate(false);
                break;
        }
    }

    private void stopLocationUpdate() {
        Log.d(TAG, "stopLocationUpdate");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        state = UpdatingState.STOPPED;
    }

    public void onClickReplaceButton(View v) {
        setupPieChartView();

        Log.d(TAG, "onClickReplaceButton");
//        Log.d(TAG, String.valueOf(state));
//        if (googleApiClient.isConnected())
//            startLocationUpdate(true);
    }

    private void setupPieChartView() {
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

//    private void createPieChart() {
//        PieChart pieChart = (PieChart) findViewById(R.id.pie_chart);
//
//        pieChart.setDrawHoleEnabled(true); // 真ん中に穴を空けるかどうか
//        pieChart.setHoleRadius(50f);       // 真ん中の穴の大きさ(%指定)
////        pieChart.setHoleColorTransparent(true);
//        pieChart.setTransparentCircleRadius(55f);
//        pieChart.setRotationAngle(270);          // 開始位置の調整
//        pieChart.setRotationEnabled(true);       // 回転可能かどうか
//        pieChart.getLegend().setEnabled(true);   //
////        pieChart.setDescription("PieChart 説明");
//        pieChart.setData(createPieChartData());
//
////        // 更新
////        pieChart.invalidate();
////        // アニメーション
////        pieChart.animateXY(2000, 2000); // 表示アニメーション
//    }
//
//    // pieChartのデータ設定
//    private PieData createPieChartData() {
//        ArrayList<Entry> yVals = new ArrayList<>();
//        ArrayList<String> xVals = new ArrayList<>();
//        ArrayList<Integer> colors = new ArrayList<>();
//
//        xVals.add("A");
//        xVals.add("B");
//        xVals.add("C");
//
//        yVals.add(new Entry(20, 0));
//        yVals.add(new Entry(30, 1));
//        yVals.add(new Entry(50, 2));
//
//        PieDataSet dataSet = new PieDataSet((List)yVals, "Data");
//        dataSet.setSliceSpace(5f);
//        dataSet.setSelectionShift(1f);
//
//        // 色の設定
////        colors.add(ColorTemplate.COLORFUL_COLORS[0]);
////        colors.add(ColorTemplate.COLORFUL_COLORS[1]);
////        colors.add(ColorTemplate.COLORFUL_COLORS[2]);
//        colors.add(-2535286);
//        colors.add(-92921);
//        colors.add(-67720);
////        colors.add(-9787514);
////        colors.add(-13253935);
//        dataSet.setColors(colors);
//        dataSet.setDrawValues(true);
//
//        PieData data = new PieData(dataSet);
////        data.setValueFormatter(new PercentFormatter());
//
//        // テキストの設定
//        data.setValueTextSize(12f);
//        data.setValueTextColor(Color.WHITE);
//        return data;
//    }
}