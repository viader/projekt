package com.daniel.czaterv2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CzatListActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private String macAddress;
    private TextView tv_user_name;
    private EditText et_user_name;
    private ListView chatList;
    private Button goToMap;
    private Button addCzat;
    private WebService webService;
    private UserAnonymous userAnonymous1;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private CzatListResponseDetails czatListResponseDetails;
    private double latitude, longitude;
    private List<CzatListResponseDetails> listChats = new ArrayList<>();
    protected static final int REQUEST_CHECK_SETTINGS = 1;
    private CzatListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_czat_list);

        tv_user_name = (TextView) findViewById(R.id.tv_user_name);
        et_user_name = (EditText) findViewById(R.id.et_user_name);
        chatList = (ListView) findViewById(R.id.lv_czat_list);
        goToMap = (Button) findViewById(R.id.btn_goToMap);
        addCzat = (Button) findViewById(R.id.btn_addCzat);

        getAnonymousUserName();
        checkPermision();
        getCzatList();
        if (App.getInstance().getGoogleApiClient() == null) {
            createGoogleApiClient();
        } else {
            googleApiClient = App.getInstance().getGoogleApiClient();
        }
        createLocationRequest();

        adapter = new CzatListAdapter(CzatListActivity.this, listChats);
        chatList.setAdapter(adapter);
        chatList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                chatList.getItemAtPosition(position);
            }
        });
        goToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToMapClick();
            }
        });
        addCzat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCzatOnClick();
            }
        });
    }

    private void chatListClick() {
        Toast tost = Toast.makeText(getApplicationContext(), "Tutaj będzie przeniesienie do czatu", Toast.LENGTH_SHORT);
        tost.show();
    }

    private void addCzatOnClick() {
        Intent intent = new Intent(this, AddCzatActivity.class);
        startActivity(intent);
    }

    private void goToMapClick() {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    private void getMacAdress() {
        // POBRANIE ADRESU MAC URZĄDZENIA
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        macAddress = info.getMacAddress();
    }

    private void buildRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(App.getSendURL())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        webService = retrofit.create(WebService.class);
    }

    private void getAnonymousUserName() {
        buildRetrofit();
        getMacAdress();

        try {
            final Call<UserAnonymous> userAnonymous = webService.userAnonymous(new AnonymousSend(macAddress));
            Log.d("MainActivity", "TRY");
            userAnonymous.enqueue(new Callback<UserAnonymous>() {
                @Override
                public void onResponse(Call<UserAnonymous> call, Response<UserAnonymous> response) {
                    Log.d("CzatListActivity", "ON RESPONSE");
                    Log.d("CzatListActivity", response.toString());
                    userAnonymous1 = response.body();
                    User user = new User(userAnonymous1);
                    App.getInstance().setUser(user);
                    Log.d("UserAnonymous name", userAnonymous1.name);
                    Log.d("UserAnonymous token", userAnonymous1.token);
                    et_user_name.setText(App.getInstance().getUser().getName().toString());
                }

                @Override
                public void onFailure(Call<UserAnonymous> call, Throwable t) {
                    Log.d("MainActivity", "ON FAILURE");
                    Log.d("ON FAILURE", call.toString());
                    Log.d("ON FAILURE", t.toString());
                }
            });

        } catch (Exception e) {
            Log.d("MainActivity", e.toString());
        }
    }

    private void getCzatList() {
        buildRetrofit();
        Call<Chats> getCzatList = webService.getChatList(App.getInstance().getMyPosition().latitude, App.getInstance().getMyPosition().longitude);
        getCzatList.enqueue(new Callback<Chats>() {
            @Override
            public void onResponse(Call<Chats> call, Response<Chats> response) {
                Chats chats = response.body();
                Log.d("CzatListActivity", "Response");
                listChats.clear();
                listChats.add(new CzatListResponseDetails("1", "TEST", 51.00, 23.33, 5000, 8));
                //listChats.addAll(chats.getChats());
                adapter.notifyDataSetChanged();
                App.getInstance().setCzatListResponseDetailses(listChats);
            }

            @Override
            public void onFailure(Call<Chats> call, Throwable t) {
                Log.d("CzatListActivity", "Failure");
            }
        });
    }

    protected void addExampleChats() {
        CzatProperties czat1;
        CzatProperties czat2;
        CzatProperties czat3;

        czat1 = new CzatProperties();
        czat1.setName("Czat 1");
        czat1.setRange(5000);
        czat1.setMaxUsers(10);

        czat2 = new CzatProperties();
        czat2.setName("Czat 2");
        czat2.setRange(10000);
        czat2.setMaxUsers(10);

        czat3 = new CzatProperties();
        czat3.setName("Czat 3");
        czat3.setRange(15000);
        czat3.setMaxUsers(10);
    }

    protected void createLocationRequest() {
        Log.d("CzatListActivity - clr", "CzatListActivity");
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        final PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates locationSettingsStates = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(CzatListActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("CATCH", e.toString());
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
        AppIndex.AppIndexApi.start(googleApiClient, getIndexApiAction());
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(googleApiClient, getIndexApiAction());
    }

    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    public void checkPermision() {
        int checkPermissionLocalizationFine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int checkPermissionLocalizationCoarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (checkPermissionLocalizationFine != 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 777);
            }
        }
        if (checkPermissionLocalizationCoarse != 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 666);
            }
        }
    }

    private void createGoogleApiClient() {
        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API)
                .build();
        App.getInstance().setGoogleApiClient(googleApiClient);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
