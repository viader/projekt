package com.daniel.czaterv2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Bundle bundle;
    private Intent intent;
    private List<CzatListResponseDetails> czatListResponseDetailses = new ArrayList<>();
    private LatLng myPosition;
    private int czatRadius = 0;
    private CzatProperties czat1;
    private CzatProperties czat2;
    private CzatProperties czat3;
    private Marker marker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        czatListResponseDetailses = App.getInstance().getCzatListResponseDetailses();
        myPosition = App.getInstance().getMyPosition();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(Marker marker) {
                String id = marker.getId();
                Intent intent = new Intent(getApplicationContext(),CzatActivity.class);
                intent.putExtra("id",id);
                startActivity(intent);
            }
        });
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 10));
        for (int i = 0; i<czatListResponseDetailses.size();i++){
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(czatListResponseDetailses.get(i).getLatitude(),czatListResponseDetailses.get(i).getLongitude()))
                    .title(czatListResponseDetailses.get(i).getName()));
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(czatListResponseDetailses.get(i).getLatitude(),czatListResponseDetailses.get(i).getLongitude()))
                    .radius(czatListResponseDetailses.get(i).getRangeInMeters())
                    .strokeColor(Color.RED)
                    .strokeWidth(5));
        }
    }

    //------------------- KONIEC OnMapReady ----------------------------------

    private void setExamplesChats(){
        czat1 = new CzatProperties();
        czat1.setName("Czat 1");
        czat1.setRange(5000);
        czat1.setLatitude(51.227863);
        czat1.setLongitude(22.434850);
        czat1.setMaxUsers(10);

        czat2 = new CzatProperties();
        czat2.setName("Czat 2");
        czat2.setRange(10000);
        czat2.setLatitude(51.228430);
        czat2.setLongitude(22.527721);
        czat2.setMaxUsers(10);

        czat3 = new CzatProperties();
        czat3.setName("Czat 3");
        czat3.setRange(15000);
        czat2.setLatitude(51.285379);
        czat2.setLongitude(22.598139);
        czat3.setMaxUsers(10);
    }

    private void setExamplesMarkers(){
        Marker marker1 = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(czat1.getLatitude(),czat1.getLongitude()))
                .title("Tutaj będzie centrum czatu"));
        Circle circle1 = mMap.addCircle(new CircleOptions()
                .center(new LatLng(czat1.getLatitude(),czat1.getLongitude()))
                .radius(czatRadius)
                .strokeColor(Color.RED)
                .strokeWidth(3));
        Marker marker2 = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(czat2.getLatitude(),czat2.getLongitude()))
                .title("Tutaj będzie centrum czatu"));
        Circle circle2 = mMap.addCircle(new CircleOptions()
                .center(new LatLng(czat2.getLatitude(),czat2.getLongitude()))
                .radius(czatRadius)
                .strokeColor(Color.RED)
                .strokeWidth(3));
        Marker marker3 = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(czat3.getLatitude(),czat3.getLongitude()))
                .title("Tutaj będzie centrum czatu"));
        Circle circle3 = mMap.addCircle(new CircleOptions()
                .center(new LatLng(czat3.getLatitude(),czat3.getLongitude()))
                .radius(czatRadius)
                .strokeColor(Color.RED)
                .strokeWidth(3));
    }
}

