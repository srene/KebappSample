package ucl.kebappsample;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;
import android.widget.TextView;

import net.named_data.jndn.*;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn_xx.util.*;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import android.graphics.Color;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.*;
import de.robv.android.xposed.XposedBridge.*;


public class HelloService extends Service implements
        ConnectionInfoListener,PeerListListener, IXposedHookLoadPackage {

    private Channel channel;
    private BroadcastReceiver receiver = null;
    private List peers = new ArrayList();

    int devices = 0;

    public static final String TAG = "HelloService";

    // TXT RECORD properties
    public static final long SERVICE_BROADCASTING_INTERVAL = 1000;
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_kebapptest";
    public static final String SERVICE_REG_TYPE = "_kebapp._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager manager;

    static final int SERVER_PORT = 4545;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiP2pInfo info;
    //public MyListFragment peersList;

    private final IntentFilter intentFilter = new IntentFilter();
    private TextView statusTxtView;

    private WiFiP2pService service;

    private boolean isRunning  = false;

    private Face mFace;
    private String mAddress = "";
    private KeyChain keyChain;

    IBinder mBinder = new LocalBinder();

    private Handler mServiceBroadcastingHandler;

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return mBinder;
    }
    public class LocalBinder extends Binder {
        public HelloService getServerInstance() {
            return HelloService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //statusTxtView = (TextView) findViewById(R.id.status_text);
        //super.onResume();
        //Indicates whether Wi-Fi P2P is enabled
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        //Indicates that the available peer list has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        //Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        //Indicates this device's configuration details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(HelloService.this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this,(KebappApplication)getApplication());
        mServiceBroadcastingHandler = new Handler();
        this.registerReceiver(this.receiver, intentFilter);
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");

        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
        //new Thread(new Runnable() {
       //     @Override
       //     public void run() {

                Log.i(TAG, "Service onStartCommand thread");


               // startRegistrationAndDiscovery();


                //Your logic that service will perform will be placed here
                //In this example we are just looping and waits for 1000 milliseconds in each loop.
                /*for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }

                    if(isRunning){
                        Log.i(TAG, "Service running");
                    }
                }*/

                //Stop service once it finishes its task
                //stopSelf();
        //    }
        //}).start();


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Log.i(TAG, "Start produce service thread");

                    final ArrayList<String> prefixData = new ArrayList<>();

                    KeyChain keyChain = buildTestKeyChain();
                    mFace = new Face("localhost");
                    mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                    KebappApplication app = (KebappApplication) getApplication();
                    //String oAddress = app.getOwnerAddress();
                    //Log.i(TAG, "Address " + oAddress + " " + app.getMyAddress());
                    //if(oAddress!=app.getMyAddress()) {

                    //}
                    Log.i(TAG, "My Address is: " + mAddress);
                  //  Log.i(TAG, "Address " + info.groupOwnerAddress + " " + info.isGroupOwner);

                    // Register the prefix with the device's address
                    mFace.registerPrefix(new Name("/kebapp/maps"), new OnInterest() {
                        @Override
                        public void onInterest(Name name, Interest interest, Transport transport, long l) {
                            //      try {

                            //Toast.makeText(getApplicationContext(), "Interest Received: " + interest.getName().toUri(), Toast.LENGTH_LONG).show();
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
                                    // return;
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

        isRunning = false;
        stopSelf();
        disconnect();
        unregisterReceiver(this.receiver);
        Log.i(TAG, "Service onDestroy");
    }

    public void disconnect() {
        if (manager != null && channel != null) {
            manager.requestGroupInfo(channel, new GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && manager != null && channel != null
                            && group.isGroupOwner()) {
                        manager.removeGroup(channel, new ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "removeGroup onFailure -" + reason);
                            }
                        });

                    }
                }
            });
        }
    }
    /**
     * Registers a local service and then initiates a service discovery
     */

    private Runnable mServiceBroadcastingRunnable = new Runnable() {
        @Override
        public void run() {
            /*manager.stopPeerDiscovery(channel,new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG,"Peers discovery stopped");
                }

                @Override
                public void onFailure(int error) {
                }
            });*/
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG,"Peers discovery initiated");
                }

                @Override
                public void onFailure(int error) {
                }
            });
            if(isRunning)mServiceBroadcastingHandler
                    .postDelayed(mServiceBroadcastingRunnable, SERVICE_BROADCASTING_INTERVAL);
        }
    };


    public void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
       // record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
        record.put("available", "visible");

       final WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        /*manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG,"Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG,"Failed to add a service");
            }
        });*/

        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    manager.addLocalService(channel, service,
                            new WifiP2pManager.ActionListener() {

                                @Override
                                public void onSuccess() {
                                    // service broadcasting started

                                 //   mServiceBroadcastingHandler.postDelayed(mServiceBroadcastingRunnable, SERVICE_BROADCASTING_INTERVAL);
                                }

                                @Override
                                public void onFailure(int error) {
                                    // react to failure of adding the local service
                                }
                            });
                }

                @Override
                public void onFailure(int error) {
                    // react to failure of clearing the local services
                }
            });


       discoverService();

    }

    private void discoverService() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        manager.setDnsSdResponseListeners(channel,
                new DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?
                        Log.d(TAG, "onBonjourServiceAvailable "
                                + instanceName + " " + registrationType + " " + srcDevice);
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            devices++;

                            // update the UI and add the item the discovered
                            // device.
                         /*   WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                    .findFragmentByTag("services");
                            if (fragment != null) {
                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                adapter.add(service);
                                adapter.notifyDataSetChanged();*/
                            service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            connectP2p(service);
                            Log.d(TAG, "onBonjourServiceAvailable "
                                    + instanceName);
                            //  }
                        }

                    }
                }, new DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE) + " " + devices);
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest,
                new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(TAG,"Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        Log.d(TAG,"Failed adding service discovery request");
                    }
                });
        manager.discoverServices(channel, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG,"Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(TAG,"Service discovery failed");

            }
        });
    }

    public void connectP2p(WiFiP2pService service) {
        Log.d(TAG, "connectP2p ");
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        //config.groupOwnerIntent = 15; // I want this device to become the owner
       /* if (serviceRequest != null)
            manager.removeServiceRequest(channel, serviceRequest,
                    new ActionListener() {

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });
*/
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG,"Connecting to service");

              //  ReceiverMulticastAsyncTask task = new ReceiverMulticastAsyncTask();
              //  task.execute();
               /* new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DatagramSocket clientSocket=null;
                        try {
                            clientSocket = new DatagramSocket(5454);
                            byte[] receiveData = new byte[1024];
                            byte[] sendData = new byte[1024];
                            while(true)
                            {
                                DatagramPacket receivePacket = new     DatagramPacket(receiveData,receiveData.length);
                                clientSocket.receive(receivePacket);
                                String sentence = new String(receivePacket.getData(),0,receivePacket.getLength());
                                InetAddress IPAddress = receivePacket.getAddress();
                                int port = receivePacket.getPort();
                                Log.d(TAG,"MESSAGE RECEIVED  "+sentence+"  "+IPAddress+"         "+port);
                            }
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }).start();*/

            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG,"Failed connecting to service");
            }
        });
        Log.d(TAG, "connectedP2p ");

    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        try {
            Log.d(TAG, "handleloadPackage ");

            Class<?> wifiP2pService = Class.forName("android.net.wifi.p2p.WifiP2pService", false, lpparam.classLoader);
            for (Class<?> c : wifiP2pService.getDeclaredClasses()) {
                XposedBridge.log("inner class " + c.getSimpleName());
                if ("P2pStateMachine".equals(c.getSimpleName())) {
                    XposedBridge.log("Class " + c.getName() + " found");
                    Method notifyInvitationReceived = c.getDeclaredMethod("notifyInvitationReceived");
                    final Method sendMessage = c.getMethod("sendMessage", int.class);

                    XposedBridge.hookMethod(notifyInvitationReceived, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            final int PEER_CONNECTION_USER_ACCEPT = 0x00023000 + 2;
                            sendMessage.invoke(param.thisObject, PEER_CONNECTION_USER_ACCEPT);
                            return null;
                        }
                    });

                    break;
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        //MyListFragment fragment = (MyListFragment) getFragmentManager()
        //        .findFragmentByTag("services");
        Log.d(TAG, "Connected info "+ p2pInfo);
        info = p2pInfo;
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner "+ p2pInfo.groupOwnerAddress.getHostAddress());
          /*  try {
                handler = new GroupOwnerSocketHandler(
                        ((MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }*/
        } else {
            Log.d(TAG, "Connected as peer " + p2pInfo.groupOwnerAddress.getHostAddress());
           /*  handler = new ClientSocketHandler(
                    ((MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();*/
        }
        // chatFragment = new WiFiChatFragment();
        //getFragmentManager().beginTransaction()
        //        .replace(R.id.container_root, chatFragment).commit();
        //peersList.addItems(p2pInfo);
        //statusTxtView.setVisibility(View.GONE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Nfdc nfdc = new Nfdc();
                    int faceId = 0;
                    faceId = nfdc.faceCreate("udp4://192.168.49.255");
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
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        // Out with the old, in with the new.
        peers.clear();
        peers.addAll(peerList.getDeviceList());

        // If an AdapterView is backed by this data, notify it
        // of the change.  For instance, if you have a ListView of available
        // peers, trigger an update.
        // ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

      //  peersList.clearItems();
        ArrayList<String> list = new ArrayList<String>();
        for(int i= 0;i<peers.size();i++)
        {
            WifiP2pDevice dev = (WifiP2pDevice)peers.get(i);
            Log.d(TAG, "Device "+dev);
            if(dev.status==0) {
                Log.d(TAG, "Peers " + peerList.getDeviceList().size());

                list.add("Device: " + dev.deviceName);
            }

        }
        KebappApplication app = (KebappApplication) getApplication();
        app.setDeviceList(list);

        Log.d(TAG, "Peers "+peers.size());
        if (peers.size() == 0) {
            Log.d(TAG, "No devices found");
            return;
        }

    }

    // AsyncTask for register the routes on NFD
    private class RegisterNFD extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "Register NFD Task";
        private boolean shouldStop = false;
        private Face mFace;
        //private KebappApplication app;
        //private Context context;

        @Override
        protected Void doInBackground(Void... params) {

            try {

                Nfdc ndfc = new Nfdc();
                int faceID = ndfc.faceCreate("udp://127.0.0.1");
                ndfc.ribRegisterPrefix(new Name("/kebapp"), faceID, 10, true, false);
                ndfc.shutdown();

            } catch (SecurityException e) {
                Log.e(RegisterNFD.TAG, "Register Failed");
            } catch(Exception e) {
                Log.e(RegisterNFD.TAG, e.toString());
            }
            return null;
        }
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

        //char[] buffer = new char[1024];

        /*
                                     JSONArray routes = jsonObject.getJSONArray("routes");

                                //JSONObject legs = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance");
                                JSONArray legs = routes.getJSONObject(0).getJSONArray("legs");
                                JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");

                                String content = steps.toString();

                                //JSONObject obj = new JSONObject();
                                //obj.put("id", "JSON Object test");
                                //String content = obj.toString();

         */

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

    public class ReceiverMulticastAsyncTask extends AsyncTask<Void, Integer ,String > {

        @Override
        protected String doInBackground(Void... params) {


            DatagramSocket clientSocket=null;
            try {
                clientSocket = new DatagramSocket(5454);
                byte[] receiveData = new byte[1024];
                byte[] sendData = new byte[1024];
                while(true)
                {
                    DatagramPacket receivePacket = new     DatagramPacket(receiveData,receiveData.length);
                    clientSocket.receive(receivePacket);
                    String sentence = new String(receivePacket.getData(),0,receivePacket.getLength());
                    InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();
                    Log.d(TAG,"MESSAGE RECEIVED  "+sentence+"  "+IPAddress+"         "+port);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            //do whatever...
        }
    }

}