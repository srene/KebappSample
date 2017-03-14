package ucl.kebappsample;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v4.app.FragmentActivity;
import android.content.res.Configuration;
import android.content.ServiceConnection;
import android.widget.Toast;
import android.graphics.Color;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.transport.Transport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import ucl.kebappsample.*;

import static com.google.common.collect.ComparisonChain.start;

public class InterestActivity extends FragmentActivity implements OnMapReadyCallback,ConnectionCallbacks {

    public static final String TAG = "InterestActivity";

    private TextView statusTxtView;
    private TextView resultTxtView;
    private Button button, button2, button3;
    private EditText source, dest;
    private CheckBox enableCheck, enableCheckDirect;
    private CheckBox transit,driving,walking;
    private String mode;
    private GoogleMap map;
    private SupportMapFragment fragment;
    private GoogleApiClient mGoogleApiClient;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private KebappServiceAp mServer;
    private KebappServiceDirect mServerDirect;
    private KebappApplication app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kebapp);
        statusTxtView = (TextView) findViewById(R.id.status_text);
        resultTxtView = (TextView) findViewById(R.id.result);
        source = (EditText) findViewById(R.id.source);
        dest = (EditText) findViewById(R.id.dest);
        fragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        fragment.getMapAsync(this);
        mode = "transit";
        button = (Button) this.findViewById(R.id.button);
        app = (KebappApplication) getApplication();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearStatus();
                StartRequest();
            }
        });

        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(this, new String[] {  Manifest.permission.ACCESS_COARSE_LOCATION  },
                    MY_PERMISSION_ACCESS_COARSE_LOCATION );
        }

        String[] PERMS_INITIAL={
                Manifest.permission.ACCESS_FINE_LOCATION,
        };
        ActivityCompat.requestPermissions(this, PERMS_INITIAL, 127);


        enableCheck = (CheckBox) this.findViewById(R.id.checkBox);
        enableCheckDirect = (CheckBox) this.findViewById(R.id.checkBox3);
        transit = (CheckBox) this.findViewById(R.id.transit);
        driving = (CheckBox) this.findViewById(R.id.driving);
        walking = (CheckBox) this.findViewById(R.id.walking);


        transit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (((CheckBox) v).isChecked()) {
                    mode = "transit";
                    if (driving.isChecked()) driving.toggle();
                    if (walking.isChecked()) walking.toggle();

                } else {
                    transit.toggle();
                }
            }
        });

        driving.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (((CheckBox) v).isChecked()) {
                    mode = "driving";
                    if (transit.isChecked()) transit.toggle();
                    if (walking.isChecked()) walking.toggle();
                } else {
                    driving.toggle();
                }

            }
        });

        walking.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (((CheckBox) v).isChecked()) {
                    mode = "walking";
                    if (transit.isChecked()) transit.toggle();
                    if (driving.isChecked()) driving.toggle();
                } else {
                    walking.toggle();
                }
            }
        });

        enableCheck.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "Checkbox listen");

                if (((CheckBox) v).isChecked()) {
                    Log.d(TAG, "Checkbox is checked");
                    mServer.startKebapp();
                    // Intent intent = new Intent(InterestActivity.this, KebappService.class);
                    // startService(intent);
                    app.setServiceEnabled(true);

                } else {
                    mServer.disconnect();
                    Log.d(TAG, "Checkbox is not checked");
                    app.setServiceEnabled(false);
                    // Intent intent = new Intent(InterestActivity.this, KebappService.class);
                    //  stopService(intent);
                }

            }
        });

        enableCheckDirect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "Checkbox listen");

                if (((CheckBox) v).isChecked()) {
                    Log.d(TAG, "Checkbox is checked");
                    mServerDirect.startRegistrationAndDiscovery();
                    // Intent intent = new Intent(InterestActivity.this, KebappService.class);
                    // startService(intent);
                    app.setServiceDirectEnabled(true);

                } else {
                    mServerDirect.disconnect();
                    Log.d(TAG, "Checkbox is not checked");
                    app.setServiceDirectEnabled(false);
                    // Intent intent = new Intent(InterestActivity.this, KebappService.class);
                    //  stopService(intent);
                }

            }
        });

        button2 = (Button) this.findViewById(R.id.button2);
        button2.setEnabled(true);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InterestActivity.this, DeviceListActivity.class);
                startActivity(intent);
            }
        });


        button3 = (Button) this.findViewById(R.id.button3);
        button3.setEnabled(false);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
            // See https://g.co/AppIndexing/AndroidStudio for more information.
            mGoogleApiClient = new Builder(this)
                    .addConnectionCallbacks(this)
                    //.addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(AppIndex.API).build();
        }
        mGoogleApiClient.connect();

        Intent intent = new Intent(InterestActivity.this, KebappServiceAp.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
        startService(intent);

        Intent intentDirect = new Intent(InterestActivity.this, KebappServiceDirect.class);
        bindService(intentDirect, mConnectionDirect, BIND_AUTO_CREATE);
        startService(intentDirect);
    }

    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
                mServerDirect = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
           // Toast.makeText(Client.this, "Service is connected", 1000).show();
            Log.d(TAG,"Name " + name.getClassName() + " " +KebappServiceAp.class.getName()+  " " + KebappServiceDirect.class.getName());
                KebappServiceAp.LocalBinder mLocalBinder = (KebappServiceAp.LocalBinder)service;
                mServer = mLocalBinder.getServerInstance();
        }
    };

    ServiceConnection mConnectionDirect = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            mServerDirect = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            // Toast.makeText(Client.this, "Service is connected", 1000).show();
            Log.d(TAG,"Name " + name.getClassName() + " " +KebappServiceAp.class.getName()+  " " + KebappServiceDirect.class.getName());
            KebappServiceDirect.LocalBinder mLocalBinder = (KebappServiceDirect.LocalBinder)service;
            mServerDirect = mLocalBinder.getServerInstance();
        }
    };


    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG,"On map ready");
        map = googleMap;

    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(current + "\n" + status);
    }

    public void clearStatus() {
        statusTxtView.setText("");
    }



    @Override
    public void onStop() {
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(mGoogleApiClient, getIndexApiAction());
        Intent intent = new Intent(InterestActivity.this, KebappServiceAp.class);
        stopService(intent);
        Intent intentDirect = new Intent(InterestActivity.this, KebappServiceDirect.class);
        stopService(intentDirect);
        mGoogleApiClient.disconnect();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"Ondestroy");
        Intent intent = new Intent(InterestActivity.this, KebappServiceAp.class);
        stopService(intent);
        Intent intentDirect = new Intent(InterestActivity.this, KebappServiceDirect.class);
        stopService(intentDirect);
        System.exit(0);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "On connected");
        Location mLastLocation;

        Log.d(TAG,"Set location");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d(TAG,"Set location");

        if (mLastLocation != null) {
            Log.d(TAG,"Set location " + mLastLocation.getLatitude() + " " + mLastLocation.getLongitude());

            LatLng coordinate = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 15);
            map.animateCamera(yourLocation);
            Circle circle = map.addCircle(new CircleOptions()
                    .center(coordinate)
                    .radius(1)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.BLUE));

        }
    }

    @Override
    public void onConnectionSuspended(int cause) {

    }



    private void StartRequest() {
        Log.d(TAG,"Start request");
        appendStatus("Start request");

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.FILL_PARENT);
        lp.weight = 0;
        fragment.getView().setLayoutParams(lp);

       /* if (isNetworkAvailable(getApplicationContext())) {
            Log.d(TAG,"Connected ");
            SenderLocalAsyncTask task = new SenderLocalAsyncTask((KebappApplication) getApplication(), getApplicationContext());
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {*/
            Log.d(TAG,"Not connected ");
            RequestDeviceListTask task = new RequestDeviceListTask((KebappApplication) getApplication(), getApplicationContext());
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        //}

    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Interest Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient.connect();
        AppIndex.AppIndexApi.start(mGoogleApiClient, getIndexApiAction());
    }


    // AsyncTask for requesting device list, input is the owner's address
    // /oAddress/deviceList
    private class RequestDeviceListTask extends AsyncTask<String, Void, Void> {

        private static final String TAG = "Request Device List";
        private Face mFace;
        //private ArrayList<DeviceInfo> deviceInfos = new ArrayList<>();
        private boolean shouldStop = false;
        private boolean timeout = false;
        private KebappApplication app;
        private Context context;
        private int seqNumber = Integer.MAX_VALUE;
        private HashMap<Integer, String> results = new HashMap<Integer, String>();

        public RequestDeviceListTask(KebappApplication application, Context context) {
            this.app = application;
            this.context = context;

            Log.d(TAG,"Start request");

        }
        @Override
        protected Void doInBackground(String... params) {

            try {
                final KeyChain keyChain = app.keyChain;
                mFace = new Face("localhost");
                mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
                Log.d(TAG, "Background request");
                final Name requestName = new Name("/kebapp/maps/routefinder/" + source.getText().toString() + "/" + dest.getText().toString() + "/" + mode);
                requestName.appendSequenceNumber(1);
                Interest interest = new Interest(requestName);
                interest.setInterestLifetimeMilliseconds(10000);
                Log.i(RequestDeviceListTask.TAG, "Interest created " + interest.getName().get(3).toEscapedString());

                results.clear();
                mFace.expressInterest(interest, new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {

                        try {

                            Log.i(RequestDeviceListTask.TAG, data.getName().toUri());
                            seqNumber = Integer.parseInt(data.getContent().toString());
                            Log.i(RequestDeviceListTask.TAG, "" + seqNumber);
                            Face contentFace = new Face("localhost");
                            for (int i = 2; i < 2 + seqNumber; i++) {
                                shouldStop = false;
                                timeout = false;
                                Log.i(RequestDeviceListTask.TAG, "Request for data sequence " + i);
                                contentFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                                Name contentName = new Name("/kebapp/maps/routefinder/" + source.getText().toString() + "/" + dest.getText().toString() + "/" + mode);
                                contentName.appendSequenceNumber(i);
                                Interest cInterest = new Interest(contentName);
                                cInterest.setInterestLifetimeMilliseconds(2000);

                                contentFace.expressInterest(cInterest, new OnData() {
                                    @Override
                                    public void onData(Interest interest, Data data) {
                                        try {
                                            Log.i(RequestDeviceListTask.TAG, "Send second interest");
                                            Name dName = data.getName();
                                            int size = dName.size();
                                            int seqNo = (int) dName.get(size - 1).toSequenceNumber();
                                            String content = data.getContent().toString();
                                            results.put(seqNo, content);
                                            Log.i(RequestDeviceListTask.TAG, "Received " + content.toString());
                                            Log.i(RequestDeviceListTask.TAG, "" + results.keySet().size());
                                            // Log.i(RequestTask.TAG, "" + content);
                                            shouldStop = true;
                                        } catch (EncodingException e) {
                                            Log.i(RequestDeviceListTask.TAG, data.getName().toUri());
                                            Log.e(RequestDeviceListTask.TAG, "Encoding Failed " + seqNumber);
                                        }
                                    }
                                }, new OnTimeout() {
                                    @Override
                                    public void onTimeout(Interest interest) {
                                        Log.e(RequestDeviceListTask.TAG, "Time Out During Retriving Data");
                                        shouldStop = true;
                                        timeout = true;
                                    }
                                });


                                while (!shouldStop&&!isCancelled()) {
                                    contentFace.processEvents();
                                }
                                if (timeout) i--;
                            }
                        } catch (SecurityException e) {
                            Log.e(RequestDeviceListTask.TAG, "Security Failed " + seqNumber);
                        } catch (IOException e) {
                            Log.e(RequestDeviceListTask.TAG, "IO Exception");
                        } catch (EncodingException e) {
                            Log.e(RequestDeviceListTask.TAG, "Encoding Failed");
                        }

                        StringBuffer sb = new StringBuffer();
                        for (int i = 2; i < 2 + seqNumber; i++) {
                            sb.append(results.get(i));
                        }
                        // receiveVal = sb.toString();
                        String content = sb.toString();
                        try {
                            Log.i(RequestDeviceListTask.TAG, "The content has been received " + content);
                            final JSONObject object = new JSONObject(content);
                            Log.i(RequestDeviceListTask.TAG, "The content has been received " + object.toString());

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {

                                        appendStatus("Result received");
                                        JSONArray routes = object.getJSONArray("routes");

                                        JSONArray legs = routes.getJSONObject(0).getJSONArray("legs");

                                        Log.i(RequestDeviceListTask.TAG, "latlng 3 ");

                                        Log.i(RequestDeviceListTask.TAG, "latlng 2");


                                        JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");

                                        StringBuffer html = new StringBuffer();
                                        JSONObject latlng = new JSONObject();

                                        for(int i = 0; i < steps.length(); i++)
                                        {
                                            Log.d(TAG,"step :"+steps.getJSONObject(i).getString("html_instructions"));
                                            html.append(steps.getJSONObject(i).getString("html_instructions"));
                                            html.append("<br>");
                                            Log.d(TAG,"step :"+html.toString());
                                            latlng = steps.getJSONObject(i).getJSONObject("end_location");

                                        }
                                        String content = html.toString();
                                        LatLng coordinate = new LatLng(Double.parseDouble(latlng.getString("lat")), Double.parseDouble(latlng.getString("lng")));
                                        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 15);
                                        map.animateCamera(yourLocation);
                                        Circle circle = map.addCircle(new CircleOptions()
                                                .center(coordinate)
                                                .radius(1)
                                                .strokeColor(Color.BLUE)
                                                .fillColor(Color.BLUE));

                                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.FILL_PARENT);
                                        lp.weight = 1;
                                        fragment.getView().setLayoutParams(lp);
                                        resultTxtView.setText(Html.fromHtml(content));
                                    } catch (Exception e) {
                                        Log.e(RequestDeviceListTask.TAG, "Failed to construct the JSON");
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                appendStatus("We got no valid results from GMaps");

                                            }

                                        });

                                    }
                                }
                            });
                            shouldStop = true;
                        } catch (JSONException e) {
                            Log.e(RequestDeviceListTask.TAG, "Failed to construct the JSON");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    appendStatus("We got no valid results from GMaps");
                                }

                            });
                            shouldStop = true;
                        }
                    }
                }, new OnTimeout() {
                    @Override
                    public void onTimeout(Interest interest) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                                lp.weight = 0;
                                fragment.getView().setLayoutParams(lp);
                            }

                        });
                        Log.e(RequestDeviceListTask.TAG, "Time Out!");
                        appendStatus("Time Out!");
                        shouldStop = true;
                    }
                });

                while (!shouldStop) {
                    mFace.processEvents();
                }

            } catch (SecurityException e) {
                Log.e(RequestDeviceListTask.TAG, "Secrity Failed");
            } catch (IOException e) {
                Log.e(RequestDeviceListTask.TAG, "IO Failed");
            } catch (EncodingException e) {
                Log.e(RequestDeviceListTask.TAG, "Encoding Error");
            } catch (Exception e) {
                Log.e(RequestDeviceListTask.TAG, e.toString());
            }
            return null;
        }
    }

    public static KeyChain buildTestKeyChain() throws SecurityException {
        MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
        MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
        IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);
        KeyChain keyChain = new KeyChain(identityManager);
        try {
            keyChain.getDefaultCertificateName();
        } catch (SecurityException e) {
            keyChain.createIdentity(new Name("/test/identity"));
            keyChain.getIdentityManager().setDefaultIdentity(new Name("/test/identity"));
        }
        return keyChain;
    }

    public class SenderLocalAsyncTask extends  AsyncTask<String, Void, Void> {

        private static final String TAG = "Sender Local task";
        private HashMap<Integer, String> results = new HashMap<Integer, String>();

        public SenderLocalAsyncTask(KebappApplication application, Context context) {

            Log.d(TAG,"Start request");

        }
        @Override
        protected Void doInBackground(String... params) {
            Log.d(TAG,"Start request");

            try {

                String urlString = new String("http://maps.googleapis.com/maps/api/directions/json?origin=" + source.getText().toString() + "&destination=" + dest.getText().toString() + "&mode=" + mode);
                urlString = urlString.replaceAll(" ", "%20");
                Log.d(TAG,"Start request2 "+urlString);

                JSONObject jsonObject = null;
                try {
                    Log.d(TAG,"Start request3 ");
                    jsonObject = KebappServiceAp.getJSONObjectFromURL(urlString);
                    Log.d(TAG,"Start request4 ");
                } catch (java.net.UnknownHostException e) {
                    e.printStackTrace();
                    return null;
                }
                final JSONObject object = jsonObject;

                Log.i(SenderLocalAsyncTask.TAG, "The content has been received " + object.toString());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            appendStatus("Result received");
                            JSONArray routes = object.getJSONArray("routes");
                            JSONArray legs = routes.getJSONObject(0).getJSONArray("legs");
                            JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");

                            StringBuffer html = new StringBuffer();
                            JSONObject latlng = new JSONObject();
                            for (int i = 0; i < steps.length(); i++) {
                                Log.d(TAG, "step :" + steps.getJSONObject(i).getString("html_instructions"));
                                html.append(steps.getJSONObject(i).getString("html_instructions"));
                                html.append("<br>");
                                Log.d(TAG, "step :" + html.toString());
                                latlng = steps.getJSONObject(i).getJSONObject("end_location");
                            }
                            String content = html.toString();
                            LatLng coordinate = new LatLng(Double.parseDouble(latlng.getString("lat")), Double.parseDouble(latlng.getString("lng")));
                            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 15);
                            map.animateCamera(yourLocation);

                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                            lp.weight = 1;
                            fragment.getView().setLayoutParams(lp);
                            resultTxtView.setText(Html.fromHtml(content));
                        } catch (Exception e) {
                            Log.e(RequestDeviceListTask.TAG, "Failed to construct the JSON");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    appendStatus("We got no valid results from GMaps");

                                }

                            });

                        }
                    }
                });
            } catch (Exception e)
            {

            }
            return null;
        }


    }




}