package com.cherwell.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.cherwell.android.R;
import com.cherwell.fragments.FullStoryFragment;
import com.cherwell.rss_utilities.Article;
import com.cherwell.rss_utilities.Section;

public class FullStoryBaseActivity extends SherlockFragmentActivity {

    private PagerAdapter mPagerAdapter;
    private ViewPager mPager;
    private Section sec;
    private int articleIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager_layout);

        sec = (Section) getIntent().getSerializableExtra("section");
        articleIndex = getIntent().getExtras().getInt("index", 0);

        mPager = (ViewPager) findViewById(R.id.viewpager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(articleIndex);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);                                                                                                            // up button back to home
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean dealt = ((FullStoryFragment) mPagerAdapter.instantiateItem(mPager, mPager.getCurrentItem())).onKeyDown(keyCode, event);

        if (!dealt) return super.onKeyDown(keyCode, event);
        return true;
    }

    public void setNextPageArticle(Article article, int position) {
        if (position < sec.sectionArticles.size()) {
            return;
        }
        sec.sectionArticles.add(position, article);
        mPagerAdapter.notifyDataSetChanged();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new FullStoryFragment(sec.sectionArticles.get(position), position);
        }

        @Override
        public int getCount() {
            return sec.sectionArticles.size();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:                                          // Up to home page - icon clicked in action bar
                Intent upIntent = new Intent(this, BaseActivity.class);
                upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(upIntent);
                finish();
                break;
        }
        return false;
    }
}
