package com.cherwell.fragments;

import java.text.ParseException;
import java.util.List;

import android.content.Context;
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
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.cherwell.activities.BaseActivity;
import com.cherwell.activities.FullStoryBaseActivity;
import com.cherwell.android.R;
import com.cherwell.rss_utilities.Article;
import com.cherwell.rss_utilities.RSSFeed;
import com.cherwell.rss_utilities.Section;
import com.cherwell.rss_utilities.UpdateArticlesRunnable;
import com.cherwell.utilities.EllipsizingTextView;
import com.cherwell.utilities.GeneralUtils;
import com.google.analytics.tracking.android.Tracker;

public class NewsFragment extends SherlockFragment implements OnClickListener {

	public static final int fragId = 0;
	
	public static final String showSectionKey = "showSection";
	
	private static final String feedErrorMessage = "There was an error loading the feed. Check your internet connection, or try again.";
	private static final int forceRefreshTime = 5;
	private static final String dividerInt = "1234";
	
	public static String editorsIdKey = "newestId";
		
	private Typeface font;
	private Typeface headerFont;
	private View view;
	private RSSFeed currentRss;
	private RSSFeed loadingRss;
	private SharedPreferences prefs;
	private boolean showLoading = true;
	private boolean cancelTask = false;
	private String feedURL;
	private String feedSaveFile;
	private ViewGroup[] sectionLayouts;	
	private Tracker analytics;
	
	public NewsFragment() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.news_layout, container, false);

	    setHasOptionsMenu(true);
	    setRetainInstance(true); 
	    	    
	    // start loading feed
		feedURL = RSSFeed.newsURL;
		feedSaveFile = RSSFeed.newsSave;
		
		analytics = ((BaseActivity) getSherlockActivity()).getEasyTracker();
		analytics.sendView("News");

		GeneralUtils.executeTask(new UpdateFeedTask(false));

		font = GeneralUtils.getStandardFont(getSherlockActivity());
		headerFont = GeneralUtils.getHeaderFont(getSherlockActivity());
		
	    return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);		
	    prefs = PreferenceManager.getDefaultSharedPreferences(getSherlockActivity());
	}
	
	@Override
	public void onDestroy() {
		cancelTask = true;
		super.onDestroy();
	}

	@Override
	public void onResume() {
		if (currentRss != null) {
			List<Article> allArticles = currentRss.getAllArticles();
			for (int i = 0; i < allArticles.size(); i++) {
				View row = getSherlockActivity().findViewById(allArticles.get(i).rowId);
				if (row != null) {
					View readIndicator = row.findViewById(R.id.readIndicator);
					if (readIndicator != null) {
						if (allArticles.get(i).hasBeenOpened(getSherlockActivity())) 
							readIndicator.setVisibility(View.GONE);
					}	
				}	
			}
		}
		super.onResume();
	}
	
	public void onClick(View v) {

		if (v.getId() == R.id.header) {
			int sectionId = ((View) v.getParent().getParent()).getId();
			toggleBody(sectionId);
			
//		} else if (v.getId() == R.id.ad_image) {
//			 int sectionId = ((View) v.getParent()).getId();
//			 String adLink = currentRss.sections[sectionId].adLink;
//			 Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(adLink));
//			 analytics.sendEvent("Ad clicked", "news page", adLink, null);
//			 startActivity(browserIntent);
//
		} else if (v.getId() == R.id.showMoreButton) {
			int sectionId = ((View) v.getParent().getParent()).getId();
			LinearLayout body = (LinearLayout) sectionLayouts[sectionId].findViewById(R.id.body);
			int children = body.getChildCount();
			if (children < currentRss.sections[sectionId].sectionArticles.size()) {
				for (int i = children; i < currentRss.sections[sectionId].sectionArticles.size() && i < children + 3; i++){
					addRow(currentRss.sections[sectionId].sectionArticles.get(i), body, sectionId, i);
				}
				return;
			}
			
			v.setEnabled(false);
			
			List<Article> secArticles = currentRss.sections[sectionId].sectionArticles;
			int lastArticleId = secArticles.get(secArticles.size()-1).id;
			GeneralUtils.executeTask((AsyncTask<String,Void,String>) new MoreArticlesTask(sectionId, lastArticleId));
		
		} else if (v.getId() == R.id.showLessButton) {
			int sectionId = ((View) v.getParent().getParent()).getId();
			LinearLayout body =  (LinearLayout) sectionLayouts[sectionId].findViewById(R.id.body);
			
			int scrollUpHeight = 0;
			for (int i = 0; i < 3; i++) {
				View vw = body.getChildAt(body.getChildCount()-1);
				scrollUpHeight += vw.getHeight();
				body.removeView(vw);
			}
			ScrollView scrollView = (ScrollView) getSherlockActivity().findViewById(R.id.scroll);
			scrollView.scrollBy(0, -1 * scrollUpHeight);
			
			if (body.getChildCount() <= 3) {
				sectionLayouts[sectionId].findViewById(R.id.showLessButton).setVisibility(View.GONE);
				sectionLayouts[sectionId].findViewById(R.id.less_divider).setVisibility(View.GONE);				
			}
		
		} else if (String.valueOf(v.getId()).contains(dividerInt)) {
			String rowId = String.valueOf(v.getId());
			
			String secIndex = rowId.substring(0, rowId.indexOf(dividerInt));
			if (secIndex.equals("")) secIndex = "0";
			String articleIndex = rowId.substring(rowId.indexOf(dividerInt) + dividerInt.length());
			if (articleIndex.equals("")) articleIndex = "0";

			Intent intent = new Intent(getSherlockActivity(), FullStoryBaseActivity.class);
			
			Section sec = currentRss.sections[Integer.parseInt(secIndex)];
			intent.putExtra("section", sec);
			intent.putExtra("index", Integer.valueOf(articleIndex));
			
			v.findViewById(R.id.readIndicator).setVisibility(View.GONE);
			
			startActivity(intent);
		}
	}
	
	private class MoreArticlesTask extends AsyncTask<String, Void, String> {

		public int sectionIndex;
		public boolean success;
		public int lastArticleId;
		
		public MoreArticlesTask(int index, int articleId) {
			sectionIndex = index;
			lastArticleId = articleId;
			showLoading = true;
			getSherlockActivity().invalidateOptionsMenu();
		}
		
        @Override
        protected String doInBackground(String... params) {
        	try {
				currentRss.getMoreArticles(currentRss.sections[sectionIndex].sectionId, sectionIndex, 3, lastArticleId);
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
        	
        	if (success) {
        		Section sec  = currentRss.sections[sectionIndex];
        		LinearLayout body =  (LinearLayout) sectionLayouts[sectionIndex].findViewById(R.id.body);
        		for (int i = body.getChildCount(); i < sec.sectionArticles.size(); i++) {
        			addRow(sec.sectionArticles.get(i), body, sectionIndex, i);
        		}
        	} else {
        		Toast.makeText(getSherlockActivity(), feedErrorMessage, Toast.LENGTH_LONG).show();
        	}
        	sectionLayouts[sectionIndex].findViewById(R.id.showMoreButton).setEnabled(true);
        	showLoading = false;
			getSherlockActivity().invalidateOptionsMenu();
        }
	}	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.news_fragment_menu, menu);									// Inflate the menu; this adds items to the action bar if it is present.		
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		if(showLoading) {																															// even if orientation changed this is called, and showLoading keeps prev value
			menu.findItem(R.id.refreshRss).setActionView(R.layout.progress_bar);
		} else {
			menu.findItem(R.id.refreshRss).setActionView(null);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.refreshRss:
	        	GeneralUtils.executeTask(new UpdateFeedTask(true));
	        	break;    
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    return true;
	}

	
	private class UpdateFeedTask extends AsyncTask<String, Void, String> {

		public boolean forced;
		public boolean success;
		
		public UpdateFeedTask(boolean forceOnline) {
			forced = forceOnline;
			showLoading = true;
			getSherlockActivity().invalidateOptionsMenu();
		}
		
        @Override
        protected String doInBackground(String... params) {
        	loadingRss = new RSSFeed(feedURL, feedSaveFile, getSherlockActivity());

			try {
				loadingRss.readFeed(forced);
				success = true;
			} catch (Exception e) {
				Log.v("user-gen", "updatefeedtask " + e.toString());
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
        		currentRss = loadingRss;										// so user can click on row while refresh occuring - only full values go into current rss
        		refreshViews();
    			new Thread(new UpdateArticlesRunnable(currentRss, prefs.edit(), getSherlockActivity())).start();

				try {
					long minsSince = currentRss.getTimeSinceUpdate();
					Log.v("user-gen", minsSince +"");
					if (minsSince > forceRefreshTime && !forced)
						GeneralUtils.executeTask(new UpdateFeedTask(true)); 
				} catch (ParseException e) {
					Log.v("user-gen", e.toString());
				}
				
        	} else {
        		Toast.makeText(getSherlockActivity(), feedErrorMessage, Toast.LENGTH_LONG).show();
        	}
        }
	}
	
	private void toggleBody(int i) {
		LinearLayout body = (LinearLayout) sectionLayouts[i].findViewById(R.id.body);
		LinearLayout buttonsLayout = (LinearLayout) sectionLayouts[i].findViewById(R.id.buttonsLayout);
		View bottomBodyColour = sectionLayouts[i].findViewById(R.id.bottom_body_colour);
		View bottomSectionColour = sectionLayouts[i].findViewById(R.id.bottom_section_colour);
		
		Editor edit = prefs.edit();
		int sectionId = currentRss.sections[i].sectionId;
		
		if (body.getVisibility() == View.VISIBLE) {
			body.setVisibility(View.GONE);
			buttonsLayout.setVisibility(View.GONE);
			bottomBodyColour.setVisibility(View.GONE);
			bottomSectionColour.setVisibility(View.GONE);
			((ImageView) sectionLayouts[i].findViewById(R.id.expand_arrow)).setImageDrawable(getResources().getDrawable(R.drawable.arrow_down));

			edit.putBoolean(showSectionKey + sectionId, false);
		} else {
			body.setVisibility(View.VISIBLE);				
			bottomBodyColour.setVisibility(View.VISIBLE);
			bottomSectionColour.setVisibility(View.VISIBLE);
			if (i != 0) buttonsLayout.setVisibility(View.VISIBLE);
			((ImageView) sectionLayouts[i].findViewById(R.id.expand_arrow)).setImageDrawable(getResources().getDrawable(R.drawable.arrow_up));
			
			edit.putBoolean(showSectionKey + sectionId, true);
		}
		edit.commit();
	}

	private void refreshViews() {       
		LinearLayout linear = (LinearLayout) view.findViewById(R.id.linear_parent);
		linear.removeAllViews();
		
		sectionLayouts = new ViewGroup[currentRss.sections.length];
		
		for (int i = 0; i < currentRss.sections.length; i++) {
			Section mSec = currentRss.sections[i];
			ViewGroup sectionLayout;
			
			if (mSec.sectionId == Section.advertSectionId) {
				sectionLayout = (ViewGroup) getSherlockActivity().getLayoutInflater().inflate(R.layout.ad_layout, linear, false);
				WebView adImgWeb = (WebView) sectionLayout.findViewById(R.id.ad_image);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    adImgWeb.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                String data = GeneralUtils.getAdHTML(mSec.adImageLink, mSec.adLink, false);
                adImgWeb.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
                adImgWeb.getSettings().setSupportZoom(true);
                adImgWeb.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                linear.addView(sectionLayout);
				continue;
			}
			sectionLayout = (ViewGroup) getSherlockActivity().getLayoutInflater().inflate(R.layout.section_layout, linear, false);
			
			sectionLayout.findViewById(R.id.showLessButton).setVisibility(View.GONE);
			sectionLayout.findViewById(R.id.less_divider).setVisibility(View.GONE);
			RelativeLayout parent = (RelativeLayout) sectionLayout.findViewById(R.id.parent);
			parent.setId(i);
			sectionLayout.findViewById(R.id.header).setOnClickListener(this);

            sectionLayout.findViewById(R.id.divider).setBackgroundColor(mSec.getColour(false));

			sectionLayout.findViewById(R.id.showMoreButton).setOnClickListener(this);
			sectionLayout.findViewById(R.id.showLessButton).setOnClickListener(this);
			((TextView) sectionLayout.findViewById(R.id.showMoreButton)).setTypeface(font);
			((TextView) sectionLayout.findViewById(R.id.showLessButton)).setTypeface(font);
			
			TextView sectionTitle = ((TextView) sectionLayout.findViewById(R.id.section_title));
			sectionTitle.setText(mSec.title.toUpperCase());
			sectionTitle.setTypeface(headerFont, Typeface.BOLD);

			sectionLayouts[i] = sectionLayout;

			for (int j = 0; j < mSec.sectionArticles.size(); j++) {
				Article mArticle = mSec.sectionArticles.get(j);
				
				LinearLayout body;
				if (mArticle.isHeadline) {
					body = linear;
				} else {
					body = (LinearLayout) sectionLayout.findViewById(R.id.body);
				}
				addRow(mArticle, body, i, j);
			}

			if (i == 0) {
		    	sectionLayout.findViewById(R.id.buttonsLayout).setVisibility(View.GONE);
		    } else {
				boolean showSection = prefs.getBoolean(showSectionKey + mSec.sectionId, true);
				if (!showSection) toggleBody(i);
		    }
			
			linear.addView(sectionLayout);
		}
	}
	
	private void addRow(Article article, ViewGroup body, int sectionNum, int articleNum) {
		View row;
		
		if (article.isHeadline) {
			row = getSherlockActivity().getLayoutInflater().inflate(R.layout.editors_pick_layout, body, false);
			Editor edit = prefs.edit();
			edit.putInt(editorsIdKey, article.id);
			edit.commit();
		} else {
			row = getSherlockActivity().getLayoutInflater().inflate(R.layout.rss_row_layout, body, false);
			
			TextView author = (TextView) row.findViewById(R.id.article_author);
			author.setTypeface(font);
			author.setText(article.authorStr);
		}
		
		row.setClickable(true);
	    row.setOnClickListener(this);
	    int rowId = Integer.parseInt(sectionNum + "" + dividerInt + "" + articleNum); 
	    article.rowId = rowId;
	    row.setId(rowId);
	    
	    TextView title = (TextView) row.findViewById(R.id.article_title);
	    title.setTypeface(font, Typeface.BOLD);
	    title.setText(article.title);
	    
	    EllipsizingTextView description = (EllipsizingTextView) row.findViewById(R.id.article_description);
	    description.setTypeface(font);
	    description.setText(Html.fromHtml(article.description));
	   	    
	    ImageView img = (ImageView) row.findViewById(R.id.article_image);
	    GeneralUtils.executeTask(new UpdateImageTask(getSherlockActivity(), article, img));
		
		body.addView(row);
		
		if (article.hasBeenOpened(getSherlockActivity())) {
        	row.findViewById(R.id.readIndicator).setVisibility(View.GONE);
        }
        
		if (!article.isHeadline) {
			if (body.getChildCount() > 3) {
				sectionLayouts[sectionNum].findViewById(R.id.showLessButton).setVisibility(View.VISIBLE);
				sectionLayouts[sectionNum].findViewById(R.id.less_divider).setVisibility(View.VISIBLE);
			} else {
				sectionLayouts[sectionNum].findViewById(R.id.showLessButton).setVisibility(View.GONE);
				sectionLayouts[sectionNum].findViewById(R.id.less_divider).setVisibility(View.GONE);
			}
		}
	}
	
	private class UpdateImageTask extends AsyncTask<String, Void, String> {

		private ImageView imageView;
		private Article article;
		private Bitmap bmp;
		private Context ctx;
		
		public UpdateImageTask(Context ctx, Article mArticle, ImageView imgView) {
			article = mArticle;
			imageView = imgView;
			this.ctx = ctx;
		}

        @Override
        protected String doInBackground(String... params) {
		
        	try {
        		bmp = article.getImage(true, ctx);
			} catch (Exception e) {
				Log.v("user-gen", e.toString());
			}
    	
              return "Executed";
        }      

        @Override
        protected void onPostExecute(String result) {
        	if (cancelTask) return;
        	
        	if (bmp != null) {
              imageView.setImageBitmap(bmp);
        	}
        }
	}
}
