package ucl.kebappsample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.os.AsyncTaskCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.net.wifi.WifiManager;
import android.net.DhcpInfo;
import android.support.v4.app.FragmentActivity;
import android.content.res.Configuration;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.net.*;

public class InterestActivity extends FragmentActivity implements OnMapReadyCallback{

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
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    // private GoogleApiClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dssa);
        statusTxtView = (TextView) findViewById(R.id.status_text);
        resultTxtView = (TextView) findViewById(R.id.result);
        source = (EditText) findViewById(R.id.source);
        dest = (EditText) findViewById(R.id.dest);
        fragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        fragment.getMapAsync(this);
        mode="transit";
        button = (Button) this.findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //  clearStatus();
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
                    mode="transit";
                    if(driving.isChecked())driving.toggle();
                    if(walking.isChecked())walking.toggle();

                } else {
                    transit.toggle();
                }
            }
        });

        driving.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (((CheckBox) v).isChecked()) {
                    mode="driving";
                    if(transit.isChecked())transit.toggle();
                    if(walking.isChecked())walking.toggle();
                }else {
                    driving.toggle();
                }

            }
        });

        walking.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (((CheckBox) v).isChecked()) {
                    mode="walking";
                    if(transit.isChecked())transit.toggle();
                    if(driving.isChecked())driving.toggle();
                }else {
                    walking.toggle();
                }
            }
        });

        enableCheck.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG,"Checkbox listen");

                if (((CheckBox) v).isChecked()) {
                    Log.d(TAG,"Checkbox is checked");
                    Intent intent = new Intent(InterestActivity.this, HelloService.class);
                    startService(intent);
                } else {
                    Log.d(TAG,"Checkbox is not checked");
                    Intent intent = new Intent(InterestActivity.this, HelloService.class);
                    stopService(intent);
                }

            }
        });

        button2 = (Button) this.findViewById(R.id.button2);
        button2.setEnabled(true);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InterestActivity.this, DiscoverActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
      /*  bNavigation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isTravelingToParis)
                {
                    isTravelingToParis = true;
                    findDirections( AMSTERDAM.latitude, AMSTERDAM.longitude,PARIS.latitude, PARIS.longitude, GMapV2Direction.MODE_DRIVING );
                }
                else
                {
                    isTravelingToParis = false;
                    findDirections( AMSTERDAM.latitude, AMSTERDAM.longitude, FRANKFURT.latitude, FRANKFURT.longitude, GMapV2Direction.MODE_DRIVING );
                }
            }
        });*/
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
        resultTxtView.setText("");
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
        super.onStop();
        Intent intent = new Intent(InterestActivity.this, KebappService.class);
        stopService(intent);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(InterestActivity.this, KebappService.class);
        stopService(intent);
        System.exit(0);
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
        RequestDeviceListTask task = new RequestDeviceListTask((KebappApplication) getApplication(), getApplicationContext());
        task.execute();
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
        public RequestDeviceListTask(KebappApplication application, Context context) {
            this.app = application;
            this.context = context;

            Log.d(TAG,"Start request");

        }
        @Override
        protected ArrayList<DeviceInfo> doInBackground(String... params) {

            try {
                // KeyChain keyChain = buildTestKeyChain();
                KeyChain keyChain = app.keyChain;
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

                Interest interest = new Interest(new Name("/kebapp/maps/routefinder/"+source.getText().toString()+"/"+dest.getText().toString()+"/"+mode));
                interest.setInterestLifetimeMilliseconds(10000);
                Log.i(RequestDeviceListTask.TAG, "Interest created "+ interest.getName().get(3).toEscapedString());

                mFace.expressInterest(interest, new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {
                        String content = data.getContent().toString();
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
                        }
                    }
                }, new OnTimeout() {
                    @Override
                    public void onTimeout(Interest interest) {
                        Log.e(RequestDeviceListTask.TAG, "Time Out!");
                        shouldStop = true;
                    }
                });

                while(!shouldStop) {
                    mFace.processEvents();
                }

            } catch (net.named_data.jndn.security.SecurityException e) {
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