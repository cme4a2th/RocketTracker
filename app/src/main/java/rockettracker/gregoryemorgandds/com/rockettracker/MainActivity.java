package rockettracker.gregoryemorgandds.com.rockettracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import android.Manifest;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;

import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import android.support.annotation.NonNull;

import java.io.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.nlopez.smartlocation.*;
import io.nlopez.smartlocation.location.config.*;

import android.location.Location;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

import app.akexorcist.bluetotohspp.library.*;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.*;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends Activity implements OnMapReadyCallback {

    /* The bluetooth address of the handheld device. */
    public static final String HANDHELD_BT_DEVICE_ADDR = "98:D3:31:70:6D:F6";
    /* The vendor ID of the USB serial device. */
    public static final int HANDHELD_USB_DEVICE_VID = 0x0403;
    /* The product ID of the USB serial device. */
    public static final int HANDHELD_USB_DEVICE_PID = 0x6015;

    public static String DegreesToCardinalDetailed(double degrees) {
        if (degrees < 0) return "Error";
        degrees *= 10;

        String[] caridnals = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW", "N" };
        return caridnals[(int)Math.round((degrees % 3600) / 225) ];
    }

    public static double GreatCircle(double lon1, double lat1, double lon2, double lat2) {
        double x1 = Math.toRadians(lon1);
        double y1 = Math.toRadians(lat1);
        double x2 = Math.toRadians(lon2);
        double y2 = Math.toRadians(lat2);

        // great circle distance in radians
        double angle1 = Math.acos(Math.sin(x1) * Math.sin(x2) + Math.cos(x1) * Math.cos(x2) * Math.cos(y1 - y2));
        // convert back to degrees
        angle1 = Math.toDegrees(angle1);
        // each degree on a great circle of Earth is 60 nautical miles
        double distance1 = 60 * angle1 * 6076.12;

        return distance1;
    }

    public static double bearingDegrees(double lat1, double long1, double lat2, double long2)
    {
        double degToRad= Math.PI/180.0;

        double phi1= lat1 * degToRad;
        double phi2= lat2 * degToRad;
        double lam1= long1 * degToRad;
        double lam2= long2 * degToRad;

        double val = Math.atan2(Math.sin(lam2-lam1) * Math.cos(phi2),
                Math.cos(phi1)*Math.sin(phi2) - Math.sin(phi1)*Math.cos(phi2)*Math.cos(lam2-lam1)
        ) * 180/Math.PI;

        if (val < 0) {
            val = 360 - (val*-1);
        }

        return val;
    }

    public final double ToDecimalDegrees(double v, String d) {
        int thousands = (int)(getDecimal(v*0.00001)*1000);
        double base = v - (thousands*100);
        double val = thousands + (base/60);
        if (d.equals("S") || d.equals("W")) {
            val *= -1;
        }
        return val;
    }

    public final double getDecimal(double d) {
        int integer = (int)d;
        return (10 * d - 10 * integer)/10;
    }

    public final String DumbTimeToGoodTime(double time) {
        int thousands = (int)(getDecimal(time*0.000001)*100);
        int hundreds = (int)(getDecimal(time*0.0001)*100);
        int tens = (int)(getDecimal(time*0.01)*100);
        return String.format("%02d:%02d:%02d", thousands, hundreds, tens);
    }

    public static final int LOCATION_PERMISSION_ID = 1001;
    public static final int READ_PERMISSION_ID = 1002;

    /* UI TEXT VIEWS */

    private TextView connLabel;
    private TextView myLatLabel;
    private TextView myLonLabel;
    private TextView myAltLabel;
    private TextView latLabel;
    private TextView lonLabel;
    private TextView altLabel;
    private TextView timeLabel;
    private TextView angleLabel;
    private TextView rangeLabel;
    private TextView bearingLabel;
    private TextView maximum;
    private TextView textdata;
    private TextView manlat;
    private TextView manlong;
    private TextView manalt;

    /* UI BUTTONS */

    private Button connButton;
    private Button calButton;
    private Button gotomeButton;
    private Button gotorocketButton;
    private Button mapdataButton;
    private Button manButton;

    /* UI SWITCHES */

    private Switch btusb;

    private String markerString;

    private Marker meMarker;
    private Marker rocketMarker;

    private double lat;
    private double lon;
    private double alt;
    private double mylat;
    private double mylon;
    private double myalt;
    private double lastalt = 0;
    private double altcal = 0;
    private double maxalt = 0;
    private double mlon = 0;
    private double mlat = 0;
    private double malt = 0;

    private double time = 0;

    private boolean manflag = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        /* Connected status text field. */
        connLabel = (TextView) findViewById(R.id.conn_id);
        connLabel.setTextColor(Color.RED);

        /* Tablet lat, lon, alt text fields. */
        myLatLabel = (TextView) findViewById(R.id.mylat_id);
        myLonLabel = (TextView) findViewById(R.id.mylon_id);
        myAltLabel = (TextView) findViewById(R.id.myalt_id);

        /* Rocket lat, lon, alt text fields. */
        latLabel = (TextView) findViewById(R.id.lat_id);
        lonLabel = (TextView) findViewById(R.id.lon_id);
        altLabel = (TextView) findViewById(R.id.alt_id);

        /* Time text field. */
        timeLabel = (TextView) findViewById(R.id.time_id);

        /* Angle, range, bearing text fields. */
        angleLabel = (TextView) findViewById(R.id.angle_id);
        rangeLabel = (TextView) findViewById(R.id.range_id);
        bearingLabel = (TextView) findViewById(R.id.bearing_id);

        /* Max alt field. */
        maximum = (TextView) findViewById(R.id.maximum);

        /* Manual lat, lon, alt text fields. */
        manlat = (TextView) findViewById(R.id.manlat);
        manlong = (TextView) findViewById(R.id.manlong);
        manalt = (TextView) findViewById(R.id.manalt);

        /* Data text view. */
        textdata = (TextView) findViewById(R.id.textdata);
        textdata.setTextColor(Color.BLUE);
        textdata.setMovementMethod(new ScrollingMovementMethod());
        textdata.setVerticalScrollBarEnabled(true);
        textdata.setMaxLines(Integer.MAX_VALUE);
        textdata.setGravity(Gravity.BOTTOM);

        /* Map view. */
        mv = findViewById(R.id.map_id);
        mv.onCreate(savedInstanceState);
        mv.getMapAsync(this);

        connButton = findViewById(R.id.connect_id);
        connButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (anyConnected) {
                    disconnectAll();
                } else {
                    connectAll();
                }
            }
        });

        mapdataButton = findViewById(R.id.mapdata);
        mapdataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mv.getVisibility() == View.INVISIBLE) {
                    showMap();
                } else {
                    hideMap();
                }
            }
        });

        gotomeButton = findViewById(R.id.gotome);
        gotomeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goToMe();
                showMap();
            }
        });

        gotorocketButton = findViewById(R.id.gotorocket);
        gotorocketButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goToRocket();
                showMap();
            }
        });

        manButton = findViewById(R.id.manButton);
        manButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (manButton.getText().equals("----")) {

                    if (manlat.getText().toString().equals("")) return;
                    if (manlong.getText().toString().equals("")) return;
                    if (manalt.getText().toString().equals("")) return;

                    manButton.setText("Remote-2");
                    manflag = true;

                    mlat = Double.valueOf(manlat.getText().toString());
                    mlon = Double.valueOf(manlong.getText().toString());
                    malt = Double.valueOf(manalt.getText().toString());
                    mlat = ToDecimalDegrees(mlat, "N");
                    mlon = ToDecimalDegrees(mlon, "W");

                } else {
                    manButton.setText("----");
                    manflag = false;
                }

                updatePositions();
                goToRocket();
            }
        });

        calButton = findViewById(R.id.cal_id);
        calButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (lastalt != 0) {
                    altcal = lastalt-myalt;
                }
            }
        });

        requestPermissions();
        configureBluetooth();
        configureUSB();
        updateUI();

    }

    /* Requests all of the permissions needed for the app to function properly. */
    private void requestPermissions() {

        /* Request storage R/W permission. */
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_PERMISSION_ID);
        }

        /* Request location permission. */
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_ID);
        }

    }

    /* Called when we hear back from the permissions intent. */
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /* If we were granted access to location services, start listening for location data. */
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            listenLocation();
        }
    }

    /* Bluetooth connection handler functions. */

    /* Captures the connected state of the Bluetooth device. */
    private boolean bluetoothConnected = false;
    /* The bluetooth manager. */
    private BluetoothSPP bt = null;

    private void configureBluetooth() {

        bt = new BluetoothSPP(getApplicationContext());
        bt.setupService();
        bt.startService(BluetoothState.DEVICE_OTHER);

        bt.setOnDataReceivedListener(new OnDataReceivedListener() {
            /* This is called when a new bluetooth data packet is received. */
            public void onDataReceived(byte[] data, String message) {
                parseBluetoothData(message);
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothConnectionListener() {
            /* This is called when a bluetooth device is connected. */
            public void onDeviceConnected(String name, String address) {
                bluetoothConnected = true;
                updateUI();
            }
            /* This is callde with a bluetooth device is disconnected. */
            public void onDeviceDisconnected() {
                bluetoothConnected = false;
                closeLogFile();
                updateUI();
            }
            /* This is called when the tablet fails to connect to a bluetooth device. */
            public void onDeviceConnectionFailed() {
                bluetoothConnected = false;
                showAlert("Failed to connect to Bluetooth device.");
                updateUI();
            }

        });

    }

    private void connectBluetooth() {
        bt.connect(HANDHELD_BT_DEVICE_ADDR);
    }

    private void disconnectBluetooth() {
        bt.disconnect();
    }

    /* USB connection handler functions. */

    private boolean usbConnected = false;
    UsbDevice usbDevice = null;
    UsbDeviceConnection usbConnection = null;
    UsbSerialDevice usbSerial = null;

    private static final String ACTION_USB_ATTACHED  = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED  = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String ACTION_USB_PERMISSION  = "com.blecentral.USB_PERMISSION";

    private static final ByteArrayOutputStream usbStream = new ByteArrayOutputStream();

    private void configureUSB() {

        System.out.println("Connecting USB");

        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);

        BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            System.out.println("Permission granted!");
                        } else {
                            System.out.println("Permission revoked!");
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_USB_DETACHED);
        registerReceiver(mUsbReceiver,filter);

        Map<String, UsbDevice> map = manager.getDeviceList();
        Iterator<UsbDevice> deviter = map.values().iterator();

        /* Enumerate the attached USB devices. */
        while (deviter.hasNext()) {
            UsbDevice d = deviter.next();
            System.out.println("Found USB device: " + d.getDeviceName());
            /* Match on the USB device that has the VID and PID we want. */
            if (d.getVendorId() == HANDHELD_USB_DEVICE_VID && d.getProductId() == HANDHELD_USB_DEVICE_PID) {
                usbDevice = d;
            }
        }

        if (usbDevice == null) {
            System.out.println("No device!");
            showAlert("Failed to connect to USB device.");
            return;
        }

        /* Request permissions to access the USB device. */
        if (!manager.hasPermission(usbDevice)) {
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(usbDevice, mPermissionIntent);
            return;
        }

        /* Open the USB device. */
        usbConnection = manager.openDevice(usbDevice);

    }

    private void connectUSB() {
        /* Create a serial endpoint */
        usbSerial = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbConnection);

        /* Configure the serial endpoint. */
        usbSerial.open();
        usbSerial.setBaudRate(9600);
        usbSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        usbSerial.setParity(UsbSerialInterface.PARITY_NONE);
        usbSerial.setStopBits(1);
        usbSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

        UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
            @Override public void onReceivedData(byte[] data) {
                try {
                    usbStream.write(data);
                } catch (IOException e) {

                }

                if (data[data.length - 1] == '\n') {
                    parseUSBData(usbStream.toString());
                    usbStream.reset();
                }
            }
        };

        /* Start receiving USB data. */
        usbSerial.read(mCallback);
    }

    private void disconnectUSB() {
        usbSerial.close();
        usbConnection.close();
    }

    /* Global connection handler functions. */

    /* Connects to all of the available devices. */
    private void connectAll() {
        openLogFile();
        connectBluetooth();
        connectUSB();
    }

    /* Disconnects from all of the available devices. */
    private void disconnectAll() {
        disconnectBluetooth();
        disconnectUSB();
        closeLogFile();
    }

    /* --- Location handler functions. --- */

    private GoogleMap map;
    private MapView mv;

    public void listenLocation() {
        long mLocTrackingInterval = 1000 * 5; // 5 sec
        float trackingDistance = 0;


        LocationAccuracy trackingAccuracy = LocationAccuracy.HIGH;

        LocationParams.Builder builder = new LocationParams.Builder()
                .setAccuracy(trackingAccuracy)
                .setDistance(trackingDistance)
                .setInterval(mLocTrackingInterval);

        SmartLocation.with(getApplicationContext()).location().continuous().config(builder.build()).start(new OnLocationUpdatedListener() {
            @Override public void onLocationUpdated(Location l) {
                System.out.println("Got new location.");

                /* The the tablet's latitude. */
                mylat = l.getLatitude();
                /* Set the tablet's longitude. */
                mylon = l.getLongitude();
                /* Convert the altitude from meters to feet. Set the tablet's altitude. */
                myalt = l.getAltitude() * 3.28084;

                updatePositions();
                updateUI();
            }
        });
    }

    /* Called when we are ready to display the map. */
    @Override public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        LatLng me = new LatLng(mylat, mylon);
        LatLng rocket = new LatLng(lat, lon);
        meMarker = map.addMarker(new MarkerOptions().position(me).title("Me"));
        rocketMarker = map.addMarker(new MarkerOptions().position(rocket).title("Rocket"));

        mv.onResume();
    }

    /* SD and logging handler functions. */

    private PrintWriter stream = null;
    FileOutputStream os = null;

    /* Opens a new log file on the SD card. */
    private void openLogFile() {
        File dir = new File ("/sdcard/logs");
        System.out.println("Going into " + dir.getPath().toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        File file = new File(dir.getAbsolutePath(), "ReceivingLog " + currentDateTimeString + ".txt");
        try {
            file.createNewFile();
            os = new FileOutputStream(file);
            stream = new PrintWriter(os);
        } catch (IOException e) {
            System.out.println("ERROR: " + e.toString());
        }
    }

    /* Writes to the open log file. */
    private void writeLogFile(String message) {

    }

    /* Closes the open log file */
    private void closeLogFile() {


    }

    /* --- User interface related functions. --- */

    private boolean anyConnected = false;
    private boolean haveLOS = true;

    /* Displays a simple UI alert. */
    private void showAlert(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

        alertDialog.setTitle("Alert");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }

    /* Updates the labels and user interface elements to reflect the current app state. */
    private void updateUI() {
        /* Run UI updates on the UI thread. */
        runOnUiThread(new Runnable() {
            @Override public void run() {

                /* OR the connected states together to get the current connected state */
                anyConnected = bluetoothConnected | usbConnected;

                /* Update the connection status UI. */
                if (anyConnected) {
                    connButton.setText("Disconnect");
                    connLabel.setText("Connected");
                    connLabel.setTextColor(Color.GREEN);
                } else {
                    connLabel.setText("Disconnected");
                    connLabel.setTextColor(Color.RED);
                    connButton.setText("Connect");
                }

                /* Update the tablet position UI. */
                myLatLabel.setText(String.format("%.08f", mylat));
                myLonLabel.setText(String.format("%.08f", mylon));
                myAltLabel.setText(String.format("%.00f' MSL", myalt + altcal));

                timeLabel.setText(DumbTimeToGoodTime(time));

                if (haveLOS) {

                    latLabel.setText("LOS");
                    lonLabel.setText("LOS");
                    altLabel.setText("LOS");
                    bearingLabel.setText("LOS");
                    angleLabel.setText("LOS");
                    rangeLabel.setText("LOS");

                } else {

                    double lat1 = mylat;
                    double lon1 = mylon;
                    double lalt = myalt + altcal;

                    double lat2 = 0;
                    double lon2 = 0;
                    double alt2 = 0;

                    if (manflag) {
                        lon2 = mlon;
                        lat2 = mlat;
                        alt2 = malt;
                    } else {
                        lon2 = lon;
                        lat2 = lat;
                        alt2 = alt;
                    }

                    double bearingDeg = bearingDegrees(lat1, lon1, lat2, lon2);
                    String bearing = DegreesToCardinalDetailed(bearingDeg);
                    double range = GreatCircle(lat1, lon1, lat2, lon2);
                    double angle = Math.toDegrees(Math.atan2(alt2-lalt, range));
                    if (angle < 0) angle = 0;

                    latLabel.setText(String.format("%.08f", lat));
                    lonLabel.setText(String.format("%.08f", lon));
                    altLabel.setText(String.format("%.00f' MSL", alt2));
                    bearingLabel.setText(String.format("%.00f\u00b0 : %s", bearingDeg, bearing));
                    angleLabel.setText(String.format("%.00f\u00b0", angle));

                    if (range < 5280/2) {
                        markerString = String.format("%.00f' MSL : %.00f\u00b0 %s : %.00f feet : %.00f\u00b0", alt2, bearingDeg, bearing, range, angle);
                        rangeLabel.setText(String.format("%.00f feet", range));
                    } else {
                        markerString = String.format("%.00f' MSL : %.00f\u00b0 %s : %.02f miles : %.00f\u00b0", alt2, bearingDeg, bearing, range * 0.000189394, angle);
                        rangeLabel.setText(String.format("%.02f miles", range * 0.000189394));
                    }

                    double agl = maxalt-(myalt+altcal);
                    if (agl<0) agl = 0;

                    maximum.setText(String.format("%.00f' MSL\n%.00f' AGL", maxalt, agl));

                }

            }
        });
    }

    private void updateTextView(String message) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                textdata.append(message + '\n');
            }
        });
    }

    private void showMap() {
        mapdataButton.setText("Data");
        textdata.setVisibility(View.INVISIBLE);
        mv.setVisibility(View.VISIBLE);
    }

    private void hideMap() {
        mapdataButton.setText("Data");
        textdata.setVisibility(View.INVISIBLE);
        mv.setVisibility(View.VISIBLE);
    }

    private void goToRocket() {
        double lat2 = 0;
        double lon2 = 0;

        if (manflag) {
            lon2 = mlon;
            lat2 = mlat;
        } else {
            lon2 = lon;
            lat2 = lat;
        }

        LatLng rocket = new LatLng(lat2, lon2);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(rocket, 17.0f));
    }

    private void goToMe() {
        LatLng me = new LatLng(mylat, mylon);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 17.0f));
    }

    Boolean zoomed = false;

    public void updatePositions() {

        if (meMarker == null || rocketMarker == null) return;

        double lat2 = 0;
        double lon2 = 0;

        if (manflag) {
            lon2 = mlon;
            lat2 = mlat;
        } else {
            lon2 = lon;
            lat2 = lat;
        }

        LatLng me = new LatLng(mylat, mylon);
        meMarker.setPosition(me);
        meMarker.showInfoWindow();

        LatLng rocket = new LatLng(lat2, lon2);
        rocketMarker.setPosition(rocket);
        rocketMarker.setTitle(markerString);
        rocketMarker.showInfoWindow();

        if (!zoomed) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 17.0f));
            zoomed = true;
        }
    }

    /* --- Data parsing functions. --- */

    private void parseUSBData(final String message) {
        System.out.println("Received USB message: '" + message + "'");

        /* If there is no data, skip the parse. */
        if (message.length() == 0) {
            System.out.println("Tried to parse an empty USB message.");
            return;
        }

        updateTextView(message);
        writeLogFile(message);

        /* Start by splitting by comma. */
        final String[] separated = message.split(",");

        if (!separated[0].equals("$BRBTX")) {
            System.out.println("Tried to parse a bad USB message.");
            return;
        }

        /* Grab the time from the message. */
        time = Double.valueOf(separated[3]);

        if (separated[4].equals("")) {
            haveLOS = true;
        } else {

            /* Parse the remaining data if we don't have LOS. */

            String latString = separated[4];
            String decimalLatString = latString.substring(0, latString.length() - 1);
            double tlat = Double.valueOf(decimalLatString);
            char latDChar = latString.charAt(latString.length() - 1);
            String latD = Character.toString(latDChar);

            String lonString = separated[5];
            String decimalLonString = lonString.substring(0, lonString.length() - 1);
            double tlon = Double.valueOf(decimalLonString);
            char lonDChar = lonString.charAt(lonString.length() - 1);
            String lonD = Character.toString(lonDChar);
            lastalt = alt;

            lat = ToDecimalDegrees(tlat, latD);
            lon = ToDecimalDegrees(tlon, lonD);
            // reported alt at 8 is already in feet
            alt = Double.valueOf(separated[8]) * 1;

            if (alt > maxalt) {
                maxalt = alt;
            }
        }

        updatePositions();
        updateUI();

    }

    private void parseBluetoothData(final String message) {
        System.out.println("Got bluetooth message: '" + message + "'");

        /* If there is no data, skip the parse. */
        if (message.length() == 0) {
            System.out.println("Tried to parse an empty bluetooth message.");
            return;
        }

        updateTextView(message);
        writeLogFile(message);

        /* Start by splitting by comma. */
        String[] separated = message.split(",");

        if (!separated[0].equals("$GPGGA")) {
            System.out.println("Tried to parse a bad bluetooth packet.");
            return;
        }

        /* Grab the time from the message. */
        time = Double.valueOf(separated[1]);

        if (separated[2].equals("")) {
            haveLOS = true;
        } else {

            double tlat = Double.valueOf(separated[2]);
            String latD = separated[3];

            double tlon = Double.valueOf(separated[4]);
            String lonD = separated[5];

            lastalt = alt;

            lat = ToDecimalDegrees(tlat, latD);
            lon = ToDecimalDegrees(tlon, lonD);
            alt = Double.valueOf(separated[9]) * 3.28084;

            if (alt > maxalt) {
                maxalt = alt;
            }

        }

        updatePositions();
        updateUI();
    }

}
