package jp.ac.titech.itpro.sdl.simplemap;




import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final static String TAG = "MainActivity";
    private static final int REQUEST_MULTI_PERMISSIONS = 101;

    public boolean isAllowedLocation = false;
    public boolean isAllowedExternalWrite = false;

    private TextView infoView;
    private Button buttonRegister;

    private InternalFileReadWrite fileReadWrite;
    private PieChart mPieChart;
    private GoogleMap googleMap;
    private Marker marker;
//    private GoogleApiClient googleApiClient;

    private Map<LatLng, Integer> placeMap = new HashMap<>();
    private Map<LatLng, Integer> unknown = new HashMap<>();
    int[] counter = new int[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoView = findViewById(R.id.info_view);
        buttonRegister = findViewById(R.id.button_register);
        buttonRegister.setEnabled(false);

        Context context = getApplicationContext();
        fileReadWrite = new InternalFileReadWrite(context);

        // API 23以上でパーミッシンの確認
        checkMultiPermissions();

        // コンテンツ部分のLayoutを取ってくる
        LinearLayout layout = (LinearLayout) findViewById(R.id.target);
        // 内容を全部消す
        layout.removeAllViews();

        checkMem();
        checkLog();

        if(unknown.size() > 0){
            infoView.setText("ここはどこですか？場所登録ボタンを押して登録してください");
            // 内容を全部消す
            layout.removeAllViews();
            // test_sub.xmlに変更する
            getLayoutInflater().inflate(R.layout.map_layout, layout);
            setupMapView();
         }else{
             // test_sub.xmlに変更する
            getLayoutInflater().inflate(R.layout.chart_layout, layout);
            setupPieChartView();
        }
    }

    // 位置情報許可の確認、外部ストレージのPermissionにも対応できるようにしておく
    private void checkMultiPermissions() {
        int permissionLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        //int permissionExtStorage = ContextCompat.checkSelfPermission(this,
        //        Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> reqPermissions = new ArrayList<>();

        // permission が許可されているか確認
        if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
            isAllowedLocation = true;
            Log.d("debug", "permissionLocation:GRANTED");
        } else {
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
        } else {
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

    private void showDialog () {
        final String[] items = {"自宅", "ラボ", "その他"};
        new AlertDialog.Builder(this)
                .setTitle("どの場所に該当しますか？")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        marker.remove();
                        LatLng latlng = new ArrayList<>(unknown.keySet()).get(0);
                        placeMap.put(latlng, (Integer)which);
                        Context context = getApplicationContext();
                        // 内部ストレージにログを保存
                        fileReadWrite = new InternalFileReadWrite(context);
                        StringBuilder strBuf = new StringBuilder();
                        strBuf.append(which + ",");
                        strBuf.append(latlng.latitude + ",");
                        strBuf.append(latlng.longitude);
                        strBuf.append("\n");
                        fileReadWrite.writeLMemFile(strBuf.toString());
                        Log.d("debug", placeMap.toString());

                        Log.d("debug", unknown.toString());
                        for(int i=0;i<unknown.size();i++){
                            LatLng key = new ArrayList<>(unknown.keySet()).get(i);
                            Integer value = new ArrayList<>(unknown.values()).get(i);
                            if (compareLatLng(key) >= 0) {
                                counter[compareLatLng(key)] += value;
                                unknown.remove(key);
                                i--;
                            }
                        }
                        if(unknown.size() > 0){
                            if (googleMap != null) {
                                // marker 追加
                                marker = googleMap.addMarker(new MarkerOptions().position(new ArrayList<>(unknown.keySet()).get(0)));
                                googleMap.animateCamera(CameraUpdateFactory.newLatLng(new ArrayList<>(unknown.keySet()).get(0)));
                            }
                        }else{
                            buttonRegister.setEnabled(false);

                            // コンテンツ部分のLayoutを取ってくる
                            LinearLayout layout = (LinearLayout) findViewById(R.id.target);
                            // 内容を全部消す
                            layout.removeAllViews();
                            // test_sub.xmlに変更する
                            getLayoutInflater().inflate(R.layout.chart_layout, layout);
                            setupPieChartView();
                        }
                    }
                })
                .show();
    }

    private void showConfirmation () {
        new AlertDialog.Builder(this)
                .setTitle("Alart")
                .setMessage("本当に削除してもいいですか？")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // OK button pressed
                            fileReadWrite.clearFile();
                         infoView.setText("消去しました");
                         placeMap = new HashMap<>();
                         unknown = new HashMap<>();
                         counter = new int[4];
                         setupPieChartView();

                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startLocationService() {
//        Log.d("debug", "startService");

        Intent intent = new Intent(getApplication(), MyLocationService.class);
        startService(intent);



        Button buttonRegister = findViewById(R.id.button_register);
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("debug", "push");
                showDialog();
            }
        });

        Button buttonClear = findViewById(R.id.button_clear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("debug", "push");
                showConfirmation();
            }
        });


        Button buttonStop = findViewById(R.id.button_stop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Serviceの停止
                Intent intent = new Intent(getApplication(), MyLocationService.class);
                stopService(intent);
                infoView.setText("位置情報の取得を停止しました。再開するにはアプリを再起動してください");

//                fileReadWrite.clearFile();
//                infoView.setText("消去しました");

            }
        });

//        Button button = findViewById(R.id.button_start);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Context context = getApplicationContext();
//                fileReadWrite = new InternalFileReadWrite(context);
//                String log = fileReadWrite.readLogFile();
//                String[] logs = log.split("\n");
//                ArrayList<LatLng> list = new ArrayList<>();
//                for (String line : logs) {
//                    String[] elements = line.split(",");
//                    if(elements.length == 3) {
//                        LatLng latlng = new LatLng(Double.parseDouble(elements[1]), Double.parseDouble(elements[2]));
//                        list.add(latlng);
//                    }
//                }
//                Log.d("debug", list.toString());
//                Log.d("debug", Arrays.toString(counter));
//                Log.d("debug", placeMap.toString());
//                }
//        });
    }

    private void setupPieChartView() {
        mPieChart = (PieChart) findViewById(R.id.pie_chart);
        mPieChart.setUsePercentValues(true);

        Legend legend = mPieChart.getLegend();
        legend.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
        int sum = 0;
        int max_key = 0;
        for(int i=0;i<counter.length;i++){
            sum += counter[i];
            if(counter[i] > counter[max_key]) max_key = i;
        }

        // 円グラフに表示するデータ
        List<Float> values = Arrays.asList((float)counter[0], (float)counter[1], (float)counter[2], (float)counter[3]);
        List<String> labels = Arrays.asList("自宅", "ラボ", "その他", "移動中");
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

        if(sum == 0) return;
        if(counter[max_key]/sum > 0.5) {
                switch (max_key) {
                    case 0: infoView.setText("もしかして引きこもり…？");break;
                    case 1: infoView.setText("ラボ畜かも！？！？");break;
                    case 2: infoView.setText("ちょっとわからん");break;
                    case 3: infoView.setText("アクティブな生活だね");break;
                }
            }else{
                infoView.setText("まあまあのバランスっぽい");
            }


    }

    private void setupMapView() {
        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");
        map.moveCamera(CameraUpdateFactory.zoomTo(15f));
        googleMap = map;
        if (googleMap != null) {
            // marker 追加
            marker = googleMap.addMarker(new MarkerOptions().position(new ArrayList<>(unknown.keySet()).get(0)));
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(new ArrayList<>(unknown.keySet()).get(0)));
            buttonRegister.setEnabled(true);
        }
    }


    private void checkMem() {
        Context context = getApplicationContext();
        fileReadWrite = new InternalFileReadWrite(context);
        String mem = fileReadWrite.readMemFile();
        String[] mems = mem.split("\n");
        for (String line : mems) {
            String[] elements = line.split(",");
            if (elements.length == 3) placeMap.put(
                    new LatLng(Double.parseDouble(elements[1]), Double.parseDouble(elements[2])),
                    Integer.parseInt(elements[0]));
        }
        Log.d("debug", placeMap.toString());
    }

    private void checkLog() {
        Context context = getApplicationContext();
        fileReadWrite = new InternalFileReadWrite(context);
        String log = fileReadWrite.readLogFile();
        String[] logs = log.split("\n");
        ArrayList<LatLng> list = new ArrayList<>();
        for (String line : logs) {
            String[] elements = line.split(",");
            if(elements.length == 3) {
                LatLng latlng = new LatLng(Double.parseDouble(elements[1]), Double.parseDouble(elements[2]));
                list.add(latlng);
            }
        }
        Log.d("debug", list.toString());
        int count = 1;
        for(int i=0;i<list.size();) {
            while(i+count<list.size() && list.get(i).equals(list.get(i+count))){
                count++;
            }
            if(count == 1) {
                counter[3]++;
            }else {
                if (compareLatLng(list.get(i)) >= 0) {
                    counter[compareLatLng(list.get(i))] += count;
                } else {
                    Integer value = unknown.get(list.get(i));
                    unknown.put(list.get(i), (value == null) ? count : value+count);
                }
            }
            i += count;
            count = 1;
        }

    }

    private int compareLatLng (LatLng latlng) {
        for (Map.Entry<LatLng, Integer> entry : placeMap.entrySet()) {
            if (HubenyDistance.calcDistance(entry.getKey(), latlng) < 10) return entry.getValue();
        }
        return -1;
    }


}

