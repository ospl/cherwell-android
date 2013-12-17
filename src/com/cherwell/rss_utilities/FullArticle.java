package com.cherwell.rss_utilities;

import java.io.File;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.Context;

import com.cherwell.utilities.FeedUtilities;
import com.cherwell.utilities.GeneralUtils;

public class FullArticle {
	
	public Article mainArticle;
	public Article olderArticle;
	public Article newerArticle;
	
	private Context ctx;
	private String url;
	
	public FullArticle(Article article, Context ctx) throws Exception {
		this.ctx = ctx;
		this.mainArticle = article;
		this.url = article.bodyLink;
	}
	
	public void readFeed(boolean forced) throws Exception {

		boolean sdCardPresent = GeneralUtils.checkForSd();																								// have to check for SD card in each method in case removed in between
		Document xmlDoc;
		
		if (sdCardPresent) {	
			File saveFile = getSaveFile(mainArticle.id, mainArticle.parentSectionId, ctx);
			if (!saveFile.exists() || saveFile.length() == 0 || forced) {																																					// downloaded file from internet - savefile is empty
				downloadAndWrite(saveFile, url);
			}
			xmlDoc = FeedUtilities.readSaveFile(saveFile);

		} else {																																														// no SD card - download required
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			xmlDoc = db.parse(url);								
		}
		
		xmlDoc.getDocumentElement().normalize();
		populateArticles(xmlDoc);
	}
	
	public static File getSaveFile(int articleId, int parentSecId, Context context) throws Exception {
		File saveFile = new File(context.getExternalFilesDir(null), articleId + "parentSection" + parentSecId + ".xml");

		return saveFile;
	}
	
	public static void downloadAndWrite(File saveFile, String UrlStr) throws Exception {
		FeedUtilities.downloadAndWrite(saveFile, UrlStr);
	}
	
	private void populateArticles(Document doc) throws Exception {
		NodeList mainArticleList = doc.getElementsByTagName("article");
		if (mainArticleList.getLength() > 0) {
			Element mainArticleElement = (Element) mainArticleList.item(0);
			fillInArticle(mainArticle, mainArticleElement);
		}

		NodeList olderArticleList = doc.getElementsByTagName("olderArticle");
		if (olderArticleList.getLength() > 0) {
			Element olderArticleElement = (Element) olderArticleList.item(0);
			NodeList idList = olderArticleElement.getElementsByTagName("articleId");
			String idStr = FeedUtilities.getTagText(idList);
			if (idStr.trim().equals("")) idStr = "-1";
			int olderArticleId = Integer.parseInt(idStr);
			if (olderArticleId != -1) {
				olderArticle = new Article(olderArticleId, mainArticle.parentSectionId);
				fillInArticle(olderArticle, olderArticleElement);
			}
		}		
	}
	
	private Article fillInArticle(Article art, Element elmnt) throws Exception{
		if (!art.metaDataIsComplete()) {																			// fill in missing info description etc
			NodeList titleList = elmnt.getElementsByTagName("title");
			art.setTitle(FeedUtilities.getTagText(titleList));
			
			NodeList descriptionList = elmnt.getElementsByTagName("description");
			art.setDescription(FeedUtilities.getTagText(descriptionList));
						
			NodeList pubDateList = elmnt.getElementsByTagName("pubDate");				
		    String dateStr = FeedUtilities.getTagText(pubDateList);
		    art.setPubDate(dateStr);
		    if (!dateStr.equals("")) {
			    SimpleDateFormat inForm = new SimpleDateFormat(RSSFeed.longDateFormat);
				String originalDate = ((Element) pubDateList.item(0)).getFirstChild().getTextContent();
				SimpleDateFormat outForm = new SimpleDateFormat(RSSFeed.shortDateFormat);
				dateStr = outForm.format(inForm.parse(originalDate)).toString();
				art.setPubDate(dateStr);
		    }
		    
			NodeList authorList = elmnt.getElementsByTagName("author");
			String authors[] = {""};
			int authorIds[] = {0};
			art.setAuthors(authors);
			art.setAuthorIds(authorIds);
			if (authorList.getLength() > 0) { 
				authors = new String[authorList.getLength()];
				authorIds = new int[authorList.getLength()];
				for (int x = 0; x < authorList.getLength(); x++) {
					authors[x] = ((Element) authorList.item(x)).getFirstChild().getTextContent();
					authorIds[x] = Integer.parseInt( ((Element) authorList.item(x)).getAttribute("id"));
				}
				art.setAuthors(authors);
				art.setAuthorIds(authorIds);
			}
			
			NodeList imgList = elmnt.getElementsByTagName("img");
			art.setImageLink(FeedUtilities.getTagText(imgList));				
		}

		NodeList bodyList = elmnt.getElementsByTagName("body");
		if (bodyList != null)
			art.setBodyString(getFormattedHtml(FeedUtilities.getTagText(bodyList)));
		
		return art;
	}
	
	private String getFormattedHtml(String bodyHtml) {

		String css = "<head>\n" +
				"<meta http-equiv=\"Content-Type\" content=\"text/html;\n" +
				"charset=utf-8\">\n" +
				"<meta name=\"viewport\" content=\"width=device-width\" /> \n" +
				"</head>\n" +
				"<style type=\"text/css\"> \n" +
				"@font-face { \n" +
				"font-family: roboto; \n" +
				"src: url(\"file:///android_asset/fonts/" + GeneralUtils.getStoryFont(ctx) + "\")}\n" +
				"body { \n" +
				"font-family: roboto;\n" +
				"line-height:125%;\n" +
				"}\n" +
				"</style> \n";
		
		String finalStr = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"> \n" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"> \n" + css
				+"<body style=\"margin: 0; padding: 0\"> \n"
				+ bodyHtml + "\n" +
				" </body> \n</html>";

		return finalStr;
	}
	
	
}
