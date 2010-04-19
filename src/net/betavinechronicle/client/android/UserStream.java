package net.betavinechronicle.client.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomFeed;
import org.onesocialweb.xml.xpp.imp.DefaultXppActivityReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class UserStream extends ListActivity {
	static final int RESULTCODE_SUBACTIVITY_CHAINCLOSE = 99;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_PROFILE = 98;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE = 97;
	
	private List<PostItem> mPostItems;
	private PostItemAdapter mPostItemAdapter;
	private ProgressDialog mProgressDialog = null;
	private String mProgDialogTitle = "";
	
	//private Runnable viewPostItems;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {  
    	super.onCreate(savedInstanceState);
    	//Debug.startMethodTracing("tracer");
        mPostItems = new ArrayList<PostItem>();
        mPostItemAdapter = new PostItemAdapter(this, R.layout.post_item, mPostItems);
        
        //add a circling progress loading display feature to the top 3right of our application
        this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.main);
        this.setProgressBarIndeterminateVisibility(false); 
        
        this.registerForContextMenu(getListView());   
        
        this.setListAdapter(mPostItemAdapter);

        this.requestFeeds(this.getString(R.string.service_endpoint_uri));
        
        mProgressDialog = ProgressDialog.show(this, "Retrieving Stream", "Please wait...");
        
    }
    
    @Override
    protected void onDestroy() {
    	//Debug.stopMethodTracing();
    	super.onDestroy();
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
    	case R.id.userStream_context_addComment: 
    		PostItem newPostItem = new PostItem("New Title", 
    				"This is the content... newly add", 
    				PostItem.SOURCE_PICASA, 
    				PostItem.TYPE_STATUS);
    		mPostItems.add(0, newPostItem);
    		mPostItemAdapter.insert(newPostItem, 0);
    		mPostItemAdapter.notifyDataSetChanged();
    		
    		break;
    		
    	case R.id.userStream_context_editPost: break;
    	case R.id.userStream_context_deletePost: 
    		mPostItemAdapter.remove(mPostItems.get((int) menuInfo.position));
    		mPostItems.remove(menuInfo.position);
    		mPostItemAdapter.notifyDataSetChanged();
    		break;
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
    
    //requesting feeds to the end-point (refreshing post-items list)
    private void requestFeeds(String endpointUri) {
    	new HttpTasks(endpointUri, HttpTasks.HTTP_GET) {
			
			@Override
			public void run() {
				long runtime = System.currentTimeMillis();
				super.run();
				runtime = System.currentTimeMillis() - runtime;
				Log.d("INSIDE requestFeeds()", "HttpTasks execution time: " + runtime + "ms");
				
				if (this.hasHttpResponse()) {
					AtomFeed feed = null;
			        try {
			        	mProgDialogTitle = "Parsing Stream";
			        	runOnUiThread(changeProgDialogTitle);
			        	Log.d("inside try before making xpp", "PHASE-2");
			        	runtime = System.currentTimeMillis();
						XmlPullParserFactory xppFactory = XmlPullParserFactory.newInstance();
			        	xppFactory.setNamespaceAware(true);
			        	XmlPullParser xpp = xppFactory.newPullParser();
			        	xpp.setInput(this.getHttpResponse().getEntity().getContent(), "UTF-8");
			        	DefaultXppActivityReader xppActivityReader = new DefaultXppActivityReader();
			        	xpp.next();
			        	runtime = System.currentTimeMillis();
			        	feed = xppActivityReader.parse(xpp);
			        	runtime = System.currentTimeMillis() - runtime;
			        	Log.d("INSIDE requestFeeds()", "Feed-parsing execution time: " + runtime + "ms");
			        	Log.d("the end of try", "PHASE-3");
			        }
			        catch(XmlPullParserException ex) {
			        	Log.e("INSIDE requestFeeds()", ex.getMessage());
			        }
			        catch(IOException ex) {
			        	Log.e("INSIDE requestFeeds()", ex.getMessage());
			        }
			        catch(Exception ex) {
			        	Log.e("INSIDE requestFeeds()", ex.getMessage());
			        }
			        
			        Log.d("before feed != null", "PHASE-4");
			        if (feed != null) {
			        	mProgDialogTitle = "Displaying Stream";
			        	runOnUiThread(changeProgDialogTitle);
			        	runtime = System.currentTimeMillis();
			        	mPostItems = new ArrayList<PostItem>();
			        	List<AtomEntry> atomEntries = feed.getEntries();
			        	int maxEntries = Integer.parseInt(getString(R.string.max_entries));
			        	AtomEntry atomEntry = null;
			        	for (int i=0; i<maxEntries; i++) {
			        		atomEntry = atomEntries.get(i);
			        		String entryTitle = atomEntry.getTitle();
		        			int maxChar = Integer.parseInt(getString(R.string.max_char_title));
		        			if (entryTitle.length() > maxChar)
		        				entryTitle = entryTitle.substring(0, maxChar).trim() + "....";
		        			
		        			String entryContent = "";
		        			if (atomEntry.hasContent()) {
		        				entryContent = atomEntry.getContent().getValue();
		        				maxChar = Integer.parseInt(getString(R.string.max_char_content));
			        			if (entryContent.length() > maxChar)
			        				entryContent = entryContent.substring(0, maxChar).trim() + "....";
		        			}
		        			
		        			mPostItems.add(new PostItem(
		        					entryTitle,
		        					entryContent,
		        					PostItem.SOURCE_PICASA,
		        					PostItem.TYPE_STATUS));
		        			
			        		if (atomEntry instanceof ActivityEntry) {
			        			//ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			        			// TODO: things that need to be altered if it is an activity entry
			        		}
			        	}
			        	runtime = System.currentTimeMillis() - runtime;
			        	Log.d("INSIDE requestFeeds()", "Adding post items execution time: " + runtime + "ms");
			        }
			        else {
			        	Log.e("INSIDE requestFeeds()", "Feed is null");
			        }
		    		
		    		runOnUiThread(updateUi);
				}
				else
					Log.e("INSIDE requestFeeds()", this.getExceptionMessage());
			};
			
		}.start();
    }
    
    //change the progress dialog's title
    private Runnable changeProgDialogTitle = new Runnable() {
    	@Override
    	public void run() {
    		mProgressDialog.setTitle(mProgDialogTitle);
    	};
    };
    
    //update the UI by synchronizing the TabActivity and its adapter
    private Runnable updateUi = new Runnable() {
    	
    	@Override
    	public void run() {
    		
    		if (mPostItems != null && mPostItems.size() > 0) {
    			long runtime = System.currentTimeMillis();
	    		mPostItemAdapter.clear();
	    		int postItemsSize = mPostItems.size();
	    	
	    		mPostItemAdapter.notifyDataSetChanged();
    			for (int i = 0; i < postItemsSize; i++)
    				mPostItemAdapter.add(mPostItems.get(i));
	    		
	    		mProgressDialog.dismiss();
	    		mPostItemAdapter.notifyDataSetChanged();
	    		runtime = System.currentTimeMillis() - runtime;
	    		Log.d("INSIDE updateUi runnable", "Updating adapter execution time: " + runtime + "ms");
	    		Log.d("returnResult runnable", "Thread finished");
    		}
    		else {
    			Toast.makeText(getApplicationContext(), 
    					"Failed to retrieve post stream..", 
    					Toast.LENGTH_SHORT).show();
    			mProgressDialog.dismiss();
    		}
    	};
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
					titleTextView.setText(postItem.getTitle());
				}
				if (contentTextView != null) {
					contentTextView.setText(postItem.getContent());
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