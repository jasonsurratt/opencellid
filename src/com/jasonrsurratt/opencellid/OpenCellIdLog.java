/*
 * Copyright (C) 2012 Jason R. Surratt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.jasonrsurratt.opencellid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class is not thread safe.
 * 
 * @author jason.surratt
 * 
 */
public class OpenCellIdLog extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_NAME = "log";
	private static final String TABLE_CREATE = "CREATE TABLE " + DATABASE_NAME
			+ " (x REAL, y REAL, accuracy REAL, locationTime REAL, time REAL, kvp TEXT);";
	
	private static OpenCellIdLog _theInstance;
	
	public class ObservationIterator implements Iterator<Observation>
	{
		SQLiteDatabase _db;
		Cursor _cursor;
		
		public ObservationIterator()
		{
			_db = getReadableDatabase();
			_cursor = _db.rawQuery("SELECT * FROM " + DATABASE_NAME, new String[0]);
		}
		public boolean hasNext() {
			return _cursor.isLast() == false;
		}

		public Observation next() {
			_cursor.moveToNext();
			Observation o = new Observation();
			o.setX(_cursor.getDouble(0));
			o.setY(_cursor.getDouble(1));
			o.setAccuracy(_cursor.getDouble(2));
			o.setLocationTime(_cursor.getLong(3));
			o.setTime(_cursor.getLong(4));
			o.setKvp(parseKvp(_cursor.getString(5)));
			return o;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

	private OpenCellIdLog(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
	}

	public Map<String, String> parseKvp(String str) {
		TreeMap<String, String> result = new TreeMap<String, String>();
		String[] rows = str.split("\n");
		for (String r : rows)
		{
			String[] kv = r.split("\t");
			if (kv.length == 2)
			{
				result.put(kv[0], kv[1]);
			}
		}
		return result;
	}

	public static OpenCellIdLog getInstance(Context context) {
		if (_theInstance == null) {
			_theInstance = new OpenCellIdLog(context);
		}
		return _theInstance;
	}

	public long countLogEntries() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DATABASE_NAME,
				new String[0]);

		if (c.moveToNext()) {
			return c.getLong(0);
		}
		else
		{
		throw new RuntimeException("Error retrieving row count from log.");
		}
		
	}

	public void log(double x, double y, double acc, long locationTime, long time, Map<String, String> kvp) {
		SQLiteDatabase db = getWritableDatabase();

		try {

			ContentValues values = new ContentValues();
			values.put("x", x);
			values.put("y", y);
			values.put("accuracy", acc);
			values.put("locationTime", locationTime);
			values.put("time", time);
			values.put("kvp", convertToString(kvp));
			db.insertOrThrow(DATABASE_NAME, null, values);
		} catch (Throwable t) {
			Log.w("OpenCellIdLog", t);
		}

	}

	private String convertToString(Map<String, String> kvp) {
		StringBuffer result = new StringBuffer();

		for (Map.Entry<String, String> e : kvp.entrySet()) {
			result.append(e.getKey());
			result.append("\t");
			result.append(e.getValue());
			result.append("\n");
		}

		return result.toString();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// good for debugging, nothing else.
		db.execSQL("DROP TABLE " + DATABASE_NAME);
		db.execSQL(TABLE_CREATE);
	}

	public void dump(File output, ProgressDialog progress) throws IOException {
		
		FileOutputStream fos = new FileOutputStream(output);
		
		GZIPOutputStream gz = new GZIPOutputStream(fos);

		SQLiteDatabase db = getReadableDatabase();
		
		Cursor c = db.rawQuery("SELECT * FROM " + DATABASE_NAME, new String[0]);
		
		int count = c.getCount();

		int lastP = 0;
		while (c.moveToNext()) {
			int p = c.getPosition() * 100 / count;
			if (p != lastP)
			{
				lastP = p;
				progress.setProgress(p * 100);
			}
				
			StringBuffer buf = new StringBuffer();
			buf.append(c.getDouble(0)).append("\t");
			buf.append(c.getDouble(1)).append("\t");
			buf.append(c.getDouble(2)).append("\t");
			buf.append(c.getLong(3)).append("\t");
			buf.append(c.getLong(4)).append("\t");
			buf.append(encodeKvp(c.getString(5))).append("\n");
			gz.write(buf.toString().getBytes());
		}
		
		gz.close();
		fos.close();
	}

	private String encodeKvp(String str) {
		return str.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
	}

	public Iterable<Observation> getAllObservations() {
		return new Iterable<Observation>()
		{
			public Iterator<Observation> iterator() {
				return new ObservationIterator();
			}
		};
	}

}
