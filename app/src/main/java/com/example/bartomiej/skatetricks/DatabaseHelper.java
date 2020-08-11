package com.example.bartomiej.skatetricks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG="DatabaseHelper";
    private static final String TABLE_NAME="people_table";
    private static final String COL1="ID";
    private static final String COL2="login"; //Trick Name | (First one) User MAC Address | (Second one) User Stance
    private static final String COL3="password"; //How many times trick was made| (First one) If Handmode | (Second one) If Debug Mode

    public DatabaseHelper(Context context)//, String name, SQLiteDatabase.CursorFactory factory, int version)
    {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY/* AUTOINCREMENT*/, " + COL2 + " TEXT, " + COL3 + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    public boolean addData(int ID, String item1, String item2){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, ID);
        contentValues.put(COL2, item1);
        contentValues.put(COL3, item2);
        Log.d(TAG, "addData: Adding " + item1 + " to " + TABLE_NAME);

        long result = db.insert(TABLE_NAME, null, contentValues);

        if (result == -1) return false;
        else return true;
    }

    public boolean deleteData(String item){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2, item);

        Log.d(TAG, "addData: Deleting " + item + " from " + TABLE_NAME);

        long result = db.insert(TABLE_NAME, null, contentValues);

        String whereArgs="name LIKE '%"+item+"%";

        db.delete(TABLE_NAME, COL2, new String[] {item});

        if(result==-1)
            return false;
        else
            return true;
    }

    public void deleteAllData(){
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    public Cursor getData()
    {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            Cursor data = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
            return data;
        }
        catch (Exception e){
            return null;
        }
    }

    public Cursor getIfExists(String item)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor data = db.rawQuery("SELECT " + COL1 + ", " + COL2 + ", " + COL3 +
                " FROM " + TABLE_NAME +
                " WHERE " + COL2 + " = '" + item + "'", null);
        return data;
    }

    public Cursor getIfExists(int ID)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor data = db.rawQuery("SELECT " + COL1 + ", " + COL2 + ", " + COL3 +
                " FROM " + TABLE_NAME +
                " WHERE " + COL1 + " = " + ID + "", null);
        return data;
    }

    public boolean checkIfExists(String item)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor data = db.rawQuery("SELECT " + COL1 + ", " + COL2 + ", " + COL3 +
                " FROM " + TABLE_NAME +
                " WHERE " + COL2 + " = '" + item + "'", null);

        if (data.moveToFirst()) return true;
        else return false;
    }

    public boolean checkIfExists(int ID)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor data = db.rawQuery("SELECT " + COL1 + ", " + COL2 + ", " + COL3 +
                " FROM " + TABLE_NAME +
                " WHERE " + COL1 + " = " + ID + "", null);

        if (data.moveToFirst()) return true;
        else return false;
    }

    public int updateUserInfo(String id, String login, String password)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL2, login);
        contentValues.put(COL3, password);

        String whereStatement = COL1 + "=" + id;

        int toReturn = db.update(TABLE_NAME, contentValues, whereStatement,null);

        return toReturn;
    }

    public int updateUserInfoCOL2(String id, String login)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL2, login);

        String whereStatement = COL1 + "=" + id;

        int toReturn = db.update(TABLE_NAME, contentValues, whereStatement,null);

        return toReturn;
    }

    public int updateUserInfoCOL3(String id, String password)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL3, password);

        String whereStatement = COL1 + "=" + id;

        int toReturn = db.update(TABLE_NAME, contentValues, whereStatement,null);

        return toReturn;
    }

    public Cursor getLastFromDB() {
        //SELECT * FROM Table ORDER BY ID DESC LIMIT 1
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlStatement = "SELECT * FROM " + TABLE_NAME + " ORDER BY ID DESC LIMIT 1";

        Cursor data = db.rawQuery(sqlStatement, null);
        return data;
    }
}