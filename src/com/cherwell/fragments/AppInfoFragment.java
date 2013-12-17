package com.cherwell.fragments;

import com.cherwell.android.R;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.cherwell.activities.BaseActivity;

public class AppInfoFragment extends SherlockFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);														
	    
	    View view = inflater.inflate(R.layout.app_info_layout, container, false);

	    ((BaseActivity) getSherlockActivity()).getEasyTracker().sendView("App Info");

	    TextView hyperlinkText = (TextView) view.findViewById(R.id.hyperlinkTextView);
	    hyperlinkText.setText(Html.fromHtml("<a href=\"https://play.google.com/store/apps/developer?id=Sarab\">" + ((String) hyperlinkText.getText()) + "</a>"));
	    hyperlinkText.setMovementMethod(LinkMovementMethod.getInstance());
	    
	    return view;    		
	}
}
