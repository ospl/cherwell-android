package com.cherwell.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.cherwell.activities.FullStoryBaseActivity;
import com.cherwell.android.R;
import com.cherwell.rss_utilities.Article;
import com.cherwell.rss_utilities.FullArticle;
import com.cherwell.utilities.GeneralUtils;
import com.google.analytics.tracking.android.EasyTracker;

public class FullStoryFragment extends SherlockFragment {
	
	public Article article;
	public FullArticle fullStory;

	private boolean cancelTask = false;
	private boolean showLoading;
	private int pagePosition;
	private View view;
	
	private ScrollView scrollView;	
    private WebView webView;
    private FrameLayout customViewContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;
    private myWebChromeClient mWebChromeClient;
    private myWebViewClient mWebViewClient;

	public FullStoryFragment(Article article, int pagePosition) {
		this.article = article;
		this.pagePosition = pagePosition;
	}
	
	public FullStoryFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {				
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		view = inflater.inflate(R.layout.full_story_layout, container, false);		
	    
		scrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        customViewContainer = (FrameLayout) view.findViewById(R.id.customViewContainer);
    	webView = (WebView) view.findViewById(R.id.webView);
        mWebViewClient = new myWebViewClient();
//        webView.setWebViewClient(mWebViewClient);
        mWebChromeClient = new myWebChromeClient();
        webView.setWebChromeClient(mWebChromeClient);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setSaveFormData(true);
		webView.getSettings().setPluginState(PluginState.ON);
		webView.getSettings().setLoadsImagesAutomatically(true);
		webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);

	    if (article.metaDataIsComplete()) {
			fillInHeaderInfo(article);
		}
		
	    GeneralUtils.executeTask(new LoadStoryTask(false, false, getSherlockActivity(), article));

		return view;		
	}
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (isVisibleToUser) {
            this.onResume();
			article.setRead(getSherlockActivity());
			getSherlockActivity().invalidateOptionsMenu();
        } else {
            this.onPause();
        }
	}
	
	@Override
	public void onResume() {
		cancelTask = false;
        if (webView != null)
            webView.onResume();
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		cancelTask = true;
		getSherlockActivity().invalidateOptionsMenu();
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	Log.v("user-gen","refresharticle");

	    switch (item.getItemId()) {
	        case R.id.refreshArticle:
	        	showLoading = true;
	        	getSherlockActivity().invalidateOptionsMenu();
	        	GeneralUtils.executeTask(new LoadStoryTask(true, true, getSherlockActivity(), article));
	        	break;
	        	
	        case R.id.shareArticle:
	        	String text = "\"" + article.title + "\" - " + article.officialLink;
				Intent sharingIntent = new Intent(Intent.ACTION_SEND);
				sharingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				sharingIntent.setType("text/plain");
				sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
				startActivity(Intent.createChooser(sharingIntent,"Share using"));
	        	break;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    return true;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.full_story, menu);									// Inflate the menu; this adds items to the action bar if it is present.		
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		if(showLoading) {																															// even if orientation changed this is called, and showLoading keeps prev value
			menu.findItem(R.id.refreshArticle).setActionView(R.layout.progress_bar);
		} else {
			menu.findItem(R.id.refreshArticle).setActionView(null);
		}
	}

	private class LoadStoryTask extends AsyncTask<String, Void, String> {

		private boolean forced;
		private boolean success = true;
		private Context ctx;
		private Article article;
		private boolean showError;
		
		public LoadStoryTask(boolean forced, boolean showError, Context ctx, Article article) {
			showLoading = true;
			getSherlockActivity().invalidateOptionsMenu();
			
			this.showError = showError;
			this.forced = forced;
			this.ctx = ctx;
			this.article = article;
			
			Log.v("user-gen", article.bodyLink);

		}
		
        @Override
        protected String doInBackground(String... params) {
        	try {
				fullStory = article.getFullArticle(ctx);
				fullStory.readFeed(forced);				
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
				fillInHeaderInfo(fullStory.mainArticle);
				webView.loadDataWithBaseURL("http://www.cherwell.org" ,fullStory.mainArticle.bodyHtml, "text/html", "utf-8", null);
				if (fullStory.olderArticle != null && article.parentSectionId != 0) {
					((FullStoryBaseActivity) getSherlockActivity()).setNextPageArticle(fullStory.olderArticle, pagePosition+1);
				}
			}
			else {
				Log.v("user-gen", "error " + article.bodyLink);
				if (showError)
					Toast.makeText(ctx, "There was an error - please check your internet connection. \nIf this occurs repeatadly this article may not be available for the Cherwell app yet", Toast.LENGTH_LONG).show();
			}
			
			showLoading = false;
        	getSherlockActivity().invalidateOptionsMenu();
      		}
	}	
	
	private void fillInHeaderInfo(Article article) {
		Typeface headerFont = GeneralUtils.getHeaderFont(getSherlockActivity());
		
		TextView title = (TextView) view.findViewById(R.id.title);
		title.setText(article.title);
		title.setTypeface(headerFont,Typeface.BOLD);
		
		TextView description = (TextView) view.findViewById(R.id.description);
		description.setMovementMethod(LinkMovementMethod.getInstance());
		description.setText(Html.fromHtml(article.description));
		description.setTypeface(headerFont);
		
		TextView pubDate = (TextView) view.findViewById(R.id.date);
		pubDate.setText(article.pubDate);
		pubDate.setTypeface(headerFont);
		
		TextView author = (TextView) view.findViewById(R.id.author);
		author.setText(("By " + article.authorStr).toUpperCase());
		author.setTypeface(headerFont);
		
    	EasyTracker.getTracker().sendView("Full Story: " + title.getText());

    	GeneralUtils.executeTask(new UpdateImageTask(true, getSherlockActivity(), article, (ImageView) view.findViewById(R.id.image_header)));
	}
	
	public Article getOlderArticle() {
		return fullStory.olderArticle;
	}
	
	private class UpdateImageTask extends AsyncTask<String, Void, String> {
		
		private boolean thumb;
		private Bitmap bmp;
		private Context ctx;
		private Article article;
		private ImageView img;
		
		public UpdateImageTask(boolean thumb, Context ctx, Article article, ImageView img) {
			this.thumb = thumb;
			this.ctx = ctx;
			this.article = article;
			this.img = img;
		}
		
        @Override
        protected String doInBackground(String... params) {
        	try {
				bmp = article.getImage(thumb, ctx);
			} catch (Exception e) {
				Log.v("user-gen", e.toString());
			}
    	
              return "Executed";
        }      

        @Override
        protected void onPostExecute(String result) {
        	if (cancelTask) return;
        	
        	if (bmp != null) {
        		img.setImageBitmap(bmp);
        	}
        	if (thumb) GeneralUtils.executeTask(new UpdateImageTask(false, ctx, article, img));
        }
        
	}	
	
	public boolean inCustomView() {
        return (mCustomView != null);
    }

    public void hideCustomView() {
        mWebChromeClient.onHideCustomView();
        ((ScrollView) getSherlockActivity().findViewById(R.id.scroll_view)).scrollTo(500,500);

    }

    @Override
	public void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.

        if (webView != null)
            webView.onPause();

        if (inCustomView())
            hideCustomView();
    }

    @Override
	public void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        if (inCustomView()) {
            hideCustomView();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (inCustomView()) {
                hideCustomView();
                return true;
            }
        }
        return false;

    }
	
	class myWebChromeClient extends WebChromeClient {
        private View mVideoProgressView;
        
        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
           onShowCustomView(view, callback);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onShowCustomView(View view,CustomViewCallback callback) {

            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;
            
            getSherlockActivity().getSupportActionBar().hide();
            scrollView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;
        }

        @Override
        public View getVideoLoadingProgressView() {

            if (mVideoProgressView == null) {
                LayoutInflater inflater = LayoutInflater.from(getSherlockActivity());
                mVideoProgressView = inflater.inflate(R.layout.video_progress, null);
            }
            return mVideoProgressView;
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();    //To change body of overridden methods use File | Settings | File Templates.
            if (mCustomView == null)
                return;
            
            getSherlockActivity().getSupportActionBar().show();
            scrollView.setVisibility(View.VISIBLE);
            customViewContainer.setVisibility(View.GONE);

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            mCustomView = null;
        }
    }

    class myWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

}
