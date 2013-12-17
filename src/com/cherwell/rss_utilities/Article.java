package com.cherwell.rss_utilities;

import java.io.Serializable;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import com.cherwell.utilities.CherwellImage;

@SuppressWarnings("serial")

public class Article implements Serializable {
		
	public static final String readKey = "read";

	public String title;
	public String description;
	public String pubDate;
	public int id;
	public String authorStr;
	public int[] authorIds;
	public String imageLink;
	public boolean isHeadline;
	public String bodyLink;
	public String officialLink;
	public String bodyHtml;
	
	public int rowId;
	
	public int parentSectionId;
		
	public Article(String title, String description, String pubDate, int id, String[] authors, int[] authorIds, String imageLink, boolean isHeadline, int parentSectionId) {
		this.parentSectionId = parentSectionId;
		this.setTitle(title);
		this.setAuthors(authors);
		this.setDescription(description);
		this.setPubDate(pubDate);
		this.setId(id);
		this.setAuthorIds(authorIds);
		this.setImageLink(imageLink);
		this.setHeadline(isHeadline);
	}

	public Article(int id, int parentSectionId) {
		this.parentSectionId = parentSectionId;
		this.setId(id);
	}
	
	public boolean metaDataIsComplete() {
		if (this.title != null && this.description != null && this.pubDate != null && this.authorStr != null && this.authorIds != null && this.imageLink != null) 
			return true;
		else 
			return false;
	}
	
	public Bitmap getImage(boolean thumbnail, Context ctx) throws Exception {
		String fileName = thumbnail + "image" + imageLink + "article" + id + ".png";
		
		CherwellImage img = new CherwellImage(imageLink, ctx);
		return img.getImage(thumbnail, fileName, false);							// never need to force refresh article image
	}
	
	public FullArticle getFullArticle(Context context) throws Exception {
		return new FullArticle(this, context);
	}
	
	public void setRead(Context ctx) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	    Editor edit = prefs.edit();
		edit.putBoolean(readKey + id, true);
		edit.commit();
	}
	
	public boolean hasBeenOpened(Context ctx) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		return prefs.getBoolean(readKey + id, false);
	}
	
	public void setId(int id) {
		this.id = id;
		this.officialLink = "http://www.cherwell.org/content/" + id;
		this.bodyLink =  "http://www.cherwell.org/app/getArticleBody.xml?articleId=" + id + "&sectionId=" + parentSectionId + "&app=android";
	}
	public void setHeadline(boolean isHeadline) {
		this.isHeadline = isHeadline;
	}
	public void setImageLink(String imageLink) {
		this.imageLink = imageLink;
	}
	public void setAuthorIds(int[] authorIds) {
		this.authorIds = authorIds;
	}
	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}
	public void setAuthors(String[] authors) {
		for (int i = 0; i < authors.length; i++)  {
			if (i > 0) this.authorStr = this.authorStr + ", ";
			
			if (i==0) 
				this.authorStr = authors[i];
			else 
				this.authorStr = this.authorStr + authors[i];
		}	
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setBodyString(String bodyHtml) {
		this.bodyHtml = bodyHtml;
	}
}
