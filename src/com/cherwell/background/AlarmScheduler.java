package com.cherwell.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

public class AlarmScheduler {
	public static final int HALF_HOURLY = 1;
	public static final int HOURLY = 2;
	public static final int DAILY = 3;
	
	public static void scheduleUpdate(Context context) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		int freq = Integer.parseInt(sharedPrefs.getString("update_freq", Integer.toString(HOURLY)));
		Time now = new Time();
		now.setToNow();
		int minsToUpdate = 60 - now.minute;

		if (freq == HALF_HOURLY) {
			Log.v("user-gen", "half hourly updates");
			int check = 30 - now.minute;
			if (check > 0) {
				minsToUpdate = check;
			} else {
				minsToUpdate = 30 + check;
			}
		}
		
		else if (freq == HOURLY) {
			Log.v("user-gen", "hourly updates");
			minsToUpdate = 60 - now.minute;
		} 
		
		else if (freq == DAILY) {
			Log.v("user-gen", "daily updates");
			int checkHr = 12 - now.hour;
			if (checkHr > 0) {
				minsToUpdate = (checkHr - 1)*60 + (60 - now.minute);
			} else {
				minsToUpdate = (24 + checkHr - 1)*60 + (60 - now.minute);
			}
		}

		AlarmManager mgr=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i=new Intent(context, AlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0,i,0);

		long millsToUpdate = minsToUpdate*60*1000;
//		long millsToUpdate = 10000;         // for testing purposes
		
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + millsToUpdate, pi);
	}

	public static void cancelUpdate(Context context) {
		Intent canceI =new Intent(context, AlarmReceiver.class);
		PendingIntent cancelPi = PendingIntent.getBroadcast(context, 0,canceI,0);

		AlarmManager cancelMgr=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		cancelMgr.cancel(cancelPi);
	}
}
