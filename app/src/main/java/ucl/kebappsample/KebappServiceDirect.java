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
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
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


public class KebappServiceDirect extends Service implements
        ConnectionInfoListener,PeerListListener{ //, IXposedHookLoadPackage {

    private Channel channel;
    private BroadcastReceiver receiver = null;

    private List peers = new ArrayList();

    int devices = 0;

    public static final String TAG = "KebappService";

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

    private WiFiP2pService service;


    IBinder mBinder = new LocalBinder();


    //private int faceId;
    KebappApplication kapp;
    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return mBinder;
    }
    public class LocalBinder extends Binder {
        public KebappServiceDirect getServerInstance() {
            return KebappServiceDirect.this;
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



        kapp = (KebappApplication)getApplication();

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(KebappServiceDirect.this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this,kapp);

        this.registerReceiver(this.receiver, intentFilter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");

        return Service.START_STICKY;
    }


    @Override
    public void onDestroy() {

       // isRunning = false;
        stopSelf();
        unregisterReceiver(this.receiver);
        KebappApplication kapp = (KebappApplication)getApplication();



        disconnect();
        Log.i(TAG, "Service onDestroy");
    }

    public void disconnect()
    {
        Log.d(TAG,"Disconnect");
     //   if(isAPRegistered)unregisterReceiver(this.apReceiver);
     //   if(isDirectRegistered)unregisterReceiver(this.receiver);
        disconnectWifiDirect();
    }


    public void disconnectWifiDirect() {
        //isDirectRegistered = false;
        //stopSelf();
        Log.d(TAG,"DisconnectWifiDirect");
        final int faceId = kapp.getWifiDirectFaceId();
        if(faceId!=-1) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Nfdc nfdc = new Nfdc();

                            Log.d(TAG,"Face destroy");
                            nfdc.ribUnregisterPrefix(new Name("/kebapp/maps/routefinder/"),faceId);
                            nfdc.faceDestroy(faceId);

                    } catch (Exception e) {
                        Log.d(TAG, "Error " + e);
                    }
                }
            }).start();

        } else {
            Log.d(TAG,"No face");
        }

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



    public void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
       // record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        record.put("listenport", String.valueOf(SERVER_PORT));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
        record.put("available", "visible");

       final WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);

        manager.clearLocalServices(channel, new ActionListener() {
                @Override
                public void onSuccess() {
                    manager.addLocalService(channel, service,
                            new ActionListener() {

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


        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG,"Connecting to service");

            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG,"Failed connecting to service");
            }
        });
        Log.d(TAG, "connectedP2p ");

    }


    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        Log.d(TAG, "Connected info "+ p2pInfo);
        info = p2pInfo;
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner "+ p2pInfo.groupOwnerAddress.getHostAddress());

        } else {
            Log.d(TAG, "Connected as peer " + p2pInfo.groupOwnerAddress.getHostAddress());

        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Nfdc nfdc = new Nfdc();
                    int faceId = 0;
                    // sleep(5000);
                    faceId = nfdc.faceCreate("udp4://192.168.49.255");
                    kapp.setWifiDirectFaceId(faceId);
                    //if(!info.isGroupOwner)faceId = nfdc.faceCreate("udp4://"+info.groupOwnerAddress.getHostAddress());
                    //        else faceId = nfdc.faceCreate("udp://"+app.getMyAddress());
                    nfdc.ribRegisterPrefix(new Name("/kebapp/maps/routefinder/"), faceId, 0, true, false);
                    nfdc.shutdown();
                    Log.d(TAG,"Face created "+ faceId + " " + kapp.getWifiDirectFaceId());
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
            if(app.getServiceDirectEnabled())startRegistrationAndDiscovery();
            return;
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






}