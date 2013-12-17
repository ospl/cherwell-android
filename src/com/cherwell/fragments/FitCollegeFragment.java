package com.cherwell.fragments;

import java.text.ParseException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.cherwell.activities.BaseActivity;
import com.cherwell.fit_college_utilities.FitCollegeCompetition;
import com.cherwell.fit_college_utilities.FitCollegeCompetitor;
import com.cherwell.android.R;
import com.cherwell.utilities.ExpandAnim;
import com.cherwell.utilities.GeneralUtils;
import com.google.analytics.tracking.android.Tracker;

public class FitCollegeFragment extends SherlockFragment implements OnClickListener {

	private boolean showLoading;
	private boolean cancelTask = false;
	private Typeface font;
	private Typeface headerFont;
	private SharedPreferences prefs;
	
	private static final int forceRefreshTime = 5;
	public static final String competitor1NamesKey = "comp1nameskey";

	private static final String feedErrorMessage = "There was an error loading the feed. Check your internet connection, or try again.";
	private static final String externalVoteLink = "http://www.cherwell.org/lifestyle/fitcollege";
	
	private FitCollegeCompetition competition;
	private FitCollegeCompetition loadingCompetition;
	private Tracker analytics;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
		analytics = ((BaseActivity) getSherlockActivity()).getEasyTracker();
		analytics.sendView("Fit College");
		
		setHasOptionsMenu(true);
	    setRetainInstance(true); 
	    
	    font = GeneralUtils.getStandardFont(getSherlockActivity());
		headerFont = GeneralUtils.getHeaderFont(getSherlockActivity());

		return inflater.inflate(R.layout.fit_college_layout, container, false);		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);		
	    prefs = PreferenceManager.getDefaultSharedPreferences(getSherlockActivity());
	}
	
	@Override 
	public void onStart() {
		GeneralUtils.executeTask(new UpdateFitCollegeTask(false)); 			// onstart so score can be calculated from width of view - has to be set up
		super.onStart();
	}
	@Override
	public void onDestroy() {
		cancelTask = true;
		showLoading = false;
		getSherlockActivity().invalidateOptionsMenu();
		super.onDestroy();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fit_college_fragment_menu, menu);									// Inflate the menu; this adds items to the action bar if it is present.		
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		if(showLoading) {																															// even if orientation changed this is called, and showLoading keeps prev value
			menu.findItem(R.id.refreshFit).setActionView(R.layout.progress_bar);
		} else {
			menu.findItem(R.id.refreshFit).setActionView(null);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.refreshFit:
	        	GeneralUtils.executeTask(new UpdateFitCollegeTask(true));
	        	break;    
	        case R.id.share:
	        	Intent sharingIntent = new Intent(Intent.ACTION_SEND);
				sharingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				sharingIntent.setType("text/plain");
				String text = "Vote on Cherwell's latest Fit College competition: " + externalVoteLink;
				sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
				startActivity(Intent.createChooser(sharingIntent,"Share using"));
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    return true;
	}
	
	private class UpdateImagesTask extends AsyncTask<String, Void, String> {

		public ImageView imgView;
		public FitCollegeCompetitor fc;
		public Bitmap bmp;
		
		public UpdateImagesTask(FitCollegeCompetitor fitComp, ImageView imageView) {
			fc = fitComp;
			imgView = imageView;
		}
		
		@Override
		protected String doInBackground(String... params) {
			try {
				bmp = fc.getImage(false);
			} catch (Exception e) {
				Log.v("user-gen", e.toString());
			}
			return null;	
		}
		
		  @Override
	        protected void onPostExecute(String result) {
			  if (cancelTask) return;
			  
			  if (bmp != null) imgView.setImageBitmap(bmp);
		  }
	}
	
	private class UpdateFitCollegeTask extends AsyncTask<String, Void, String> {

		public boolean forced;
		public boolean success;
		
		public UpdateFitCollegeTask(boolean forceOnline) {
			forced = forceOnline;
			showLoading = true;
			getSherlockActivity().invalidateOptionsMenu();
		}
		
        @Override
        protected String doInBackground(String... params) {      
			loadingCompetition = new FitCollegeCompetition(getSherlockActivity());

			try {
				loadingCompetition.getCurrentFitCollege(forced);
				success = true;
			} catch (Exception e) {
				Log.v("user-gen", e.toString());
				success = false;
			}
              return "Executed";
        }
        
        @Override
        protected void onPostExecute(String result) {
        	if (cancelTask) return;
 
        	showLoading = false;
			getSherlockActivity().invalidateOptionsMenu();

        	if (success) {
        		competition = loadingCompetition;
        		refreshViews();
        		
        		try {
    				long minsSince = competition.getTimeSinceUpdate();
    				Log.v("user-gen", minsSince +"");
    				if (minsSince > forceRefreshTime && !forced)
    					GeneralUtils.executeTask(new UpdateFitCollegeTask(true)); 		
    			} catch (ParseException e) {
    				Log.v("user-gen", e.toString());
    			}
        	} else {
        		Toast.makeText(getSherlockActivity(), feedErrorMessage, Toast.LENGTH_LONG).show();
        	}
        }
	}
	
	private void refreshViews() {
		if (competition.competitors.size() == 0) return;
				
		FitCollegeCompetitor comp1 = competition.competitors.get(0);
		FitCollegeCompetitor comp2 = competition.competitors.get(1);

		getSherlockActivity().findViewById(R.id.competitor_1_view).setOnClickListener(this);
		comp1.layoutId = R.id.competitor_1_view;
		getSherlockActivity().findViewById(R.id.competitor_2_view).setOnClickListener(this);
		comp2.layoutId = R.id.competitor_2_view;

		Editor edit = prefs.edit();
		edit.putString(competitor1NamesKey, comp1.names);
		edit.commit();
		
		GeneralUtils.executeTask(new UpdateImagesTask(comp1, (ImageView) getSherlockActivity().findViewById(R.id.fit_1)));
		GeneralUtils.executeTask(new UpdateImagesTask(comp2, (ImageView) getSherlockActivity().findViewById(R.id.fit_2)));
		
		if (competition.competitors.size() > 2) {
			FitCollegeCompetitor advert = competition.competitors.get(2);
            WebView adImgWeb = ((WebView) getSherlockActivity().findViewById(R.id.adImgView));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                adImgWeb.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            String data = GeneralUtils.getAdHTML(advert.adImgLink, advert.adLink, true);
            adImgWeb.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
            adImgWeb.getSettings().setSupportZoom(true);
            adImgWeb.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        }
		
		((TextView) getSherlockActivity().findViewById(R.id.names_1)).setText(comp1.names);
		((TextView) getSherlockActivity().findViewById(R.id.names_1)).setTypeface(font);
		((TextView) getSherlockActivity().findViewById(R.id.names_2)).setText(comp2.names);
		((TextView) getSherlockActivity().findViewById(R.id.names_2)).setTypeface(font);

		((TextView) getSherlockActivity().findViewById(R.id.college_1)).setText(comp1.college);
		((TextView) getSherlockActivity().findViewById(R.id.college_1)).setTypeface(headerFont, Typeface.BOLD);
		((TextView) getSherlockActivity().findViewById(R.id.college_2)).setText(comp2.college);
		((TextView) getSherlockActivity().findViewById(R.id.college_2)).setTypeface(headerFont, Typeface.BOLD);
			
		
		float totalVotes = (int) (comp1.score + comp2.score);
		double percentageComp1 = (comp1.score / totalVotes) * 100;
		double percentageComp2 = (comp2.score / totalVotes) * 100;
		
		TextView score1 = (TextView) getSherlockActivity().findViewById(R.id.score_1);
		score1.setTypeface(font);
		score1.setText(Math.round(percentageComp1) + "%");
		TextView score2 = (TextView) getSherlockActivity().findViewById(R.id.score_2);
		score2.setTypeface(font);
		score2.setText(Math.round(percentageComp2) + "%");

		int animTime = 500;
		
		View scale1 = getSherlockActivity().findViewById(R.id.scale_1);
		int scale1Width = (int) ((((View) scale1.getParent()).getWidth() / 100) * percentageComp1);
		Animation anim1 = new ExpandAnim(scale1, scale1Width);
		anim1.setDuration(animTime);
		anim1.setInterpolator(new DecelerateInterpolator());
		if (scale1Width != scale1.getWidth()) scale1.startAnimation(anim1);

		View scale2 = getSherlockActivity().findViewById(R.id.scale_2);
		int scale2Width = (int) ((((View) scale2.getParent()).getWidth() / 100) * percentageComp2);
		Animation anim2 = new ExpandAnim(scale2, scale2Width);
		anim2.setDuration(animTime);
		anim2.setInterpolator(new DecelerateInterpolator());
		if (scale2Width != scale2.getWidth()) scale2.startAnimation(anim2);
		
		if (comp1.score < comp2.score) {
			scale1.setBackgroundResource(R.drawable.red);
			score1.setTextColor(getSherlockActivity().getResources().getColor(R.drawable.red));
		} else if (comp2.score < comp1.score){
			scale2.setBackgroundResource(R.drawable.red);
			score2.setTextColor(getSherlockActivity().getResources().getColor(R.drawable.red));
		}

		if (competition.getVotedCompetition()) {
			  View layout = getSherlockActivity().findViewById(competition.competitors.get(competition.getVotedId()).layoutId);
			  layout.findViewById(R.id.voted_indicator).setVisibility(View.VISIBLE);
			  getSherlockActivity().findViewById(R.id.competitor_1_view).setClickable(false);
			  getSherlockActivity().findViewById(R.id.competitor_2_view).setClickable(false);
		}
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
			case R.id.competitor_1_view:
				if (competition.competitors.size() < 2 || showLoading) return;
				voteFor(competition.competitors.get(0));
				break;
			case R.id.competitor_2_view:
				if (competition.competitors.size() < 2 || showLoading) return;
				voteFor(competition.competitors.get(1));
				break;
//			case R.id.adImgView:
//				if (competition.competitors.size() < 3) return;
//
//				String adLink = competition.competitors.get(2).adLink;
//				 Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(adLink));
//				 analytics.sendEvent("Ad clicked", "fit college page", adLink, null);
//				 startActivity(browserIntent);
//				break;
		}
	}
	
	private void voteFor(final FitCollegeCompetitor competitor) {
		AlertDialog.Builder alert = new AlertDialog.Builder(getSherlockActivity());
		alert.setTitle("Vote for " + competitor.college + "?");
		alert.setMessage(competitor.names);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		    	GeneralUtils.executeTask(new VoteTask(competitor.id - 1, competition));
		    }
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
//		    	dialog.dismiss();
		    }
		});
		
		alert.show();
	}
	
	private class VoteTask extends AsyncTask<String, Void, String> {
		
		private int competitorId;
		private FitCollegeCompetition mCompetition;
		private boolean success = true;
		
		public VoteTask(int competitorId, FitCollegeCompetition mCompetition) {
			this.competitorId = competitorId;
			this.mCompetition = mCompetition;
			
			showLoading = true;
			getSherlockActivity().invalidateOptionsMenu();
		}
		
		@Override
		protected String doInBackground(String... params) {
			try {
				mCompetition.voteFor(competitorId);
			} catch (Exception e) {
				success = false;
				Log.v("user-gen", e.toString());
			}
			return null;	
		}
		
		  @Override
	        protected void onPostExecute(String result) {
			  if (cancelTask) return;
			  
			  showLoading = false;
			  getSherlockActivity().invalidateOptionsMenu();
			  
			  if (success) {
				  FitCollegeCompetitor competitor = mCompetition.competitors.get(competitorId);
				  analytics.sendEvent("Fit college vote sent", "fit college page", competitor.college + ": " + competitor.names, null);

				  View layout = getSherlockActivity().findViewById(competitor.layoutId);
				  layout.findViewById(R.id.voted_indicator).setVisibility(View.VISIBLE);
				  getSherlockActivity().findViewById(R.id.competitor_1_view).setClickable(false);
				  getSherlockActivity().findViewById(R.id.competitor_2_view).setClickable(false);
				  
				  GeneralUtils.executeTask(new UpdateFitCollegeTask(true));
			  }
		  }
	}
}

