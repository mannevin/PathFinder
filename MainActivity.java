package com.example.vmann.mapbox;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener, MapboxMap.OnMapClickListener, AdapterView.OnItemClickListener {
    private static final String TAG = "MainActivity";

    //Buttons and text editors for text to speech
    //text sms variables
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    //Create a BroadcastReciever for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReciever1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //When discovery finds a device
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    //maps and buttons for main map
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    BluetoothAdapter mBluetoothAdapter;
    Button btnEnableDisable_Discoverable;
    BluetoothConnectionService mBluetoothConnection;
    Button btnStartConnection;
    Button btnSend;
    EditText etSend;
    BluetoothDevice mBTDevice;
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };
    ListView lvNewDevices;
    //calling variables
    Button callButton;
    //text view for speech to text
    private TextView txvResult;

    //Bluetooth Adapter and button to connect to bluetooth devices
    private TextToSpeech mTTS;
    private EditText mEditText;
    private Button mButtonSpeak;
    private MapView mapView;
    private MapboxMap map;
    private Button startButton;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;
    private com.mapbox.geojson.Point originPosition;
    private com.mapbox.geojson.Point destinationPosition;
    private Marker destinationMarker;
    //Broadcast Receiver for changes made to bluetooth states such as:
    //1) Discoverability mode on/off or expire
    private NavigationMapRoute navigationMapRoute;

    //Broadcast Receiver for listing devices that are not yet paired
    //excecuted by btnDiscover() method
    private Button sendSMS;
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final EditText phoneNumber = findViewById(R.id.phoneNumber);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission()) {
                Log.e("permission", "Permission already granted.");
            } else {
                requestPermission();
            }
        }
        final EditText smsText = findViewById(R.id.message);
        sendSMS = findViewById(R.id.sendSMS);
        sendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sms = smsText.getText().toString();
                String phoneNum = phoneNumber.getText().toString();
                if (!TextUtils.isEmpty(sms) && !TextUtils.isEmpty(phoneNum)) {
                    if (checkPermission()) {
                        //get the default SmsManager
                        SmsManager smsManager = SmsManager.getDefault();
                        //send the sms
                        smsManager.sendTextMessage(phoneNum, null, sms, null, null);
                    } else {
                        Toast.makeText(SMSActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        mButtonSpeak = findViewById(R.id.button_speak);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission()) {
                Log.e("permission", "Permission already granted.");
            } else {
                //if app doesn't have CALL_PHONE permission, request it
                requestPermission();
            }
        }
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        mButtonSpeak.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });


        mButtonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });


        Button btnONOFF = findViewById(R.id.btnONOFF);
        btnEnableDisable_Discoverable = findViewById(R.id.btnDiscoverable_on_off);
        lvNewDevices = findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        btnStartConnection = findViewById(R.id.btnStartConnection);
        btnSend = findViewById(R.id.btnSend);
        etSend = findViewById(R.id.editText);

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        lvNewDevices.setOnItemClickListener(MainActivity.this);
        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
            }
        });

        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
            }
        });
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        startButton = findViewById(R.id.startButton);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .origin(originPosition)
                        .destination(destinationPosition)
                        .shouldSimulateRoute(true)
                        .build();
                NavigationLauncher.startNavigation(MainActivity.this, options);
            }
        });
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(SMSActivity.this, Manifest.permission.SEND_SMS);
        return result == PackageManager.PERMISSION_GRANTED;
        int CallPermissionResult = ContextCompat
                .checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE);

        return CallPermissionResult == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                {
                        Manifest.permission.CALL_PHONE
                }, PERMISSION_REQUEST_CODE);
    }


    private void speak() {
        String text = "";
        if (mBroadcastReciever1 == 1) {
            text = null;
        } else if (mBroadcastReciever1 == 2) {
            text = "right";

        } else if (mBroadcastReciever1 == 3) {
            text = "left";
        } else {
            text = "back";
        }
        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    //gets speech input
    public void getSpeechInput(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 10);
        } else {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
        }
    }


    //create method for starting connection
    //remember the connection will fail and app will crash if you havent paired first
    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    //starting chat service method
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device, uuid);
    }

    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReciever1, BTIntent);
        }
        if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReciever1, BTIntent);
        }

    }

    public void btnEnableDisable_Discoverable(View view) {
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);

    }

    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if (!mBluetoothAdapter.isDiscovering()) {

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }

    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.addOnMapClickListener(this);
        enableLocation();
    }

    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
            initiateLocationLayer();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initiateLocationLayer() {
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }

    private void setCameraPosition(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()), 13.0));
    }


    @Override
    public void onMapClick(@NonNull LatLng point) {
        if (destinationMarker != null) {
            map.removeMarker(destinationMarker);
        }
        destinationMarker = map.addMarker(new MarkerOptions().position(p1));

        destinationPosition = com.mapbox.geojson.Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originPosition = com.mapbox.geojson.Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());
        getRoute(originPosition, destinationPosition);

        startButton.setEnabled(true);
        startButton.setBackgroundResource(R.color.mapboxBlue);
    }

    //Method to build a route to the destination from the current location of the user
    private void getRoute(com.mapbox.geojson.Point origin, com.mapbox.geojson.Point destination) {
        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    //Responses for if the route is not found
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, check user and access token");

                            return;
                        } else if (response.body().routes().size() == 0) {
                            Log.e(TAG, "No routes found");
                            return;
                        }
                        //if there is already a route then replace it with the new one
                        DirectionsRoute currentRoute = response.body().routes().get(0);
                        if (navigationMapRoute != null) {
                            //remove the old route to be replaced
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, map);
                        }
                        //replace the old route with the new route
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    //if the route is not create execute the method
                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "Error: " + t.getMessage());
                    }
                });

    }

    @Override
    @SuppressWarnings("MissingPermission")
    //execute this method when the android phone is connected to WiFi or the Internet
    public void onConnected() {
        //request location updates as the user moves
        locationEngine.requestLocationUpdates();
    }

    //execute the method when the location is changed
    @Override
    public void onLocationChanged(Location location) {
        //if there is no stored location
        if (location != null) {
            //set the new location as the current location and set camera onto that position
            originLocation = location;
            setCameraPosition(location);
        }
    }

    //this method is not required
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    //if the locations permission needs to be granted execute this method
    @Override
    public void onPermissionResult(boolean granted) {
        //if permission is granted
        if (granted) {
            //enable the location
            enableLocation();
        }
    }

    //checks status of permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(SMSActivity.this, "Permission accepeted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SMSActivity.this,
                            "Permission denied", Toast.LENGTH_LONG).show();
                    Button sendSMS = findViewById(R.id.sendSMS);
                    sendSMS.setEnabled(false);
                }
                break;
        }
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //cell phone
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                callButton = findViewById(R.id.call);

                if (grantResults.length > 0) {
                    boolean CallPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (CallPermission) {
                        Toast.makeText(MainActivity.this, "Permission accepted", Toast.LENGTH_LONG).show();
                    } else {
                        //if permission is denied
                        Toast.makeText(MainActivity.this,
                                "Permission denied", Toast.LENGTH_LONG).show();
                        callButton.setEnabled(false);
                    }
                    break;
                }
        }
    }

    public void launchSMS(View view) {
        Intent intent = new Intent(MainActivity.this, SMSActivity.class);
        startActivity(intent);
    }

    //call using the cell service
    public void call(View view) {
        //create a variable for phone number
        final EditText phoneNumber = findViewById(R.id.phoneNumber);
        //create a new string for the phone number
        String phoneNum = phoneNumber.getText().toString();
        if (!TextUtils.isEmpty(phoneNum)) {
            //create another string to find what to dial
            String dial = "tel:" + phoneNum;
            //Make an Intent object of type intent.ACTION_CALL
            startActivity(new Intent(Intent.ACTION_CALL,
                    //Extract the telephone number from the URI
                    Uri.parse(dial)));
        } else {
            //if the phone number is not valid
            Toast.makeText(MainActivity.this, "Please enter a valid telephone number", Toast.LENGTH_SHORT).show();
        }
    }

    //execute this method when app is started
    @SuppressWarnings("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        //if location is not available
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStart();
        }
        //start the map
        mapView.onStart();
    }

    //executed when map is paused and resumed
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    //executed when map is paused
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    //executed when the map is terminated
    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStop();
        }
        //stop map
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy:called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReciever1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        mapView.onDestroy();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String place = result.get(0);
                    LatLng p1 = getLocationFromAddress(this, place);
                }
                break;
        }
    }

    public LatLng getLocationFromAddress(Context context, String strAddress) {
        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            p1 = new LatLng(location.getLatitude(), location.getLongitude());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return p1;
    }
}
