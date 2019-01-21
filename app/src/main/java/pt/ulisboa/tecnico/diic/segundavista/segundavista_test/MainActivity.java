package pt.ulisboa.tecnico.diic.segundavista.segundavista_test;

import android.Manifest;

import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.content.Intent;

import android.widget.Toast;
import io.flic.lib.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

public class MainActivity extends AppCompatActivity {

    Button json_button;
    public static TextView data;

    public static int clicks = 0;
    public static int state = 0; // 0 - looking for beacons; 1 - found beacon; 2 - inside beacon quiz

    public static Map<String, Beacon> beaconMap = new HashMap<>();
    public static Map<String, Double> beaconList = new HashMap<>();//used in the bluetooth part, unique id + distance(maybe rssi)
    public static Beacon currentBeacon;
    public static Quiz quiz;

    private String introductoryText = " one click to hear painting description; two clicks to hear fun facts; three clicks to play quiz.";

    public static TextToSpeech mTTs;

    private ProximityManager proximityManager; //new bluetooth

    private static final String API_KEY = "djmUhucbrdemoKOyHTvCmlDFtsmRojVY"; //KONTAKT API KEY
    private static final String TAG = "IBEACONS";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing Kontakt SDK. Insert your API key to allow all samples to work correctly
        KontaktSDK.initialize(API_KEY);

        //text to speech
        mTTs = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result =  mTTs.setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        Toast.makeText(MainActivity.this, "Language Available", Toast.LENGTH_SHORT).show();
                    }
                } else Log.e("TTS", "Initialization failed");
            }
        });

        //flic thingies
        FlicManager.setAppCredentials("99df8ff9-e927-4bf2-bec1-9bc1d982dee9", "498f136f-ce96-4e43-9929-1b2484218c83", "segunda vISTa");
        try {
            FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
                @Override
                public void onInitialized(FlicManager manager) {
                    manager.initiateGrabButton(MainActivity.this);
                }
            });
        } catch (FlicAppNotInstalledException err) {
            Toast.makeText(this, "Flic App is not installed", Toast.LENGTH_SHORT).show();
        }

        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        //Initialize and configure proximity manager
        setupProximityManager(); //NEW BLUETOOTH

        //Setup iBeacon and Eddystone filters
        //setupFilters(); //NEW BLUETOOTH


        // json info
        json_button = findViewById(R.id.jsonButton);
        data =  findViewById(R.id.fetchedJson);

        json_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FetchData process = new FetchData();
                process.execute();
                startScanning();//new bluetooth
            }
        });

        //MUDEI AQUI O COUNTDOWNINTERVAL PARA 1 SEGUNDO
        CountDownTimer timer = new CountDownTimer(1000000000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                //percorrer lista de beacons
                String closestBeacon;
                if (currentBeacon == null)
                    closestBeacon = null;
                else
                    closestBeacon = currentBeacon.getBeaconID();

                for (Map.Entry<String, Double> entry : beaconList.entrySet()) {
                    Log.d("compareBeacons",closestBeacon + ": " + beaconList.get(closestBeacon) + " vs " + entry.getKey() + ": " + entry.getValue());
                    //ver qual o mais próximo
                    if (entry.getValue() < 3.0f) {
                        if (closestBeacon == null)
                            closestBeacon = entry.getKey();
                        else if (beaconList.get(closestBeacon) > entry.getValue() && closestBeacon != entry.getKey())
                            closestBeacon = entry.getKey();
                    }
                }
                if (beaconMap.containsKey(closestBeacon)) {
                    Log.d("onTick", "closest beacon: " + closestBeacon);

                    if (currentBeacon != null) {
                        if (closestBeacon != currentBeacon.getBeaconID()) {
                            currentBeacon = beaconMap.get(closestBeacon);
                            MainActivity.state = 1;
                            mTTs.speak(currentBeacon.getName() + introductoryText, TextToSpeech.QUEUE_FLUSH, null);
                            Log.d("beaconsCycle", currentBeacon.getBeaconID() + ": " + beaconList.get(currentBeacon.getBeaconID()));
                        }
                    } else {
                        currentBeacon = beaconMap.get(closestBeacon);
                        MainActivity.state = 1;
                        mTTs.speak(currentBeacon.getName() + introductoryText, TextToSpeech.QUEUE_FLUSH, null);
                        Log.d("beaconsCycle null", currentBeacon.getBeaconID() + ": " + beaconList.get(currentBeacon.getBeaconID()));
                    }
                }
                //play music
                //stall for 5 seconds?
                //continue
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }

    //NEW BLUETOOTH THINGIES
    private void setupProximityManager() {
        proximityManager = ProximityManagerFactory.create(this);

        //Configure proximity manager basic options
        proximityManager.configuration()
                //Using ranging for continuous scanning or MONITORING for scanning with intervals
                .scanPeriod(ScanPeriod.RANGING)
                //Using BALANCED for best performance/battery ratio
                .scanMode(ScanMode.BALANCED)
                //OnDeviceUpdate callback will be received with 1 second interval
                .deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(1));

        //Setting up iBeacon listener
        proximityManager.setIBeaconListener(createIBeaconListener());
    }

    /*private void setupFilters() {
        //Setup sample iBeacon filter that only allows iBeacons with major and minor lower or equal 100.
        proximityManager.filters().iBeaconFilter(new IBeaconFilter() {
            @Override
            public boolean apply(IBeaconDevice iBeacon) {
                //return iBeacon.getMajor() <= 100 && iBeacon.getMinor() <= 100;
                return iBeacon.getDistance()<= 3.0f;
            }
        });
    }*/

    private void startScanning() {
        //Connect to scanning service and start scanning when ready
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                //Check if proximity manager is already scanning
                if (proximityManager.isScanning()) {
                    //Toast.makeText(ScanFiltersActivity.this, "Already scanning", Toast.LENGTH_SHORT).show();
                    return;
                }
                proximityManager.startScanning();
                //progressBar.setVisibility(View.VISIBLE);
                //Toast.makeText(ScanFiltersActivity.this, "Scanning started", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopScanning() {
        //Stop scanning if scanning is in progress
        if (proximityManager.isScanning()) {
            proximityManager.stopScanning();
            //progressBar.setVisibility(View.GONE);
            //Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show();
        }
    }

    //DO STUFF HERE
    private IBeaconListener createIBeaconListener() {
        return new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.i(TAG, "onIBeaconDiscovered: " + iBeacon.getUniqueId() + " distance = " + iBeacon.getDistance());
                if (iBeacon.getDistance() <= 3.0f) {
                    beaconList.put(iBeacon.getUniqueId(), iBeacon.getDistance());
                }
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                for (int i =0; i<iBeacons.size(); i++){
                    if (iBeacons.get(i).getDistance() <= 2.5f) {
                        beaconList.replace(iBeacons.get(i).getUniqueId(), iBeacons.get(i).getDistance());
                        Log.i(TAG, "onIBeaconsUpdated: " + iBeacons.get(i).getUniqueId() + " distance = " + iBeacons.get(i).getDistance());
                    }
                    else if(beaconList.containsKey(iBeacons.get(i).getUniqueId())){
                        beaconList.replace(iBeacons.get(i).getUniqueId()



                                 , 3.1);
                        Log.i(TAG, "onIBeaconsUpdated: " + iBeacons.get(i).getUniqueId() + " distance = " + iBeacons.get(i).getDistance() + "(removed)");
                    }
                }
            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.e(TAG, "onIBeaconLost: " + iBeacon.getUniqueId());
                if(beaconList.containsKey(iBeacon.getUniqueId())){
                    beaconList.replace(iBeacon.getUniqueId(), 3.1);
                }
            }
        };
    }


    //flic thingies
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
            @Override
            public void onInitialized(FlicManager manager) {
                FlicButton button = manager.completeGrabButton(requestCode, resultCode, data);
                if (button != null) {
                    button.registerListenForBroadcast(FlicBroadcastReceiverFlags.UP_OR_DOWN | FlicBroadcastReceiverFlags.REMOVED);
                    Toast.makeText(MainActivity.this, "Grabbed a button", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Did not grab any button", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        if(mTTs != null) {
            mTTs.stop();
            mTTs.shutdown();
        }
        stopScanning();
        proximityManager.disconnect();
        super.onDestroy();
    }
}
