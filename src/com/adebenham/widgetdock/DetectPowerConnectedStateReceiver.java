package com.adebenham.widgetdock;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DetectPowerConnectedStateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

		// Test for power connected
		if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
			if (settings.getBoolean("startonpower", true)) {
				if (settings.getBoolean("startontime", false)) {
					Calendar now = Calendar.getInstance();
					Calendar start_time = Calendar.getInstance();
					Calendar end_time = Calendar.getInstance();
					start_time.set(Calendar.HOUR_OF_DAY,
							settings.getInt("start_time.hour", 0));
					start_time.set(Calendar.MINUTE,
							settings.getInt("start_time.minute", 0));
					end_time.set(Calendar.HOUR_OF_DAY,
							settings.getInt("end_time.hour", 0));
					end_time.set(Calendar.MINUTE,
							settings.getInt("end.minute", 0));

					if (start_time.after(end_time)) {
						if (now.after(start_time) || now.before(end_time)) {
							start_widgetdock(context);
						}
					} else if (now.after(start_time) && now.before(end_time)) {
						start_widgetdock(context);
					}
				} else {
					start_widgetdock(context);
				}
			}

		}
	}

	private void start_widgetdock(Context context) {
		Intent i = new Intent(context, WidgetDock.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
}
