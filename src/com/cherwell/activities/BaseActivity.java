package com.cherwell.activities;

import java.io.File;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.cherwell.android.R;
import com.cherwell.background.AlarmScheduler;
import com.cherwell.fragments.AppInfoFragment;
import com.cherwell.fragments.FitCollegeFragment;
import com.cherwell.fragments.NewsDeskFragment;
import com.cherwell.fragments.NewsFragment;
import com.cherwell.fragments.SlidingMenuFragment;
import com.cherwell.utilities.GeneralUtils;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnClosedListener;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class BaseActivity  extends SlidingFragmentActivity {
		
	public static String fragmentToOpenKey = "fragmentToOpenKey";
	public static String chosenLayoutIdKey = "chosenLayoutId";
	
	public static final int openNewsKey = 1;
	public static final int openFitKey = 2;
	
	protected SlidingMenuFragment mFrag;
	private Fragment mContent;
	public boolean tabletLayout = false;

    private SharedPreferences prefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		GeneralUtils.checkForSd();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        AlarmScheduler.scheduleUpdate(this);

		// set the Above View
		setContentView(R.layout.content_frame);

		int layoutPickedId = R.id.newsLayout;
		
		if (savedInstanceState == null) {
			mContent = new NewsFragment();
		}	else {
			mContent = getSupportFragmentManager().getFragment(savedInstanceState, fragmentToOpenKey);
			layoutPickedId = savedInstanceState.getInt(chosenLayoutIdKey);
		}
		
		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.content_frame, mContent)
		.commit();
	
		// customize the SlidingMenu
		this.setSlidingActionBarEnabled(false);
		getSlidingMenu().setShadowWidthRes(R.dimen.shadow_width);
		getSlidingMenu().setShadowDrawable(R.drawable.shadow);
		getSlidingMenu().setBehindWidthRes(R.dimen.sliding_menu_width);
		getSlidingMenu().setFadeDegree(0.35f);

		if (this.findViewById(R.id.menu_frame) == null) { 							// normal phone layout
			setBehindContentView(R.layout.menu_frame);
		    getSupportActionBar().setDisplayHomeAsUpEnabled(true);        
		    getSupportActionBar().setHomeButtonEnabled(true);
			getSlidingMenu().setSlidingEnabled(true);
			getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		} else {																										// tablet layout
			tabletLayout = true;
			View v = new View(this);
			setBehindContentView(v);
			getSlidingMenu().setSlidingEnabled(false);
			getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
		}
		
		FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
		mFrag = new SlidingMenuFragment(layoutPickedId);
		t.replace(R.id.menu_frame, mFrag);
		t.commit();
	}
	
	@Override
	public void onPostCreate(Bundle savedInstanceState) {
	    super.onPostCreate(savedInstanceState);
	    new Handler().postDelayed(new Runnable() {
	        @Override
	        public void run() {
	        	if (!tabletLayout) {
                    try {
                        int vers = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                        if (prefs.getBoolean("firstrun" + vers, true)) {
                            getSlidingMenu().showMenu(false);
                            Editor edit = prefs.edit();
                            edit.putBoolean("firstrun" + vers, false);
                            edit.commit();
                        }
                    } catch (NameNotFoundException e) {
                        Log.v("user-gen", e.toString());
                    }
	        	}
	        }
	    }, 10);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getSupportFragmentManager().putFragment(outState, fragmentToOpenKey, mContent);
		outState.putInt(chosenLayoutIdKey, mFrag.getLayoutPicked());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.home_screen, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	        	startActivity(new Intent(this,SettingsActivity.class));
	        	break;
//	        case R.id.clearPrefs:
//	        	Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
//	        	edit.clear();
//	        	edit.commit();
//	        	break;
	        case android.R.id.home:
	        	getSlidingMenu().toggle();
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    return true;
	}
	
	public void openNews(View v) {
		switchContent(new NewsFragment(), v);
	}
	public void openFitCollege(View v) {
		switchContent(new FitCollegeFragment(), v);
	}
	public void openInfo(View v){
		switchContent(new AppInfoFragment(), v);
	}
	public void contactNewsdesk(View v) {
		switchContent(new NewsDeskFragment(), v);
	}
	public void giveFeedback(View v) {
		Intent goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.cherwell.android"));
		goToMarket.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		startActivity(goToMarket);
	}
	public void openSettings(View v) {
		
		startActivity(new Intent(this,SettingsActivity.class));
	}
	
	public void switchContent(Fragment fragment, View v) {
		if (!mContent.getClass().equals(fragment.getClass())) {
			mFrag.indicatePicked(v.getId());

			if (!tabletLayout) {
				FragmentManager fragManager = getSupportFragmentManager();
				FragmentTransaction trans = fragManager.beginTransaction();
				trans.setCustomAnimations(R.anim.fade_in,R.anim.fade_out);
				trans.remove(mContent);
				trans.commit();
			
				getSlidingMenu().setOnClosedListener(new OpenOnceClosed(fragment, fragManager));
			} else {
				mContent = fragment;
				FragmentManager fragManager = getSupportFragmentManager();
				FragmentTransaction trans = fragManager.beginTransaction();
				trans.setCustomAnimations(R.anim.fade_in,R.anim.fade_out);
				trans.replace(R.id.content_frame, mContent);
				trans.commit();
			}
		}
		getSlidingMenu().showContent();
	}
	
	public Tracker getEasyTracker() {
		EasyTracker.getInstance().setContext(this);
		return EasyTracker.getTracker();
	}

	public void indicateMenuItemPressed() {
		
	}
	private class OpenOnceClosed implements OnClosedListener {

		private Fragment fragment;
		private FragmentManager fragmentMan;

		public OpenOnceClosed(Fragment fragToOpen, FragmentManager fragMan) {
			fragment = fragToOpen;
			fragmentMan = fragMan;
		}
		
		@Override
		public void onClosed() {
			mContent = fragment;
			FragmentTransaction trans = fragmentMan.beginTransaction();
			trans.add(R.id.content_frame, fragment);
			trans.commit();

			getSlidingMenu().setOnClosedListener(null);
		}
		
	}
}
