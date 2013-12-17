package com.cherwell.utilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.util.Log;

public class FeedUtilities {

	public static Document readSaveFile(File saveFile) throws Exception {
		BufferedInputStream savedStream = new BufferedInputStream(new FileInputStream(saveFile));						//parse file from SD card
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource(savedStream);
		Document doc = db.parse(is);
	
		savedStream.close();
		
		Log.v("user-gen", "file read from SD card");
		
		return doc;
	}
	
	public static void downloadAndWrite(File saveFile, String UrlStr) throws Exception {
		
		URL feedUrl = new URL(UrlStr);
		BufferedInputStream urlStream = new BufferedInputStream(feedUrl.openConnection().getInputStream());	

		FileOutputStream fileOs = new FileOutputStream(saveFile);
		byte[] buffer = new byte[8192];
		int len1 = 0;
				
		while ( (len1 = urlStream.read(buffer)) > 0 ) {
		        fileOs.write(buffer,0, len1);
		}
		fileOs.close();
		
		Log.v("user-gen", "full article downloaded and saved");
	}
	
	public static String getTagText(NodeList list) {
		if (list.getLength() > 0 && ((Element) list.item(0)).hasChildNodes()) {
			return ((Element) list.item(0)).getFirstChild().getTextContent();
		} else {
			return "";
		}
	}

}
