package com.cherwell.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBoot extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent bootintent) {
		AlarmScheduler.scheduleUpdate(context);		
	}

}