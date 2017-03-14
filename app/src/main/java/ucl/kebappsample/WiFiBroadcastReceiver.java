
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
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.List;

//import net.named_data.jndn.*;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
public class WiFiBroadcastReceiver extends BroadcastReceiver {

    /*private WifiP2pManager manager;
    private Channel channel;
    private Service service;

    private KebappApplication app;

    private WifiP2pDevice myself;
*/
    private WifiManager wifiManager;
    private Service service;
    private KebappApplication app;

    private Context context;
    public static final String TAG = "WiFiReceiver";
  //  private String ssid = "KEBAP";
  //  private String passphrase = "raspberry";
    private WifiConfiguration wifiConfiguration;

    //String oAddress;
    //String localIP;
    //boolean isOwner;
    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param service activity associated with the receiver
    x
     */


    public WiFiBroadcastReceiver(WifiManager manager,
                                 Service service, KebappApplication app) {
        super();
        Log.d(TAG,"Receiver2");

        this.wifiManager = manager;
        //this.channel = channel;
        this.service = service;
        this.app = app;

        wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + KebappServiceAp.KEBAPP_SSID + "\"";
        wifiConfiguration.preSharedKey = "\"" + KebappServiceAp.passphrase + "\"";

    //    Log.d(TAG,"WiFiBroadcastReceiver created");

    }



    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        this.context = context;
        BroadcastReceiver awaitIPAddress = null;
        Log.d(TAG,"WiFiBroadcastReceiver onreceive "+action);
        if(app.getServiceEnabled()) {
            if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                Log.d(TAG, "Supplicant state changed " + state);
                KebappServiceAp ks = (KebappServiceAp) service;
                if (SupplicantState.isValidState(state)
                        && state == SupplicantState.COMPLETED) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    Log.d(TAG, "Supplicant state changed " + wifiInfo.getSSID());

                    if (wifiInfo.getSSID().equals(wifiConfiguration.SSID)) {
                        Log.d(TAG, "Disconnect"+wifiInfo.getIpAddress() + " " + wifiIpAddress(wifiInfo.getIpAddress()));

                        if(!ks.isAPRegistered) {
                          //  ks.disconnectWifiDirect();
                            ks.connectAp("10.0.0.1");
                            ks.isAPRegistered = true;
                        }
                    }
                    Log.d(TAG, "Connected");
                } else if (SupplicantState.isValidState(state)
                        && state == SupplicantState.DISCONNECTED) {
                    if(ks.isAPRegistered) {
                        ks.disconnectAP();
                       // ks.startRegistrationAndDiscovery();
                        Log.d(TAG, "Disconnected");
                    }
                }
            } //else
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
             //   Log.d(TAG, "Scan results available");
                //ScanWiFiActivity a = ScanWiFiActivity.instance();
                //WifiManager w = (WifiManager) context
                //        .getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> l = wifiManager.getScanResults();
                //a.Clear();
                //new ScanResult()
                //l.contains()
                for (ScanResult r : l) {
                    //use r.SSID or r.BSSID to identify your home network and take action
               //     Log.d(TAG, r.SSID + "" + r.level + "\r\n");
                    if (r.SSID.equals(KebappServiceAp.KEBAPP_SSID) && !isConnectedViaWifi()) {
                        Log.d(TAG, "Connect to Pi3");
                        int networkId = wifiManager.addNetwork(wifiConfiguration);
                        Log.d(TAG, "networkid "+ networkId);
                        if (networkId != -1) {
                            wifiManager.disconnect();
                            wifiManager.enableNetwork(networkId, true);
                            if(wifiManager.reconnect()==false)
                            {
                                Log.d(TAG,"Reconnect failed");
                            } else {
                                Log.d(TAG,"Reconnect success");
                            }
                            app.setNetId(networkId);
                            // Use this to permanently save this network
                            // Otherwise, it will disappear after a reboot
                            //wifiManager.saveConfiguration();
                        }

                    /*
                    List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                    for( WifiConfiguration i : list ) {
                        if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                            wifiManager.disconnect();
                            wifiManager.enableNetwork(i.networkId, true);
                            wifiManager.reconnect();

                            break;
                        }
                    }*/
                    }
                    // Log.d(TAG, "conneting to: ssid");
                }
            }
        }

    }

    private boolean isConnectedViaWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) service.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    protected String wifiIpAddress(int ipAddress) {

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }



}
