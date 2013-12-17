package com.cherwell.background;

import com.cherwell.android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cherwell.activities.BaseActivity;
import com.cherwell.fit_college_utilities.FitCollegeCompetition;
import com.cherwell.fragments.FitCollegeFragment;
import com.cherwell.fragments.NewsFragment;
import com.cherwell.rss_utilities.Article;
import com.cherwell.rss_utilities.RSSFeed;
import com.cherwell.rss_utilities.UpdateArticlesRunnable;
import com.cherwell.utilities.GeneralUtils;

public class BackgroundUpdater extends Service {

	public static final int handleNewsUpdate = 1;
	public static final int handleFitUpdate = 2;
	
	private SharedPreferences prefs;
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startid) {

		Log.v("user-gen", "service started");
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (!GeneralUtils.checkForSd()) {
			Log.v("user-gen", "no sd card so no point in background update");
			return;
		}

		if (prefs.getBoolean("perform_updates", true)) new UpdateNewsTask(this).execute();
		if (prefs.getBoolean("perform_updates_fit", true)) new UpdateFitTask(this).execute();
	}
	
	private class UpdateNewsTask extends AsyncTask<String, Void, String> {

		private RSSFeed rss;
		private boolean success = true;
		private Bitmap articleImage;
		private Context ctx;
		
		public UpdateNewsTask(Context ctx) {
			this.ctx = ctx;
		}
		
        @Override
        protected String doInBackground(String... params) {
        	try {
        		rss = new RSSFeed(RSSFeed.newsURL, RSSFeed.newsSave, ctx);
				rss.readFeed(true);
				
				Article article = rss.sections[0].sectionArticles.get(0);
				articleImage = article.getImage(true, ctx);
        	} catch (Exception e) {
        		success = false;
        		Log.v("user-gen", e.toString());
        	}    	
              return "Executed";
        }      

        @Override
        protected void onPostExecute(String result) {
        	if (!success) return;
        	
        	Article editorsArticle = rss.sections[0].sectionArticles.get(0);
			
			int editorsId = editorsArticle.id;
			int previousEditorsId = prefs.getInt(NewsFragment.editorsIdKey, 0);
			
			if (editorsId != previousEditorsId) {
				try {
					makeNotification(editorsArticle.title, editorsArticle.description, articleImage, BaseActivity.openNewsKey);
					Editor edit = prefs.edit();
					edit.putInt(NewsFragment.editorsIdKey, editorsId);
					edit.commit();
				} catch (Exception e) {
					Log.v("user-gen", e.toString());
				} 
			} else {
				Log.v("user-gen", "background update done but no new articles");
			}
			
			new UpdateArticlesRunnable(rss, prefs.edit(), ctx).run();
      	}
	}	
	
	private class UpdateFitTask extends AsyncTask<String, Void, String> {

		private FitCollegeCompetition comp;
		private boolean success = true;
		private Context ctx;
		
		public UpdateFitTask(Context ctx) {
			this.ctx = ctx;
		}
		
        @Override
        protected String doInBackground(String... params) {
        	try {
        		comp = new FitCollegeCompetition(ctx);
        		comp.getCurrentFitCollege(true);
        	} catch (Exception e) {
        		success = false;
        		Log.v("user-gen", e.toString());
        	}    	
              return "Executed";
        }      

        @Override
        protected void onPostExecute(String result) {
        	if (!success) return;
        	
        	String thisCompetitor1Names = comp.competitors.get(0).names;
			String previousCompetitor1Names = prefs.getString(FitCollegeFragment.competitor1NamesKey, "");

			if (!thisCompetitor1Names.equals(previousCompetitor1Names)) {
				try {
					Bitmap img = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.fit_notif_icon);
					makeNotification("New Fit College Competition", comp.competitors.get(0).college + " vs " + comp.competitors.get(1).college, img, BaseActivity.openFitKey);
					Editor edit = prefs.edit();
					edit.putString(FitCollegeFragment.competitor1NamesKey, thisCompetitor1Names);
					edit.commit();
				} catch (Exception e) {
					Log.v("user-gen", e.toString());
				} 
			} else {
				Log.v("user-gen", "background update done but no new articles");
			}
//        	if (comp.)
      	}
	}	
	
	private void makeNotification(String title, String content, Bitmap image, int notificationType) throws Exception {
//		String title = article.title;
//		String content = article.description;
//		int id = article.id;
		
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Intent notificationIntent = new Intent(this, BaseActivity.class);
		notificationIntent.putExtra(BaseActivity.fragmentToOpenKey, notificationType);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);		
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

		builder.setContentIntent(contentIntent)
		            .setSmallIcon(R.drawable.ic_notification)
		            .setLargeIcon(image)
		            .setTicker(content)
		            .setWhen(System.currentTimeMillis())
		            .setAutoCancel(true)
		            .setContentTitle(title)
		            .setContentText(content)
		            .setStyle(new NotificationCompat.BigTextStyle().bigText(content));
		
		Notification notification = builder.getNotification();

		
		if (prefs.getBoolean("notif_sound", true)) {
			notification.defaults |= Notification.DEFAULT_SOUND;
		}
		if (prefs.getBoolean("notif_flash", true)) {
			notification.ledARGB = Color.GREEN;
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			notification.defaults |= Notification.DEFAULT_LIGHTS;
		}
		if (prefs.getBoolean("notif_vibrate", true)) {
			long[] vibrate = {0, 150, 200, 150, 200, 150};
			notification.vibrate = vibrate;
		}
		
		nm.notify(notificationType, notification);
	}
}
