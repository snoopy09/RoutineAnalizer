package jp.ac.titech.itpro.sdl.simplemap;

import com.google.android.gms.maps.model.LatLng;

public class HubenyDistance {

    //世界観測値系
    public static final double GRS80_A = 6378137.000;//長半径 a(m)
    public static final double GRS80_E2 = 0.00669438002301188;//第一遠心率  eの2乗

    public static double deg2rad(double deg){
        return deg * Math.PI / 180.0;
    }

    public static double calcDistance(LatLng latlng1, LatLng latlng2){
        double lat1 = latlng1.latitude;
        double lat2 = latlng2.latitude;
        double lng1 = latlng1.longitude;
        double lng2 = latlng2.longitude;

        double my = deg2rad((lat1 + lat2) / 2.0); //緯度の平均値
        double dy = deg2rad(lat1 - lat2); //緯度の差
        double dx = deg2rad(lng1 - lng2); //経度の差

        //卯酉線曲率半径を求める(東と西を結ぶ線の半径)
        double sinMy = Math.sin(my);
        double w = Math.sqrt(1.0 - GRS80_E2 * sinMy * sinMy);
        double n = GRS80_A / w;

        //子午線曲線半径を求める(北と南を結ぶ線の半径)
        double mnum = GRS80_A * (1 - GRS80_E2);
        double m = mnum / (w * w * w);

        //ヒュベニの公式
        double dym = dy * m;
        double dxncos = dx * n * Math.cos(my);
        return Math.sqrt(dym * dym + dxncos * dxncos);
    }
}
