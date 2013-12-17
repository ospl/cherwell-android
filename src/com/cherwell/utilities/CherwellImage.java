package com.cherwell.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class CherwellImage {

	public String imageLink;
	
	private Context ctx;
	
	public CherwellImage(String imageLink, Context ctx) {		
		this.imageLink = imageLink;
		this.ctx = ctx;
	}
	
	public Bitmap getImage(boolean thumbnail, String fileName, boolean forceOnline) throws Exception {		
		String root = "";
		
		if(!imageLink.contains("http://")) {
			if (thumbnail) root = "http://www.cherwell.org/library/image/thumb/";
			else root = "http://www.cherwell.org/library/image/";
		}
		String fullLink = root  + imageLink;
		
		boolean sdCardPresent = GeneralUtils.checkForSd();
		if (!sdCardPresent) {
			return BitmapFactory.decodeStream((InputStream)new URL(fullLink).getContent());
		}
		
		File imgFile = new File(ctx.getExternalFilesDir(null), fileName);
		
		if (imgFile.exists() && imgFile.length() > 0) {
			return BitmapFactory.decodeFile(imgFile.getPath());
		} else {
			Bitmap bmp = BitmapFactory.decodeStream((InputStream)new URL(fullLink).getContent());
		    FileOutputStream fOut = new FileOutputStream(imgFile);
		    bmp.compress(Bitmap.CompressFormat.PNG, 85, fOut);
		    fOut.flush();
		    fOut.close();
			return bmp;
		}
	}
}
