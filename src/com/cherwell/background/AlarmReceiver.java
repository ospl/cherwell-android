package com.cherwell.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		AlarmScheduler.scheduleUpdate(context);

		Intent i = new Intent(context, BackgroundUpdater.class);
		context.startService(i);
	}
}
