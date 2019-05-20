package github.biswajee.lastleaf;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, BeaconConsumer {

    // Initiating bluetooth connect request to raspberry pi
    final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    TextView status;
    public static final String TAG = "BeaconsEverywhere";
    private BeaconManager beaconManager;
    private BeaconTransmitter beaconTransmitter;
    DataOutputStream os = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        status = findViewById(R.id.status);
        FloatingActionButton gpsBtn = findViewById(R.id.gps_btn);
        FloatingActionButton fab = findViewById(R.id.fab);
        AppCompatButton piConnect = findViewById(R.id.btn_connect);
        // Call to BeaconGenerator class for Nearby Messages API...
        // final BeaconGenerator cookieMonster = new BeaconGenerator(getApplicationContext());


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Feedback: roy.biswajeet161@gmail.com", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, 123);

        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Triangulating now...", Toast.LENGTH_SHORT).show();
              GPStracker g = new GPStracker(getApplicationContext());
              Location l = g.getLocation();
              if(l!=null) {
                  status.setText("Latitude : " + l.getLatitude() + " Longitude : " + l.getLongitude());
              } else {
                  Toast.makeText(MainActivity.this, "Location update failed", Toast.LENGTH_SHORT).show();
              }
            }
        });

        piConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GPStracker g = new GPStracker(getApplicationContext());
                Location l = g.getLocation();
                if (l != null) {
                    Beacon beacon = new Beacon.Builder()
                            .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
       	                    .setId2("" + (short)l.getLatitude())
                            .setId3("" + (short)l.getLongitude())
                            .setManufacturer(0x004C)
                            .setTxPower(-59)
                            .setDataFields(Arrays.asList(new Long[]{(long)l.getLatitude(), (long)l.getLongitude()}))
                            .build();
                    BeaconParser beaconParser = new BeaconParser()
                            .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
                    beaconTransmitter = new BeaconTransmitter(MainActivity.this, beaconParser);
                    beaconTransmitter.startAdvertising(beacon);
                    Toast.makeText(MainActivity.this, "Transmitting Beacon! ", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Could not get location data!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Register for bluetooth broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryResult, filter);

        // Bluetooth button trigger
        FloatingActionButton bluetoothBtn = findViewById(R.id.bluetooth_btn);
        bluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Initiating connection", Toast.LENGTH_SHORT).show();
                registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                bluetooth.enable();
                if (!bluetooth.isDiscovering()) {
                    bluetooth.startDiscovery();
                }
                Toast.makeText(MainActivity.this, "Operation Complete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBeaconServiceConnect() {
        final Region region = new Region("<someUUID>", Identifier.parse("<replaceBySomeUIID>"), null, null);

        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                try {
                    Log.d(TAG, "didEnterRegion");
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                try {
                    Log.d(TAG, "didExitRegion");
                    beaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for(Beacon oneBeacon : beacons) {
                    Log.d(TAG, "distance: " + oneBeacon.getDistance() + " id:" + oneBeacon.getId1() + "/" + oneBeacon.getId2() + "/" + oneBeacon.getId3());
                }
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

public class clientSock extends Thread {
        public void run() {
            try {
                os.writeBytes("anything you want"); // anything you want
                os.flush();
            } catch (Exception e1) {
                e1.printStackTrace();
                return;
            }
        }
    }

    // Bluetooth broadcast receiver function...
    BroadcastReceiver discoveryResult = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String remoteDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            BluetoothDevice remoteDevice;

            remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            Toast.makeText(getApplicationContext(), "Discovered: " + remoteDeviceName + " address " + remoteDevice.getAddress(), Toast.LENGTH_SHORT).show();

            try{
                BluetoothDevice device = bluetooth.getRemoteDevice(remoteDevice.getAddress());

                Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});

                BluetoothSocket clientSocket =  (BluetoothSocket) m.invoke(device, 1);

                clientSocket.connect();

                os = new DataOutputStream(clientSocket.getOutputStream());

                new clientSock().start();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("BLUETOOTH", e.getMessage());
            }
        }
    };

    // GENERIC CODE BELOW...DO NOT MODIFY...
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_tools) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        Toast.makeText(this, "Beacon advertising stopped", Toast.LENGTH_SHORT).show();
        // unregister the ACTION_FOUND receiver.
        unregisterReceiver(discoveryResult);
        super.onDestroy();
    }
}
