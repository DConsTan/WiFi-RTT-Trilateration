package com.darryncampbell.wifi_rtt_trilateration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lemmingapex.trilateration.LinearLeastSquaresSolver;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    boolean mPermissions = false;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final String TAG = "Wifi-RTT";
    TextView txtDebugOutput;
    Button btnFindAccessPointsAndRange;
    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;
    private List<ScanResult> WifiRttAPs;
    private WifiRttManager mWifiRttManager;
    private RttRangingResultCallback mRttRangingResultCallback;
    final Handler mRangeRequestDelayHandler = new Handler();
    private int mMillisecondsDelayBeforeNewRangingRequest = 1000;
    private boolean bStop = true;
    private Configuration configuration;
    private List<AccessPoint> buildingMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtDebugOutput = findViewById(R.id.txtDebugOutput);
        btnFindAccessPointsAndRange = findViewById(R.id.btnFindAccessPoints);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanReceiver = new WifiScanReceiver();
        mWifiRttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mRttRangingResultCallback = new RttRangingResultCallback();
        configuration = new Configuration(Configuration.CONFIGURATION_TYPE.TWO_DIMENSIONAL_2);
        buildingMap = configuration.getConfiguration();
        Collections.sort(buildingMap);

        //  Map Testing
        //Intent mapIntent = new Intent(this, MapActivity.class);
        //startActivity(mapIntent);
        //  End Map Testing

    }

    @Override
    protected void onResume() {
        super.onResume();
        mPermissions =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        if (!mPermissions) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            showMessage("Location permissions granted");
        }

        registerReceiver(
                mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void onClickFindAccessPointsAndRange(View view) {
        if (mPermissions) {
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT))
            {
                showMessage("This device does not support WIFI RTT");
            }
            else
            {
                //  startScan() is marked as deprecated but no alternative API is available (yet)
                bStop = false;
                showMessage("Searching for access points");
                mWifiManager.startScan();
            }
        } else
            showMessage("Location permissions not granted");
    }

    public void onClickStopRanging(View view)
    {
        showMessage("Stopping ranging...");
        bStop = true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions denied");
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                showMessage("Location permissions granted");
            }
        }
    }

    public void showMessage(String message) {
        Log.i(TAG, message);
        txtDebugOutput.setText(message + '\n' + txtDebugOutput.getText());
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            if (scanResults != null) {

                if (mPermissions) {
                    WifiRttAPs = new ArrayList<>();
                    for (ScanResult scanResult : scanResults) {
                        if (scanResult.is80211mcResponder())
                            WifiRttAPs.add(scanResult);
                        if (WifiRttAPs.size() >= RangingRequest.getMaxPeers()) {
                            break;
                        }
                    }

                    showMessage(scanResults.size()
                            + " APs discovered, "
                            + WifiRttAPs.size()
                            + " RTT capable.");
                    for (ScanResult wifiRttAP : WifiRttAPs) {
                        showMessage("AP Supporting RTT: " + wifiRttAP.SSID + " (" + wifiRttAP.BSSID + ")");
                    }
                    //  Start ranging
                    if (WifiRttAPs.size() < configuration.getConfiguration().size())
                        showMessage("Did not find enough RTT enabled APs.  Found: " + WifiRttAPs.size());
                    else {
                        startRangingRequest(WifiRttAPs);
                    }
                } else {
                    showMessage("Permissions not allowed.");
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest(List<ScanResult> scanResults) {
        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(scanResults).build();

        mWifiRttManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), mRttRangingResultCallback);
    }

    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private class RttRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest() {
            mRangeRequestDelayHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            startRangingRequest(WifiRttAPs);
                        }
                    },
                    mMillisecondsDelayBeforeNewRangingRequest);
        }

        @Override
        public void onRangingFailure(int code) {
            Log.d(TAG, "onRangingFailure() code: " + code);
            queueNextRangingRequest();
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Log.d(TAG, "onRangingResults(): " + list);

            // Because we are only requesting RangingResult for one access point (not multiple
            // access points), this will only ever be one. (Use loops when requesting RangingResults
            // for multiple access points.)
            if (list.size() >= configuration.getConfiguration().size()) {
                Collections.sort(list, new Comparator<RangingResult>() {
                    @Override
                    public int compare(RangingResult o1, RangingResult o2) {
                        return o1.getMacAddress().toString().compareTo(o2.getMacAddress().toString());
                    }
                });

                List<RangingResult> rangingResultsOfInterest = new ArrayList<RangingResult>();
                rangingResultsOfInterest.clear();
                for (int i = 0; i < list.size(); i++) {
                    RangingResult rangingResult = list.get(i);
                    if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                        if (!configuration.getMacAddresses().contains(rangingResult.getMacAddress().toString())) {
                            //  The Mac address found is not in our configuration
                            showMessage("Unrecognised MAC address: " + rangingResult.getMacAddress().toString());
                        } else {
                            rangingResultsOfInterest.add(rangingResult);
                        }
                    } else if (rangingResult.getStatus() == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC) {
                        showMessage("RangingResult failed (AP doesn't support IEEE80211 MC.");
                    } else {
                        showMessage("RangingResult failed.");
                    }
                }

                //  Expect us to have n APs in the rangingResults to our nearby APs
                //  todo remove any APs from the building map that we couldn't range to (need at least 2)
                if (rangingResultsOfInterest.size() != configuration.getConfiguration().size())
                {
                    showMessage("Could not find all the APs defined in the configuration to range off of");
                    if (!bStop)
                        queueNextRangingRequest();
                    return;
                }

                //  Build up the position array of the APs we are ranging to
                double[][] positions = new double[buildingMap.size()][3];
                for (int i = 0; i < buildingMap.size(); i++)
                {
                    positions[i] = buildingMap.get(i).getPosition();
                }

                for (int i = 0; i < rangingResultsOfInterest.size(); i++)
                {
                    showMessage("Distance to " + rangingResultsOfInterest.get(i).getMacAddress().toString() +
                            ": " + rangingResultsOfInterest.get(i).getDistanceMm() + "mm");
                }

                double[] distances = new double[rangingResultsOfInterest.size()];
                for (int i = 0; i < rangingResultsOfInterest.size(); i++)
                {
                    distances[i] = rangingResultsOfInterest.get(i).getDistanceMm();
                }


                //TrilaterationFunction trilaterationFunction = new TrilaterationFunction(positions, distances);
                //LinearLeastSquaresSolver lSolver = new LinearLeastSquaresSolver(trilaterationFunction);
                //NonLinearLeastSquaresSolver nlSolver = new NonLinearLeastSquaresSolver(trilaterationFunction, new LevenbergMarquardtOptimizer());
                //  x is the same result as centroid.
                //RealVector x = lSolver.solve();
                //LeastSquaresOptimizer.Optimum optimum = nlSolver.solve();
                try {
                    NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                    LeastSquaresOptimizer.Optimum optimum = solver.solve();

                    double[] centroid = optimum.getPoint().toArray();
                    //RealVector standardDeviation = optimum.getSigma(0);

                    String sCentroid = "Trilateration (centroid): ";
                    for (int i = 0; i < centroid.length; i++)
                        sCentroid += "" + (int)centroid[i] + ", ";
                    showMessage(sCentroid);
                    //  todo - move this logic into a foreground service - this is a hack to get things working
                    Intent mapIntent = new Intent(getApplicationContext(), MapActivity.class);
                    mapIntent.putExtra("location", centroid);
                    mapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(mapIntent);

                }
                catch (Exception e)
                {
                    showMessage("Error during trilateration: " + e.getMessage());
                }

            }
            else
            {
                showMessage("Could not find enough Ranging Results");
            }
            if (!bStop)
                queueNextRangingRequest();
        }
    }
}
