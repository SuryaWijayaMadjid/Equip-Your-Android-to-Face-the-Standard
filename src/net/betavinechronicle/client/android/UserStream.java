package net.betavinechronicle.client.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.entity.StringEntity;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomFeed;
import org.onesocialweb.model.atom.AtomLink;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class UserStream extends ListActivity {
	
	static final int LIST_ITEM_PREVIEW_IMAGE_ID = 9876;
	static final int LIST_ITEM_CONTENT_TEXT_ID = 9877;
	
	private List<PostItem> mPostItems;
	private PostItemAdapter mPostItemAdapter;
	private ProgressDialog mProgressDialog = null;
	private Porter mPorter;
	
	private String mProgDialogTitle = "";
	
	private int mMaxTitleLength;
	private int mMaxContentLength;
	private int mMaxEntriesCount;
	
	//private Runnable viewPostItems;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {  
    	super.onCreate(savedInstanceState);
    	//Debug.startMethodTracing("tracer");
    	
        mPorter = (Porter) this.getApplication();
    	mPostItems = new ArrayList<PostItem>();
        mPostItemAdapter = new PostItemAdapter(this, R.layout.post_item, mPostItems);
        
        // load setup values
        mMaxEntriesCount = Integer.parseInt(getString(R.string.max_entries));
        mMaxTitleLength = Integer.parseInt(getString(R.string.max_char_title));
        mMaxContentLength = Integer.parseInt(getString(R.string.max_char_content));
        
        // add a circling progress loading display feature to the top 3right of our application
        this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.user_stream);
        
        this.setProgressBarIndeterminateVisibility(false); 
        
        this.registerForContextMenu(getListView());   
        
        this.setListAdapter(mPostItemAdapter);
        
        ListView postsList = this.getListView();
        postsList.setTextFilterEnabled(true);
        postsList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(getApplicationContext(), 
						net.betavinechronicle.client.android.EntryDetail.class);
				intent.putExtra(Porter.EXTRA_KEY_REQUESTCODE, Porter.REQUESTCODE_VIEW_ENTRY);
				intent.putExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, position);
				startActivityForResult(intent, Porter.REQUESTCODE_VIEW_ENTRY);
			}
		});

        this.promptForUsername();
        //this.requestFeeds(this.getString(R.string.service_endpoint_uri));
    }
    
    @Override
    protected void onResume() {
    	// TODO: if a set of interval has been reached, refresh the feed
    	super.onResume();
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
    		intent.putExtra(Porter.EXTRA_KEY_REQUESTCODE, Porter.REQUESTCODE_POST_OR_SHARE);
			this.startActivityForResult(intent, Porter.REQUESTCODE_POST_OR_SHARE);
    		return true;
    		
    	case R.id.userStream_options_setFilter: return true;
    	case R.id.userStream_options_profile: 
    		intent = new Intent(this, net.betavinechronicle.client.android.EditProfile.class);
    		intent.putExtra(Porter.EXTRA_KEY_REQUESTCODE, Porter.REQUESTCODE_EDIT_PROFILE);
    		this.startActivityForResult(intent, Porter.REQUESTCODE_EDIT_PROFILE);
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
    				null,
    				PostItem.SOURCE_PICASA, 
    				PostItem.TYPE_STATUS,
    				"fasefwe",
    				0);
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
    	
    	if (requestCode == Porter.REQUESTCODE_PROMPT_USERNAME && resultCode == RESULT_OK) {
    		String username = data.getCharSequenceExtra("username").toString();
    		mPorter.savePreferenceString(Porter.PREFERENCES_KEY_USERNAME, username);
    		this.requestFeeds(this.getString(R.string.service_endpoint_uri) + "?username=" + username); 
            mProgressDialog = ProgressDialog.show(this, "Retrieving Stream of " + username, 
            		"Please wait...");
    		return;
    	}
    	
    	Intent intent = null;
    	String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, "ekoadipg");
    	int postItemIndex = -1;
    	if (data != null)
    		postItemIndex = data.getIntExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
		
    	switch (resultCode) {
    	case Porter.RESULTCODE_SUBACTIVITY_CHAINCLOSE:
    		finish();
    		break;
    	
    	case Porter.RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE:
    		intent = new Intent(this, net.betavinechronicle.client.android.PostOrShare.class);
			this.startActivityForResult(intent,1);
			break;
			
    	case Porter.RESULTCODE_SWITCH_ACTIVITY_TO_PROFILE:
    		intent = new Intent(this, net.betavinechronicle.client.android.EditProfile.class);
			this.startActivityForResult(intent,1);
			break;
    	
    	case Porter.RESULTCODE_EDITING_ENTRY:
    		if (postItemIndex < -1) break;
    		mProgressDialog = ProgressDialog.show(this, 
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_DIALOG_TITLE), 
    				"Please wait...");
    		this.editEntry(data.getCharSequenceExtra(Porter.EXTRA_KEY_TARGET_URI).toString(),
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY).toString(),
    				postItemIndex);
    		break;
    		
    	case Porter.RESULTCODE_DELETING_ENTRY:
    		if (postItemIndex < 0) break;
    		mProgressDialog = ProgressDialog.show(this, 
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_DIALOG_TITLE), 
    				"Please wait...");
    		this.deleteEntry(data.getCharSequenceExtra(Porter.EXTRA_KEY_TARGET_URI).toString(),
    				postItemIndex);
    		break;
    	
    	case Porter.RESULTCODE_POSTING_ENTRY:
    		mProgressDialog = ProgressDialog.show(this, 
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_DIALOG_TITLE), 
    				"Please wait...");
    		this.postEntry(this.getString(R.string.service_endpoint_uri) + "?username=" + username,
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY).toString());
    		break;
    	}
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
    
    private void promptForUsername() {
    	Intent intent = new Intent(getApplicationContext(), 
				net.betavinechronicle.client.android.EnterUsername.class);
    	startActivityForResult(intent, Porter.REQUESTCODE_PROMPT_USERNAME);
    }
    
    private void deleteEntry(String endpointUri, final int postItemIndex) {
    	HttpTasks httpDeleteRequest = new HttpTasks(endpointUri, HttpTasks.HTTP_DELETE) {
    		
    		@Override
    		public void run() {
    			super.run();
    			Log.d("INSIDE deleteEntry()", "Finished executing DELETE request");
    			if (this.hasHttpResponse()) {
    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
    				Log.d("INSIDE deleteEntry()", "Has HttpResponse, Status Code:" + statusCode);
    				if (statusCode != 200)
    					return;
    				mProgDialogTitle = "Updating Stream";
		        	runOnUiThread(mChangeProgDialogTitle);
    				PostItem postItem = mPostItems.get(postItemIndex);
	        		AtomEntry originalEntry = mPorter.getEntryById(postItem.getEntryId());
			        mPorter.getFeed().getEntries().remove(originalEntry);
			        mPostItems.remove(postItem);
			        runOnUiThread(new Runnable() {
			    		@Override
			    		public void run() {
			    			mPostItemAdapter.remove(mPostItemAdapter.getItem(postItemIndex));
			    		}		
			    	});
			        runOnUiThread(mRefreshAdapter);
    			}
    			else 
					Log.e("INSIDE deleteEntry() HttpResponse check", 
							(this.getExceptionMessage() != null)? 
									this.getExceptionMessage():"Http response is null");
    			mProgressDialog.dismiss();
    		}
    	};
    	
    	httpDeleteRequest.addHeaderToHttpDelete("User-Agent", "AMC_BV/0.1");
    	Log.d("INSIDE deleteEntry()", "Finished setting the entity and headers");
    	httpDeleteRequest.start();
    }
    
    private void editEntry(String endpointUri, String xmlActivityEntry, final int postItemIndex) {
    	HttpTasks httpPutRequest = new HttpTasks(endpointUri, HttpTasks.HTTP_PUT) {
    		
    		@Override
    		public void run() {
    			super.run();
    			Log.d("INSIDE editEntry()", "Finished executing PUT request");
    			if (this.hasHttpResponse()) {
    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
    				Log.d("INSIDE editEntry()", "Has HttpResponse, Status Code:" + statusCode);
    				if (statusCode != 200)
    					return;
    				AtomEntry atomEntry = null;
    				try {
						mProgDialogTitle = "Parsing Response";
			        	runOnUiThread(mChangeProgDialogTitle);
			        	XmlPullParserFactory xppFactory = XmlPullParserFactory.newInstance();
			        	xppFactory.setNamespaceAware(true);
			        	XmlPullParser xpp = xppFactory.newPullParser();
			        	xpp.setInput(this.getHttpResponse().getEntity().getContent(), "UTF-8");
			        	DefaultXppActivityReader xppActivityReader = new DefaultXppActivityReader();
			        	xpp.next();
			        	atomEntry = xppActivityReader.parseEntry(xpp);
			        }
			        catch(XmlPullParserException ex) {
			        	Log.e("INSIDE editEntry() XPP", ex.getMessage());
			        }
			        catch(IOException ex) {
			        	Log.e("INSIDE editEntry() XPP", ex.getMessage());
			        }
			        catch(Exception ex) {
			        	Log.e("INSIDE editEntry() XPP", ex.getMessage());
			        }
			        
			        if (atomEntry != null) {
			        	mProgDialogTitle = "Updating Stream";
			        	runOnUiThread(mChangeProgDialogTitle);
			        	if (postItemIndex == -1) {
			        		String titleToDisplay = "";
			        		int sourceToDisplay = PostItem.SOURCE_STORYTLR;
			        		int typeToDisplay = PostItem.NOT_SPECIFIED;
			        		mPorter.getFeed().addEntry(atomEntry);
			        		titleToDisplay = generateTitle(atomEntry);
			        		
			        		// make the content of the post-item to be displayed
			        		if (atomEntry instanceof ActivityEntry) { 
			        			// each object will represent a post-item
			        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			        			List<ActivityObject> objects = activityEntry.getObjects();
			        			int objectCount = 0;
			        			for (ActivityObject object : objects) {
			        				typeToDisplay = PostItem.getTypeByObjectType(object.getType());
			        				mPostItems.add(0, new PostItem(
					        				titleToDisplay,
					        				generateContent(object),
					        				mPorter.generateImagePreview(object),
					        				sourceToDisplay,
					        				typeToDisplay,
					        				activityEntry.getId(),
					        				objectCount));
			        				runOnUiThread(new Runnable() {
							    		@Override
							    		public void run() {
							    			mPostItemAdapter.insert(mPostItems.get(0), 0);
							    		}		
							    	});
			        				objectCount++;
			        			}
			        		}
			        		else { // The entry is a regular atom-entry        			
			        			mPostItems.add(0, new PostItem(
				        				titleToDisplay,
				        				generateContent(atomEntry),
				        				null,
				        				sourceToDisplay,
				        				typeToDisplay,
				        				atomEntry.getId(),
				        				-1));
			        			runOnUiThread(new Runnable() {
						    		@Override
						    		public void run() {
						    			mPostItemAdapter.insert(mPostItems.get(0), 0);
						    		}		
						    	});
			        		} 
			        		runOnUiThread(mRefreshAdapter);
			        		runOnUiThread(new Runnable() {
					    		@Override
					    		public void run() {
					        		setSelection(0);
					    		}		
					    	});
			        	}
			        	else {
			        		final PostItem postItem = mPostItems.get(postItemIndex);
			        		mPorter.replaceEntry(atomEntry, postItem.getEntryId());
			        		postItem.setTitle(generateTitle(atomEntry));
			        		// make the content of the post-item to be displayed
			        		if (atomEntry instanceof ActivityEntry) {
			        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			        			ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex()); 
			        			postItem.setImagePreview(mPorter.generateImagePreview(object));
			        			postItem.setContent(generateContent(object));
			        			postItem.setSource(PostItem.SOURCE_STORYTLR);
			        			
			        		}
			        		else { // The entry is a regular atom-entry        			
			        			postItem.setContent(generateContent(atomEntry));
			        			postItem.setSource(PostItem.SOURCE_STORYTLR);
			        		} 
			        	}
		        		
		        		runOnUiThread(mRefreshAdapter);
			        }
			        else 
			        	Log.e("INSIDE editEntry()", "Response entry is null");
			        
    			}
    			else 
					Log.e("INSIDE editEntry() HttpResponse check", 
							(this.getExceptionMessage() != null)? 
									this.getExceptionMessage():"Http response is null");
    			mProgressDialog.dismiss();
    		}
    	};
    	
    	try {
    		httpPutRequest.getHttpPut().setEntity(new StringEntity(xmlActivityEntry));
    		httpPutRequest.addHeaderToHttpPut("User-Agent", "AMC_BV/0.1");
    		httpPutRequest.addHeaderToHttpPut("Content-Type", "application/atom+xml;type=entry");
	    	Log.d("INSIDE editEntry()", "Finished setting the entity and headers");
	    	httpPutRequest.start();
		} 
    	catch (UnsupportedEncodingException ex) {
    		Log.e("CREATING StringEntity", ex.getMessage());
    		mProgressDialog.dismiss();
		}
    }
    
    private void postEntry(String endpointUri, String xmlActivityEntry)	{
    	HttpTasks httpPostRequest = new HttpTasks(endpointUri, HttpTasks.HTTP_POST) {
    		
    		@Override
    		public void run() {
    			super.run();
    			Log.d("INSIDE postEntry()", "Finished executing POST request");
    			if (this.hasHttpResponse()) {
    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
    				Log.d("INSIDE postEntry()", "Has HttpResponse, Status Code:" + statusCode);
    				if (statusCode != 200 && statusCode != 201)
    					return;
    				AtomEntry atomEntry = null;
    				try {
						mProgDialogTitle = "Parsing Response";
			        	runOnUiThread(mChangeProgDialogTitle);
			        	XmlPullParserFactory xppFactory = XmlPullParserFactory.newInstance();
			        	xppFactory.setNamespaceAware(true);
			        	XmlPullParser xpp = xppFactory.newPullParser();
			        	xpp.setInput(this.getHttpResponse().getEntity().getContent(), "UTF-8");
			        	DefaultXppActivityReader xppActivityReader = new DefaultXppActivityReader();
			        	xpp.next();
			        	atomEntry = xppActivityReader.parseEntry(xpp);
			        }
			        catch(XmlPullParserException ex) {
			        	Log.e("INSIDE postEntry()", ex.getMessage());
			        }
			        catch(IOException ex) {
			        	Log.e("INSIDE postEntry()", ex.getMessage());
			        }
			        catch(Exception ex) {
			        	Log.e("INSIDE postEntry()", ex.getMessage());
			        }
			        
			        if (atomEntry != null) {
			        	mProgDialogTitle = "Updating Stream";
			        	runOnUiThread(mChangeProgDialogTitle);
			        	String titleToDisplay = "";
		        		int sourceToDisplay = PostItem.SOURCE_STORYTLR;
		        		int typeToDisplay = PostItem.NOT_SPECIFIED;
		        		mPorter.getFeed().addEntry(atomEntry);
		        		titleToDisplay = generateTitle(atomEntry);
		        		
		        		// make the content of the post-item to be displayed
		        		if (atomEntry instanceof ActivityEntry) { 
		        			// each object will represent a post-item
		        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
		        			List<ActivityObject> objects = activityEntry.getObjects();
		        			int objectCount = 0;
		        			for (ActivityObject object : objects) {
		        				typeToDisplay = PostItem.getTypeByObjectType(object.getType());
		        				mPostItems.add(0, new PostItem(
				        				titleToDisplay,
				        				generateContent(object),
				        				mPorter.generateImagePreview(object),
				        				sourceToDisplay,
				        				typeToDisplay,
				        				activityEntry.getId(),
				        				objectCount));
		        				runOnUiThread(new Runnable() {
						    		@Override
						    		public void run() {
						    			mPostItemAdapter.insert(mPostItems.get(0), 0);
						    		}		
						    	});
		        				objectCount++;
		        			}
		        		}
		        		else { // The entry is a regular atom-entry        			
		        			mPostItems.add(0, new PostItem(
			        				titleToDisplay,
			        				generateContent(atomEntry),
			        				null,
			        				sourceToDisplay,
			        				typeToDisplay,
			        				atomEntry.getId(),
			        				-1));
		        			runOnUiThread(new Runnable() {
					    		@Override
					    		public void run() {
					    			mPostItemAdapter.insert(mPostItems.get(0), 0);
					    		}		
					    	});
		        		} 
		        		runOnUiThread(mRefreshAdapter);
		        		runOnUiThread(new Runnable() {
				    		@Override
				    		public void run() {
				        		setSelection(0);
				    		}		
				    	});
			        }
			        else {
			        	Log.e("INSIDE postEntry()", "Response entry is null");
			        }
    			}
    			else
					Log.e("INSIDE postEntry()", 
							(this.getExceptionMessage() != null)? 
									this.getExceptionMessage():"Http response is null");
        		runOnUiThread(mDismissProgDialog);
    		}
    		
    	};
    	
    	try {
			httpPostRequest.getHttpPost().setEntity(new StringEntity(xmlActivityEntry));
	    	httpPostRequest.addHeaderToHttpPost("User-Agent", "AMC_BV/0.1");
	    	httpPostRequest.addHeaderToHttpPost("Content-Type", "application/atom+xml;type=entry");
	    	Log.d("INSIDE postEntry()", "Finished setting the entity and headers");
	    	httpPostRequest.start();
		} 
    	catch (UnsupportedEncodingException ex) {
    		Log.e("CREATING StringEntity", ex.getMessage());
    		mProgressDialog.dismiss();
		}
    }
    
    //requesting feeds to the end-point (refreshing post-items list)
    private void requestFeeds(String endpointUri) {
    	Log.d("INSIDE requestFeeds()", "endpointUri=" + endpointUri);
    	HttpTasks httpGetRequest = new HttpTasks(endpointUri, HttpTasks.HTTP_GET) {
			
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
			        	runOnUiThread(mChangeProgDialogTitle);
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
			        	mPorter.setFeed(feed);
			        	mProgDialogTitle = "Displaying Stream";
			        	runOnUiThread(mChangeProgDialogTitle);
			        	runtime = System.currentTimeMillis();
			        	//displayUserProfile(feed.getAuthors().get(0));
			        	mPostItems = new ArrayList<PostItem>();
			        	List<AtomEntry> atomEntries = feed.getEntries();
			        	int totalEntries = atomEntries.size();
			        	AtomEntry atomEntry = null;
			        	for (int i=0; (i<mMaxEntriesCount) && (i<totalEntries); i++) {
			        		atomEntry = atomEntries.get(i);
			        		String titleToDisplay = "";
			        		int sourceToDisplay = i%4;
			        		int typeToDisplay = PostItem.NOT_SPECIFIED;
			        		// TODO: if there's no title (invalid atom-feed)			        			        		
			        		
			        		// the title to be displayed will always be only one
			        		titleToDisplay = generateTitle(atomEntry);
			        		
			        		// TODO: determine the source of the post-item
			        		
			        		// make the content of the post-item to be displayed
			        		if (atomEntry instanceof ActivityEntry) { 
			        			// each object will represent a post-item
			        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			        			List<ActivityObject> objects = activityEntry.getObjects();
			        			int objectCount = 0;
			        			for (ActivityObject object : objects) {
			        				typeToDisplay = PostItem.getTypeByObjectType(object.getType());
			        				mPostItems.add(new PostItem(
					        				titleToDisplay,
					        				generateContent(object),
					        				mPorter.generateImagePreview(object),
					        				sourceToDisplay,
					        				typeToDisplay,
					        				activityEntry.getId(),
					        				objectCount));
			        				objectCount++;
			        			}
			        		}
			        		else { // The entry is a regular atom-entry        			
			        			mPostItems.add(new PostItem(
				        				titleToDisplay,
				        				generateContent(atomEntry),
				        				null,
				        				sourceToDisplay,
				        				typeToDisplay,
				        				atomEntry.getId(),
				        				-1));
			        		} 
			        		
			        	}
			        	mPorter.setPostItems(mPostItems);
			        	runtime = System.currentTimeMillis() - runtime;
			        	Log.d("INSIDE requestFeeds()", "Adding post items execution time: " + runtime + "ms");
			        }
			        else {
			        	Log.e("INSIDE requestFeeds()", "Feed is null");
			        }
		    		
		    		runOnUiThread(mUpdateUi);
				}
				else
					Log.e("INSIDE requestFeeds()", this.getExceptionMessage());
			};
			
		};
		
		httpGetRequest.addHeaderToHttpGet("Accept", "text/atom+xml");
		httpGetRequest.start();
    }
    
    /*private void displayUserProfile(AtomPerson atomAuthor) {
    	final FrameLayout authorFrame = (FrameLayout) findViewById(R.id.userStream_main_author_frame);
    	authorFrame.setVisibility(View.VISIBLE);
    	final ImageView authorPhoto = (ImageView) findViewById(R.id.userStream_main_author_photo);
    	String photoUrl = "http://picasaweb.google.com/lh/photo/vzyeSjb3NLlOxvVBlljD4g?feat=directlink";
    	authorPhoto.setImageBitmap(this.getImageBitmapFromUrlString(photoUrl));
    }*/

    private String generateTitle(AtomEntry atomEntry) {
    	String titleToDisplay = null;
    	/*
    	// generating the title based on the verb and object
    	if (atomEntry instanceof ActivityEntry) {
    		ActivityEntry activityEntry = (ActivityEntry) atomEntry;
    		List<ActivityVerb> verbs = activityEntry.getVerbs();
    		for (ActivityVerb verb : verbs) {
    			String verbValue = verb.getValue();
    			if (verbValue.equals(ActivityVerb.POST)) {
    				
    				break;
    			}
    			else if (verbValue.equals(ActivityVerb.SAVE)) {
    				//titleToDisplay = 
    				break;
    			}
    			else if (verbValue.equals(ActivityVerb.SHARE)) {
    				
    				break;
    			}
    		}
    		if (titleToDisplay == null) { // there is no known activity:verb
    			titleToDisplay = this.ifHtmlRemoveMarkups(atomEntry.getTitle());
    		}
    	}
    	else {
    		titleToDisplay = this.ifHtmlRemoveMarkups(atomEntry.getTitle());
    	}*/
    	if (atomEntry.hasTitle()) {
    		titleToDisplay = GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getTitle());
    		titleToDisplay = GeneralMethods.getShortVersionString(titleToDisplay, mMaxTitleLength);
    	}
    	return (titleToDisplay == null)? "(untitled)":titleToDisplay;
    }
    
    private String generateContent(AtomEntry atomEntry) {
    	String contentToDisplay = null;
    	if (atomEntry instanceof ActivityObject) {
    		ActivityObject activityObject = (ActivityObject) atomEntry;
    		String objectType = activityObject.getType();
    		if (objectType.equals(ActivityObject.ARTICLE)) {
    			if (activityObject.hasSummary())
    				contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(activityObject.getSummary());
    			else if (activityObject.hasContent())
    				if (!activityObject.getContent().hasSrc())
    					contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(activityObject.getContent());
    		}
    		else if (objectType.equals(ActivityObject.AUDIO)) {
    			if (activityObject.hasSummary())
    				contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(activityObject.getSummary());
    		}
    		else if (objectType.equals(ActivityObject.BOOKMARK)) {
    			List<AtomLink> links = activityObject.getLinks();
    			for (AtomLink link : links) {
    				if (link.hasRel() && link.hasHref()) {
    					if (link.getRel().equals(AtomLink.REL_RELATED)) {
    						contentToDisplay = "Link: " + link.getHref();
    						break;
    					}
    				}
    			}
    			if (activityObject.hasSummary())
    				contentToDisplay += "\n" + GeneralMethods.ifHtmlRemoveMarkups(activityObject.getSummary());
    		}
    		else if (objectType.equals(ActivityObject.COMMENT)) {
    			// TODO: not implemented yet.
    		}
    		else if (objectType.equals(ActivityObject.PHOTO)) {
    			List<AtomLink> links = activityObject.getLinks();
    			boolean isPreviewable = false;
    			for (AtomLink link : links) {
    				if (link.hasRel() && link.hasHref()) {
    					if (link.getRel().equals(AtomLink.REL_PREVIEW)) {
    						isPreviewable = true;
    						break;
    					}
    				}
    			}
    			if (isPreviewable)
					contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(activityObject.getTitle());
    			else
    				contentToDisplay  = "\n[no preview due to the big size]";
    		}
    		else if (objectType.equals(ActivityObject.STATUS)) {
    			contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(activityObject.getContent());
    			contentToDisplay = "\"" + GeneralMethods.getShortVersionString(
    					activityObject.getContent().getValue(), mMaxContentLength - 6) + "\"";
    		}
    		else if (objectType.equals(ActivityObject.VIDEO)) {
    			if (activityObject.hasSummary())
    				contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(activityObject.getSummary());
    		}
    		else { // unrecognized object-type of activity:object
    			if (activityObject.hasContent())
    				if (!activityObject.getContent().hasSrc())
    					contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(activityObject.getContent());
    		}
    	}
    	else {
    		if (atomEntry.hasContent())
				if (!atomEntry.getContent().hasSrc())
					contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getContent());
    	}

    	if (contentToDisplay != null)
    		contentToDisplay = GeneralMethods.getShortVersionString(contentToDisplay, mMaxContentLength);
    	return contentToDisplay;
    }
    
    //change the progress dialog's title
    private Runnable mChangeProgDialogTitle = new Runnable() {
    	@Override
    	public void run() {
    		mProgressDialog.setTitle(mProgDialogTitle);
    	};
    };
    
    private Runnable mDismissProgDialog = new Runnable() {
		@Override
		public void run() {
			mProgressDialog.dismiss();
		}    	
    };
    
    private Runnable mRefreshAdapter = new Runnable() {
		@Override
		public void run() {
			mPostItemAdapter.notifyDataSetChanged();
		}
	};
    
    //update the UI by synchronizing the TabActivity and its adapter (with all of its list items)
    private Runnable mUpdateUi = new Runnable() {
    	
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
    	
		public PostItemAdapter(Context context, int textViewResourceId,	List<PostItem> postItems) {
			super(context, textViewResourceId, postItems);
			
			mPostItems = postItems;
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
				final RelativeLayout contentBlock = (RelativeLayout) view.findViewById(R.id.userStream_listItem_blockContent);
				final TextView titleTextView = (TextView) view.findViewById(R.id.userStream_listItem_title);
				final ImageView sourceImageView = (ImageView) view.findViewById(R.id.userStream_listItem_sourceIcon);
				final ImageView typeImageView = (ImageView) view.findViewById(R.id.userStream_listItem_typeIcon);
				
				if (titleTextView != null) {
					titleTextView.setText(postItem.getTitle());
				}
				if (contentBlock != null) {
					ImageView previewImageView = (ImageView) view.findViewById(LIST_ITEM_PREVIEW_IMAGE_ID);
					TextView contentTextView = (TextView) view.findViewById(LIST_ITEM_CONTENT_TEXT_ID);
					if (postItem.hasImagePreview()) {
						if (previewImageView == null) {
							previewImageView = new ImageView(getApplicationContext());
							previewImageView.setId(LIST_ITEM_PREVIEW_IMAGE_ID);
							previewImageView.setAdjustViewBounds(true);
							previewImageView.setMaxHeight(75);
							previewImageView.setMaxWidth(75);
							previewImageView.setImageBitmap(postItem.getImagePreview());
							final RelativeLayout.LayoutParams layoutParams = 
								new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 
										RelativeLayout.LayoutParams.WRAP_CONTENT);
							layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
							layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
							contentBlock.addView(previewImageView, layoutParams);
						}
						else {
							previewImageView.setImageBitmap(postItem.getImagePreview());
						}
					}
					else { // check whether there is a reusable view, if so remove it from contentFrame
						if (previewImageView != null)
							contentBlock.removeView(previewImageView);
					}
					
					if (postItem.hasContent()) {
						if (contentTextView == null) {
							contentTextView = new TextView(getApplicationContext());
							contentTextView.setId(LIST_ITEM_CONTENT_TEXT_ID);
							contentTextView.setText(postItem.getContent());
							final RelativeLayout.LayoutParams layoutParams = 
								new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, 
										RelativeLayout.LayoutParams.WRAP_CONTENT);
							layoutParams.addRule(RelativeLayout.BELOW, LIST_ITEM_PREVIEW_IMAGE_ID);
							layoutParams.addRule(RelativeLayout.ALIGN_LEFT, LIST_ITEM_PREVIEW_IMAGE_ID);
							layoutParams.alignWithParent = true;
							contentBlock.addView(contentTextView, layoutParams);
						}
						else {
							contentTextView.setText(postItem.getContent());
						}
					}
					else { // check whether there is a reusable view, if so remove it from contentFrame
						if (contentTextView != null)
							contentBlock.removeView(contentTextView);
					}
				}
				
				if (sourceImageView != null) {
					int resId = postItem.getSourceIconResId();
					if (resId == -1)
						sourceImageView.setImageBitmap(null);
					else
						sourceImageView.setImageResource(resId);				
				}
				if (typeImageView != null) {
					int resId = postItem.getTypeIconResId();
					if (resId == -1)
						typeImageView.setImageBitmap(null);
					else
						typeImageView.setImageResource(resId);
				}
			}
			
			return view;
		}
    }
}