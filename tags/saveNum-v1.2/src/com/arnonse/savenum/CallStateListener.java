package com.arnonse.savenum;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
//import android.util.Log;
import android.text.ClipboardManager;

public class CallStateListener extends PhoneStateListener {

	Context ctx = null;
	SharedPreferences prefs;
	Timer t = null;
	PowerManager pm; 
	PowerManager.WakeLock wl;
	private static boolean hasLock = false;

	CallStateListener(Context ctx) {
		super();
		this.ctx = ctx;
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		pm  = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
		wl  = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag"); 
	}

	public void onCallStateChanged(int state, String incomingNumber) {
		switch (state) {
		case TelephonyManager.CALL_STATE_IDLE:
			handleRemoveNotification();
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			activateNotification();
			break;
		case TelephonyManager.CALL_STATE_RINGING:
			break;
		}
	}

	public void activateNotification() {

		cancelTimer();

		final int APP_ID = 0;

		NotificationManager mManager = (NotificationManager) ctx
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, null,
				System.currentTimeMillis());

		Intent notificationIntent = new Intent(ctx, saveNum.class);
		PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(ctx, "Number Saver",
				"Click to open application", contentIntent);
		notification.flags = Notification.FLAG_ONGOING_EVENT;

		mManager.notify(APP_ID, notification);
	}

	private void cancelTimer() {
		if (t != null) {
			t.cancel();
			t.purge();
			t = null;
		}
		
		if (hasLock)
		{
			wl.release();
			hasLock=false;
		}
	}

	private void handleRemoveNotification() {
		cancelTimer();
		
		ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
		
		int timeout = Integer.parseInt(prefs.getString("suspendTime", "0")) * 1000;
		if ((timeout == 0) || (clipboard.getText().toString().equals("")))
			clearNotification();
		else {
			
			if (!hasLock) 
			{ 
				wl.acquire();
				hasLock = true;
			}

			t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					clearNotification();
					if (hasLock)
					{
						wl.release();
						hasLock=false;
					}
				}
			}, timeout);
		}
	}

	public void clearNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) ctx
				.getSystemService(ns);
		mNotificationManager.cancel(0);
	}

}