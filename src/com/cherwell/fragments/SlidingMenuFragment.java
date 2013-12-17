package com.cherwell.fragments;

import com.cherwell.android.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragment;

public class SlidingMenuFragment extends SherlockFragment {
	
	private View view;
	private int chosenLayoutId = R.id.newsLayout;

	public SlidingMenuFragment() {
		setRetainInstance(true);
	}
	
	public SlidingMenuFragment(int chosenLayoutId) {
		this.chosenLayoutId = chosenLayoutId;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.sliding_menu, null);
		
		
		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		indicatePicked(chosenLayoutId);
	}
	
	public void indicatePicked(int layoutId) {
		int[] layouts = {R.id.newsLayout,R.id.fitLayout,R.id.infoLayout,R.id.newsdeskLayout};
		for (int i=0; i<layouts.length;i++) {
		    ((LinearLayout) view.findViewById(layouts[i])).setBackgroundResource(R.drawable.row_bg);
		}
		
		((LinearLayout) view.findViewById(layoutId)).setBackgroundResource(R.drawable.half_darker_gray);
		chosenLayoutId = layoutId;
	}
	
	public int getLayoutPicked() {
		return chosenLayoutId;
	}
}
