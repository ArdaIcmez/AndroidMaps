package com.example.arda.mapspls;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import android.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Location lastLocation;
    private LocationListener gpsLocationListener;

    // GPS Location settings
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0.1f; // 1 meter
    private static final long MIN_TIME_BW_UPDATES = 1000*3; // 3 seconds

    //User personal settings
    private UserDrawing thisUser;
    private HashMap<String,UserDrawing> drawingList = new HashMap<String,UserDrawing>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) { promptGPS();}

    }

    //GPS and permission functions
    private void promptGPS() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
    private void getGPSPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                1);
    }

    //Initialize location and drawing
    private void getInitLocation(){
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            getGPSPermission();
        }
        else{ // Permission exists, get the starting coordonate if possible

            //Puts marker on the map, enables blue dot
            mMap.setMyLocationEnabled(true);

            //Add GPS Listener to LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, gpsLocationListener);

            //Get last known location
            lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            //Add location as the starting point of drawing, if exists
            List<LatLng> points = thisUser.getSelfDrawing().getPoints();
            if(lastLocation != null) {
                points.add(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lastLocation.getLatitude(),
                        lastLocation.getLongitude())));

            }
            else{
                promptGPS();
            }
            thisUser.setDrawingPoints(points);

        }
    }
    private void initSelf(){
        thisUser = new UserDrawing(5,Color.GRAY, "ArdaI");
        PolylineOptions lineOptions = new PolylineOptions().width(thisUser.getSelfWidth())
                .color(thisUser.getSelfColor());
        thisUser.setSelfDrawing(mMap.addPolyline(lineOptions));
        drawingList.put(thisUser.getNickname(),thisUser);

    }

    //Multiplayer user functions
    private void addUser(UserDrawing drawing){
        PolylineOptions lineOptions = new PolylineOptions().width(drawing.getSelfWidth()).
                color(drawing.getSelfColor());
        drawing.setSelfDrawing(mMap.addPolyline(lineOptions));
        drawingList.put(drawing.getNickname(),drawing);
    }
    private void updateDrawing(String userName, List<LatLng> points)  {
        UserDrawing k = drawingList.get(userName);
        List<LatLng> oldPoints = k.getSelfDrawing().getPoints();
        for(LatLng p : points)  {
            oldPoints.add(p);
        }
        k.setDrawingPoints(oldPoints);
        drawingList.put(userName,k);
    }
    private void removeUser(String userName){
        UserDrawing l = drawingList.get(userName);
        for(Polyline p : l.getSelfDrawings()){
            p.remove();
        }
        drawingList.remove(userName);
    }
    //UI functions
    public void drawPressed(View view){
        thisUser.setIsDrawing(!thisUser.isDrawing());
        if(thisUser.isDrawing()){
            PolylineOptions lineOptions = new PolylineOptions()
                    .width(thisUser.getSelfWidth())
                    .add(new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude()))
                    .color(thisUser.getSelfColor());

            thisUser.setSelfDrawing(mMap.addPolyline(lineOptions));
        }
        //TODO : Send new status to server
    }
    public void hidePressed(View view){
        boolean locEnable = !mMap.isMyLocationEnabled();
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            getGPSPermission();
        } else{
            mMap.setMyLocationEnabled(locEnable);
        }

        //TODO : Send new status to server
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initSelf();

        //Zoom camera
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(mMap.getMaxZoomLevel()-3);
        mMap.animateCamera(zoom);

        // -------------------TEST-------------------------------
        UserDrawing testUser= new UserDrawing(5,Color.GREEN,"test");
        addUser(testUser);

        //-------------------------------------------------------

        gpsLocationListener =new LocationListener(){

            public void onStatusChanged(String provider, int status, Bundle extras) {
                switch (status) {
                    case LocationProvider.AVAILABLE:
                        break;
                    case LocationProvider.OUT_OF_SERVICE:
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        break;
                }
            }


            public void onProviderEnabled(String provider) {

            }

            public void onProviderDisabled(String provider) {
                promptGPS();
            }

            public void onLocationChanged(Location location) {
                if(thisUser.isDrawing()){
                    List<LatLng> points = thisUser.getSelfDrawing().getPoints();
                    points.add(new LatLng(location.getLatitude(),location.getLongitude()));
                    thisUser.setDrawingPoints(points);
                }

                // -------------------TEST-------------------------------
                List<LatLng> lk = new ArrayList<>();
                lk.add(new LatLng(location.getLatitude()+0.5,location.getLongitude()+0.5));
                updateDrawing("test",lk);

                //-------------------------------------------------------

                lastLocation = location;
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lastLocation.getLatitude(),
                //        lastLocation.getLongitude())));

                //TODO : Sending user location to server

            }
        };
        getInitLocation();

    }
    //TODO : Getting other user information from servers and drawing

    /**
     * To remove a user from the map (its line), simply do line.remove
     */

    public class UserDrawing{
        private String nickname;
        private int selfColor ;
        private int selfWidth;
        private List<Polyline> selfDrawings; //Because more than 1 polyline
        private Polyline selfDrawing; //Current polyline that the user is drawing
        private boolean isDrawing;

        public UserDrawing(int width,int color, String nickname ){
            this.nickname = nickname;
            this.selfColor = color;
            this.selfWidth = width;
            this.selfDrawings = new ArrayList<>();
            this.isDrawing = true;
        }
        public int getSelfColor() {
            return selfColor;
        }

        public void setSelfColor(int selfColor) {
            this.selfColor = selfColor;
        }

        public int getSelfWidth() {
            return selfWidth;
        }

        public void setSelfWidth(int selfWidth) {
            this.selfWidth = selfWidth;
        }

        public Polyline getSelfDrawing() {
            return selfDrawing;
        }

        public void setSelfDrawing (Polyline selfDrawing) {
            this.selfDrawing =  selfDrawing;
            this.selfDrawings.add(selfDrawing);
        }

        public void setDrawingPoints(List<LatLng> points) {
            this.selfDrawing.setPoints(points);
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public boolean isDrawing() {
            return isDrawing;
        }

        public void setIsDrawing(boolean isDrawing) {
            this.isDrawing = isDrawing;
        }

        public List<Polyline> getSelfDrawings() {
            return selfDrawings;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        getInitLocation();
                    } else {
                        getGPSPermission();
                    }
                }
            }
        }
    }
}
