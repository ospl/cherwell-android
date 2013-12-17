package com.cherwell.rss_utilities;

import java.io.File;
import java.util.List;

import com.cherwell.fit_college_utilities.FitCollegeCompetition;
import com.cherwell.fragments.NewsFragment;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class UpdateArticlesRunnable  implements Runnable		{
	
	public RSSFeed rss;
	public Editor edit;
	public Context ctx;
	
	public UpdateArticlesRunnable(RSSFeed rss, Editor edit, Context ctx) {
		this.rss = rss;
		this.edit = edit;
		this.ctx = ctx;
	}
	
    @Override
    public void run() {
			try {
				List<Article> allArticles = rss.getAllArticles();
								
				for (int i = 0; i < allArticles.size(); i++) {
					Article mArticle = allArticles.get(i);

					File saveFile = FullArticle.getSaveFile(mArticle.id, mArticle.parentSectionId, ctx);
					
					if (!saveFile.exists() || saveFile.length() == 0) {
						FullArticle.downloadAndWrite(saveFile, mArticle.bodyLink); 			
					}
				}
								
				File directory = ctx.getExternalFilesDir(null);
				File[] files = directory.listFiles();
				for (int x = 0; x < files.length; x++) {
					boolean fileNecessary = false;
					File mFile = files[x];
					String fileName = mFile.getName();
					
					for (int y = 0; y < allArticles.size(); y++) {
						if (fileName.contains(".xml") && fileName.contains(String.valueOf(allArticles.get(y).id))) {
							fileNecessary = true;
						}
						if (fileName.contains(allArticles.get(y).imageLink) && fileName.contains("true")) {
							fileNecessary = true;
						}
						if (fileName.contains(RSSFeed.newsSave) || fileName.contains(FitCollegeCompetition.saveFileName) || fileName.contains("fit-college")) {
							fileNecessary = true;
						}
						
					}
					
					if (!fileNecessary) {
						mFile.delete();
						Log.v("user-gen", mFile.getName());
					}
				}
				
				edit.putInt(NewsFragment.editorsIdKey, rss.sections[0].sectionArticles.get(0).id);
				edit.commit();
			
			} catch (Exception e){
				Log.v("user-gen",  "update articles " + e.toString());
			}
		
    }
}