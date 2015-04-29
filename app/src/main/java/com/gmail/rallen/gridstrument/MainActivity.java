package com.gmail.rallen.gridstrument;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends ActionBarActivity {
    private final static int PREF_REQ_CODE = 99;

    private GridGLSurfaceView mGLView;

    private String     mOSCServerIP   = "";
    private int        mOSCServerPort = 0;
    private OSCPortOut mOSCPortOut    = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a non-GL View
        //setContentView(R.layout.sample_grid_view);

        // or, create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        mGLView = new GridGLSurfaceView(this);
        setContentView(mGLView);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mGLView.setDPI(dm.xdpi,dm.ydpi);

        SetupOSC();

    }

    private void SetupOSC() {
        boolean needsUpdate = false;

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String prefServerIP = SP.getString("server_ip", getString(R.string.default_host_ip));
        String prefServerPort = SP.getString("server_port", getString(R.string.default_host_port));
        int prefServerPortNum;
        try {
            prefServerPortNum = Integer.parseInt(prefServerPort);
        } catch(NumberFormatException ex) {
            Log.e("SetupOSC","bad port value.  Using default port.");
            prefServerPortNum = Integer.parseInt(getString(R.string.default_host_port));
        }
        Log.i("preferences", "server_ip=" + prefServerIP);
        Log.i("preferences", "server_host=" + prefServerPortNum);
        if(mOSCServerIP != prefServerIP) {
            needsUpdate = true;
            mOSCServerIP = prefServerIP;
        }
        if(mOSCServerPort != prefServerPortNum) {
            needsUpdate = true;
            mOSCServerPort = prefServerPortNum;
        }

        if(needsUpdate) {
            try {
                // Connect to some IP address and port
                mOSCPortOut = new OSCPortOut(InetAddress.getByName(mOSCServerIP), mOSCServerPort);
            } catch (UnknownHostException e) {
                // Error handling when your IP isn't found
                Log.e("OSCPortOut", "Cannot find OSCServerIP " + mOSCServerIP + ":" + mOSCServerPort + " " + e);
                mOSCPortOut = null;
            } catch (Exception e) {
                // Error handling for any other errors
                Log.e("OSCPortOut", "Unknown exception " + e);
                mOSCPortOut = null;
            }
            mGLView.setOSCPortOut(mOSCPortOut);  // TODO test mOSCPortOut == null;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.action_notes_off:
            Log.d("select", "NOTES OFF!");
            if (mOSCPortOut != null) {
                for (int c = 0; c < 16; c++) {
                    new OSCSendMessageTask("/allNotesOff").execute(c);
                }
            }
            return true;
        case R.id.action_settings:
            Log.d("select", "preferences...");
            Intent i = new Intent(this, MainPreferenceActivity.class);
            startActivityForResult(i,PREF_REQ_CODE);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PREF_REQ_CODE) {
            SetupOSC();
        }
    }

    private class OSCSendMessageTask extends AsyncTask<Object, Void, Boolean> {
        private String mAddress;
        OSCSendMessageTask(String address) {
            mAddress = address;
        }
        protected Boolean doInBackground(Object... objs) {
            try {
                OSCMessage message = new OSCMessage(mAddress, Arrays.asList(objs));
                mOSCPortOut.send(message);
            } catch (Exception e) {
                Log.e("OSCSendMessageTask","Unknown exception "+e);
            }
            return true;
        }
    }
}
