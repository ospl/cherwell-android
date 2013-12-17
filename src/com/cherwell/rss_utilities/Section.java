package com.cherwell.rss_utilities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.cherwell.utilities.CherwellImage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

@SuppressWarnings("serial")
public class Section implements Serializable {
	public static final int advertSectionId = 1000;
	
	private String colour;
	public String title;
	public int sectionId;
	public String adLink;
	public String adImageLink;
		
	public List< Article> sectionArticles = new ArrayList<Article>();
	
	public Section(String title, String colour, int sectionId) {
		this.title = title;
		this.colour = colour;
		this.sectionId = sectionId;
	}
	
	public int getColour(boolean lighter) {
		if (lighter) {
			String fullColour = "#25" + colour.substring(colour.indexOf("#") + 1);
			return Color.parseColor(fullColour);
		} else {
			return Color.parseColor(colour);
		}

	}
	
	public void addArticle(Article article) {
		sectionArticles.add(article);
	}
	
	public void removeArticle(Article article) {
		sectionArticles.remove(article);
	}
}
