package edu.uw.jiayi827.geopaint;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.thebluealliance.spectrum.SpectrumDialog;


import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static edu.uw.jiayi827.geopaint.GeoJsonConverter.convertToGeoJson;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private static final String TAG = "***MainActivity***";
    private final int PERMISSION_CODE = 1;
    private boolean isPenDown = false;

    private GoogleApiClient myGoogleApiClient;
    private GoogleMap myMap;
    private Polyline polyline;
    private List<Polyline> polylinesList;
    private Location curLocation;
    private int color;
    private GeoJsonConverter converter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(edu.uw.jiayi827.geopaint.R.layout.activity_main);

        // initialize variables
        polylinesList = new ArrayList<>();
        converter = new GeoJsonConverter();
        color = Color.BLUE;

        // create api client
        if (myGoogleApiClient == null) {
            myGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }


        // let the user locate to the current location
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(edu.uw.jiayi827.geopaint.R.id.map);
        myMap = mapFragment.getMap();

        // retain the fragment when activity re-creates
        mapFragment.setRetainInstance(true);

        try {
            myMap.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String bestProvider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(bestProvider);
        } catch (SecurityException e) {
        }

        }


    protected void onStart() {
        super.onStart();
        myGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        myGoogleApiClient.disconnect();
    }


    @Override
    public void onMapReady(GoogleMap map) {
        myMap = map;
        myMap.animateCamera(CameraUpdateFactory.zoomTo(10));
    }


    @Override
    public void onLocationChanged(Location location) {

        LatLng newLocation = new LatLng(location.getLatitude(),location.getLongitude());
        curLocation = location;

        // draw the line if the pen is down
        if (isPenDown) {
            List<LatLng> points = polyline.getPoints();
            points.add(newLocation);
            polyline.setPoints(points);
        }
    }


    private void setPenDown(boolean isPenDown) {
        if (isPenDown==false) {
            this.isPenDown = false;
            Toast.makeText(this, "Pen Up!", Toast.LENGTH_SHORT).show();
        } else {
            if (!this.isPenDown) {
                if (curLocation != null) {
                    this.isPenDown = true;

                    // draw the polyline on the map
                    LatLng latLng = new LatLng(curLocation.getLatitude(), curLocation.getLongitude());
                    PolylineOptions options = new PolylineOptions().add(latLng)
                            .color(this.color)
                            .geodesic(true);

                    polyline = myMap.addPolyline(options);
                    polylinesList.add(polyline);

                } else {
                    Toast.makeText(this, "No Location Info!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        }


    private void colorPicker() {
         new SpectrumDialog.Builder(this)
                 .setColors(R.array.demo_colors)
                 .setOutlineWidth(2)
                .setDismissOnColorSelected(true)
                .setFixedColumnCount(4)
                .setOnColorSelectedListener(new SpectrumDialog.OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(boolean positiveResult, @ColorInt int color) {
                        if (positiveResult) {
                            changePenColor(color);
                        }
                    }
                })
                .build().show(getSupportFragmentManager(),"colorPicker");
    }

    private void changePenColor(int newColor) {
        color = newColor;
        // make a change when the pen is down
        if (isPenDown) {
            this.isPenDown = false;
            setPenDown(true);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // build GPS request
        LocationRequest request = new LocationRequest();
        request.setInterval(1000);
        request.setFastestInterval(500);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // check permission from the user
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(myGoogleApiClient, request, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permission[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE:
                // if have permission
                if (permission.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onConnected(null);
            }
        }
        super.onRequestPermissionsResult(requestCode,permission,grantResults);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pen_up:
                setPenDown(false);
                return true;
            case R.id.pen_down:
                setPenDown(true);
                Toast.makeText(this, "Start painting!", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.change_color:
                colorPicker();
                return true;
            case R.id.save:
                savePaint();
                return true;
            case R.id.share:
                sharePaint();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void savePaint() {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(this.getExternalFilesDir(null),"drawing.geojson");
            String string = converter.convertToGeoJson(polylinesList);
            try {
                 FileOutputStream outputStream = new FileOutputStream(file);
                 outputStream.write(string.getBytes());
                 outputStream.close();
                 Toast.makeText(this, "Painting saved!", Toast.LENGTH_SHORT).show();
            } catch (IOException ioe) {
                Log.v(TAG,Log.getStackTraceString(ioe));
            }
        }
    }

    private void sharePaint() {
        Uri fileUri;
        File file = new File(this.getExternalFilesDir(null), "drawing.geojson");
        fileUri = Uri.fromFile(file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);

        Intent chooser = Intent.createChooser(intent, "Share my File");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        }
    }

}


