package ucl.kebappsample;

import android.*;
import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.net.wifi.WifiManager;
import android.net.DhcpInfo;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import ucl.kebappsample.HelloService.LocalBinder;

public class InterestActivity extends FragmentActivity implements OnMapReadyCallback,ConnectionCallbacks {

    public static final String TAG = "InterestActivity";

    private TextView statusTxtView;
    private TextView resultTxtView;
    private Button button, button2;
    private EditText source, dest;
    private CheckBox enableCheck;
    private CheckBox transit,driving,walking;
    private String mode;
    private GoogleMap map;
    private SupportMapFragment fragment;
    private GoogleApiClient mGoogleApiClient;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private HelloService mServer;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    // private GoogleApiClient client;
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

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearStatus();

              /*  LinearLayout myRoot = (LinearLayout) findViewById(R.id.my_root);
                LinearLayout a = new LinearLayout(this);
                a.setOrientation(LinearLayout.HORIZONTAL);
                a.addView(view1);
                a.addView(view2);
                a.addView(view3);
                myRoot.addView(a);*/
                StartRequest();
            }
        });


        enableCheck = (CheckBox) this.findViewById(R.id.checkBox);
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
                    mServer.startRegistrationAndDiscovery();
                    // Intent intent = new Intent(InterestActivity.this, HelloService.class);
                    // startService(intent);
                } else {
                    mServer.disconnect();
                    Log.d(TAG, "Checkbox is not checked");
                    // Intent intent = new Intent(InterestActivity.this, HelloService.class);
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

        Intent intent = new Intent(InterestActivity.this, HelloService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
        startService(intent);


    }

    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
           // Toast.makeText(Client.this, "Service is disconnected", 1000).show();
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
           // Toast.makeText(Client.this, "Service is connected", 1000).show();
            LocalBinder mLocalBinder = (LocalBinder)service;
            mServer = mLocalBinder.getServerInstance();
        }
    };

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
        //resultTxtView.setText("");
    }

    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }


    @Override
    public void onStop() {
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(mGoogleApiClient, getIndexApiAction());
        Intent intent = new Intent(InterestActivity.this, HelloService.class);
        stopService(intent);
        mGoogleApiClient.disconnect();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(InterestActivity.this, HelloService.class);
        stopService(intent);
        System.exit(0);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "On connected");
        Location mLastLocation;
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_COARSE_LOCATION  },
                    MY_PERMISSION_ACCESS_COARSE_LOCATION );
        }
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
        /*try {
            Log.d(TAG,"Address " + getBroadcastAddress());
        } catch (java.io.IOException e) {
        Log.e(RequestDeviceListTask.TAG, "Failed get address");

        }
        */
        //SenderMulticastAsyncTask task = new SenderMulticastAsyncTask();
        //AsyncTaskCompat.executeParallel( task,null);

        //task.execute();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.FILL_PARENT);
        lp.weight = 0;
        fragment.getView().setLayoutParams(lp);

        RequestDeviceListTask task = new RequestDeviceListTask((KebappApplication) getApplication(), getApplicationContext());
        //task.cancel(true);
       // task.execute();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
    private class RequestDeviceListTask extends AsyncTask<String, Void, ArrayList<DeviceInfo>> {

        private static final String TAG = "Request Device List";
        private Face mFace;
        private ArrayList<DeviceInfo> deviceInfos = new ArrayList<>();
        private boolean shouldStop = false;
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
        protected ArrayList<DeviceInfo> doInBackground(String... params) {

            try {
                // KeyChain keyChain = buildTestKeyChain();
                final KeyChain keyChain = app.keyChain;
                mFace = new Face("localhost");
                mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
                Log.d(TAG,"Background request");

                // Invalid input
                /*if(params.length < 1) {
                    Log.e(RequestDeviceListTask.TAG, "No owner address!");
                    return null
                }*/

                KebappApplication app = (KebappApplication) getApplication();
                String oAddress = app.getOwnerAddress();
               // if(oAddress==app.getMyAddress())return deviceInfos;
               // Log.i(RequestDeviceListTask.TAG, "Address "+oAddress);
               // Nfdc nfdc = new Nfdc();
               // int faceId = nfdc.faceCreate("udp://"+app.getOwnerAddress());
               // nfdc.ribRegisterPrefix(new Name("/kebapp/maps/routefinder/"), faceId, 10, true, false);
               // nfdc.shutdown();
                Log.i(RequestDeviceListTask.TAG, "Face created");

                final Name requestName = new Name("/kebapp/maps/route/finder/"+source.getText().toString()+"/"+dest.getText().toString()+"/"+mode);
                requestName.appendSequenceNumber(1);
                Interest interest = new Interest(requestName);
                interest.setInterestLifetimeMilliseconds(5000);
                Log.i(RequestDeviceListTask.TAG, "Interest created "+ interest.getName().get(4).toEscapedString());
                results.clear();
                mFace.expressInterest(interest, new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {

                        /*String content = data.getContent().toString();
                        try {
                            Log.i(RequestDeviceListTask.TAG, "The content has been received ");
                            final JSONObject object = new JSONObject(content);
                            Log.i(RequestDeviceListTask.TAG, "The content has been received " + object.getString("text"));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        appendStatus("Result received");
                                        resultTxtView.setText("The distance is " + object.getString("text"));
                                      //  Toast.makeText(getApplicationContext(), "The distance is " +  object.getString("text"), Toast.LENGTH_LONG).show();

                                    } catch (JSONException e) {
                                        Log.e(RequestDeviceListTask.TAG, "Failed to construct the JSON");

                                    }
                                }
                            });
                            shouldStop = true;
                        } catch (JSONException e) {
                            Log.e(RequestDeviceListTask.TAG, "Failed to construct the JSON");
                            shouldStop = true;
                        }*/
                        try{

                            Log.i(RequestDeviceListTask.TAG, data.getName().toUri());
                            seqNumber = Integer.parseInt(data.getContent().toString());
                            Log.i(RequestDeviceListTask.TAG, "" + seqNumber);
                            Face contentFace = new Face("localhost");
                            for (int i = 2; i < 2 + seqNumber; i++) {
                                shouldStop = false;
                                Log.i(RequestDeviceListTask.TAG, "Request for data sequence " + i);
                                // Face contentFace = new Face("localhost");
                                contentFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                                Name contentName = new Name("/kebapp/maps/route/finder/"+source.getText().toString()+"/"+dest.getText().toString()+"/"+mode);
                                contentName.appendSequenceNumber(i);
                                Interest cInterest = new Interest(contentName);
                                cInterest.setInterestLifetimeMilliseconds(5000);
                                contentFace.expressInterest(cInterest, new OnData() {
                                    @Override
                                    public void onData(Interest interest, Data data) {
                                        try {
                                            Log.i(RequestDeviceListTask.TAG,"Send second interest");
                                            Name dName = data.getName();
                                            int size = dName.size();
                                            int seqNo = (int) dName.get(size - 1).toSequenceNumber();
                                            String content = data.getContent().toString();
                                            results.put(seqNo, content);
                                            Log.i(RequestDeviceListTask.TAG,"Received "+content.toString());
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
                                    }
                                });

                                while (!shouldStop&&!isCancelled()) {
                                    contentFace.processEvents();
                                }
                            }
                        }catch (SecurityException e) {
                            Log.e(RequestDeviceListTask.TAG, "Security Failed " + seqNumber);
                        } catch (IOException e) {
                            Log.e(RequestDeviceListTask.TAG, "IO Exception");
                        } catch (EncodingException e) {
                            Log.e(RequestDeviceListTask.TAG, "Encoding Failed");
                        }

                        StringBuffer sb = new StringBuffer();
                        for(int i = 2; i < 2 + seqNumber; i++) {
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
                                        //appendStatus("Result received");
                                        //resultTxtView.setText("The distance is " + object.getString("text"));
                                        //  Toast.makeText(getApplicationContext(), "The distance is " +  object.getString("text"), Toast.LENGTH_LONG).show();
                                        appendStatus("Result received");
                                        JSONArray routes = object.getJSONArray("routes");
                                        //JSONObject legs = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance");
                                        Log.i(RequestDeviceListTask.TAG, "latlng ");
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
                                        //JSONObject obj = new JSONObject();
                                        //obj.put("id", "JSON Object test");
                                        //String content = obj.toString();
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
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.FILL_PARENT);
                                lp.weight = 0;
                                fragment.getView().setLayoutParams(lp);
                            }

                        });
                        Log.e(RequestDeviceListTask.TAG, "Time Out!");
                        appendStatus("Time Out!");
                        shouldStop = true;
                    }
                });

                while(!shouldStop) {
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
            return deviceInfos;
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

    public class SenderMulticastAsyncTask extends AsyncTask<Void, Integer, String> {

        public SenderMulticastAsyncTask() {


            Log.d(TAG,"Start request");

        }
        @Override
        protected String doInBackground(Void... params) {

            int port =5454;

            InetAddress group = null;
            DatagramSocket socket = null;
            Log.d("Send", "Sending Packet1");

            try {
                Log.d("Send", "Sending Packet2");

                group = InetAddress.getByName("192.168.49.255");
            } catch (UnknownHostException e) {
                Log.d("Send", "Sending Packet3");
                // TODO Auto-generated catch block
               // socket.close();
                e.printStackTrace();
            }
            Log.d("Send", "Sending Packet4");

            //Sending to Multicast Group
            String message_to_send ="Test";
            byte[] buf = message_to_send.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
            try {
                socket= new DatagramSocket();
                socket.send(packet);
                Log.d("Send", "Sending Packet");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                socket.close();
                e.printStackTrace();
            }

            socket.close();
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            //do whatever ...
        }
    }

}