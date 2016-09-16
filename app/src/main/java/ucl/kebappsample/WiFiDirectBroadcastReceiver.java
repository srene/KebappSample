
package ucl.kebappsample;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Face;
import net.named_data.jndn.security.KeyChain;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

//import net.named_data.jndn.*;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private Channel channel;
    private Service service;

    private KebappApplication app;

    private WifiP2pDevice myself;

    public static final String TAG = "WiFiDirect";


    String oAddress;
    String localIP;
    boolean isOwner;
    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param service activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       Service service,KebappApplication app) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.service = service;
        this.app = app;

    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(HelloService.TAG, action);
        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d(HelloService.TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION received");
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d(HelloService.TAG,
                        "Connected to p2p network. Requesting network details");
                manager.requestConnectionInfo(channel, (WifiP2pManager.ConnectionInfoListener) service);
                RequestOwner task = new RequestOwner();
                task.execute();

                //manager.requestPeers(channel, (PeerListListener) activity);

            } else {
                // It's a disconnect
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(HelloService.TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION received");

            WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            //MyListFragment fragment = (MyListFragment) MainActivity.getFragmentManager()
            //        .findFragmentByTag("services");
            //   MainActivity.peersList;

            Log.d(HelloService.TAG, "Device status -" + device.status);

        }    else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(HelloService.TAG, "WIFI_P2P_PEERS_CHANGED_ACTION received");

            // Request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                //           manager.requestPeers(channel, MainActivity.peerListListener);
                manager.requestPeers(channel, (PeerListListener) service);
            }
            Log.d(HelloService.TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            Log.d(HelloService.TAG, "WIFI_P2P_STATE_CHANGED_ACTION received");

        }


    }


    private byte[] getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return inetAddress.getAddress();
                        }
                        //return inetAddress.getHostAddress().toString(); // Galaxy Nexus returns IPv6
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "getLocalIPAddress()", ex);
        } catch (NullPointerException ex) {
            Log.e(TAG, "getLocalIPAddress()", ex);
        }
        return null;
    }

    private String getDottedDecimalIP(byte[] ipAddr) {
        //convert to dotted decimal notation:
        String ipAddrStr = "";
        for (int i=0; i<ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr += ".";
            }
            ipAddrStr += ipAddr[i]&0xFF;
        }
        return ipAddrStr;
    }

    private class RequestOwner extends AsyncTask<Void, Void, Void> {
        private InetAddress groupOwnerAddress;
        //private ProducerActivityFragment fragment;
        private Face mFace;

        private String returnData = "No return data";

        @Override
        protected Void doInBackground(Void... params) {

            Log.i(WiFiDirectBroadcastReceiver.TAG, "Start asyntask");

            manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {

                    try {
                        groupOwnerAddress = info.groupOwnerAddress;
                        //fragment = (ProducerActivityFragment)
                        //        mProducerActivity.getFragmentManager().findFragmentById(R.id.producer_fragment);
                        oAddress = groupOwnerAddress.getHostAddress();
                        isOwner = info.isGroupOwner;
                        mFace = new Face("localhost");
                        KeyChain keyChain = DiscoverActivity.buildTestKeyChain();
                        mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
                        // String name = groupOwnerAddress.getHostName();
                        int mPort = 3000;
                        if (!isOwner) {
                            localIP = getDottedDecimalIP(getLocalIPAddress());

                            Log.i(WiFiDirectBroadcastReceiver.TAG, "NON-owner LOGIC DONE");
                        } else {
                            Log.i(WiFiDirectBroadcastReceiver.TAG, "i'm  the owner");
                            localIP = oAddress;
                            Log.i(WiFiDirectBroadcastReceiver.TAG, "OWNER LOGIC DONE");

                        }
                        app.setOwnerAddress(oAddress);
                        Log.i(WiFiDirectBroadcastReceiver.TAG, "Owner Address: " + oAddress);
                        app.setMyAddress(localIP);
                        Log.i(WiFiDirectBroadcastReceiver.TAG, "My Address:" + localIP);
                       // if(oAddress.equals(localIP)) {
                      //      app.addDevice("/"+localIP, myself.deviceName);
                      //  }
                        Log.i(WiFiDirectBroadcastReceiver.TAG, "Register status: " + returnData);
                       // activity.StartRequest();


                        //fragment.updateGroupOwner(isOwner, oAddress);
                        //fragment.updateMyAddress(localIP);
                        //fragment.updateDisplayContent("myself", localIP, returnData, isOwner);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(WiFiDirectBroadcastReceiver.TAG, e.toString());
                    }
                }
            });


            return null;
        }
    }



}
