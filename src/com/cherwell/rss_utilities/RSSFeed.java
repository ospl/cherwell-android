package com.cherwell.rss_utilities;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cherwell.utilities.FeedUtilities;
import com.cherwell.utilities.GeneralUtils;

public class RSSFeed {
	
	public static final String newsURL = "http://cherwell.org/app/frontPage.xml?app=android";
	public static final String newsSave = "news.xml";
	
	public static final int allSections = 999;
	
	public static final String shortDateFormat = "EEEE d LLLL ";
	public static final String longDateFormat = 	"EEE, dd MMM yyyy HH:mm:ss Z";

	public static final String dateNewsUpdatedPrefs = "rssUpdated";
	
	public Section[] sections;
	
	public String url;
	public String saveFileName;
	
	private SharedPreferences prefs;
	private Editor edit;
	private Context ctx;
	
	public RSSFeed(String url, String saveFileName, Context ctx) {		
		this.url = url;	
		this.saveFileName = saveFileName;

		this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		this.edit = prefs.edit();
		this.ctx = ctx;
	}
	
	public void readFeed(boolean forced) throws Exception {

		boolean sdCardPresent = GeneralUtils.checkForSd();																								// have to check for SD card in each method in case removed in between
		Document xmlDoc;
		
		if (sdCardPresent) {	
			File saveFile = new File(ctx.getExternalFilesDir(null), saveFileName);
            Log.v("user-gen", saveFile.getAbsolutePath() + "   " + saveFile.exists());
			if (!saveFile.exists() || saveFile.length() == 0 || forced) {																																					// downloaded file from internet - savefile is empty
				downloadAndWrite(saveFile, url, edit);
			}
			xmlDoc = FeedUtilities.readSaveFile(saveFile);

		} else {																																														// no SD card - download required
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Date today = new Date();
			edit.putString(dateNewsUpdatedPrefs, today.toString());
			edit.commit();
			xmlDoc = db.parse(url);								
		}
		
		xmlDoc.getDocumentElement().normalize();
		populateSections(xmlDoc, allSections);

	}
	
	public void getMoreArticles(int sectionId, int sectionIndex, int numArticles, int lastArticleId) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		String moreUrl = "http://www.cherwell.org/app/getMoreFromSection.php?sectionId=" + sectionId + "&numArticles=" + numArticles + "&startingAfterArticleId=" + lastArticleId;		
		Document doc = db.parse(moreUrl);
		populateSections(doc, sectionIndex);
	}
	
	public List<Article> getAllArticles() {
		List<Article> allArticles = new ArrayList<Article>();
		for (int i = 0; i < sections.length; i++) {
			allArticles.addAll(sections[i].sectionArticles);
		}
		return allArticles;
	}

	private void populateSections(Document doc, int singleSection) throws ParseException {
		NodeList sectionList = doc.getElementsByTagName("section");
		int numSections = sectionList.getLength();
		
		if (sections == null) sections = new Section[numSections];
		
		for (int i=0; i < numSections; i++) {			
			Node sectionNode = sectionList.item(i);
			Element sectionElmnt = (Element) sectionNode;
			
			Section mSection;

			if (singleSection == allSections) {
				String sectionTitle = sectionElmnt.getAttribute("title");
				String sectionColor = sectionElmnt.getAttribute("colour");
				String sectionId = sectionElmnt.getAttribute("sectionId");
				
				mSection = new Section(sectionTitle, sectionColor, Integer.parseInt(sectionId));
			} else {
				mSection = sections[singleSection];
			}
			
			if (mSection.sectionId == Section.advertSectionId) {
				Element elmnt = (Element) sectionElmnt.getElementsByTagName("advert").item(0);
				NodeList adLinkList = elmnt.getElementsByTagName("link");
				mSection.adLink = FeedUtilities.getTagText(adLinkList);
				
				NodeList adImageLinkList = elmnt.getElementsByTagName("img");
				mSection.adImageLink = FeedUtilities.getTagText(adImageLinkList);
			}
				
			NodeList articleList = sectionElmnt.getElementsByTagName("article");
			
			for (int j = 0; j < articleList.getLength(); j++) {		
				Node articleNode = articleList.item(j);
				Element articleElmnt = (Element) articleNode;
				
				NodeList titleList = articleElmnt.getElementsByTagName("title");
				String titleStr = FeedUtilities.getTagText(titleList);
				
				NodeList descriptionList = articleElmnt.getElementsByTagName("description");
				String descriptionStr = FeedUtilities.getTagText(descriptionList);
				
				NodeList idList = articleElmnt.getElementsByTagName("articleId");
				String idStr =FeedUtilities.getTagText(idList);
							
				NodeList pubDateList = articleElmnt.getElementsByTagName("pubDate");				
			    String dateStr = FeedUtilities.getTagText(pubDateList);
			    if (!dateStr.equals("")) {
				    SimpleDateFormat inForm = new SimpleDateFormat(longDateFormat);
					String originalDate = ((Element) pubDateList.item(0)).getFirstChild().getTextContent();
					SimpleDateFormat outForm = new SimpleDateFormat(shortDateFormat);
					dateStr = outForm.format(inForm.parse(originalDate)).toString();
			    }
			    
				NodeList authorList = articleElmnt.getElementsByTagName("author");
				String authors[] = {""};
				int authorIds[] = {0};
				if (authorList.getLength() > 0) { 
					authors = new String[authorList.getLength()];
					authorIds = new int[authorList.getLength()];
					for (int x = 0; x < authorList.getLength(); x++) {
						authors[x] = ((Element) authorList.item(x)).getFirstChild().getTextContent();
						authorIds[x] = Integer.parseInt( ((Element) authorList.item(x)).getAttribute("id"));
					}
				}
				
				NodeList imgList = articleElmnt.getElementsByTagName("img");
				String imgLinkStr = FeedUtilities.getTagText(imgList);
				
				boolean headline = false;
				if (i == 0 && j == 0 && singleSection == allSections) headline = true;
				
				Article mArticle = new Article(titleStr, descriptionStr, dateStr, Integer.parseInt(idStr), authors, authorIds, imgLinkStr, headline, mSection.sectionId);
				mSection.addArticle(mArticle);
			}
			if (singleSection == allSections) sections[i] = mSection;
			else sections[singleSection] = mSection;
		}
	}
	
	private static void downloadAndWrite(File saveFile, String UrlStr, Editor editor) throws Exception {
		
		FeedUtilities.downloadAndWrite(saveFile, UrlStr);
		
		Date today = new Date();
		editor.putString(dateNewsUpdatedPrefs, today.toString());
		editor.commit();
		
		Log.v("user-gen", "rss feed downloaded and saved");
	}
	
	public long getTimeSinceUpdate() throws ParseException {
			SimpleDateFormat df = new SimpleDateFormat( "EEE MMM dd HH:mm:ss zzz yyyy");
			Date now = new Date();
			
			String dateStr = prefs.getString(RSSFeed.dateNewsUpdatedPrefs, "");
			if (dateStr.equals("")) {
				dateStr = now.toString();
				prefs.edit().putString(RSSFeed.dateNewsUpdatedPrefs, dateStr).commit();
			}
			Date dateUpdated = df.parse(dateStr);

			return (now.getTime() - dateUpdated.getTime()) / (1000*60);
	}
}
