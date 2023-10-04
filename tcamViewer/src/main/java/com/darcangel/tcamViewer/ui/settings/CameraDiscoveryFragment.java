package com.darcangel.tcamViewer.ui.settings;

//mDNS Discovery
//        The cameras advertise themselves on the local network using mDNS (Bonjour) starting with
//        firmware revision 3.0 to make discovering their IPV4 addresses easier.
//
//        Service Type: "_tcam-socket._tcp."
//        Host/Instance Name: Camera Name (e.g. "tCam-Mini-87E9")
//        TXT Records:
//        "model": Camera model (e.g. "tCam", "tCam-Mini", "tCam-POE")
//        "interface": Communication interface (e.g. "Ethernet", "WiFi")
//        "version": Firmware version (e.g. "3.0")
//
// Reference: http://developer.android.com/training/connect-devices-wirelessly/nsd.html

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.darcangel.tcamViewer.MainActivity;
import com.darcangel.tcamViewer.R;
import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.databinding.FragmentCameraDiscoveryBinding;
import com.darcangel.tcamViewer.model.Settings;

import java.net.InetAddress;

import timber.log.Timber;

public class CameraDiscoveryFragment extends Fragment implements View.OnClickListener {
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mServiceInfo;
    private String mCameraAddress;
    private MainActivity mainActivity = MainActivity.getInstance();
    private SettingsViewModel settingsViewModel;
    private FragmentCameraDiscoveryBinding binding;
    private ViewGroup container;
    private Settings settings;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        settingsViewModel = mainActivity.getSettingsViewModel();

        binding = FragmentCameraDiscoveryBinding.inflate(inflater, container, false);

        mainActivity.getSupportActionBar().setTitle(R.string.title_camera_discovery);

        //get the settings model
        settings = mainActivity.getSettings();
        binding.setSettings(settings);

        binding.btnCancelSave.btnSave.setOnClickListener(this);
        binding.btnCancelSave.btnCancel.setOnClickListener(this);

        initializeDiscoveryListener();
        initializeResolveListener();
        mNsdManager = (NsdManager) (mainActivity.getSystemService(Context.NSD_SERVICE));
        mNsdManager.discoverServices(Constants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        return binding.getRoot();
    }

    private void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Timber.d("\\\\NSD\\\\ Discovery Started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                String name = service.getServiceName();
                String type = service.getServiceType();
                Timber.d("\\\\NSD\\\\ Service Name=" + name);
                Timber.d("\\\\NSD\\\\ Service Type=" + type);
                if (type.equals(Constants.SERVICE_TYPE)) {
                    Timber.d("\\\\NSD\\\\ Service Found @ '" + name + "'");
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.tvCameraName.setText(name);
                        }
                    });

                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Timber.d("\\\\NDS\\\\ Service Lost");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Timber.d("\\\\NDS\\\\ Service Stopped");
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
                Timber.d("\\\\NDS\\\\ Start Service Discovery Failed, error = %d", errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
                Timber.d("\\\\NDS\\\\ Stop Service Discovery Failed, error = %d", errorCode);
            }
        };
    }

    private void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Timber.e("\\\\NSD\\\\ Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                mServiceInfo = serviceInfo;

                // Port is being returned as 9. Not needed.
                //int port = mServiceInfo.getPort();

                InetAddress host = mServiceInfo.getHost();
                mCameraAddress = host.getHostAddress();
                Timber.d("\\\\NSD\\\\ Resolved address = " + mCameraAddress);
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.tvCameraAddress.setText(mCameraAddress);
                    }
                });
            }
        };
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.btnSave) {
            settings.setCameraAddress(mCameraAddress);
        }
        mainActivity.getNavController().popBackStack();
    }
}
