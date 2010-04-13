package net.betavinechronicle.client.android;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class UserStream extends ListActivity {
	static final int RESULTCODE_SUBACTIVITY_CHAINCLOSE = 99;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_PROFILE = 98;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE = 97;
	
	private List<PostItem> mPostItems = null;
	private PostItemAdapter mPostItemAdapter;
	private ProgressDialog mProgressDialog = null;
	private Runnable viewPostItems;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPostItems = new ArrayList<PostItem>();
        mPostItemAdapter = new PostItemAdapter(this, R.layout.post_item, mPostItems);
        
        //add a circling progress loading display feature to the top 3right of our application
        this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.main);
        this.setProgressBarIndeterminateVisibility(false); 
        
        this.registerForContextMenu(getListView());   
        
        this.setListAdapter(mPostItemAdapter);
        viewPostItems = new Runnable() {
        	
        	@Override
        	public void run() {
        		getPostItems();
        	}
        };
        Log.d("onCreate","Thread started");
/*        File makeDir = new File("/sdcard/Music");
        try {
        	makeDir.mkdir();
        }
        catch(SecurityException ex) {
        	Toast.makeText(getApplicationContext(), ex.getMessage(), 1).show();
        }
        catch(Exception ex) {
        	Toast.makeText(getApplicationContext(), ex.getMessage(), 1).show();
        }*/
        
        new Thread(viewPostItems).start();
        mProgressDialog = ProgressDialog.show(this, "Retrieving Stream", "Please wait...");
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
    	MenuInflater menuInflater = this.getMenuInflater();
    	menuInflater.inflate(R.menu.userstream_options_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	Intent intent = null;
    	switch (item.getItemId()) {
    	case R.id.userStream_options_postOrShare: 
    		intent = new Intent(this, net.betavinechronicle.client.android.PostOrShare.class);
			this.startActivityForResult(intent,1);
    		return true;
    		
    	case R.id.userStream_options_setFilter: return true;
    	case R.id.userStream_options_profile: 
    		intent = new Intent(this, net.betavinechronicle.client.android.EditProfile.class);
			this.startActivityForResult(intent,1);
    		return true;
    	
    	case R.id.userStream_options_exit: 
    		this.finish(); 
    		return true;
    	}
    	
    	return false;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenuInfo menuInfo) {
    	MenuInflater menuInflater = this.getMenuInflater();
    	
    	menuInflater.inflate(R.menu.userstream_context_menu, menu);
    	super.onCreateContextMenu(menu, v, menuInfo);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
    	
    	switch (item.getItemId()) {
    	case R.id.userStream_context_viewDetail: break;
    	case R.id.userStream_context_addComment: break;
    	case R.id.userStream_context_editPost: break;
    	case R.id.userStream_context_deletePost: break;
    	}
    	return super.onContextItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	Intent intent = null;
    	
    	switch (resultCode) {
    	case RESULTCODE_SUBACTIVITY_CHAINCLOSE:
    		finish();
    		break;
    	
    	case RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE:
    		intent = new Intent(this, net.betavinechronicle.client.android.PostOrShare.class);
			this.startActivityForResult(intent,1);
			break;
			
    	case RESULTCODE_SWITCH_ACTIVITY_TO_PROFILE:
    		intent = new Intent(this, net.betavinechronicle.client.android.EditProfile.class);
			this.startActivityForResult(intent,1);
			break;
    	}
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
    
    //do request to the back end to get the feed
    private void getPostItems() {
    	
    	try {
	    	mPostItems = new ArrayList<PostItem>();
	    	PostItem postItem1 = new PostItem("The Number One", "This announces him as the number one", 2, 2);
	    	PostItem postItem2 = new PostItem("The Number Two", "Being the number two is not so bad at all", 1, 2);
	    	
	    	mPostItems.add(postItem1);
	    	mPostItems.add(postItem2);
	    	Thread.sleep(3000);
    	}
    	catch (InterruptedException ex) {
    		Log.e("getPostItems method", "Thread interrupted");
    	}

    	Log.i("getPostItems method", ""+ mPostItems.size());
    	
    	this.runOnUiThread(updateUi);
    }
    
    //update the UI by synchronizing the TabActivity and its adapter
    private Runnable updateUi = new Runnable() {
    	
    	@Override
    	public void run() {
    		int postItemsSize = (mPostItems != null)? mPostItems.size() : 0;
    	
    		if (mPostItems != null && postItemsSize > 0) {
    			mPostItemAdapter.notifyDataSetChanged();
    			for (int i = 0; i < postItemsSize; i++)
    				mPostItemAdapter.add(mPostItems.get(i));
    		}
    		
    		mProgressDialog.dismiss();
    		mPostItemAdapter.notifyDataSetChanged();
    		Log.d("returnResult runnable", "Thread finished");
    	}
    };
    
    //we declare a custom adapter class for our custom list item layout (post_item.xml)
    private class PostItemAdapter extends ArrayAdapter<PostItem> {
    	private List<PostItem> mPostItems;
    	
		public PostItemAdapter(Context context, int textViewResourceId,	List<PostItem> postItem) {
			super(context, textViewResourceId, postItem);
			
			mPostItems = postItem;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			
			if (view == null) {
				LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(R.layout.post_item, null);
			}
			
			PostItem postItem = mPostItems.get(position);
			if (postItem != null) {
				TextView titleTextView = (TextView) view.findViewById(R.id.userStream_listItem_title);
				TextView contentTextView = (TextView) view.findViewById(R.id.userStream_listItem_content);
				ImageView sourceImageView = (ImageView) view.findViewById(R.id.userStream_listItem_sourceIcon);
				ImageView typeImageView = (ImageView) view.findViewById(R.id.userStream_listItem_typeIcon);
				
				if (titleTextView != null) {
					titleTextView.setText("Title: " + postItem.getTitle());
				}
				if (contentTextView != null) {
					contentTextView.setText("Content: " + postItem.getContent());
				}
				if (sourceImageView != null) {
					switch (postItem.getSource()) {
					case PostItem.SOURCE_STORYTLR: sourceImageView.setImageResource(R.drawable.storytlr); break;
					case PostItem.SOURCE_TWITTER: sourceImageView.setImageResource(R.drawable.twitter); break;
					case PostItem.SOURCE_PICASA: sourceImageView.setImageResource(R.drawable.picasa); break;
					}					
				}
				if (typeImageView != null) {
					switch (postItem.getSource()) {
					case PostItem.TYPE_STATUS: break;
					case PostItem.TYPE_BLOG: break;
					case PostItem.TYPE_LINK: break;
					case PostItem.TYPE_PICTURE: break;
					case PostItem.TYPE_AUDIO: break;
					case PostItem.TYPE_VIDEO: break;
					}
				}
			}
			
			return view;
		}
    }
}