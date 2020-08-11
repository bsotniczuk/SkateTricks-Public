package com.example.bartomiej.skatetricks;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Settings;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    // TODO: (Storing user data) It can store data for how long made trick will appear on the screen [change the (public short time = 50;) value]
    // TODO: Modify checkIfDisconnected thread to check if MTH is connected and when it is not it should show a disconnected animation, or show out of range
    // TODO: Show magnitude of tricks power by displaying max G from accelerometer before/during making a trick
    // TODO: repair resetTrickTextAfterTime() method to improve reset after new trick done
    // TODO: ADD Sleep Mode info, ADD Disconnected Info, ADD BT Dropped Info

    static String MW_MAC_ADDRESS = "CA:A8:C2:B0:46:AB"; //MW Address from a user
    private MetaWearBoard board;

    BluetoothAdapter bluetoothAdapter;

    private BtleService.LocalBinder serviceBinder;

    private int sleepTime = 500; //in ms, to decide whether it is disconnected

    //final Settings settings = board.getModule(Settings.class);

    //frequency of Data Flow in Accelerometer
    static float accelerometerDataFrequency = 50f; //accelerometerDataFrequency must match gyroDataFrequency
    //range of Data in Accelerometer
    private float accelerometerDataRange = 8f;
    //frequency of Data Flow in Gyroscope
    private GyroBmi160.OutputDataRate gyroDataFrequency = GyroBmi160.OutputDataRate.ODR_50_HZ;
    //range of Gyroscope Data
    private GyroBmi160.Range gyroDataRange = GyroBmi160.Range.FSR_2000;
    //when this threshold is exceeded then the app knows that trick has been made
    static float thresholdAcce = (float) 1.50/*2.50*/; //min value -1.05 or 1.05; -1 or 1 is the value of earths gravity
    static float thresholdAcceHandMode = (float) 1.50; //min value -1.05 or 1.05; -1 or 1 is the value of earths gravity
    static float thresholdAcceNormalMode = (float) 2.50; //min value -1.05 or 1.05; -1 or 1 is the value of earths gravity
    public float thresholdGyro = (float) 400.00; //min value -1.05 or 1.05; -1 or 1 is the value of earths gravity

    public boolean isConnectedToMW = false; //unusable, use -> board.isConnected()
    static boolean isAccelerometer = false;
    static boolean isGyroscope = false;

    public boolean isConfigured = false;

    public boolean isStartedAcc = false;
    public boolean isStartedGyr = false;

    public boolean thresholdReached = false;

    static boolean stanceOfUser = false; //false is Goofy stance, true is Regular stance

    static boolean applicationMode = false; //true is debug, false is normal

    //    public boolean accePopulated = false;
    public boolean gyroPopulated = false;

    public boolean isAfterFirstConfig = false;

    public boolean btDropped = false;

    TextView textView1;
    TextView textView2;
    TextView textView3;
    TextView textView4;
    TextView trickTextView;
    TextView trickRotationTextView;
    TextView trickRotationText;
    TextView textViewConn;

    ProgressBar batteryLife;

    LinearLayout LinearLayout03;

    Button stopAllAndSetName;

    pl.droidsonroids.gif.GifImageView trickGif;

    static final int REQUEST_BLUETOOTH = 1;

    //private Accelerometer accelerometer;
    static GyroBmi160 gyroscope;
    //private AccelerometerBmi160 accelerometer;
    static Accelerometer accelerometer;
    //Battery settings
    private Settings settings;

    private Led led;

    byte batteryPercent = -1; //to improve, deJSONify and cast to int
    float counterAcc = 0;
    float counterGyr = 0;

    String acce = "";
    String gyro = "";
    String acceGyro = "";

    String progressData = "";
    String accelerometerDataOutput = "";
    String gyroscopeDataOutput = "";

    String checkAccelerometerChanged = "";

    static String trickRotationTextViewString = "";

    static String recentlyMadeTrick = "";

    StringBuilder acc;
    StringBuilder gyr;
    StringBuilder accGyr;

    List<Acceleration> trickAcc;
    List<AngularVelocity> trickGyr;

    static List<String> trickListDB;

    StringBuilder temp1;

    private String m_Text = "";

    TrickMade trickMade;

    Thread trickThread;

    private int iterator = -1;

    static DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        refreshTextViewThread(); //creates a thread to reload data and info

        initDB(); //create DB if not created yet, populate if not populated

        initViews();

        startBtAndConnectToMetaWear();

        checkIfDisconnected(); //thread that checks if Data Flows from the MetaWear device

        if (bluetoothAdapter != null) //if bluetooth is supported then start on AppClosedService
            startService(new Intent(this, AppClosedService.class)); //start Service that checks if app was closed from recent apps tab
    }

    public void startBtAndConnectToMetaWear() {
        //getting bluetooth object
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //confirming devices bluetooth connectivity ability
        if (bluetoothAdapter == null) {
            trickRotationTextView.setText("Your device does not\nsupport Bluetooth 4.0 LE\nconnectivity :(");
            textViewConn.setText("");
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                //Request user to enable BT
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_BLUETOOTH);

                ShowToast("Bluetooth is not enabled");
            } else {
                //Bind the service if BT is enabled
                getApplicationContext().bindService(new Intent(this, BtleService.class),
                        this, Context.BIND_AUTO_CREATE);

                if (applicationMode == true) ShowToast("Bluetooth is enabled");
                Log.i("SkateTricks", "Bluetooth is enabled");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            stopAccelerometer();
            stopGyroscope();

            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_stats) {
            stopAccelerometer();
            stopGyroscope();

            Intent intent = new Intent(getApplicationContext(), StatsActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            stopAccelerometer();
            stopGyroscope();

            finish();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public short iterator10 = 0;
    public short time = 50; //10 is 1 second after setting a trick

    public int timeOfProgramLaunched = 0; //10 is 1 second after setting a trick

    //refreshing data and progress
    public void refreshTextViewThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(100);  //1000ms = 1 sec

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView1.setText(progressData);
                                textView2.setText(accelerometerDataOutput);
                                textView3.setText(gyroscopeDataOutput);

                                if (batteryLife.getProgress() <= 0 && batteryPercent != -1) { //enter only once
                                    batteryLife.setProgress(batteryPercent);
                                    StringBuilder sb = new StringBuilder(getString(R.string.battery1));
                                    sb.append(" ");
                                    sb.append(batteryPercent);
                                    sb.append("%");
                                    textView4.setText(sb.toString());
                                    Log.i("BT", "Entered");
                                }

                                timeOfProgramLaunched++;
                                if (timeOfProgramLaunched == 10 * 60 * 10 * 10) //reset program timer after 100 minutes
                                    timeOfProgramLaunched = 0;

                                resetTrickTextAfterTime(time);
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
    }

    public void checkIfDisconnected() {
        Thread s = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(sleepTime);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (isStartedAcc /*|| checkAccelerometerChanged.compareToIgnoreCase(accelerometerDataOutput) != 0*/) {
                                    if (checkAccelerometerChanged.compareToIgnoreCase(accelerometerDataOutput) == 0) {
                                        //data didn't change
                                        if (iterator == 2) {
                                            textViewConn.setText(R.string.services_reset);
                                        }

                                        if (iterator == 0) {
                                            Log.i("SkateTricks", "sleep mode or disconnected from MTH");
                                            textViewConn.setText(R.string.sleep_mode);
                                            //isStartedAcc = false;

                                            iterator = 1; // ???

                                            if (timeOfProgramLaunched == 5 * 60 * 10) { //after 10 mins of no app usage
                                                stopAccelerometer();
                                                stopGyroscope();
                                            }
                                        }
                                    } else {
                                        //data changed
                                        Log.i("SkateTricks", "MTH Streams Data");
                                        textViewConn.setText(R.string.mth_streams_data);
                                    }
                                    checkAccelerometerChanged = accelerometerDataOutput;
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        s.start();
    }

    public boolean trickSet = false;
    public boolean breakOut = false;

    //upgrade it to work asynchronously not every second
    public void checkTrick() { //thread to synchronize incoming data and to send it to data analyzer
        trickThread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        //Thread.sleep(1000);  //1000ms = 1 sec
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DataSamples trickInside = null; //initiate

                                breakOut = false;
                                //wait until Accelerometer and Gyroscope data are fully populated,
                                //Accelerometer task is starting that thread,
                                //thus accelerometer data will always be populated, this is why I only check Gyroscope
                                while (!breakOut) { //loop to wait for a data population
                                    if (gyroPopulated) {
                                        breakOut = true;

                                        trickInside = new DataSamples(trickAcc, trickGyr);

                                        Log.d("SkateSamples", "Data fully populated");
                                        gyroPopulated = false;

                                        trickSet = true; //lock possibility to do a trick during analysing

                                        String nameOfTrick = trickMade.analyse(trickInside);
                                        recentlyMadeTrick = nameOfTrick;

                                        trickTextView.setText(nameOfTrick);
                                        trickRotationText.setText(R.string.trick_rotation);
                                        trickRotationTextView.setText(trickRotationTextViewString);

                                        setTrickAnimation(nameOfTrick);

                                        trickAcc = new ArrayList<Acceleration>();
                                        trickAcc.clear();

                                        trickGyr = new ArrayList<>();
                                        trickGyr.clear();
                                    }
                                }
                            }
                        });
                        trickThread.interrupt();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        trickThread.start();
    }

    public void setTrickAnimation(String nameOfTrick) {
        if (nameOfTrick.compareToIgnoreCase(trickListDB.get(0)) == 0) {
            trickGif.setImageDrawable(null);
            trickGif.setImageResource(R.drawable.kickflip);
        } else if (nameOfTrick.compareToIgnoreCase(trickListDB.get(2)) == 0) {
            trickGif.setImageDrawable(null);
            trickGif.setImageResource(R.drawable.popshuvit);
        } else if (nameOfTrick.compareToIgnoreCase(trickListDB.get(3)) == 0) {
            trickGif.setImageDrawable(null);
            trickGif.setImageResource(R.drawable.fspopshuvit);
        } else if (nameOfTrick.compareToIgnoreCase(trickListDB.get(1)) == 0) {
            trickGif.setImageDrawable(null);
            trickGif.setImageResource(R.drawable.heelflip);
        }
    }

    public void resetTrick(View view) {
        trickTextView.setText("");

        if (isStartedAcc == true) {
            board.tearDown();
            isStartedAcc = false;

            resetServices();
        }
    }

    public void startAccelerometer() {
        if (isAccelerometer) {
            accelerometer.acceleration().start();
            //accelerometer.acceleration().start();
            accelerometer.start();
            isStartedAcc = true;
        }
    }

    public void stopAccelerometer() {
        if (isAccelerometer) {
            accelerometer.acceleration().stop();
            //accelerometer.acceleration().stop();
            accelerometer.stop();
        }
    }

    public void stopAccelerometer(String filename) {
        if (isAccelerometer) {
            stopAccelerometer();

            acce = acc.toString();
            saveToAFile(acce, filename);
            acce = "";
            acc = new StringBuilder(acce);

            isStartedAcc = false;
        }
    }

    static void stopAccelerometer1() {
        if (isAccelerometer) {
            accelerometer.acceleration().stop();
            accelerometer.stop();
            Log.i("SkateTricks", "Accelerometer Successfully stopped by a service");
        }
    }

    public void startGyroscope() {
        if (isGyroscope) {
            gyroscope.angularVelocity().start();
            gyroscope.start();
        }
    }

    public void stopGyroscope() {
        if (isGyroscope) {
            gyroscope.angularVelocity().stop();
            gyroscope.stop();
        }
    }

    static void stopGyroscope1() {
        if (isGyroscope) {
            gyroscope.angularVelocity().stop();
            gyroscope.stop();
        }
    }

    public void stopGyroscope(String filename) {
        if (isGyroscope) {
            stopGyroscope();

            gyro = gyr.toString();
            saveToAFile(gyro, filename);
            gyro = "";
            gyr = new StringBuilder(gyro);
        }
    }

    public void startButtonAll(View view) {
        startAccelerometer();
        startGyroscope();
    }

    public void stopButtonAll(View view) {
        stopAccelerometer("accelerometerData");
        stopGyroscope("gyroscopeData");
    }

    public void stopButtonAllAndSave(View view) {
        stopAccelerometer();
        stopGyroscope();
        PopUpTextBox();
    }

    public void resetButton(View view) {
        /*if (isGyroscope && isAccelerometer) {
            board.tearDown();
        }*/
        if (isStartedAcc == true) {
            board.tearDown();
            isStartedAcc = false;

            resetServices();
        }
    }

    public void saveToAFile(String fileContents, String filename) {
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
            Log.i("SkateTricks", "Data saved to a file");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCurrentDateWithMs() {
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);

        return strDate;
    }

    public String getCurrentDateWithMsFileSafeFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss,SSS");
        Date now = new Date();
        String strDate = sdf.format(now);

        return strDate;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i("SkateTricks", "OnDestroy called");

        if (bluetoothAdapter != null) {//if bluetooth is supported, then unbind services

            stopGyroscope();
            stopAccelerometer();

            doUnbindService(); //unbind app closed service

            //tearDown board
            //if (isGyroscope && isAccelerometer) board.tearDown();
            // Unbind the service when the activity is destroyed
            getApplicationContext().unbindService(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {

        if (requestCode == REQUEST_BLUETOOTH) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                if (bluetoothAdapter.isEnabled()) {
                    //enabled bluetooth
                    ShowToast("Bluetooth is now enabled, starting service");

                    //Bind the service when BT is enabled
                    getApplicationContext().bindService(new Intent(this, BtleService.class),
                            this, Context.BIND_AUTO_CREATE);
                }
            } else {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_BLUETOOTH);
            }
        }
    }

    /**
     * Called when a connection to the Service has been established, with
     * the {@link IBinder} of the communication channel to the
     * Service.
     *
     * @param name    The concrete component name of the service that has
     *                been connected.
     * @param service The IBinder of the Service's communication channel,
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;

        progressData = "Service is connected";
        Log.i("SkateTricks", "Service is connected");

        retrieveBoard(MW_MAC_ADDRESS);
    }

    /**
     * Called when a connection to the Service has been lost.  This typically
     * happens when the process hosting the service has crashed or been killed.
     * This does <em>not</em> remove the ServiceConnection itself -- this
     * binding to the service will remain active, and you will receive a call
     * to {@link #onServiceConnected} when the Service is next running.
     *
     * @param name The concrete component name of the service whose
     *             connection has been lost.
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        //textView1.setText("!!! Service not connected");
        progressData = "!!! Service has been disconnected";
        Log.i("SkateTricks", "!!! Service is disconnected");
    }

    /**
     * Called when the binding to this connection is dead.  This means the
     * interface will never receive another connection.  The application will
     * need to unbind and rebind the connection to activate it again.  This may
     * happen, for example, if the application hosting the service it is bound to
     * has been updated.
     *
     * @param name The concrete component name of the service whose
     *             connection is dead.
     */
    @Override
    public void onBindingDied(ComponentName name) {
//        textView2.setText("!!! Binding died");
        progressData = "!!! Binding died";
        Log.i("SkateTricks", "!!! Binding died");
    }

    /**
     * Called when pointer capture is enabled or disabled for the current window.
     *
     * @param hasCapture True if the window has pointer capture.
     */
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void retrieveBoard(String macAddress) {
        final BluetoothManager btManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice =
                btManager.getAdapter().getRemoteDevice(macAddress);

        // Create a MetaWear board object for the Bluetooth Device
        board = serviceBinder.getMetaWearBoard(remoteDevice);
        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Void> task) throws Exception {

                led = board.getModule(Led.class); //get led on connection
                isConnectedToMW = true;
                //textView1.setText("Successfully connected to MetaTracker");
                Log.i("SkateTricks", "Successfully connected to MetaTracker");

                accelerometer = board.getModule(/*AccelerometerBmi160.class*/Accelerometer.class);
                accelerometer.configure()
                        .odr(accelerometerDataFrequency)       // Set sampling frequency to 25Hz, or closest valid ODR
                        .range(accelerometerDataRange)      // Set data range to +/-4g, or closet valid range
                        .commit();

                return accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {

                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {

                                accelerometerTasks(data);
                            }
                        });
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {

                accelerometerTasksFaulted(task);

                return null;
            }
        });

        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Void> task) throws Exception {

                //isConnectedToMW=true;
                //textView1.setText("Successfully connected to MetaTracker");
                Log.i("SkateTricks", "Entered Gyro config");

                gyroscope = board.getModule(GyroBmi160.class);
                gyroscope.configure()
                        .odr(gyroDataFrequency)
                        .range(gyroDataRange/*GyroBmi160.Range.FSR_2000*/) //Data range set to 2000 °/s
                        .commit();

                return gyroscope.angularVelocity().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {

                                gyroscopeTasks(data);
                            }
                        });
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {

                gyroscopeTasksFaulted(task);

                return null;
            }
        });
        //}

        //battery
        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Void> task) throws Exception {

                settings = board.getModule(Settings.class);

                Log.i("SkateTricks", "Entered Battery and BT config");

                return settings.battery().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                //batteryLife.setProgress(batteryPercent);
                                batteryPercent = data.value(Settings.BatteryState.class).charge;
                                Log.i("SkateTricks", "Battery State = " + data.value(Settings.BatteryState.class));
                            }
                        });
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                if (task.isFaulted()) {
                    //textView1.setText("Failed to Connect To Board");
                    Log.w("SkateTricks", "Failed to Configure App", task.getError());
                    progressData = "Failed to Configure App";

                    isAfterFirstConfig = false;
                    //resetServices(); //reset services any time app failed to configure
                } else {
                    progressData = getString(R.string.app_successfully_configured);
                    settings.battery().read();

                    isAfterFirstConfig = true;
                    return null;
                }
                return null;
            }
        });

        board.onUnexpectedDisconnect(new MetaWearBoard.UnexpectedDisconnectHandler() {
            @Override
            public void disconnected(int status) {
                Log.i("SkateErrors", "BT Connection dropped");
                progressData = "BT Connection dropped";
                btDropped = true;
                isConfigured = false;
                breakOut = true; //dont wait for Gyro data population when disconnect occured during trick analyzing

                //resetuj service
                //while (!board.isConnected()/*!isConfigured*/)
                resetServices();//try to reset until connection is established

                Log.i("SkateErrors", "Services have been reset Successfully");
            }
        });
    }

    public void accelerometerTasks(Data data) {
        String temp = data.value(Acceleration.class).toString();

        //trick is made when certain threshold had been reached at least once,
        // checks if counter started to fetch next 50 samples (1 second)
        //if z exceeded -3.00 that means that trick has been popped thus it will analyze given samples
        if ((Math.abs(data.value(Acceleration.class).z()) > thresholdAcce || thresholdReached || counterAcc > 0) && !trickSet) {
            //boolean to gather data from gyroscope after reaching a threshold in accelerometer
            thresholdReached = true;

            if (counterAcc == 0)
                Log.d("SkateSamples", "Acce Thr values: (" + counterAcc + "): " + data.value(Acceleration.class).toString());

            trickAcc.add((int) counterAcc, data.value(Acceleration.class));

            counterAcc++;

            if (counterAcc >= accelerometerDataFrequency) { // >= is important when more than 2 tricks are made within little amount of time to prevent data overflow
                counterAcc = 0;

                thresholdReached = false;

                //trickHasBeenMade, check name
                checkTrick();
            }
        }
        Log.i("SkateAccel", temp);
        populateData(temp, 0);
    }

    public void accelerometerTasksFaulted(Task<Route> task) {
        if (task.isFaulted()) {
            //textView1.setText("Failed to Connect To Board");
            Log.w("SkateTricks", "Failed to Configure Accelerometer", task.getError());
            progressData = "Failed to Configure Accelerometer";
            isAccelerometer = false;
        } else {
            //isConnectedToMW = true;
            //textView1.setText("Successfully connected to MetaTracker");
            Log.i("SkateTricks", "Accelerometer successfully configured");
            //setSuccess("App configured | MTH Connected");
            isAccelerometer = true;

            if (applicationMode == false) startAccelerometer();

            if (btDropped)
                startAccelerometer(); //automatically start Accelerometer after BT connection is re-established
            if (isStartedGyr) btDropped = false;
        }
    }

    public void gyroscopeTasks(Data data) {
        String temp = data.value(AngularVelocity.class).toString();

        if ((!thresholdReached && data.value(AngularVelocity.class).y() >= thresholdGyro && counterGyr == 0 && counterAcc == 0) && !trickSet) //nollie trick detection
        {
            thresholdReached = true;
            Log.d("SkateSamples", "Gyro Trick start: " + data.value(AngularVelocity.class).toString());
        }

        if ((thresholdReached || counterGyr > 0) && !trickSet) {
            //adding raw gyroscope data after passing trick threshold
            trickGyr.add((int) counterGyr, data.value(AngularVelocity.class));

            if (counterGyr == 0)// || counterGyr == 1 || counterGyr == 2)
                Log.d("SkateSamples", "Gyro Thr values: " + counterGyr + " : " + data.value(AngularVelocity.class).toString());

            counterGyr++;

            if (counterGyr >= accelerometerDataFrequency) {// >= is important when more than 2 tricks are made within little amount of time to prevent data overflow
                counterGyr = 0;
                thresholdReached = false;

                //DataSamples are sent within accelerometer task
                gyroPopulated = true;
            }
        }

        Log.i("SkateGyro", temp);
        populateData(temp, 1);
    }

    public void gyroscopeTasksFaulted(Task<Route> task) {
        if (task.isFaulted()) {
            //textView1.setText("Failed to Connect To Board");
            Log.w("SkateErrors", "Failed to Configure Gyro", task.getError());
            progressData = "Failed to Configure Gyro";
            isGyroscope = false;
            isConfigured = false;

            resetServices(); //reset services any time app failed to configure

        } else {
            Log.i("SkateTricks", "App successfully configured");
            progressData = "App successfully configured";
            //setSuccess("App configured | MTH Connected");
            isGyroscope = true;
            isConfigured = true;

            if (applicationMode == false) startGyroscope();

            if (btDropped) startGyroscope(); //automatically start Gyroscope in the beginning
            if (isStartedAcc) btDropped = false;

            iterator = 0;
        }
    }

    public void resetServices() {
        iterator = 2;
        getApplicationContext().unbindService(this);
        if (!bluetoothAdapter.isEnabled()) {
            //enabled bluetooth
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH);

            ShowToast("Bluetooth is not enabled");
        } else {
            // Bind the service after disconnection
            getApplicationContext().bindService(new Intent(this, BtleService.class),
                    this, Context.BIND_AUTO_CREATE);
        }
    }

    public void resetTrickTextAfterTime(short time) {
        if (trickSet == true) {
            iterator10++;

            if (iterator10 == time) { //10 is 1 second after setting a trick
                trickTextView.setText("");
                trickRotationTextView.setText("");
                iterator10 = 0;
                trickSet = false;

                trickGif.setImageDrawable(null);
                trickGif.setImageResource(R.drawable.kickfliplastframe);
            }
        }
    }

    /**
     * @param temp     data to populate certain StringBuilders
     * @param dataType 0 -> accelerometer | 1 -> gyroscope
     */
    public void populateData(String temp, int dataType) {

        if (dataType == 0) {
            accelerometerDataOutput = temp;

            temp1.delete(0, temp1.length());

            temp1.append(temp);
            temp1.append(" | ");
            temp1.append(getCurrentDateWithMs());
            temp1.append("\n");

            acc.append(temp1); //? .toString()
            accGyr.append(temp1);
        } else if (dataType == 1) {
            gyroscopeDataOutput = temp;

            temp1.delete(0, temp1.length());

            temp1.append(temp);
            temp1.append(" | ");
            temp1.append(getCurrentDateWithMs());
            temp1.append("\n");

            gyr.append(temp1); //? .toString()
            accGyr.append(temp1);
        }
    }

    public void PopUpTextBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name that trick");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT /*| InputType.TYPE_TEXT_VARIATION_PASSWORD*/);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();

                if (m_Text.compareToIgnoreCase("") == 0) m_Text = "null";

                String m_TextAcc = m_Text + ", acc";
                String m_TextGyr = m_Text + ", gyr";
                String m_TextAccGyr = m_Text + ", AccGyr";

                String filename = getUserDefinedFileName(m_TextAcc);
                stopAccelerometer(filename);

                filename = getUserDefinedFileName(m_TextGyr);
                stopGyroscope(filename);

                filename = getUserDefinedFileName(m_TextAccGyr);

                acceGyro = accGyr.toString();
                saveToAFile(acceGyro, filename);
                acceGyro = "";
                accGyr = new StringBuilder(acceGyro);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                acce = "";
                gyro = "";
                acceGyro = "";

                acc = new StringBuilder(acce);
                gyr = new StringBuilder(gyro);
                accGyr = new StringBuilder(acceGyro);

                stopAccelerometer();
                stopGyroscope();

                dialog.cancel();
            }
        });

        //automatycznie wysuwana klawiaturka
        AlertDialog alertToShow = builder.create();
        alertToShow.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertToShow.show();
        //builder.show();
    }

    public String getUserDefinedFileName(String m_Text) {
        StringBuilder filenameBuilder = new StringBuilder("");
        filenameBuilder.append(m_Text);

        filenameBuilder.append(" = ");
        filenameBuilder.append(getCurrentDateWithMsFileSafeFormat());
        filenameBuilder.append(".txt"); //.txt extension to be easy read
        String filename = filenameBuilder.toString();

        return filename;
    }

    public void ShowToast(CharSequence text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        //CharSequence text = "Zapisano do pliku" ;//+ " index dla Fiat: " + carL.indexOf("Fiat");
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public void initViews() {
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        textView4 = findViewById(R.id.textView4);
        trickTextView = findViewById(R.id.trickTextView);
        trickTextView.setText("");
        trickRotationTextView = findViewById(R.id.trickRotationTextView);
        trickRotationTextView.setText("");
        trickRotationText = findViewById(R.id.trickRotationText);
        //trickRotationText.setText("Welcome to SkateTricks!");
        textViewConn = findViewById(R.id.textViewConn);

        LinearLayout03 = findViewById(R.id.LinearLayout03);

        stopAllAndSetName = findViewById(R.id.stopAllAndSetName);

        batteryLife = findViewById(R.id.batteryLife);

        trickGif = findViewById(R.id.trickGif);

        acc = new StringBuilder(acce);
        gyr = new StringBuilder(gyro);
        accGyr = new StringBuilder(acceGyro);

        trickAcc = new ArrayList<Acceleration>();
        trickGyr = new ArrayList<>();

        temp1 = new StringBuilder();

        trickMade = new TrickMade();

        if (applicationMode == false) {
            //set layout to normal
            LinearLayout03.setVisibility(View.GONE);
            stopAllAndSetName.setVisibility(View.GONE);
            textView2.setVisibility(View.GONE);
            textView3.setVisibility(View.GONE);
        } else if (applicationMode == true) {
            //set layout to debug
            LinearLayout03.setVisibility(View.VISIBLE);
            stopAllAndSetName.setVisibility(View.VISIBLE);
            textView2.setVisibility(View.VISIBLE);
            textView3.setVisibility(View.VISIBLE);
        }
    }

    public void initDB() {
        try {
            Log.i("SkateDB", "Entered INIT of SQLite Database");

            databaseHelper = new DatabaseHelper(this);

            String macAddress = MW_MAC_ADDRESS; //default MAC ADDRESS
            String stanceOfUs = "Goofy"; //stance of a user, Goofy is set as default

            String isHandMode = "Normal";
            String TBDsetting = "Normal"; //application mode: Normal or Debug

            String howMuchTimes = "0"; //init that user made 0 tricks

            //populate Database
            if (!databaseHelper.checkIfExists(0))
                databaseHelper.addData(0, macAddress, stanceOfUs);

            if (!databaseHelper.checkIfExists(1))
                databaseHelper.addData(1, isHandMode, TBDsetting);

            trickListDB = TrickMade.trickListGenerator();
            int iterator = 0; //startID of Trick Statistic Database
            if (!databaseHelper.checkIfExists(trickListDB.get(iterator))) {//check if first trick exists, if exists then skip checking rest
                while (iterator != trickListDB.size()) {
                    if (!databaseHelper.checkIfExists(trickListDB.get(iterator)))
                        databaseHelper.addData(iterator + 2, trickListDB.get(iterator), howMuchTimes); //ID is + 2 because first two IDs in DB are used to store user data
                    iterator++;
                }
            }
            Log.i("SkateDB", "TrickListDB size: " + trickListDB.size());

            Log.i("SkateDB", "___________Saved_IN_Local_Database___________");

            iterator = 0; //startID of Trick Statistic Database
            if (databaseHelper.checkIfExists(iterator)) {//check if first trick exists, if exists then skip checking rest
                while (iterator != trickListDB.size()) {
                    if (databaseHelper.checkIfExists(iterator)) {
                        Cursor abcd = databaseHelper.getIfExists(iterator);
                        if (abcd.moveToFirst()) {
                            Log.i("SkateDB", "ID: " + abcd.getString(0) + " | name: " + abcd.getString(1) + " | times: " + abcd.getString(2));
                        }
                    }
                    iterator++;
                }
            }

            //get user's MetaWear MAC Address and user's stance
            Cursor abc = databaseHelper.getIfExists(0);
            if (abc.moveToFirst()) {
                String a = abc.getString(0);
                String b = abc.getString(1);
                String c = abc.getString(2);

                MW_MAC_ADDRESS = b;
                if (c.compareToIgnoreCase("Goofy") == 0)
                    stanceOfUser = false;
                else if (c.compareToIgnoreCase("Regular") == 0)
                    stanceOfUser = true;

                String log = "DB, record with id: "
                        + a + " contains | MacAddress: " + b + " | Stance: " + c;

                Log.i("SkateDB", log);
            }

            abc = databaseHelper.getIfExists(1);
            if (abc.moveToFirst()) {
                String a = abc.getString(0);
                String b = abc.getString(1);
                String c = abc.getString(2);

                if (b.compareToIgnoreCase("Handmode") == 0)
                    thresholdAcce = thresholdAcceHandMode;
                else if (b.compareToIgnoreCase("Normal") == 0)
                    thresholdAcce = thresholdAcceNormalMode;

                if (c.compareToIgnoreCase("Debug") == 0)
                    applicationMode = true;
                else if (c.compareToIgnoreCase("Normal") == 0)
                    applicationMode = false;

                String log = "DB, record with id: " + a + " contains | isHandmode: " + b + " | AppMode: " + c;

                Log.i("SkateDB", log);
                //Toast.makeText(getApplicationContext(), log, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            //Toast.makeText(getApplicationContext(), "Exception: " + e, Toast.LENGTH_LONG).show();
            Log.i("WATlog", "Exception: " + e);
        }
    }

    void doUnbindService() {
        /*if (mShouldUnbind) {
            // Release information about the service's state.
            unbindService(mConnection);
            mShouldUnbind = false;
        }*/
    }
}