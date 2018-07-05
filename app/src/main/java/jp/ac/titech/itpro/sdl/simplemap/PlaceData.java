package jp.ac.titech.itpro.sdl.simplemap;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;

public class PlaceData {
    private LatLng latlng;
    private Calendar calendar;

    public PlaceData (LatLng l) {
        latlng = l;
        calendar = Calendar.getInstance();
    }

    public LatLng get_latlng () {
        return latlng;
    }
    public Calendar get_calendar () {
        return calendar;
    }
}
