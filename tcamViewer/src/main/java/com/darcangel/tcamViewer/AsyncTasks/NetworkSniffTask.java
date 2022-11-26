package com.darcangel.tcamViewer.AsyncTasks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

import timber.log.Timber;


public class NetworkSniffTask extends AsyncTask<Void, Void, String> {

    private WeakReference<Context> mContextRef;

    public NetworkSniffTask(Context context) {
        mContextRef = new WeakReference<Context>(context);
    }

    @Override
    protected String doInBackground(Void... voids) {
        Timber.d("Let's sniff the network");

        try {
            Context context = mContextRef.get();

            if (context != null) {

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

                WifiInfo connectionInfo = wm.getConnectionInfo();
                int ipAddress = connectionInfo.getIpAddress();
                String ipString = Formatter.formatIpAddress(ipAddress);


                Timber.d("activeNetwork: " + String.valueOf(activeNetwork));
                Timber.d("ipString: " + String.valueOf(ipString));

                String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
                Timber.d("prefix: " + prefix);

                for (int i = 0; i < 255; i++) {
                    String testIp = prefix + String.valueOf(i);

                    InetAddress address = InetAddress.getByName(testIp);
                    boolean reachable = address.isReachable(500);
                    String hostName = address.getCanonicalHostName();

                    if (reachable)
                        Timber.i("Host: " + String.valueOf(hostName) + "(" + String.valueOf(testIp) + ") is reachable!");
                }
            }
        } catch (Throwable t) {
            Timber.e("Well that's not good. %s", t.toString());
        }

        return null;
    }

    @Override
    public void onPostExecute(String result) {

    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}
