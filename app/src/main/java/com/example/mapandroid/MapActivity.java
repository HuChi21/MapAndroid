package com.example.mapandroid;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.directions.route.RoutingListener;
import com.google.android.material.snackbar.Snackbar;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback,RoutingListener {
    private TextView txtFeaturename,txtAddress, txtRatescore,txtIsopen,txtOpeninghour,txtType,txtWeb;
    private RatingBar ratingBar;
    private Button btnDuongdi,btnGoi,btnWeb;
    private ImageView imgGps, imgView, imgInfo, imgDirect;
    private LinearLayout bottom_address_detail;
    private BottomSheetBehavior sheetBehavior;
    private boolean permissionGranted = false;
    private static final int LOCATION_REQUEST_CODE = 1234;
    private static int changeView = 0;
    private static final float DEFAULT_CAM = 15f;
    private double selectLat, selectLng;
    private List<Address> addresses;
    private String selectedAddress;
    private GoogleMap map;
    private Marker marker;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private String apikey;
    private PlacesClient placesClient;

    private LocationManager locationManager;
    private LatLng start,end;
    private MarkerOptions markerOptions = new MarkerOptions();;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        getWidget(); //ánh xạ
        getPermission(); // kiểm tra quyền sử dụng Vị trí
        apikey = getString(R.string.api_key);
    }

    private void getWidget() {
        imgGps = (ImageView) findViewById(R.id.imgGps);
        imgView = (ImageView) findViewById(R.id.imgView);
        imgInfo = (ImageView) findViewById(R.id.imgInfo);
        imgDirect = (ImageView) findViewById(R.id.imgDirect);
        //tạo bottom sheet chi tiết địa chỉ
        bottom_address_detail = (LinearLayout) findViewById(R.id.bottom_address_detail);
        sheetBehavior = BottomSheetBehavior.from(bottom_address_detail);

        txtFeaturename = findViewById(R.id.txtFeatureName);
        txtAddress = findViewById(R.id.txtAddress);
        ratingBar = findViewById(R.id.txtRating);
        txtRatescore = findViewById(R.id.txtRateScore);
        txtIsopen = findViewById(R.id.txtIsOpen);
        txtOpeninghour = findViewById(R.id.txtOpeningHour);
        txtType = findViewById(R.id.txtType);
        txtWeb = findViewById(R.id.txtWebsite);
        btnDuongdi = findViewById(R.id.btnDuongdi);
        btnGoi = findViewById(R.id.btnGoi);
        btnWeb = findViewById(R.id.btnTrangWeb);

        locationManager= (LocationManager) getSystemService(LOCATION_SERVICE);

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
                Toast.makeText(this, "Để tiếp tục, vui lòng cấp quyền vị trí!", Toast.LENGTH_SHORT).show();
                permissionGranted = false;
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
            permissionGranted = false;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionGranted = false;
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            permissionGranted = false;
                            return;
                        }
                    }
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
            getCurrentLocation(); // lấy vị trí hiện tại
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
            actionMap(); //
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
                            start = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                            moveCam(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),15f,"Vị trí của tôi","Địa chỉ");
                        }else{
                            Toast.makeText(MapActivity.this, "Không tìm được vị trí của thiết bị.\nVui lòng bật gps!", Toast.LENGTH_SHORT).show();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    requestGPS(); // yêu cầu sử dụng định vị vị trí
                                }
                            },4000);
                        }
                    }
                });
            }
        }
        catch (Exception e){
            Toast.makeText(this, "Error" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void requestGPS() {
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }
    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Để tiếp tục, hãy bật vị trí thiết bị bằng cách sử dụng dịch vụ vị trí của Google")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        finish();
                        startActivity(getIntent());

                    }
                })
                .setNegativeButton("Không, cảm ơn", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
    private void actionMap(){
        placeAutocomplete(); //tự động điền vị trí
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
                try{
                    if(sheetBehavior.getState() != sheetBehavior.STATE_EXPANDED){
                        marker.hideInfoWindow();
                        sheetBehavior.setState(sheetBehavior.STATE_EXPANDED);
                    }else {
                        sheetBehavior.setState(sheetBehavior.STATE_COLLAPSED);
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
                if(end == null){
                    Toast.makeText(MapActivity.this, "Chọn vị trí", Toast.LENGTH_SHORT).show();
                }else{
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(end, DEFAULT_CAM));
                    marker.showInfoWindow();
                    sheetBehavior.setState(sheetBehavior.STATE_COLLAPSED);
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
            try {
                addresses = geocoder.getFromLocation(mLat,mLng,1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(addresses != null){
                String mAddress = addresses.get(0).getAddressLine(0);
                String city =addresses.get(0).getLocality();
                String state =addresses.get(0).getAdminArea();
                String country =addresses.get(0).getCountryName();
                String postalCode =addresses.get(0).getCountryCode();
                String knownName=addresses.get(0).getFeatureName();
                String dis=addresses.get(0).getSubAdminArea();
                float rating =(float) ThreadLocalRandom.current().nextDouble(4.0f, 5.0f);
                String type= String.valueOf(addresses.get(0).getAdminArea());
                String web= addresses.get(0).getUrl();
                String phoneNumber= addresses.get(0).getPhone();

                selectedAddress = mAddress;
                if (mAddress != null) {
                    LatLng latLng = new LatLng(mLat,mLng);
                    markerOptions.position(latLng).title(knownName +","+ dis).snippet(selectedAddress);
                    marker = map.addMarker(markerOptions);
                    showInfo(latLng,knownName,mAddress,rating,"Đang mở cửa","Mở cửa cả ngày",type,web,phoneNumber);
                    sheetBehavior.setState(sheetBehavior.STATE_EXPANDED);
                }
                else{
                    Toast.makeText(this,"Lỗi!",Toast.LENGTH_SHORT).show();
                }
            }
        }
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
        autocompleteFragment.getView().setBackgroundColor(Color.WHITE);

        autocompleteFragment.setCountries("VN");
        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(
                Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS_COMPONENTS,
                Place.Field.PHONE_NUMBER, Place.Field.OPENING_HOURS, Place.Field.WEBSITE_URI, Place.Field.TYPES,Place.Field.UTC_OFFSET));
//      autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ADDRESS));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // TODO: Get info about the selected place.

//                Log.i(TAG, "Place: " +place.getLatLng()+ place.getName() + ", " + place.getId()+String.valueOf(place.getAddressComponents())+place.getOpeningHours());
                map.clear();
                String knownName=place.getName();
                String address ="";
                String type = String.valueOf(place.getTypes().get(0));
                String website = String.valueOf(place.getWebsiteUri());
                String phoneNumber = place.getPhoneNumber();
                for(int i=0;i<place.getAddressComponents().asList().size();i++){
                    address += place.getAddressComponents().asList().get(i).getName() +", ";
                }
                String diachi = address.substring(0, address.length() - 2);
                Toast.makeText(MapActivity.this, diachi, Toast.LENGTH_SHORT).show();

                //lấy ngày hiện tại
                Calendar calendar = Calendar.getInstance();
                Date date = calendar.getTime();
//                Date time = calendar.getTime();
                String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(date.getTime());
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date.getTime());

                //lấy thời gian mở cửa
                String openinghour = currentDay;
                String isOpen = "Đang mở cửa";
                float rating =(float) ThreadLocalRandom.current().nextDouble(4.0f, 5.0f);
                try {
                    if (String.valueOf(place.getOpeningHours().getWeekdayText())==null){
                        openinghour = currentDay;
                    }
                    if (place.getWebsiteUri()==null){
                        website = "";
                    }
                    if (place.getPhoneNumber()==null){
                        phoneNumber = "";
                    }
                    else{
                        String aaa =String.valueOf(place.getOpeningHours().getWeekdayText());
                        String dayNew = aaa.replaceAll("^\\[|\\]$", "");
                        String[] dayOfWeek = dayNew.split(",");
                        for(int i=0;i<dayOfWeek.length;i++){
                            String[] today = dayOfWeek[i].split(": ");
                            if(today[0].trim().equals(currentDay)){
                                openinghour = today[1].trim();
                            }
                        }
                        String[] hour = openinghour.split("–");
//                        Log.d("hehe", hour[0].trim());
                        if(hour[0].trim().equals("Mở cửa cả ngày")){
                            isOpen = "Đang mở cửa";
                        }
                        else if(hour[0].trim().compareTo(currentTime)>=0||hour[1].trim().compareTo(currentTime)<=0) {
                            isOpen = "Đóng cửa";
                        }
                        else{
//                            Toast.makeText(MapActivity.this, "Đúng h oi`", Toast.LENGTH_SHORT).show();
                            isOpen = "Đang mở cửa";
                        }
                    }
                }
                catch (Exception e){
                    Toast.makeText(MapActivity.this, "Error" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("error", "Error" + e.getMessage());
                }
                moveCam(place.getLatLng(),DEFAULT_CAM,knownName, diachi);
                sheetBehavior.setState(sheetBehavior.STATE_EXPANDED);
                showInfo(place.getLatLng(),knownName,diachi,rating,isOpen,openinghour,type,website,phoneNumber);
            }
            @Override
            public void onError(@NonNull Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }
    private void showInfo(LatLng latlng,String featureName, String address, float rating, String isopen, String openinghour, String type, String web,String phoneNum){
        txtFeaturename.setText(featureName);
        txtFeaturename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sheetBehavior.getState() != sheetBehavior.STATE_EXPANDED){
                    marker.hideInfoWindow();
                    sheetBehavior.setState(sheetBehavior.STATE_EXPANDED);
                }else {
                    sheetBehavior.setState(sheetBehavior.STATE_COLLAPSED);
                    marker.showInfoWindow();
                }
            }
        });
        txtAddress.setText(address);
        ratingBar.setRating(rating);
//        ratingBar.setBackT(0xFFFBBC04);
        txtRatescore.setText("("+String.format("%.02f",rating)+")");
        txtIsopen.setText(isopen);
        if(isopen.equals("Đang mở cửa")){
            txtIsopen.setTextColor(0xFF90EE90);
        }else if(isopen.equals("Đóng cửa")){
            txtIsopen.setTextColor(0xFFD93025);
        }
        txtOpeninghour.setText(String.valueOf(openinghour));
        type = type.toLowerCase();
        type = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
        txtType.setText(type);
        txtWeb.setText(web);

        markerOptions.position(latlng).title(featureName).snippet(address);
        marker = map.addMarker(markerOptions);
        end = latlng;
        Log.d("end", String.valueOf(end));
        btnDuongdi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findRoutes(start,end);
            }
        });

        btnGoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phoneNum == "" ||phoneNum == null) {
                    Toast.makeText(MapActivity.this, "Không có số điện thoại", Toast.LENGTH_SHORT).show();
                }
                else{
                    Intent intent1 = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNum,null));
                    startActivity(intent1);
                }
            }
        });
        btnWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (web == ""||web == null) {
                    Toast.makeText(MapActivity.this, "Không có trang web", Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(web));
                    startActivity(browserIntent);
                }
            }
        });
    }
    private void moveCam(LatLng latLng, float zoom, String title, String snippet){
//        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        Log.i(TAG, latLng.latitude + latLng.longitude +","+ DEFAULT_CAM + title + snippet);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("Vị trí của tôi")){
            MarkerOptions options = new MarkerOptions().position(latLng).title(title).snippet(snippet);
            marker= map.addMarker(options);
        }
        //ẩn softkeyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    private List<Polyline> polylines=null;

    public void findRoutes(LatLng Start, LatLng End)
    {
        if(Start==null || End==null) {
            Toast.makeText(MapActivity.this,"Không lấy được vị trí" + String.valueOf(Start +","+End), Toast.LENGTH_LONG).show();
        }
        else
        {

            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(Start, End)
                    .key(apikey)  //also define your api key here.
                    .build();
            routing.execute();
        }
    }
    @Override
    public void onRoutingFailure(RouteException e) {
        View parentLayout = findViewById(android.R.id.content);
        Snackbar snackbar= Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_LONG);
        snackbar.show();
//        Findroutes(start,end);
    }

    @Override
    public void onRoutingStart() {
        Toast.makeText(MapActivity.this,"Đang tìm tuyến đường ...",Toast.LENGTH_SHORT).show();
        sheetBehavior.setState(sheetBehavior.STATE_COLLAPSED);
    }

    //If Route finding success..
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(end, DEFAULT_CAM));
                map.clear();
                if(polylines!=null) {
                    polylines.clear();
                }
                PolylineOptions polyOptions = new PolylineOptions();
//
                polylines = new ArrayList<>();
                //add route(s) to the map using polyline
                for (int i = 0; i <route.size(); i++) {

                    if(i==shortestRouteIndex)
                    {
                        polyOptions.color(0xFF1A73E8);
                        polyOptions.width(8);
                        polyOptions.addAll(route.get(shortestRouteIndex).getPoints());
                        Polyline polyline = map.addPolyline(polyOptions);
                        start=polyline.getPoints().get(0);
                        int k=polyline.getPoints().size();
                        end=polyline.getPoints().get(k-1);
                        polylines.add(polyline);

                    }
                    else {

                    }

                }

                //Add Marker on route starting position
                MarkerOptions startMarker = new MarkerOptions();
                startMarker.position(start);
                startMarker.title("Vị trí của tôi");
                map.addMarker(startMarker);

                //Add Marker on route ending position

                Log.d("?",String.valueOf(end));
                markerOptions.position(end);
                marker = map.addMarker(markerOptions);
                marker.showInfoWindow();

            }
        },1500);
    }

    @Override
    public void onRoutingCancelled() {
        findRoutes(start,end);
    }

}