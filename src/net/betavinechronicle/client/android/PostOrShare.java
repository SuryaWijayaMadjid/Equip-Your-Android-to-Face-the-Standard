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
		
		TabHost tabHost = this.getTabHost();
		TabHost.TabSpec tabSpec = null; 
		Intent intent = null;
		
		// ADD PostStatus TO THE TAB-HOST
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.PostStatus.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_status").setIndicator("Status").setContent(intent);
		tabHost.addTab(tabSpec);
		
		// ADD PostBlog TO THE TAB-HOST
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.PostBlog.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_status").setIndicator("Blog").setContent(intent);
		tabHost.addTab(tabSpec);
		
		// ADD ShareLink TO THE TAB-HOST
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.ShareLink.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_status").setIndicator("Link").setContent(intent);
		tabHost.addTab(tabSpec);
		
		// ADD SharePicture TO THE TAB-HOST
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.SharePicture.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_status").setIndicator("Pict.").setContent(intent);
		tabHost.addTab(tabSpec);
		
		// ADD ShareAudio TO THE TAB-HOST
		intent = new Intent().setClass(getApplicationContext(), 
				net.betavinechronicle.client.android.ShareAudio.class);		
		tabSpec = tabHost.newTabSpec("postOrShare_status").setIndicator("Audio").setContent(intent);
		tabHost.addTab(tabSpec);
		
		// SET THE PostStatus AS THE DEFAULT TAB
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
    	
    	case R.id.postOrShare_options_settings: 
    		this.setResult(Porter.RESULTCODE_SWITCH_ACTIVITY_TO_SETTINGS);
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// TODO Auto-generated method stub
    	super.onActivityResult(requestCode, resultCode, data);
    }
	
}
