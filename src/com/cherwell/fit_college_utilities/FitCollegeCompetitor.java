package com.cherwell.fit_college_utilities;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.Settings.Secure;
import android.util.Log;

import com.cherwell.utilities.CherwellImage;

public class FitCollegeCompetitor {
	public static final int advertSectionId = 3;

	public String names;
	public String college;
	public double score;
	public int id;
	public String voteLink;
	public String imgLink;
	
	public String adLink;
	public String adImgLink;
	
	public int layoutId;
	
	private Context ctx;
	
	public FitCollegeCompetitor(Context ctx, String names, String college, int score, int id, String voteLink, String imgLink) {
		this.names = names;
		this.college = college;
		this.score = score;
		this.id = id;
		this.voteLink = voteLink;
		this.imgLink = imgLink;
		this.ctx = ctx;
	}
	
	public FitCollegeCompetitor(Context ctx, String adLink, String adImgLink, int id) {
		this.adImgLink = adImgLink;
		this.adLink = adLink;
		this.ctx = ctx;
		this.id = id;
	}

	public void vote() throws Exception {
		String uniqueId = Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID);
		
		HttpClient client = new DefaultHttpClient();
		String completeVoteLink = voteLink + uniqueId;
		Log.v("user-gen",completeVoteLink);
		HttpGet request = new HttpGet(completeVoteLink);
		client.execute(request);
	}
	
	public Bitmap getImage(boolean forceOnline) throws Exception {
		String fileName = imgLink;
		CherwellImage img = new CherwellImage(imgLink, ctx);
		return img.getImage(false, fileName, forceOnline);
	}
}
