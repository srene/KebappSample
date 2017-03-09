package ucl.kebappsample;

import android.app.Application;

import net.named_data.jndn.Name;
import net.named_data.jndn.security.*;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;

import java.util.ArrayList;

/**
 * Created by Zander on 2015/5/28.
 */
public class KebappApplication extends Application {

    private ArrayList<String> deviceList;
    private String myAddress, myDeviceName, ownerAddress, ownerName;
    private ArrayList<String> selectedPhotoPaths;
    private boolean enabled;
    private int netId;

    public static KeyChain keyChain = buildTestKeyChain();

    @Override
    public void onCreate() {
        super.onCreate();
        deviceList = new ArrayList<>();
        selectedPhotoPaths = new ArrayList<>();
    }

    public void setMyAddress(String myAddress) {
        this.myAddress = myAddress;
    }

    public boolean getServiceEnabled() {return enabled; };
    public void setServiceEnabled(boolean enabled) { this.enabled = enabled; }
    public String getMyAddress() {
        return myAddress;
    }

    public void setMyDeviceName(String myDeviceName) {
        this.myDeviceName = myDeviceName;
    }

    public String getMyDeviceName() {
        return myDeviceName;
    }

    public void setOwnerAddress(String ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public ArrayList<String> getDeviceList(){
        return deviceList;
    }

    public void setDeviceList(ArrayList<String> deviceList) {
        this.deviceList = deviceList;
    }

    public int getNetId()
    {
        return netId;
    }

    public void setNetId(int netId)
    {
        this.netId = netId;
    }

    public static KeyChain buildTestKeyChain() {
        MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
        MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
        IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);
        net.named_data.jndn.security.KeyChain keyChain = new net.named_data.jndn.security.KeyChain(identityManager);
        try {
            keyChain.getDefaultCertificateName();
        } catch (net.named_data.jndn.security.SecurityException e) {
            try {
                keyChain.createIdentity(new Name("/test/identity"));
                keyChain.getIdentityManager().setDefaultIdentity(new Name("/test/identity"));
            } catch(net.named_data.jndn.security.SecurityException ee) {
                e.printStackTrace();
            }
        }
        return keyChain;
    }
}
