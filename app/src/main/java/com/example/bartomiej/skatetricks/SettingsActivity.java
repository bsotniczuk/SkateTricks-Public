package com.example.bartomiej.skatetricks;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {

    EditText macText1;
    EditText macText2;
    EditText macText3;
    EditText macText4;
    EditText macText5;
    EditText macText6;

    Switch switchStance;
    Switch handModeSwitch;
    Switch appModeSwitch;

    TextView timeOfTrickText;
    EditText timeOfTrickTextEditText;

    TextView textRegular;
    TextView handModeOffText;
    TextView appModeNormalText;

    boolean isRestAltered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        macText1 = findViewById(R.id.macText1);
        macText2 = findViewById(R.id.macText2);
        macText3 = findViewById(R.id.macText3);
        macText4 = findViewById(R.id.macText4);
        macText5 = findViewById(R.id.macText5);
        macText6 = findViewById(R.id.macText6);

        handModeSwitch = findViewById(R.id.handModeSwitch);
        appModeSwitch = findViewById(R.id.appModeSwitch);
        switchStance = findViewById(R.id.switchStance);

        textRegular = findViewById(R.id.textRegular);
        handModeOffText = findViewById(R.id.handModeOffText);
        appModeNormalText = findViewById(R.id.appModeNormalText);

        timeOfTrickText = findViewById(R.id.timeOfTrickText);
        timeOfTrickTextEditText = findViewById(R.id.timeOfTrickTextEditText);

        timeOfTrickText.setVisibility(View.GONE);
        timeOfTrickTextEditText.setVisibility(View.GONE);

        switchStance.setChecked(MainActivity.stanceOfUser);
        appModeSwitch.setChecked(MainActivity.applicationMode);

        if (MainActivity.thresholdAcce == MainActivity.thresholdAcceHandMode)
            handModeSwitch.setChecked(true);
        else handModeSwitch.setChecked(false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_done) {
            //Toast.makeText(getApplicationContext(), "Done pressed", Toast.LENGTH_SHORT).show();

            if (macText1.getText().toString().length() != 0) //if first EditText isn't empty then set the Mac Address
            {
                getUserMacAddress();
            }

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void switchClicked(View view) {
        int id = view.getId();

        if (id == R.id.switchStance) {
            MainActivity.stanceOfUser = switchStance.isChecked();

            if (switchStance.isChecked()) {
                update(0, "Regular");
                Toast.makeText(getApplicationContext(), "Regular Stance Set", Toast.LENGTH_SHORT).show();
            } else if (!switchStance.isChecked()) {
                update(0, "Goofy");
                Toast.makeText(getApplicationContext(), "Goofy Stance Set", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.handModeSwitch) {

            if (handModeSwitch.isChecked()) {
                update(1, "Handmode");

                Toast.makeText(getApplicationContext(), "Handmode Set", Toast.LENGTH_SHORT).show();
                MainActivity.thresholdAcce = MainActivity.thresholdAcceHandMode;
            } else if (!handModeSwitch.isChecked()) {
                update(1, "Normal");

                Toast.makeText(getApplicationContext(), "Normal mode Set", Toast.LENGTH_SHORT).show();
                MainActivity.thresholdAcce = MainActivity.thresholdAcceNormalMode;
            }
        } else if (id == R.id.appModeSwitch) {
            if (appModeSwitch.isChecked()) {
                update(2, "Debug");

                Toast.makeText(getApplicationContext(), "Debug Mode Set", Toast.LENGTH_SHORT).show();
            } else if (!appModeSwitch.isChecked()) {
                update(2, "Normal");

                Toast.makeText(getApplicationContext(), "Normal App Mode Set", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void textClicked(View view) {
        int id = view.getId();

        //Toast.makeText(getApplicationContext(), "Regular Clicked", Toast.LENGTH_SHORT).show();

        if (id == R.id.textRegular) {
            if (!switchStance.isChecked()) {
                switchStance.setChecked(!switchStance.isChecked());
                update(0, "Regular");
                Toast.makeText(getApplicationContext(), "Regular Stance Set", Toast.LENGTH_SHORT).show();
            } else if (switchStance.isChecked()) {
                switchStance.setChecked(!switchStance.isChecked());
                update(0, "Goofy");
                Toast.makeText(getApplicationContext(), "Goofy Stance Set", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.handModeOffText) {
            if (!handModeSwitch.isChecked()) {
                handModeSwitch.setChecked(!handModeSwitch.isChecked());
                update(1, "Handmode");

                Toast.makeText(getApplicationContext(), "Handmode Set", Toast.LENGTH_SHORT).show();
                MainActivity.thresholdAcce = MainActivity.thresholdAcceHandMode;
            } else if (handModeSwitch.isChecked()) {
                handModeSwitch.setChecked(!handModeSwitch.isChecked());
                update(1, "Normal");

                Toast.makeText(getApplicationContext(), "Normal mode Set", Toast.LENGTH_SHORT).show();
                MainActivity.thresholdAcce = MainActivity.thresholdAcceNormalMode;
            }
        } else if (id == R.id.appModeNormalText) {
            if (!appModeSwitch.isChecked()) {
                appModeSwitch.setChecked(!appModeSwitch.isChecked());
                update(2, "Debug");

                Toast.makeText(getApplicationContext(), "Debug Mode Set", Toast.LENGTH_SHORT).show();
            } else if (appModeSwitch.isChecked()) {
                appModeSwitch.setChecked(!appModeSwitch.isChecked());
                update(2, "Normal");

                Toast.makeText(getApplicationContext(), "Normal App Mode Set", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (macText1.getText().toString().length() != 0) //if first text isn't empty than set the Mac Address
        {
            getUserMacAddress();
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void aboutAuthors(View view) {
        PopUpTextBox();
    }

    public void getUserMacAddress() {
        StringBuilder sb = new StringBuilder();

        boolean incorrectData = false;

        if (macText1.getText().toString().length() == 2) {
            sb.append(macText1.getText().toString().toUpperCase(Locale.US));
            sb.append(":");
        } else incorrectData = true;
        if (macText2.getText().toString().length() == 2) {
            sb.append(macText2.getText().toString().toUpperCase(Locale.US));
            sb.append(":");
        } else incorrectData = true;
        if (macText3.getText().toString().length() == 2) {
            sb.append(macText3.getText().toString().toUpperCase(Locale.US));
            sb.append(":");
        } else incorrectData = true;
        if (macText4.getText().toString().length() == 2) {
            sb.append(macText4.getText().toString().toUpperCase(Locale.US));
            sb.append(":");
        } else incorrectData = true;
        if (macText5.getText().toString().length() == 2) {
            sb.append(macText5.getText().toString().toUpperCase(Locale.US));
            sb.append(":");
        } else incorrectData = true;
        if (macText6.getText().toString().length() == 2)
            sb.append(macText6.getText().toString().toUpperCase(Locale.US));
        else incorrectData = true;

        if (isRestAltered == false) {//redundant check
            if (incorrectData == true) {
                Toast.makeText(getApplicationContext(), "Wrong MW MAC Address given\nMAC Address Ignored", Toast.LENGTH_LONG).show();
            } else {
                String newMacAddress = sb.toString();
                if (validate(newMacAddress)) {
                    update(3, newMacAddress);
                    //MainActivity.MW_MAC_ADDRESS = sb.toString(); //set new MacAddress
                    Toast.makeText(getApplicationContext(), "New MW MacAddress set: " + newMacAddress, Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Wrong MW MAC Address given\nMAC Address Ignored", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private static final String PATTERN = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

    private static boolean validate(String password) {
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    public void update(int ID, String item){
        if (ID == 0) {
            if (MainActivity.databaseHelper.checkIfExists(ID)) {
                Log.i("SkateDB", ID + " exists in DB");
                Cursor abc = MainActivity.databaseHelper.getIfExists(ID);
                if (abc.moveToFirst()) {
                    String temp = abc.getString(1);
                    MainActivity.databaseHelper.updateUserInfo(ID + "", temp, item);
                    Log.i("SkateDB", "Updated ID: " + ID + " MacAddress: " + temp + " | stance (Updated): " + item);
                }
            }
        }
        else if (ID == 1)
        {
            if (MainActivity.databaseHelper.checkIfExists(ID)) {
                Log.i("SkateDB", ID + " exists in DB");
                Cursor abc = MainActivity.databaseHelper.getIfExists(ID);
                if (abc.moveToFirst()) {
                    String temp = abc.getString(2);
                    MainActivity.databaseHelper.updateUserInfo(ID + "", item, temp);
                    Log.i("SkateDB", "Updated ID: " + ID + " isHandMode (Updated): " + item + " | isDebug: " + temp);
                }
            }
        }
        else if (ID == 2)
        {
            if (MainActivity.databaseHelper.checkIfExists(1)) {
                Log.i("SkateDB", 1 + " exists in DB");
                Cursor abc = MainActivity.databaseHelper.getIfExists(1);
                if (abc.moveToFirst()) {
                    String temp = abc.getString(1);
                    MainActivity.databaseHelper.updateUserInfo(1 + "", temp, item);
                    Log.i("SkateDB", "Updated ID: " + 1 + " isHandMode: " + temp + " | isDebug (Updated): " + item);
                }
            }
        }
        else if (ID == 3) { //update mac address
            if (MainActivity.databaseHelper.checkIfExists(0)) {
                Log.i("SkateDB", 0 + " exists in DB");
                Cursor abc = MainActivity.databaseHelper.getIfExists(0);
                if (abc.moveToFirst()) {
                    String temp = abc.getString(2);
                    MainActivity.databaseHelper.updateUserInfo(0 + "", item, temp);
                    Log.i("SkateDB", "Updated ID: " + 0 + " MacAddress (Updated): " + item + " | stance: " + temp);
                }
            }
        }
    }

    public void PopUpTextBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDialog;

        TextView title = new TextView(this);

        title.setText(getString(R.string.about_authors));
        title.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        title.setPadding(10, 10, 10, 10);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);
        title.setTextSize(dpToPixels(6));

        title.setTypeface(null, Typeface.BOLD); //set bold text

        builder.setCustomTitle(title);

        String a = "Application author is Bartłomiej Sotniczuk,";
        String b = "ITA WAT";
        String c = "(Military University of Technology in Warsaw) student";
        String d = "Special thanks to";
        String e = "Eric Tsai,";
        String f = "author of MbientLab MetaWear API";
        String g = "Animation creators are";
        String h = "en.nollieskateboarding.com";

        StringBuilder sb = new StringBuilder(getString(R.string.about_authors_1));
        sb.append("\n");
        sb.append(getString(R.string.about_authors_2));
        sb.append("\n");
        sb.append(getString(R.string.about_authors_3));
        sb.append("\n\n");
        sb.append(getString(R.string.about_authors_4));
        sb.append("\n");
        sb.append(getString(R.string.about_authors_5));
        sb.append("\n");
        sb.append(getString(R.string.about_authors_6));
        sb.append("\n\n");
        sb.append(getString(R.string.about_authors_7));
        sb.append("\n");
        sb.append(getString(R.string.about_authors_8));

        builder.setMessage(sb.toString());

        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
            }
        });

        alertDialog = builder.create();

        //set OK button color
        alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        });

        alertDialog.show();

        TextView msgTxt = alertDialog.findViewById(android.R.id.message);
        //assert msgTxt != null;
        if (msgTxt != null) {
            msgTxt.setTextSize(dpToPixels(5));
            msgTxt.setGravity(Gravity.CENTER);
        }
    }

    public float dpToPixels(float DP) {
        // Converts 14 dip into its equivalent px
        float px = 0;
        float dip = DP;
        Resources r = getResources();
        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );

        return px;
    }

    public void macEditTextClicked(View view) {
        macText1.setCursorVisible(true);
    }
}
