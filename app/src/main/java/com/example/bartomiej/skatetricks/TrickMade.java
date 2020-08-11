package com.example.bartomiej.skatetricks;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContextWrapper;

import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;

//Disclaimer:
////
//This class has been cut down to prevent copying of my trick analysing algorithms
//Originally the whole class is a bit bigger
////

//class analysing what trick has been made
public class TrickMade {
    // TODO: Add goofy stance checker
    // TODO: Add 2 axles (y, z) to analyze in getTrickRotationAngle method
    // TODO: Discover simple Ollie and Nollie
    // TODO: Discover Nollie tricks
    // TODO: Discover Double Flips, Double Heelflips, Triple, Quad...

    //important, You have to ignore first 3 or 4 samples because they can come from an earlier trick so they are useless
    //ignore all until the accelerometer z have reached over 3g which is a threshold that shows that trick has been popped
    //ignore all in gyroscope until that sample

    public String analyse(DataSamples trickSamples) {
        //MainActivity.
        if (trickSamples == null) return "";

        else {
            Log.d("SkateSamples", "Acce Samples size: " + trickSamples.accelerometerSamples.size());
            Log.d("SkateSamples", "Gyro Samples size: " + trickSamples.gyroscopeSamples.size());

            boolean isTailTrick;

            Log.d("SkateSamples", "Entered a trick analyzer");

            isTailTrick = checkIfTailTrick(trickSamples.gyroscopeSamples);

                /*angular velocity osi:
                z zmienia się przy shuvicie
                y zmienia się przy ollie
                x zmienia się przy kickflipie*/

            List<Float> trickRotation;

            //new filters
            if (isTailTrick) trickRotation = getTrickRotationAngleOnAllAxles(trickSamples.gyroscopeSamples, 250, 40, 100);
            else trickRotation = getTrickRotationAngleOnAllAxles(trickSamples.gyroscopeSamples, 250, 40, 100); //smaller y value

            String trickName = "";
            StringBuilder sb = new StringBuilder(trickName);

            Log.d("SkateSamples, x axle: ", String.valueOf(trickRotation.get(0)));
            Log.d("SkateSamples, y axle: ", String.valueOf(trickRotation.get(1)));
            Log.d("SkateSamples, z axle: ", String.valueOf(trickRotation.get(2)));

            StringBuilder textViewTR = new StringBuilder();
            textViewTR.append("x: ");
            textViewTR.append(trickRotation.get(0).intValue());
            textViewTR.append("° y: ");
            textViewTR.append(trickRotation.get(1).intValue());
            textViewTR.append("° z: ");
            textViewTR.append(trickRotation.get(2).intValue());
            textViewTR.append("°");

            MainActivity.trickRotationTextViewString = textViewTR.toString();

            float halfRotation = 180; //degrees equal to half of rotation
            float fullRotation = 360; //degrees equal to full rotation
            float marginOfRotErr = 60; //when someone makes a little more or a little less of a trick, limit is 89.(9) degrees

            int trickZAxleID = 0;
            int trickYAxleID = 0;
            int trickXAxleID = 0;

            //Disclaimer:
            //This method has been cut down to prevent copying of my algorithm that detects a trickname

            //z axle
            if (trickRotation.get(2) < -(halfRotation - marginOfRotErr) /*-(180-60)*/ && trickRotation.get(2) > -(halfRotation + marginOfRotErr) /*-(180+60)*/)
                trickZAxleID = 1; //shuvit
            else if (trickRotation.get(2) > +(halfRotation - marginOfRotErr) /*+(180-60)*/ && trickRotation.get(2) < +(halfRotation + marginOfRotErr) /*+(180+60)*/)
                trickZAxleID = 2; //fs shuvit
            else if (trickRotation.get(2) < -(fullRotation - marginOfRotErr) /*-(360-60)*/ && trickRotation.get(2) > -(fullRotation + marginOfRotErr) /*-(360+60)*/)
                trickZAxleID = 3; //360 shuvit
            else if (trickRotation.get(2) > +(fullRotation - marginOfRotErr) /*+(360-60)*/ && trickRotation.get(2) < +(fullRotation + marginOfRotErr) /*+(360+60)*/)
                trickZAxleID = 4; //fs 360 shuvit

            //new way of getting trick name
            int trickID = 0;
            trickID = getTrickID(trickXAxleID, trickYAxleID, trickZAxleID);
            trickName = getTrickNameByID(trickID, isTailTrick);

            if (!trickName.isEmpty()) Log.i("SkateSamples", "TrickMade: " + trickName);

            String toPop = ""; //to delete

            if(isTailTrick) toPop = "tail popped";
            else toPop = "nose popped";

            //if trickName name is not equal to "" then return trickName
            if (!trickName.isEmpty())
            {
                dbIncrement(trickName);

                return trickName;
            }
            else return toPop;
        }
    }

    public List<Float> getTrickRotationAngleOnAllAxles(List<AngularVelocity> gyroscopeSamples, float filterFromValueX, float filterFromValueY, float filterFromValueZ) {
        List<Float> rotationAngles = new ArrayList();

        //Disclaimer:
        //This method has been cut down to prevent copying of my algorithm that calculates angles based on raw gyroscope data.

        //x
        float rotationAngle = 0;
        //y
        float rotationAngleY = 0;
        //z
        float rotationAngleZ = 0;

        rotationAngles.add(rotationAngle); //rotation angle on X axle
        rotationAngles.add(rotationAngleY); //rotation angle on Y axle
        rotationAngles.add(rotationAngleZ); //rotation angle on Z axle

        return rotationAngles;
    }

    public boolean checkIfTailTrick(List<AngularVelocity> gyroscopeSamples) {
        boolean isTailTrick = false;
        float average2 = 0;

        int iterator3 = 0;
        while(iterator3 != gyroscopeSamples.size()/10)
        {
            average2 = average2 + gyroscopeSamples.get(iterator3).y();

            iterator3++;
        }

        average2 = average2 / gyroscopeSamples.size()/10;

        if(average2 <= 0) //if average from 5 first gyro samples is lower than 0 it means that it was a tail trick, else it was a nose trick
        {
            isTailTrick = true;
        }

        return isTailTrick;
    }

    public int getTrickID(int trickXAxleID, int trickYAxleID, int trickZAxleID) {
        int trickID = 0;

        if (trickXAxleID == 1 && trickYAxleID == 0 && trickZAxleID == 0)
            trickID = 1;
        else if (trickXAxleID == 2 && trickYAxleID == 0 && trickZAxleID == 0)
            trickID = 2;
        else if (trickXAxleID == 0 && trickYAxleID == 0 && trickZAxleID == 1)
            trickID = 3;
        else if (trickXAxleID == 0 && trickYAxleID == 0 && trickZAxleID == 2)
            trickID = 4;
        else if (trickXAxleID == 1 && trickYAxleID == 0 && trickZAxleID == 1)
            trickID = 5;
        else if (trickXAxleID == 2 && trickYAxleID == 0 && trickZAxleID == 1)
            trickID = 6;
        else if (trickXAxleID == 1 && trickYAxleID == 0 && trickZAxleID == 2)
            trickID = 7;
        else if (trickXAxleID == 2 && trickYAxleID == 0 && trickZAxleID == 2)
            trickID = 8;
        else if (trickXAxleID == 1 && trickYAxleID == 0 && trickZAxleID == 3)
            trickID = 9;
        else if (trickXAxleID == 2 && trickYAxleID == 0 && trickZAxleID == 3)
            trickID = 10;
        else if (trickXAxleID == 1 && trickYAxleID == 0 && trickZAxleID == 4)
            trickID = 11;
        else if (trickXAxleID == 2 && trickYAxleID == 0 && trickZAxleID == 4)
            trickID = 12;
        else if (trickXAxleID == 3 && trickYAxleID == 0 && trickZAxleID == 0)
            trickID = 13;
        else if (trickXAxleID == 4 && trickYAxleID == 0 && trickZAxleID == 0)
            trickID = 14;
        else if (trickXAxleID == 0 && trickYAxleID == 0 && trickZAxleID == 3)
            trickID = 15;
        else if (trickXAxleID == 0 && trickYAxleID == 0 && trickZAxleID == 4)
            trickID = 16;

        return trickID;
    }

    static String getTrickNameByID(int trickID, boolean isTail) {
        String trickNameString = "";

        if (MainActivity.stanceOfUser == false) { //if Goofy Stance
            if (isTail == true) {
                if (trickID == 1)
                    trickNameString = "Kickflip";
                else if (trickID == 2)
                    trickNameString = "Heelflip";
                else if (trickID == 3)
                    trickNameString = "Shuvit";
                else if (trickID == 4)
                    trickNameString = "FS Shuvit";
                else if (trickID == 5)
                    trickNameString = "Varial Kickflip";
                else if (trickID == 6)
                    trickNameString = "Inward Heelflip";
                else if (trickID == 7)
                    trickNameString = "Hardflip";
                else if (trickID == 8)
                    trickNameString = "Varial Heelflip";
                else if (trickID == 9)
                    trickNameString = "360 Flip";
                else if (trickID == 10)
                    trickNameString = "360 Inward Heelflip";
                else if (trickID == 11)
                    trickNameString = "360 Hardflip";
                else if (trickID == 12)
                    trickNameString = "Laser Flip";
                else if (trickID == 13)
                    trickNameString = "Double Kickflip";
                else if (trickID == 14)
                    trickNameString = "Double Heelflip";
                else if (trickID == 15)
                    trickNameString = "360 Shuvit";
                else if (trickID == 16)
                    trickNameString = "FS 360 Shuvit";
            } else if (isTail == false) {
                if (trickID == 1)
                    trickNameString = "Nollie Kickflip";
                else if (trickID == 2)
                    trickNameString = "Nollie Heelflip";
                else if (trickID == 3)
                    trickNameString = "Nollie BS Shuvit"; //nollie shuvit do tyłu
                else if (trickID == 4)
                    trickNameString = "Nollie FS Shuvit"; //nollie shuvit do przodu
                else if (trickID == 5)
                    trickNameString = "Nollie Hardflip";
                else if (trickID == 6)
                    trickNameString = "Nollie Varial Heelflip";
                else if (trickID == 7)
                    trickNameString = "Nollie Varial Kickflip";
                else if (trickID == 8)
                    trickNameString = "Nollie Inward Heelflip";
                else if (trickID == 9)
                    trickNameString = "Nollie 360 Hardflip";
                else if (trickID == 10)
                    trickNameString = "Nollie Laser Flip";
                else if (trickID == 11)
                    trickNameString = "Nollie 360 Flip";
                else if (trickID == 12)
                    trickNameString = "Nollie 360 Inward Heelflip";
                else if (trickID == 13)
                    trickNameString = "Nollie Double Flip";
                else if (trickID == 14)
                    trickNameString = "Nollie Double Heel";
                else if (trickID == 15)
                    trickNameString = "Nollie BS 360 Shuvit";
                else if (trickID == 16)
                    trickNameString = "Nollie FS 360 Shuvit";
            }
        } else if (MainActivity.stanceOfUser == true) { //if Regular Stance
            if (isTail == true) {
                if (trickID == 2)
                    trickNameString = "Kickflip";
                else if (trickID == 1)
                    trickNameString = "Heelflip";
                else if (trickID == 4)
                    trickNameString = "Shuvit";
                else if (trickID == 3)
                    trickNameString = "FS Shuvit";
                else if (trickID == 8)
                    trickNameString = "Varial Kickflip";
                else if (trickID == 7)
                    trickNameString = "Inward Heelflip";
                else if (trickID == 6)
                    trickNameString = "Hardflip";
                else if (trickID == 5)
                    trickNameString = "Varial Heelflip";
                else if (trickID == 12)
                    trickNameString = "360 Flip";
                else if (trickID == 11)
                    trickNameString = "360 Inward Heelflip";
                else if (trickID == 10)
                    trickNameString = "360 Hardflip";
                else if (trickID == 9)
                    trickNameString = "Laser Flip";
                else if (trickID == 14)
                    trickNameString = "Double Kickflip";
                else if (trickID == 13)
                    trickNameString = "Double Heelflip";
                else if (trickID == 16)
                    trickNameString = "360 Shuvit";
                else if (trickID == 15)
                    trickNameString = "FS 360 Shuvit";
            } else if (isTail == false) {
                if (trickID == 2)
                    trickNameString = "Nollie Kickflip";
                else if (trickID == 1)
                    trickNameString = "Nollie Heelflip";
                else if (trickID == 4)
                    trickNameString = "Nollie BS Shuvit"; //nollie shuvit do tyłu
                else if (trickID == 3)
                    trickNameString = "Nollie FS Shuvit"; //nollie shuvit do przodu
                else if (trickID == 8)
                    trickNameString = "Nollie Hardflip";
                else if (trickID == 7)
                    trickNameString = "Nollie Varial Heelflip";
                else if (trickID == 6)
                    trickNameString = "Nollie Varial Kickflip";
                else if (trickID == 5)
                    trickNameString = "Nollie Inward Heelflip";
                else if (trickID == 12)
                    trickNameString = "Nollie 360 Hardflip";
                else if (trickID == 11)
                    trickNameString = "Nollie Laser Flip";
                else if (trickID == 10)
                    trickNameString = "Nollie 360 Flip";
                else if (trickID == 9)
                    trickNameString = "Nollie 360 Inward Heelflip";
                else if (trickID == 14)
                    trickNameString = "Nollie Double Flip";
                else if (trickID == 13)
                    trickNameString = "Nollie Double Heel";
                else if (trickID == 16)
                    trickNameString = "Nollie BS 360 Shuvit";
                else if (trickID == 15)
                    trickNameString = "Nollie FS 360 Shuvit";
            }
        }

        return trickNameString;
    }

    public void getTrickID(String TrickName) {
        //if (TrickName.compareToIgnoreCase("Kickflip") == 0) trickID = 1;
    }

    static List<String> trickListGenerator() {
        List trickList = new ArrayList<String>(); //it will store ID of tricks as IndexOf a trick
        //trickList.add("Dummy"); //id 0

        boolean temp = MainActivity.stanceOfUser;
        MainActivity.stanceOfUser = false; //has to be set on false to ensure that tricks are set within goofy stance


        int sizeOfTrickLibrary = 0;
        boolean breakOut = false;
        //ollie trick list
        while(!breakOut)
        {
            if (!getTrickNameByID(sizeOfTrickLibrary + 1, true).isEmpty()) sizeOfTrickLibrary++;
            else breakOut = true;
        }
        Log.i("SkateDB", "SizeOf Trick Library: " + sizeOfTrickLibrary);

        int iterator = 1;
        //ollie trick list
        while(iterator != sizeOfTrickLibrary + 1)
        {
            trickList.add(getTrickNameByID(iterator, true));
            iterator++;
        }

        iterator = 1;
        //nollie trick list
        while(iterator != sizeOfTrickLibrary + 1)
        {
            trickList.add(getTrickNameByID(iterator, false));
            iterator++;
        }
        MainActivity.stanceOfUser = temp; //setting old user stance

        return trickList;
    }

    public void dbIncrement(String trickName) {
        if (MainActivity.databaseHelper.checkIfExists(trickName)) {
            Log.i("SkateDB", trickName + " exists in DB");
            Cursor abc = MainActivity.databaseHelper.getIfExists(trickName);
            if (abc.moveToFirst()) {
                int idOfTrickInDB = abc.getInt(0);
                String howManyTimesTrickWasMade = abc.getString(2);
                int howManyTimes;
                try {
                    howManyTimes = Integer.parseInt(howManyTimesTrickWasMade);
                } catch (NumberFormatException e) {
                    howManyTimes = 0;
                }
                howManyTimes++; //increment trick statistic

                MainActivity.databaseHelper.updateUserInfo(idOfTrickInDB + "", trickName, howManyTimes + "");
                Log.i("SkateDB", trickName + " incremented in stats to: " + howManyTimes);
            }
        }
    }

}
