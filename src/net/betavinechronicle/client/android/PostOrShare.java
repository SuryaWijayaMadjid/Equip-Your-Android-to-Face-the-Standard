package net.betavinechronicle.client.android;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;

public class PostOrShare extends TabActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.post_or_share_tabhost);
		
		//Resources resources = this.getResources(); // Resources object to get Drawables
		TabHost tabHost = this.getTabHost(); // The activity TabHost
		TabHost.TabSpec tabSpec; // Reusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab
		
		// Add Post Status tab to the tab host
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.PostStatus.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_status").setIndicator(
				getLayoutInflater().inflate(R.layout.tab_indicator, null)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		// Add Post Blog tab to the tab host
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.PostBlog.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_blog").setIndicator(
				getLayoutInflater().inflate(R.layout.tab_indicator, null)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		// Add Share Link tab to the tab host
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.ShareLink.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_link").setIndicator(
				getLayoutInflater().inflate(R.layout.tab_indicator, null)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		// Add Share Picture tab to the tab host
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.SharePicture.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_picture").setIndicator(
				getLayoutInflater().inflate(R.layout.tab_indicator, null)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		// Add Share Audio tab to the tab host
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.ShareAudio.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_audio").setIndicator(
				getLayoutInflater().inflate(R.layout.tab_indicator, null)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		// Add Share Video tab to the tab host
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.ShareVideo.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_video").setIndicator(
				getLayoutInflater().inflate(R.layout.tab_indicator, null)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		tabHost.setCurrentTab(0);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
    	MenuInflater menuInflater = this.getMenuInflater();
    	menuInflater.inflate(R.menu.post_or_share_options_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId()) {
    	case R.id.postOrShare_options_userStream: 
			this.setResult(UserStream.RESULT_OK);
    		this.finish();
    		return true;
    	
    	case R.id.postOrShare_options_profile: 
    		this.setResult(Porter.RESULTCODE_SWITCH_ACTIVITY_TO_PROFILE);
    		this.finish();
    		return true;
    	
    	case R.id.postOrShare_options_exit: 
    		this.setResult(Porter.RESULTCODE_SUBACTIVITY_CHAINCLOSE);
    		this.finish();
    		return true;
    	}
    	
    	return false;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
	
}
