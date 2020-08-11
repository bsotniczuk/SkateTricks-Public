package com.example.bartomiej.skatetricks;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    LinearLayout LinearLayout01;
    LinearLayout LinearLayout02;
    LinearLayout LinearLayout03;
    LinearLayout LinearLayout04;
    LinearLayout LinearLayout05;

    TextView trickName1;
    TextView trickName2;
    TextView trickName3;
    TextView trickName4;
    TextView trickName5;

    TextView trickName1Times;
    TextView trickName2Times;
    TextView trickName3Times;
    TextView trickName4Times;
    TextView trickName5Times;

    TextView mostRecentTrick;

    static List<Integer> trickFrequencyStatsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayout01 = findViewById(R.id.LinearLayout01);
        LinearLayout02 = findViewById(R.id.LinearLayout02);
        LinearLayout03 = findViewById(R.id.LinearLayout03);
        LinearLayout04 = findViewById(R.id.LinearLayout04);
        LinearLayout05 = findViewById(R.id.LinearLayout05);

        trickName1 = findViewById(R.id.trickName1);
        trickName2 = findViewById(R.id.trickName2);
        trickName3 = findViewById(R.id.trickName3);
        trickName4 = findViewById(R.id.trickName4);
        trickName5 = findViewById(R.id.trickName5);

        trickName1Times = findViewById(R.id.trickName1Times);
        trickName2Times = findViewById(R.id.trickName2Times);
        trickName3Times = findViewById(R.id.trickName3Times);
        trickName4Times = findViewById(R.id.trickName4Times);
        trickName5Times = findViewById(R.id.trickName5Times);

        mostRecentTrick = findViewById(R.id.mostRecentTrick);
        mostRecentTrick.setText(MainActivity.recentlyMadeTrick);

        trickFrequencyStatsList = new ArrayList<>();

        initDbStatistics();

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void initDbStatistics() {
        try {
            Log.i("SkateDB", "Entered INIT of Trick Statistic analysis");

            String trickName = "";
            int iterator = 0; //startID of Trick Statistic Database
            if(MainActivity.trickListDB.size() != 0) {
                while (iterator != MainActivity.trickListDB.size()) {
                    trickName = MainActivity.trickListDB.get(iterator);
                    //Log.i("SkateDBTrickList", "ID: " + iterator + " | name: " + trickName);

                    if (MainActivity.databaseHelper.checkIfExists(trickName)) {
                        //Log.i("SkateDB", trickName + " exists in DB");
                        Cursor abc = MainActivity.databaseHelper.getIfExists(trickName);
                        if (abc.moveToFirst()) {

                            String howManyTimesTrickWasMade = abc.getString(2);
                            int howManyTimes;
                            try {
                                howManyTimes = Integer.parseInt(howManyTimesTrickWasMade);
                                trickFrequencyStatsList.add(howManyTimes);
                            } catch (NumberFormatException e) {
                                howManyTimes = 0;
                            }
                            //howManyTimes++; //increment trick statistic

                            //MainActivity.databaseHelper.updateUserInfo(idOfTrickInDB + "", trickName, howManyTimes + "");
                            Log.i("SkateDB", "ID: " + iterator + " | name: " + trickName + " was done: " + howManyTimes + " times");
                        }
                    }

                    iterator++;
                }
            }

            //get top 5 most frequently made tricks
            List top5 = new ArrayList();

            //do poprawy
            //List<Integer> temp = trickFrequencyStatsList;
            List<Integer> temp = new ArrayList<>(trickFrequencyStatsList);
            List<Integer> temp1 = new ArrayList<>();
            List<Integer> mostFrequentTricksId = new ArrayList<>();
            Collections.sort(temp, Collections.reverseOrder()); //sort descending by done most times

            /*iterator = 0;
            if(trickFrequencyStatsList.size() != 0) {
                while (iterator != trickFrequencyStatsList.size()) {
                    trickName = trickFrequencyStatsList.get(iterator) + "";
                    Log.i("SkateDBTrickList", "trickFrequencyStatsList ID: " + iterator + " | times: " + trickName);

                    iterator++;
                }
            }*/

            iterator = 0;
            while (iterator != temp.size()) //get all tricks that had been done at least once
            {
                if (temp.get(iterator) == 0)
                {
                    iterator++;
                }
                else {
                    Log.i("SkateDB", "temp: " + temp.get(iterator));

                    temp1.add(temp.get(iterator));

                    iterator++;
                }
            }

            int tempIndex = 0;
            iterator = 0;
            while (iterator != temp1.size())
            {
                tempIndex = trickFrequencyStatsList.indexOf(temp1.get(iterator));
                trickFrequencyStatsList.set(tempIndex, 0);

                mostFrequentTricksId.add(tempIndex);

                Log.i("SkateDB", "Trick ID: " + tempIndex + " times: " + temp1.get(iterator) + " | trick name: " + MainActivity.trickListDB.get(tempIndex));

                iterator++;
            }

            String TBD = getString(R.string.to_be_decided);
            String dash = "-";

            if (temp1.size() >= 1) {
                trickName1.setText(MainActivity.trickListDB.get(mostFrequentTricksId.get(0)));
                trickName1Times.setText(temp1.get(0).toString());
            }
            else {
                trickName1.setText(TBD);
                trickName1Times.setText(dash);
            }

            if (temp1.size() >= 2) {
                trickName2.setText(MainActivity.trickListDB.get(mostFrequentTricksId.get(1)));
                trickName2Times.setText(temp1.get(1).toString());
            }
            else {
                trickName2.setText(TBD);
                trickName2Times.setText(dash);
            }

            if (temp1.size() >= 3) {
                trickName3.setText(MainActivity.trickListDB.get(mostFrequentTricksId.get(2)));
                trickName3Times.setText(temp1.get(2).toString());
            }
            else {
                trickName3.setText(TBD);
                trickName3Times.setText(dash);
            }

            if (temp1.size() >= 4) {
                trickName4.setText(MainActivity.trickListDB.get(mostFrequentTricksId.get(3)));
                trickName4Times.setText(temp1.get(3).toString());
            }
            else {
                trickName4.setText(TBD);
                trickName4Times.setText(dash);
            }

            if (temp1.size() >= 5) {
                trickName5.setText(MainActivity.trickListDB.get(mostFrequentTricksId.get(4)));
                trickName5Times.setText(temp1.get(4).toString());
            }
            else {
                trickName5.setText(TBD);
                trickName5Times.setText(dash);
            }

        } catch (Exception e) {
            //Toast.makeText(getApplicationContext(), "Exception: " + e, Toast.LENGTH_LONG).show();
            Log.i("SkateDB", "Exception: " + e);
        }
    }

}