package com.example.mapandroid;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.Arrays;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private boolean permissionGranted = false;
    private static final int LOCATION_REQUEST_CODE = 1234;
    private static int changeView = 0;
    private static final float DEFAULT_CAM = 15f;
    private double selectLat, selectLng;

    private GoogleMap map;
    private Marker marker;
    private Address address;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private String apikey;
    private PlacesClient placesClient;
    private ImageView imgGps, imgView, imgSearch, imgInfo, imgDirect;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        getWidget();
        getPermission(); // kiểm tra quyền sử dụng Vị trí
        apikey = getString(R.string.api_key);

        placeAutocomplete();
    }

    private void getWidget() {
        imgGps = (ImageView) findViewById(R.id.imgGps);
        imgView = (ImageView) findViewById(R.id.imgView);
        imgSearch = (ImageView) findViewById(R.id.imgSearch);
        imgInfo = (ImageView) findViewById(R.id.imgInfo);
        imgDirect = (ImageView) findViewById(R.id.imgDirect);
    }

    private void placeAutocomplete() {
        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(),apikey);
        }
        placesClient = Places.createClient(getApplicationContext());
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setTypeFilter(TypeFilter.ESTABLISHMENT);

        autocompleteFragment.setCountries("VN");
        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS_COMPONENTS));
//        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ADDRESS));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " +place.getLatLng()+ place.getName() + ", " + place.getId()+String.valueOf(place.getAddressComponents().asList().get(3).getName()));

                moveCam(place.getLatLng(),DEFAULT_CAM,place.getName(), String.valueOf(place.getAddressComponents()));
            }


            @Override
            public void onError(@NonNull Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

    }

    private void getPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
        String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
        if (ContextCompat.checkSelfPermission(getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                permissionGranted = true;
                initMap();  // khởi tạo map
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
                Toast.makeText(this, "PERMISSION IS REQUIRED,PLEASE ALLOW FROM SETTTINGS", Toast.LENGTH_SHORT).show();
                permissionGranted = false;
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
            permissionGranted = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        permissionGranted = false;

        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            permissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    permissionGranted = true;
                    //khởi tạo map
                    initMap();
                }
            }
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        Toast.makeText(this, "Khởi tạo map thành công!", Toast.LENGTH_SHORT).show();
        if (permissionGranted == true) {
            getCurrentLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(true);
            init();
        }
    }
    private void init(){
        imgSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                geoLocate();
            }
        });
        imgGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
            }
        });
        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(changeView == 0){
                    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    changeView++;
                }
                else if (changeView == 1){
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    changeView = 0;
                }

            }
        });
        imgInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                geoLocate();
                placeAutocomplete();
                try{
                    if(marker.isInfoWindowShown()){
                        marker.hideInfoWindow();
                    }else{
                        Log.d(TAG, "onClick: place info: " );
                        marker.showInfoWindow();
                    }
                }catch (NullPointerException e){
                    Log.e(TAG, "onClick: NullPointerException: " + e.getMessage() );
                }
            }
        });
        imgDirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {

                }
                catch(Exception e)
                {
                    Log.e(TAG, "onClick: NullPointerException: " + e.getMessage() );
                }
            }
        });
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                selectLat = latLng.latitude;
                selectLng = latLng.longitude;
                getAddress(selectLat,selectLng);
            }
        });
    }
    private void getAddress(double mLat,double mLng){
        map.clear();
        Geocoder geocoder = new Geocoder(MapActivity.this);

        if(mLat!=0){
//            try {
//                addresses = geocoder.getFromLocation(mLat,mLng,1);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            if(addresses != null){
//                String mAddress = addresses.get(0).getAddressLine(0);
//                String city =addresses.get(0).getLocality();
//                String state =addresses.get(0).getAdminArea();
//                String country =addresses.get(0).getCountryName();
//                String postalCode =addresses.get(0).getCountryCode();
//                String knownName=addresses.get(0).getFeatureName();
//                String dis=addresses.get(0).getSubAdminArea();
//
//                selectedAddress = mAddress;
//                if (mAddress != null) {
//                    MarkerOptions markerOptions = new MarkerOptions();
//                    LatLng latLng = new LatLng(mLat,mLng);
//
//                    markerOptions .position(latLng).title(knownName).snippet(selectedAddress);
//                    gMap.addMarker(markerOptions).showInfoWindow();
//                }
//                else{
//                    Toast.makeText(this,"Something error!",Toast.LENGTH_SHORT).show();
//                }
//            }

        }
    }

    public void getCurrentLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (permissionGranted) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                   @Override
                   public void onComplete(@NonNull Task task) {
                       if(task.isSuccessful() && task.getResult() != null){
                           Toast.makeText(MapActivity.this, "Tìm thấy vị trí của thiết bị!", Toast.LENGTH_SHORT).show();
                           Location currentLocation = (Location) task.getResult();

                           moveCam(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),15f,"My current Location","My address");
                       }else{
                           Toast.makeText(MapActivity.this, "Không tìm được vị trí của thiết bị!", Toast.LENGTH_SHORT).show();
                       }
                   }
                });
            }
        }
        catch (Exception e){
            Toast.makeText(this, "Error" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void moveCam(LatLng latLng, float zoom, String title, String snippet){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions().position(latLng).title(title).snippet(snippet);
            marker= map.addMarker(options);
        }
        //ẩn softkeyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

}