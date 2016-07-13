package radiusnetworks.com.eddystonedemo;

import android.content.Context;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;


public class RangingActivity extends ActionBarActivity implements BeaconConsumer, RangeNotifier {

    private BeaconManager mBeaconManager;
    String foundBeacon;
    Button mainButton;
    TelephonyManager telephonyManager;
    WebView webview;
    private Timer mTimer;
    private MyTimerTask mMyTimerTask;
    Boolean found = false;

    @Override
    public void onResume() {
        super.onResume();
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
        mBeaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.setRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                foundBeacon = namespaceId.toString();
                if (foundBeacon.equals("0xba1c51bab3147efee8e5")) {
                    Log.d("Found beacon", foundBeacon);
                    found = true;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mainButton.setText("You can open the door now");
                            mainButton.setBackgroundColor(0xffef0606);
                            mainButton.setClickable(true);
                        }
                    });
                }
                Log.d("RangingActivity", "I see a beacon transmitting namespace id: " + namespaceId +
                        " and instance id: " + instanceId +
                        " approximately " + beacon.getDistance() + " meters away.");
                runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView) RangingActivity.this.findViewById(R.id.foundbeacon)).setText("Found beacon - " + foundBeacon);
                    }
                });
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mBeaconManager.unbind(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);
        mainButton = (Button) findViewById(R.id.mainbutton);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        webview = (WebView)findViewById(R.id.webView);
        mTimer = new Timer();
        mMyTimerTask = new MyTimerTask();
        mTimer.schedule(mMyTimerTask, 1000, 5000);

    }

    public void buttonClick(View view) {
        String ret = telephonyManager.getDeviceId();
        Log.d("IMEI", ret);
        webview.loadUrl("http://192.168.0.204:7080/Redirector/?AVAYAEP__LaunchId=OpenTheDoor&beacon=" +
                foundBeacon + "&imei=" + ret.toString());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ranging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    class MyTimerTask extends TimerTask {

        @Override
        public void run() {
  /////////////////
            if (found) {
                found = false;
            }
            else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mainButton.setText("Open the door");
                        mainButton.setBackgroundColor(0xffcfc3c3);
                        mainButton.setClickable(false);
                        ((TextView) RangingActivity.this.findViewById(R.id.foundbeacon)).setText("");
                    }
                });
            }
        }
    }
}
