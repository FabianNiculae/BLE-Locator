package com.example.blelocator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback{

    // TAG for log debug
    private static final String TAG = "utwente";

    // JSON File with predefined beacons in Ravelijn building
    private static final String FILE_NAME = "beacons.json";

    // Predefined final values
    private static final int REQUEST_CODE = 101;
    private static final double MS_POWER = -69;
    private static final double N = 2;

    // Delay time between scans - Turned OFF now
    private static final long SCAN_PERIOD = 10000; //ms
    // Var that holds one MAC for a simulated beacon on mobile phones - DEBUG
    private static final String beaconSimMac = "78:08:bd:9d:e0:d1";
    // Bluetooth stuff
    BluetoothManager blManager;
    BluetoothAdapter blAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothLeScanner blLeScanner = blAdapter.getBluetoothLeScanner();
    // Handler for Intent
    Handler mHandler = new Handler();
    // All the beacons info from the JSON file in an Array of JSON Objects
    JSONArray beaconsArray;
    // Device current location
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    // Beacons
    HashMap<String, Double> nearestBeacons = new HashMap<>();
    HashMap<String, Location> posBeacons = new HashMap<>();
    // We can stop the scan at anytime by changing this value to true
    private boolean scanningEnd = false;
    private GoogleMap mMap;
    private boolean posDetected = false;

    private Location locationBeaconA;
    private Location locationBeaconB;
    private Location locationBeaconC;

    private String macBeaconA;
    private String macBeaconB;
    private String macBeaconC;

    private Double distanceBeaconA;
    private Double distanceBeaconB;
    private Double distanceBeaconC;
    private boolean markerAdded = false;
    private boolean beaconMarkersSet = false;

    // Ravelijn building coordinates
    double rLat = 52.2393990;
    double rLng = 6.8556999;

    /**
     * Executes after BT scan has been made
     */
    private final ScanCallback mLeScanCallback = new ScanCallback() {

        /**
         * Executes when Bluetooth Scan comes up with results
         * @logs error code along with the tag
         */
        @SuppressLint("LongLogTag")
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            /**
             * MAC Address of the scan result
             */
            String resMac = String.valueOf(result.getDevice()).toLowerCase();

            for (int i = 0; i < beaconsArray.length(); i++) {
                //Log.d(TAG, "Comparing " + resMac + " to " + String.valueOf(i) + " from JSON file");
                try {
                    /**
                     * Getting the information of the beacon from JSON file
                     */
                    JSONObject beacon = beaconsArray.getJSONObject(i);
                    int bID = beacon.getInt("beacon_id");
                    String bAddress = beacon.getString("mac_address");
                    String bDeviceName = beacon.getString("device_name");
                    String bLat = beacon.getString("latitude");
                    String bLng = beacon.getString("longitude");
                    int bFloor = beacon.getInt("floor");

                    /**
                     * Checks for a MATCH with the MAC address of the scanned device
                     */
                    //Log.d(TAG+" -debug", "before mac validity check");
                    if (bAddress.equals(resMac) || resMac.equals(beaconSimMac)) {
                        Log.d(TAG + " MATCH", " Found a matching MAC Address: " + resMac + " on floor: " + bFloor);

                        /**
                         * Only floor 1 of Ravelijn building
                         */
                        //if(bFloor == 1){
                        if (true) {
                            /**
                             * Calculate distance to beacon with formula:
                             * distance = 10 ^ ((MEASURED_POWER - SCAN_RSSI) / (10 * N))
                             * MEASURED_POWER = -69    <-    Calibrated 1 RSSI
                             */
                            double distance = Math.pow(10, ((MS_POWER - result.getRssi()) / (10 * N)));
                            Log.d(TAG + " -DEVICE_DETAILS Scan result device details:",
                                    "\n MAC: " + result.getDevice()
                                            + "\n RSSI: " + result.getRssi()
                                            + "\n Distance to beacon: " + String.valueOf(distance) + "m"
                                            + "\n TX Power: " + result.getTxPower()
                                            + "\n Advertising SID: " + result.getAdvertisingSid()
                                            + "\n Data Status: " + result.getDataStatus()
                                            + "\n Primary: " + result.getPrimaryPhy()
                                            + "\n Secondary: " + result.getSecondaryPhy()
                                            + "\n Timestamps: " + result.getTimestampNanos()
                                            + "\n Lat: " + bLat
                                            + "\n Lng: " + bLng
                                            + "\n AD Interval: " + result.getPeriodicAdvertisingInterval());

                            nearestBeacons.put(bAddress, distance);

                            Location beaconLocation = new Location("");
                            beaconLocation.setLatitude(Double.valueOf(bLat));
                            beaconLocation.setLongitude(Double.valueOf(bLng));

                            posBeacons.put(bAddress, beaconLocation);

                            nearestBeacons = sortMapByValue(nearestBeacons);

                            if (nearestBeacons.size() >= 3) {
                                getInfoNearestBeacons(nearestBeacons, 3);
                                Log.d(TAG + "-debugLocation", "Calling function to get location");

                                Log.d(TAG + "-deubgLocation",
                                             "\n Location Beacon A: " + String.valueOf(locationBeaconA)
                                                + "\n Location Beacon B: " + String.valueOf(locationBeaconB)
                                                + "\n  Location Beacon C: " + String.valueOf(locationBeaconC));

                                currentLocation = getLocationWithBeacons(locationBeaconA, locationBeaconB, locationBeaconC, distanceBeaconA, distanceBeaconB, distanceBeaconC);
                                //setBeaconsMarkers();
                                //beaconMarkersSet = true;
                                Log.d(TAG + "-currentLocation", String.valueOf(currentLocation));
                                updateDeviceLocation(mMap);
                                markerAdded = true;
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Executes when Bluetooth Scan fails
         * @logs error code along with the tag
         */
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG + " BTLE SCAN FAIL", String.valueOf(errorCode));
        }
    };

    /**
     * Sort required to get the 3 closest beacons
     * @return sorted HashMap
     */
    private static HashMap sortMapByValue(HashMap map) {
        List list = new LinkedList(map.entrySet());

        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        //Log.d(TAG + "-inFuncSorted", String.valueOf(sortedHashMap));
        return sortedHashMap;
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Obtain the SupportMapFragment and get notified when the map is ready to be used.
         */
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(MainActivity.this);

        /**
         * Getting BT adapter ready
         */
        blManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        blAdapter = blManager.getAdapter();

        /**
         * Checks if BT adapter is off, if yes it enables it
         */
        if (!blAdapter.isEnabled()) {
            blAdapter.enable();
        }

        /**
         * Tries to read the file using created method and puts everything to a JSON ARRAY
         * @throws I/O and JSON exceptions
         */
        try {
            String JSONobjString = readJsonFile(FILE_NAME);
            try {
                beaconsArray = new JSONArray(JSONobjString);
            } catch (JSONException e) {
                Log.d("JSON", "ERROR EXCEPTION ARRAY");
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.d("JSON", "ERROR EXCEPTION");
            e.printStackTrace();
        }

        /**
         * Start the scan
         */
        sleepFor(8000);
        scanLeDevice();
    }

    /**
     * Checks if scanning is requested to be stopped then it stopps it, otherwise starts scan
     */
    private void scanLeDevice() {
        if (!scanningEnd) {
            Log.d(TAG+" -debug", "start scan");
            blLeScanner.startScan(mLeScanCallback);
        } else {
            blLeScanner.stopScan(mLeScanCallback);
        }
    }

    /**
     * @param beaconA coordinate location of first beacon
     * @param beaconB coordinate location of second beacon
     * @param beaconC coordinate location of third beacon
     * @param distanceA device distance to second beacon
     * @param distanceB device distance to second beacon
     * @param distanceC device distance to second beacon
     *
     * @return beacons map sorted
     */
    @SuppressLint("LongLogTag")
    public Location getLocationWithBeacons(Location beaconA, Location beaconB, Location beaconC, double distanceA, double distanceB, double distanceC) {
            double METERS_IN_COORDINATE_UNITS_RATIO = 0.00001;

            double cogX = (beaconA.getLatitude() + beaconB.getLatitude() + beaconC.getLatitude()) / 3;
            double cogY = (beaconA.getLongitude() + beaconB.getLongitude() + beaconC.getLongitude()) / 3;
            Location cog = new Location("Cog");
            cog.setLatitude(cogX);
            cog.setLongitude(cogY);

            Location nearestBeacon;
            double shortestDistanceInMeters;
            if (distanceA < distanceB && distanceA < distanceC) {
                nearestBeacon = beaconA;
                shortestDistanceInMeters = distanceA;
            } else if (distanceB < distanceC) {
                nearestBeacon = beaconB;
                shortestDistanceInMeters = distanceB;
            } else {
                nearestBeacon = beaconC;
                shortestDistanceInMeters = distanceC;
            }

            double distanceToCog = Math.sqrt(Math.pow(cog.getLatitude() - nearestBeacon.getLatitude(),2)
                    + Math.pow(cog.getLongitude() - nearestBeacon.getLongitude(),2));

            double shortestDistanceInCoordinationUnits = shortestDistanceInMeters * METERS_IN_COORDINATE_UNITS_RATIO;

            double t = shortestDistanceInCoordinationUnits/distanceToCog;

            Location pointsDiff = new Location("PointsDiff");
            pointsDiff.setLatitude(cog.getLatitude() - nearestBeacon.getLatitude());
            pointsDiff.setLongitude(cog.getLongitude() - nearestBeacon.getLongitude());

            Location tTimesDiff = new Location("tTimesDiff");
            tTimesDiff.setLatitude( pointsDiff.getLatitude() * t );
            tTimesDiff.setLongitude(pointsDiff.getLongitude() * t);

            Location userLocation = new Location("UserLocation");
            userLocation.setLatitude(nearestBeacon.getLatitude() + tTimesDiff.getLatitude());
            userLocation.setLongitude(nearestBeacon.getLongitude() + tTimesDiff.getLongitude());

            return userLocation;
        }

    /**
     * Logs a HashMap
     * @param mp to be looged
     */
    public void logMap(String customTag, HashMap mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Log.d(TAG + customTag,pair.getKey() + " = " + pair.getValue());
        }
    }

    public void getInfoNearestBeacons(HashMap mp, int amount) {
        Log.d(TAG+"-debugMAP SIZE", String.valueOf(mp.size()));
        logMap(TAG+"-debugMAP", mp);
        Log.d(TAG+"-debug", "Starting beacon details getter for this number of beacons: " + String.valueOf(amount));

        Iterator it = mp.entrySet().iterator();
        int k = 0;
        while (k < amount){
                    Map.Entry pair = (Map.Entry) it.next();
                    if (k == 0) {
                        //Log.d(TAG + "-debug", "Getting info beacon A");
                        macBeaconA = String.valueOf(pair.getKey());
                        //Log.d(TAG + "-debug", "Getting Location beacon A");
                        locationBeaconA = posBeacons.get(macBeaconA);
                        //Log.d(TAG + "-debug", "Getting distance beacon A");
                        distanceBeaconA = (Double) pair.getValue();
                        //Log.d(TAG + "-debug", "VALUE OF K: " + String.valueOf(k));
                    }

                    if (k == 1) {
                        macBeaconB = String.valueOf(pair.getKey());
                        //Log.d(TAG + "-debug", "Getting info beacon B:");
                        //Log.d(TAG + "-debug", "Getting Location beacon B");
                        locationBeaconB = posBeacons.get(macBeaconB);
                        //Log.d(TAG + "-debug", "Getting distance beacon B");
                        distanceBeaconB = (Double) pair.getValue();
                        //Log.d(TAG + "-debug", "VALUE OF K: " + String.valueOf(k));
                    }

                    if (k == 2) {
                        //Log.d(TAG + "-debug", "Getting info beacon C");
                        macBeaconC = String.valueOf(pair.getKey());
                        //Log.d(TAG + "-debug", "Getting Location beacon C");
                        locationBeaconC = posBeacons.get(macBeaconC);
                       // Log.d(TAG + "-debug", "Getting distance beacon C");
                        distanceBeaconC = (Double) pair.getValue();
                        //Log.d(TAG + "-debug", "VALUE OF K: " + String.valueOf(k));
                    }
                    it.remove();
                    k = k + 1;
                    //Log.d(TAG + "-debug", "VALUE OF K: " + String.valueOf(k));
                }
    }

    /**
     * When map from parameter is ready, manipulation will be done to it
     * @param googleMap takes a Google Map as parameter for manipulation
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng ravelijnBuilding = new LatLng(rLat, rLng);
        double zoom = 18.8;
        CameraUpdate newCamUpdate = CameraUpdateFactory.newLatLngZoom(ravelijnBuilding, (float) zoom);
        mMap.animateCamera(newCamUpdate);
    }

    /**
     *
     * @param googleMap
     */
    private void updateDeviceLocation(GoogleMap googleMap){
        //Log.d(TAG+"-currentLocation", "here");
        mMap = googleMap;
        double lat = currentLocation.getLatitude();
        double lng = currentLocation.getLongitude();

        LatLng deviceLocation = new LatLng(lat, lng);

        if(markerAdded == false){
            mMap.addMarker(new MarkerOptions().position(deviceLocation).title("Device is here").icon(BitmapDescriptorFactory.fromAsset("pin")));
        }
    }

    private void setPin(GoogleMap googleMap){
        mMap = googleMap;
        double llat = 52.2393990;
        double llng = 6.8556999;

        LatLng deviceLocation = new LatLng(llat, llng);

        mMap.addMarker(new MarkerOptions().position(deviceLocation).title("TEST PIN").icon(BitmapDescriptorFactory.fromAsset("pin.png")));
    }

    /**
     * Sets markers for the 3 beacons that help locate the device
     */
    private void setBeaconsMarkers(){
        if (!beaconMarkersSet){
            LatLng beaconA = new LatLng(locationBeaconA.getLatitude(), locationBeaconA.getLongitude());
            LatLng beaconB = new LatLng(locationBeaconB.getLatitude(), locationBeaconB.getLongitude());
            LatLng beaconC = new LatLng(locationBeaconC.getLatitude(), locationBeaconC.getLongitude());

            mMap.addMarker(new MarkerOptions().position(beaconA).title("Nearest beacon").icon(BitmapDescriptorFactory.fromAsset("beaconIcon")));
            mMap.addMarker(new MarkerOptions().position(beaconB).title("Intermediate beacon").icon(BitmapDescriptorFactory.fromAsset("beaconIcon")));
            mMap.addMarker(new MarkerOptions().position(beaconC).title("Furthest beacon").icon(BitmapDescriptorFactory.fromAsset("beaconIcon")));
        }
    }

    /**
     * Reads a JSON file from /app/assets/ and converts it to a string
     * @param file_name the name and extension of the file in a string to be read
     * @return null/JSON string of the json file
     * @throws IOException ReadBuffer exception for I/O
     */
    private String readJsonFile(String file_name) throws IOException {
        String json = null;
        try {
            InputStream is = getAssets().open(file_name);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
            return json;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Makes the app sleep for an amount of milliseconds
     * @param milliseconds time to sleep
     */
    private void sleepFor(int milliseconds){
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        }
        catch (Exception e) {
            Log.d("Sleep Function error!", String.valueOf(e));
        }
    }
}