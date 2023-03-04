package com.example.live_location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.karumi.dexter.BuildConfig;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {
TextView text;

    private static final int REQUEST_CHECK_SETTINGS =100;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS =10000;
    private static final long FATEST_UPDATE_INTERVAL_IN_MILLISECONDS =1000;
    private static final String TAG =  MainActivity.class.getSimpleName();

    private FusedLocationProviderClient mfuseslocationclient;
    private SettingsClient msettingsclient;
    private LocationRequest mlocationRequest;
    private LocationSettingsRequest mlocationSettingsRequest;
     private LocationCallback mlocationCallback;
       private Location mcurrentlocation;
       private boolean mRequestingLocationUpdates =false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text= findViewById(R.id.text);
        mfuseslocationclient= LocationServices.getFusedLocationProviderClient(this);
        msettingsclient = LocationServices.getSettingsClient(this);

        mlocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mcurrentlocation = locationResult.getLastLocation();

//                double latitude= mcurrentlocation.getLatitude();
//                double longitude = mcurrentlocation.getLongitude();

               Geocoder gc= new Geocoder(MainActivity.this , Locale.getDefault());
               List<Address> addresses= null;
                try {
                    addresses=gc.getFromLocation(mcurrentlocation.getLatitude(), mcurrentlocation.getLongitude(), 1);
                    text.setText(addresses.get(0).getLocality());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //   text.setText();

            }
        };
        mlocationRequest = LocationRequest.create()
                .setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setFastestInterval(FATEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder= new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mlocationRequest);
        mlocationSettingsRequest = builder.build();

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mRequestingLocationUpdates = true;
                        startLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if(response.isPermanentlyDenied())
                        {
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }
    private void openSettings()
    {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri =Uri.fromParts("package", BuildConfig.APPLICATION_ID,null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void startLocationUpdates() {
        msettingsclient.checkLocationSettings(mlocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        mfuseslocationclient.requestLocationUpdates(mlocationRequest, mlocationCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                                Log.i(TAG, "location settings are not satisfied. attempting to upgrade location settings");

                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "locationsettings are inadequate, and cannot be fixed here.fix in settings";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private  void stopLocationUpdates()
    {
        mfuseslocationclient.removeLocationUpdates(mlocationCallback).addOnCompleteListener(this,task -> Log.d(TAG, "location updates stopped !"));
    }
    private  boolean checkPermission()
    {
        int permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return  permissionState== PackageManager.PERMISSION_GRANTED;
    }
    protected void onResume()
    {
        super.onResume();
        if (mRequestingLocationUpdates && checkPermission())
        {
            startLocationUpdates();
        }
    }
    protected void onPause()
    {
        super.onPause();
        if(mRequestingLocationUpdates)
        {
            stopLocationUpdates();
        }
    }





















}