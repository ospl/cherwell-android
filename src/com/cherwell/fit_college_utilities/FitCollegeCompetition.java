package com.cherwell.fit_college_utilities;

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

import com.cherwell.rss_utilities.RSSFeed;
import com.cherwell.utilities.FeedUtilities;
import com.cherwell.utilities.GeneralUtils;

public class FitCollegeCompetition {
	
	private static final String currentLink = "http://www.cherwell.org/app/getCurrentFitCollege.php";
	public static final String saveFileName = "fitCollege.xml";
	public static final String dateFitCollegeUpdatedPrefs = "dateFitCollegeUpdated";
	public static final String votedKey = "voted";

	public List<FitCollegeCompetitor> competitors = new ArrayList<FitCollegeCompetitor>();
	
	private Document xmlDoc;
	private SharedPreferences prefs;
	private Context ctx;
	
	public FitCollegeCompetition(Context ctx) {
		 prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		 this.ctx = ctx;
	}
	
	public void getCurrentFitCollege(boolean forceOnline) throws Exception {
		boolean sdCardPresent = GeneralUtils.checkForSd();
		
		if (sdCardPresent) {
			File saveFile = new File(ctx.getExternalFilesDir(null), saveFileName);
			if (!saveFile.exists() || saveFile.length() == 0 || forceOnline) {
				downloadAndWrite(saveFile, currentLink, prefs.edit());
			}
			xmlDoc = FeedUtilities.readSaveFile(saveFile);
		} else {																																												// no SD card - download required
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			xmlDoc = db.parse(currentLink);								
		}
		populateCompetitors(xmlDoc);
	}
	
	private void populateCompetitors(Document doc) throws ParseException {
		NodeList competitorList = doc.getElementsByTagName("competitor");
		Log.v("user-gen","got here");

		for (int i=0; i < competitorList.getLength(); i++) {
			Node competitorNode = competitorList.item(i);
			Element competitorElmnt = (Element) competitorNode;
						
			String idStr = competitorElmnt.getAttribute("id");
			int id = Integer.parseInt(idStr);
		
			FitCollegeCompetitor mCompetitor;
			
			if (id != FitCollegeCompetitor.advertSectionId) {
				NodeList imgList = competitorElmnt.getElementsByTagName("img");
				String imgStr = FeedUtilities.getTagText(imgList);
				
				NodeList namesList = competitorElmnt.getElementsByTagName("names");
				String namesStr = FeedUtilities.getTagText(namesList);
				
				NodeList collegeList = competitorElmnt.getElementsByTagName("college");
				String collegeStr = FeedUtilities.getTagText(collegeList);
				
				NodeList currentScoreList = competitorElmnt.getElementsByTagName("currentScore");
				String currentScoreStr = FeedUtilities.getTagText(currentScoreList);
                if (currentScoreStr.equals("")) currentScoreStr="0";
				
				NodeList voteLinkList = competitorElmnt.getElementsByTagName("voteLink");
				String voteLinkStr = FeedUtilities.getTagText(voteLinkList);

				mCompetitor = new FitCollegeCompetitor(ctx, namesStr, collegeStr, Integer.parseInt(currentScoreStr), id, voteLinkStr, imgStr);
			} else {
				NodeList adLinkList = competitorElmnt.getElementsByTagName("adLink");
				String adLinkStr = FeedUtilities.getTagText(adLinkList);

				NodeList adImgList = competitorElmnt.getElementsByTagName("adImg");
				String adImgStr = FeedUtilities.getTagText(adImgList);

				mCompetitor = new FitCollegeCompetitor(ctx, adLinkStr, adImgStr, id);
			}
			
			competitors.add(mCompetitor);
		}
	}
	
	private static void downloadAndWrite(File saveFile, String UrlStr, Editor editor) throws Exception {
		
		FeedUtilities.downloadAndWrite(saveFile, UrlStr);
		
		Date today = new Date();
		Log.v("user-gen", today.toString());
		editor.putString(dateFitCollegeUpdatedPrefs, today.toString());
		editor.commit();
		
		Log.v("user-gen", "fit college downloaded and saved");
	}
	
	public long getTimeSinceUpdate() throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat( "EEE MMM dd HH:mm:ss zzz yyyy");
		Date now = new Date();
		
		String dateStr = prefs.getString(FitCollegeCompetition.dateFitCollegeUpdatedPrefs, "");
		if (dateStr.equals("")) {
			dateStr = now.toString();
			prefs.edit().putString(RSSFeed.dateNewsUpdatedPrefs, dateStr).commit();
		}
		Date dateUpdated = df.parse(dateStr);

		return (now.getTime() - dateUpdated.getTime()) / (1000*60);	
	}
	
	public void voteFor(int competitorIndex) throws Exception {
		competitors.get(competitorIndex).vote();
		
		Editor edit = prefs.edit();
		edit.putBoolean(votedKey + competitors.get(1).names, true);
		edit.putInt(votedKey, competitorIndex);
		edit.commit();
	}
	
	public boolean getVotedCompetition() {
		return prefs.getBoolean(votedKey + competitors.get(1).names, false);
	}
	
	public int getVotedId() {
		return prefs.getInt(votedKey, 1);
	}
	
}
