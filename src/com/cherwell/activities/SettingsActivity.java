package com.cherwell.activities;

import com.cherwell.android.R;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;

public class SettingsActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {

	private SharedPreferences prefs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {        
		super.onCreate(savedInstanceState);        

		ActionBar actionBar = getSupportActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(true);			
	    
    	EasyTracker.getTracker().sendView("Settings");
        addPreferencesFromResource(R.xml.preferences);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	@Override
	  public void onStart() {
	    super.onStart();
	    EasyTracker.getInstance().activityStart(this);
	  }

	  @Override
	  public void onStop() {
	    super.onStop();
	    EasyTracker.getInstance().activityStop(this);
	  }

	@Override
	public void onResume() {
		super.onResume();
		
		checkDependencies();
		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		checkDependencies();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    
	        case android.R.id.home:																					// Up to home page - icon clicked in action bar
	            Intent upIntent = new Intent(this, BaseActivity.class);
	            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(upIntent);
	            finish();
	            break;
	            
	         default:	 
	        	 return super.onOptionsItemSelected(item);
	    }
	    return true;
	}
	
	private void checkDependencies() {
		PreferenceCategory notificationsCat = (PreferenceCategory) findPreference("notifications");
		ListPreference freq = (ListPreference) findPreference("update_freq");

		if (prefs.getBoolean("perform_updates", true) || prefs.getBoolean("perform_updates_fit", true)) {
			notificationsCat.setEnabled(true);
			freq.setEnabled(true);
		} else {
			notificationsCat.setEnabled(false);
			freq.setEnabled(false);
		}
	}

}
