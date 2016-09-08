package ucl.kebappsample;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

public class InterestActivity extends Activity {

    public static final String TAG = "InterestActivity";

    private TextView statusTxtView;
    private TextView resultTxtView;
    private Button button;
    private EditText source,dest;

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
        source = (EditText)findViewById(R.id.source);
        dest = (EditText)findViewById(R.id.dest);

        button = (Button) this.findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearStatus();
                StartRequest();
            }
        });
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

    private void StartRequest() {
        Log.d(TAG,"Start request");
        appendStatus("Start request");
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
                Interest interest = new Interest(new Name("/kebapp/maps/"+source.getText().toString()+"/"+dest.getText().toString()));
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

}