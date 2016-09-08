package ucl.kebappsample;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
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
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscoverActivity extends Activity implements
        ConnectionInfoListener, PeerListListener {
       // PeerListListener {

    private Channel channel;
    private BroadcastReceiver receiver = null;
    private List peers = new ArrayList();

    int devices = 0;

    public static final String TAG = "DiscoverActivity";

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_kebapptest";
    public static final String SERVICE_REG_TYPE = "_kebapp._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager manager;

    static final int SERVER_PORT = 4545;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    public MyListFragment peersList;

    private final IntentFilter intentFilter = new IntentFilter();
    private TextView statusTxtView;

    private WiFiP2pService service;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusTxtView = (TextView) findViewById(R.id.status_text);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        startRegistrationAndDiscovery();

        peersList = new MyListFragment();
        getFragmentManager().beginTransaction()
                .add(R.id.container_root, peersList, "services").commit();


        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }


    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
            }
        });

        discoverService();

    }


    @Override
    public void onResume() {
        super.onResume();

    }


    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void discoverService() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.Connecting
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
                        appendStatus("Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        appendStatus("Failed adding service discovery request");
                    }
                });
        manager.discoverServices(channel, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                appendStatus("Service discovery failed");

            }
        });
       /* manager.discoverPeers(channel, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Discovery Initiated");
            }

            @Override
            public void onFailure(int reasonCode) {
                appendStatus("Discovery Failed : " + reasonCode);
            }
        });*/
    }

    public void connectP2p(WiFiP2pService service) {
        Log.d(TAG, "connectP2p ");
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;
        //config.groupOwnerIntent = 15; // I want this device to become the owner

        if (serviceRequest != null)
            manager.removeServiceRequest(channel, serviceRequest,
                    new ActionListener() {

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });

        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Connecting to service");


            }

            @Override
            public void onFailure(int errorCode) {
                appendStatus("Failed connecting to service");
            }
        });
        Log.d(TAG, "connectedP2p ");

    }

    public void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(current + "\n" + status);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        // InetAddress from WifiP2pInfo struct.
        //InetAddress groupOwnerAddress = info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we can determine the group owner.
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.

        }
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

        peersList.clearItems();
        for(int i= 0;i<peers.size();i++)
        {
            WifiP2pDevice dev = (WifiP2pDevice)peers.get(0);
            Log.d(DiscoverActivity.TAG, "Device "+dev);
             //peersList.addItems(dev.deviceName+" "+ service.instanceName);
            if(dev.status==0)
            {
                peersList.addItems(dev.deviceName);
            }

        }
        Log.d(DiscoverActivity.TAG, "Peers "+peers.size());
        if (peers.size() == 0) {
            Log.d(DiscoverActivity.TAG, "No devices found");
            return;
        }
    }


    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void onStop() {
        super.onStop();

        disconnect();
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

    public void StartRequest() {
        Log.d(TAG,"Start request");
        RequestDeviceListTask task = new RequestDeviceListTask((KebappApplication) getApplication(), getApplicationContext());
        task.execute();
    }

    public void AddFace(){
        Log.d(TAG, "Group Formed");
        try {
            Face mFace;
            KebappApplication app = (KebappApplication) getApplication();
            String oAddress = app.getOwnerAddress();
            Log.i(RequestDeviceListTask.TAG, "Owner Address: " + oAddress);
            KeyChain keyChain = app.keyChain;
            mFace = new Face("localhost");
            mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
            Nfdc nfdc = new Nfdc();
            int faceId = nfdc.faceCreate("udp://" + oAddress);
            nfdc.ribRegisterPrefix(new Name("/kebapp"), faceId, 10, true, false);
            nfdc.shutdown();
        } catch (Exception e) {
            Log.e(RequestDeviceListTask.TAG, e.toString());
        }
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
        }
        @Override
        protected ArrayList<DeviceInfo> doInBackground(String... params) {

            try {
                // KeyChain keyChain = buildTestKeyChain();
                KeyChain keyChain = app.keyChain;
                mFace = new Face("localhost");
                mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                // Invalid input
                /*if(params.length < 1) {
                    Log.e(RequestDeviceListTask.TAG, "No owner address!");
                    return null;
                }*/

                KebappApplication app = (KebappApplication) getApplication();
                String oAddress = app.getOwnerAddress();
                Nfdc nfdc = new Nfdc();
                int faceId = nfdc.faceCreate("udp://" + oAddress);
                nfdc.ribRegisterPrefix(new Name("/kebapp"), faceId, 10, true, false);
                nfdc.shutdown();
                Log.i(RequestDeviceListTask.TAG, "Face created");
                Interest interest = new Interest(new Name("/kebapp"));
                interest.setInterestLifetimeMilliseconds(10000);
                Log.i(RequestDeviceListTask.TAG, "Interest created");

                mFace.expressInterest(interest, new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {
                        String content = data.getContent().toString();
                        try {
                            final JSONObject object = new JSONObject(content);
                            Log.i(RequestDeviceListTask.TAG, "The content has been received " + object.getString("text"));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        //appendStatus("The distance is " +  object.getString("text"));
                                        Toast.makeText(getApplicationContext(), "The distance is " +  object.getString("text"), Toast.LENGTH_LONG).show();

                                    } catch (JSONException e) {
                                        Log.e(RequestDeviceListTask.TAG, "Failed to construct the JSON");

                                    }
                                }
                            });

                        } catch (JSONException e) {
                            Log.e(RequestDeviceListTask.TAG, "Failed to construct the JSON");

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

        @Override
        protected void onPostExecute(ArrayList<DeviceInfo> deviceInfos) {
            super.onPostExecute(deviceInfos);
            if(deviceInfos != null) {
                // PhotoSharingApplication application = (PhotoSharingApplication) getApplication();
                for (DeviceInfo info : deviceInfos) {
                    app.addDevice(info.ipAddress, info.deviceName);
                }

               // intent.putParcelableArrayListExtra("devices", deviceInfos);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //context.startActivity(intent);

                RegisterNFD task = new RegisterNFD(app);
                task.execute(deviceInfos);
            }
        }
    }

    // AsyncTask for register the routes on NFD
    private class RegisterNFD extends AsyncTask<ArrayList<DeviceInfo>, Void, Void> {

        private static final String TAG = "Register NFD Task";
        private boolean shouldStop = false;
        private Face mFace;
        private KebappApplication app;
        private Context context;

        public RegisterNFD(KebappApplication app) {
            this.app = app;
            this.context = context;
        }
        @Override
        protected Void doInBackground(ArrayList<DeviceInfo>... params) {

            try {
                // KeyChain keyChain = buildTestKeyChain();

                if(params.length < 1) {
                    Log.e(RegisterNFD.TAG, "No device list");
                    return null;
                }

                ArrayList<DeviceInfo> list = params[0];
                Nfdc ndfc = new Nfdc();
                for(DeviceInfo info : list) {
                    if(info.ipAddress.equals(app.getMyAddress()))
                        continue;
                    int faceID = ndfc.faceCreate("udp:/" + info.ipAddress);
                    ndfc.ribRegisterPrefix(new Name(info.ipAddress), faceID, 10, true, false);
                }
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
        net.named_data.jndn.security.KeyChain keyChain = new net.named_data.jndn.security.KeyChain(identityManager);
        try {
            keyChain.getDefaultCertificateName();
        } catch (net.named_data.jndn.security.SecurityException e) {
            keyChain.createIdentity(new Name("/test/identity"));
            keyChain.getIdentityManager().setDefaultIdentity(new Name("/test/identity"));
        }
        return keyChain;
    }

}