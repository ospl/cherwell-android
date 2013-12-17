package com.cherwell.utilities;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;

public class GeneralUtils {
	
	public static boolean checkForSd() {

		String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state);
	}
	
	public static void executeTask(AsyncTask<String, Void, String> task) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	    } else {
	    	task.execute();
	    }
	}

	public static Typeface getStandardFont(Context ctx) {
		if (shouldUseLightFont(ctx)) {
	    	return Typeface.createFromAsset(ctx.getAssets(),"fonts/roboto_light.ttf"); 
	    } else {
	    	return Typeface.createFromAsset(ctx.getAssets(),"fonts/roboto.ttf"); 
	    }
	}
	
	public static Typeface getHeaderFont(Context ctx) {
    	return Typeface.createFromAsset(ctx.getAssets(),"fonts/roboto.ttf"); 
	}

    public static String getAdHTML(String imageLink, String adLink, boolean shouldHaveMaxHeight) {
        String maxHeight = "";
        if (shouldHaveMaxHeight)
            maxHeight = "max-height:100%;";

        String data="<html><body bgcolor=\"#8C8C8C\" style=\"margin: 0; padding: 0\" ><a href=\"" + adLink + "\"> <img style=\"display: block; margin: auto; max-width:100%;" + maxHeight + "height: auto\" src=\""+imageLink+"\" /></a></body></html>";
        return data;
    }
	
	public static String getStoryFont(Context ctx) {
		if (shouldUseLightFont(ctx)) {
			return "roboto_light.ttf"; 
	    } else {
	    	return "roboto.ttf"; 
	    }
	}
	
	private static boolean shouldUseLightFont(Context ctx) {
		float dpi =  ctx.getResources().getDisplayMetrics().density;

		if ((ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)
			return true;
		else
            return dpi >= 2;
	}
}
