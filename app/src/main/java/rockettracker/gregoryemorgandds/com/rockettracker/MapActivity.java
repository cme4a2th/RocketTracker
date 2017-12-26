package rockettracker.gregoryemorgandds.com.rockettracker;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;

    private double lat;
    private double lon;
    private double mylat;
    private double mylon;
    private String marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

//        Bundle args = getIntent().getExtras();
//        lat = args.getDouble("latkey");
//        lon = args.getDouble("lonkey");
//        mylat = args.getDouble("mylatkey");
//        mylon = args.getDouble("mylonkey");
//        marker = args.getString("bearingkey");

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map_id);
        mapFragment.getMapAsync(this);
    }

    public void updatePositions(double mlat, double mlon, double rlat, double rlon) {
        lat = rlat;
        lon = rlon;
        mylat = mlat;
        mylon = mlon;

        LatLng me = new LatLng(mylat, mylon);
        LatLng rocket = new LatLng(lat, lon);
        map.clear();
        map.addMarker(new MarkerOptions().position(me).title("Me"));
        map.addMarker(new MarkerOptions().position(rocket).title(marker));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 17.0f));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }
}
