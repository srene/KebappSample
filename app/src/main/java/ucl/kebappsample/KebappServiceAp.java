package ucl.kebappsample;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.SystemClock.sleep;


public class KebappServiceAp extends Service { //, IXposedHookLoadPackage {

    private Channel channel;
    private BroadcastReceiver receiver = null;
    private BroadcastReceiver apReceiver = null;

    private List peers = new ArrayList();

    int devices = 0;

    public static final String TAG = "KebappServiceAp";
    public static final String KEBAPP_SSID = "KEBAP";
    public static String passphrase = "raspberry";
    // TXT RECORD properties
    public static final long SERVICE_BROADCASTING_INTERVAL = 3000;

    private WifiManager apManager;

    private final IntentFilter filter = new IntentFilter();


    //private boolean isRunning  = false;
   // private boolean isDirectRegistered = false;
    public boolean isAPRegistered = false;

    private Face mFace;
    private String mAddress = "";
    private KeyChain keyChain;

    private long prefixId;
    IBinder mBinder = new LocalBinder();

    private Handler mServiceBroadcastingHandler;

    //private int faceId;
    KebappApplication kapp;
    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return mBinder;
    }
    public class LocalBinder extends Binder {
        public KebappServiceAp getServerInstance() {
            return KebappServiceAp.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");

        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        apManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        kapp = (KebappApplication)getApplication();

        apReceiver = new WiFiBroadcastReceiver(apManager, this,kapp);
        this.registerReceiver(this.apReceiver, filter);

        mServiceBroadcastingHandler = new Handler();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Log.i(TAG, "Start produce service thread");

                    final ArrayList<String> prefixData = new ArrayList<>();

                    KeyChain keyChain = buildTestKeyChain();
                    mFace = new Face("localhost");
                    mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                    Log.i(TAG, "My Address is: " + mAddress);

                    prefixId = mFace.registerPrefix(new Name("/kebapp/maps"), new OnInterest() {
                        @Override
                        public void onInterest(Name name, Interest interest, Transport transport, long l) {

                            try {

                                int size = interest.getName().size();
                                Name requestName = interest.getName();
                                Log.i(TAG, "Size: " + size);
                                Log.i(TAG, "Interest Name: " + interest.getName().toUri());
                                String source = requestName.get(3).toEscapedString();
                                String dest = requestName.get(4).toEscapedString();
                                String mode = requestName.get(5).toEscapedString();
                                Log.i(TAG, "Interest source: " + source);
                                Log.i(TAG, "Interest dest: " + dest);
                                Log.i(TAG, "Interest mode: " + mode);
                                int seqNo = (int)requestName.get(6).toSequenceNumber();

                                String urlString = new String("http://maps.googleapis.com/maps/api/directions/json?origin=" + source + "&destination=" + dest + "&mode=" + mode);

                                prefixData.clear();

                                JSONObject jsonObject = null;
                                try {
                                    jsonObject = getJSONObjectFromURL(urlString);
                                } catch (java.net.UnknownHostException e) {
                                    e.printStackTrace();
                                    return;
                                }

                                String content = jsonObject.toString();

                                Data data = new Data(requestName);

                                // Split the data
                                int fixLength = 8000;
                                int cnt = (content.length() / fixLength) + 1;
                                Log.d(TAG,"Content packets "+cnt);

                                for(int i = 0; i < cnt; i++) {
                                    Log.d(TAG,"Prefix "+1);
                                    prefixData.add(content.substring(i*fixLength, Math.min((i+1)*fixLength, content.length())));
                                }

                                if(seqNo == 1) {
                                    data.setContent(new Blob(""+prefixData.size()));
                                }
                                else {
                                    data.setContent(new Blob(prefixData.get(seqNo - 2)));
                                }


                                mFace.putData(data);


                                Log.i(TAG, "The device info has been sent");
                                Log.i(TAG, "The content is: " + content);
                                Log.i(TAG, "The content is: " + data.getContent().toString());
                                // Parse your json here

                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        }
                    }, new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name name) {
                            Log.e(TAG, "Failed to register the data");
                        }
                    });

                    while (true) {
                        // Log.i(TAG, "Service is running");
                        mFace.processEvents();
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }

        }).start();

        return Service.START_STICKY;
    }


    @Override
    public void onDestroy() {

       // isRunning = false;
        stopSelf();
       // unregisterReceiver(this.receiver);
        unregisterReceiver(this.apReceiver);
        disconnect();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    KeyChain keyChain = buildTestKeyChain();
                    Face mFace;
                    mFace = new Face("localhost");
                    mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                    Log.i(TAG, "My Address is: " + mAddress);

                    mFace.removeRegisteredPrefix(prefixId);
                } catch (Exception e) {
                    Log.d(TAG, "Error " + e);
                }
            }
        }).start();
        Log.i(TAG, "Service onDestroy");
    }

    public void disconnect()
    {
        Log.d(TAG,"Disconnect");
        disconnectAP();
    }

    public void disconnectAP()
    {
        Log.d(TAG,"DisconnectAP");
        isAPRegistered = false;
        apManager.disconnect();

       // apManager.removeNetwork(kapp.getNetId());


        List<WifiConfiguration> wifis = apManager.getConfiguredNetworks();
        for(WifiConfiguration info : wifis)
        {
            Log.d(TAG,"Wifi "+info.SSID);
            if(info.SSID.equals("\"" + KebappServiceAp.KEBAPP_SSID + "\""))
            {
                Log.d(TAG,"Remove KEbapp");
                apManager.removeNetwork(info.networkId);

            }
        }

        final int faceId = kapp.getWifiFaceId();
        if(faceId!=-1) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Nfdc nfdc = new Nfdc();

                        Log.d(TAG,"Face destroy");
                        nfdc.ribUnregisterPrefix(new Name("/kebapp/maps/routefinder/"),faceId);
                        nfdc.faceDestroy(faceId);
                        kapp.setWifiFaceId(-1);
                    } catch (Exception e) {
                        Log.d(TAG, "Error " + e);
                    }
                }
            }).start();

        } else {
            Log.d(TAG,"No face");
        }

        //if(isAPRegistered) unregisterReceiver(this.apReceiver);

    }




    public void startKebapp ()
    {
        apManager.startScan();
        mServiceBroadcastingHandler.postDelayed(mServiceBroadcastingRunnable, SERVICE_BROADCASTING_INTERVAL);

    }


    public void connectAp(final String ipAddress)
    {
        Log.d(TAG,"ConnectAp");
        int faceid = kapp.getWifiFaceId();
        if(faceid==-1) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Nfdc nfdc = new Nfdc();
                        int faceId = 0;
                        Log.d(TAG, "IP " + ipAddress);
                        sleep(5000);
                        faceId = nfdc.faceCreate("udp4://" + ipAddress);
                        kapp.setWifiFaceId(faceId);
                        //if(!info.isGroupOwner)faceId = nfdc.faceCreate("udp4://"+info.groupOwnerAddress.getHostAddress());
                        //        else faceId = nfdc.faceCreate("udp://"+app.getMyAddress());
                        nfdc.ribRegisterPrefix(new Name("/kebapp/maps/routefinder/"), faceId, 0, true, false);

                        nfdc.shutdown();
                    } catch (Exception e) {
                        Log.d(TAG, "Error " + e);
                    }
                }
            }).start();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final KeyChain keyChain = kapp.keyChain;
                    mFace = new Face("localhost");
                    mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
                    Log.d(TAG, "Background request");
                    final Name requestName = new Name("/kebapp/maps/routefinder/bcn");
                    sleep(10000);
                    requestName.appendSequenceNumber(1);
                    Interest interest = new Interest(requestName);
                    interest.setInterestLifetimeMilliseconds(5000);

                    mFace.expressInterest(interest, new OnData() {
                        @Override
                        public void onData(Interest interest, Data data) {
                        }
                    }, new OnTimeout (){
                        @Override
                        public void onTimeout(Interest interest) {

                        }
                    });

                } catch (Exception e) {
                    Log.d(TAG, "Error " + e);
                }
            }
        }).start();

    }


    public static KeyChain buildTestKeyChain() throws net.named_data.jndn.security.SecurityException {
        MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
        MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
        IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);
        KeyChain keyChain = new KeyChain(identityManager);
        try {
            keyChain.getDefaultCertificateName();
        } catch (net.named_data.jndn.security.SecurityException e) {
            keyChain.createIdentity(new Name("/test/identity"));
            keyChain.getIdentityManager().setDefaultIdentity(new Name("/test/identity"));
        }
        return keyChain;
    }

    public static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {

        HttpURLConnection urlConnection = null;

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);

        urlConnection.setDoOutput(true);

        urlConnection.connect();

        BufferedReader br=new BufferedReader(new InputStreamReader(url.openStream()));

        String jsonString;

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();

        jsonString = sb.toString();
        //Log.d(TAG, "JSON: " + jsonString);

        return new JSONObject(jsonString);
    }


    private Runnable mServiceBroadcastingRunnable = new Runnable() {
    @Override
    public void run() {
        Log.d(TAG,"Start scan");
       // apManager.startScan();
       // apManager.reconnect();
        while(apManager.startScan()==false){
            Log.d(TAG,"Start scan failed");
            apManager.startScan();
        }
        /*Log.d(TAG,"Wifi state "+apManager.getWifiState());
        List<ScanResult> res = apManager.getScanResults();
        for(ScanResult r : res)
            Log.d(TAG,"result "+r.SSID);*/
        if(kapp.getServiceEnabled())mServiceBroadcastingHandler
                .postDelayed(mServiceBroadcastingRunnable, SERVICE_BROADCASTING_INTERVAL);
        }
    };


}