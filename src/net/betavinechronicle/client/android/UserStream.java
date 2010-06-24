package net.betavinechronicle.client.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.entity.StringEntity;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.activity.ActivityVerb;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomFeed;
import org.onesocialweb.model.atom.AtomLink;
import org.onesocialweb.xml.xpp.imp.DefaultXppActivityReader;
import org.xmlpull.v1.XmlPullParserException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class UserStream extends ListActivity {
	
	static final int LIST_ITEM_PREVIEW_IMAGE_ID = 9876;
	static final int LIST_ITEM_CONTENT_TEXT_ID = 9877;
	
	private List<PostItem> mPostItems;
	private PostItemAdapter mPostItemAdapter;
	private ProgressDialog mProgressDialog = null;
	private Porter mPorter;
	
	private DefaultXppActivityReader mXppActReader;
	
	private String mAlertMessage = "";
	private String mProgDialogTitle = "";
	private String mUsername = "nobody";
	private String mPassword = "";
	private int mMaxCharsTitle;
	private int mMaxCharsContent;
	private int mMaxEntries;
	private boolean mIsFeedRetrieved = false;
	
	//private Runnable viewPostItems;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {  
    	super.onCreate(savedInstanceState);
    	//Debug.startMethodTracing("tracer");
    	
        mPorter = (Porter) this.getApplication();
    	mPostItems = new ArrayList<PostItem>();
        mPostItemAdapter = new PostItemAdapter(this, R.layout.post_item, mPostItems);
        
        mXppActReader = new DefaultXppActivityReader();
        
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

        //mPorter.resetPreferences();
        if (mPorter.isPreferenceSet()) {
        	this.loadSetup();
        	this.requestFeeds(mPorter.loadPreferenceString(
        			Porter.PREFERENCES_KEY_ENDPOINT, "") + "?username=" + mUsername);
        }
        else
        	this.configureSettings();
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
    	case R.id.userStream_options_refresh:
    		this.requestFeeds(mPorter.loadPreferenceString(
        			Porter.PREFERENCES_KEY_ENDPOINT, "") + "?username=" + mUsername);
    		return true;
    		
    	case R.id.userStream_options_postOrShare: 
    		intent = new Intent(this, net.betavinechronicle.client.android.PostOrShare.class);
    		intent.putExtra(Porter.EXTRA_KEY_REQUESTCODE, Porter.REQUESTCODE_POST_OR_SHARE);
			this.startActivityForResult(intent, Porter.REQUESTCODE_POST_OR_SHARE);
    		return true;
    		
    	case R.id.userStream_options_settings: 
    		intent = new Intent(this, net.betavinechronicle.client.android.Settings.class);
    		intent.putExtra(Porter.EXTRA_KEY_REQUESTCODE, Porter.REQUESTCODE_CONFIGURE_SETTINGS);
    		this.startActivityForResult(intent, Porter.REQUESTCODE_CONFIGURE_SETTINGS);
    		return true;
    	
    	case R.id.userStream_options_exit: 
    		this.finish(); 
    		return true;
    	}
    	
    	return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	Intent intent = null;
    	int postItemIndex = -1;
    	if (data != null)
    		postItemIndex = data.getIntExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
		
    	switch (resultCode) {
    	case Porter.RESULTCODE_SUBACTIVITY_CHAINCLOSE:
    		finish();
    		return;
    	
    	case Porter.RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE:
    		intent = new Intent(this, net.betavinechronicle.client.android.PostOrShare.class);
    		intent.putExtra(Porter.EXTRA_KEY_REQUESTCODE, Porter.REQUESTCODE_POST_OR_SHARE);
			this.startActivityForResult(intent, Porter.REQUESTCODE_POST_OR_SHARE);
			return;
			
    	case Porter.RESULTCODE_SWITCH_ACTIVITY_TO_SETTINGS:
    		this.configureSettings();
    		return;
    	
    	case Porter.RESULTCODE_EDITING_ENTRY:
    		if (postItemIndex < -1) break;
    		mProgressDialog = ProgressDialog.show(this, 
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_DIALOG_TITLE), 
    				"Please wait...");
    		this.editEntry(data.getCharSequenceExtra(Porter.EXTRA_KEY_TARGET_URI).toString(),
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY).toString(),
    				postItemIndex);
    		return;
    		
    	case Porter.RESULTCODE_DELETING_ENTRY:
    		if (postItemIndex < 0) break;
    		this.deleteEntry(data.getCharSequenceExtra(Porter.EXTRA_KEY_TARGET_URI).toString(),
    				postItemIndex);
    		return;
    	
    	case Porter.RESULTCODE_POSTING_ENTRY:
    		mProgressDialog = ProgressDialog.show(this, 
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_DIALOG_TITLE), 
    				"Please wait...");
    		this.postEntry(mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_ENDPOINT, "")
    				+ "?username=" + mUsername,
    				data.getCharSequenceExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY).toString());
    		return;
    	}
    	
    	if (requestCode == Porter.REQUESTCODE_CONFIGURE_SETTINGS) {
    		this.loadSetup();
    		if (mIsFeedRetrieved == false)
	    		this.requestFeeds(mPorter.loadPreferenceString(
	        			Porter.PREFERENCES_KEY_ENDPOINT, "") + "?username=" + mUsername);
    	}
    }
    
    /*
     *  OVERRIDE THE onConfigurationChanged 
     *  SO THAT THE ACTIVITY WON'T BE RECREATED UPON ORIENTATION CHANGE 
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }
    
    private void configureSettings() {
    	Intent intent = new Intent(getApplicationContext(), 
				net.betavinechronicle.client.android.Settings.class);
    	intent.putExtra(Porter.EXTRA_KEY_REQUESTCODE, Porter.REQUESTCODE_CONFIGURE_SETTINGS);
    	startActivityForResult(intent, Porter.REQUESTCODE_CONFIGURE_SETTINGS);
    }
    
    private void deleteEntry(String endpointUri, final int postItemIndex) {
    	HttpTasks httpDeleteRequest = new HttpTasks(endpointUri, HttpTasks.HTTP_DELETE) {
    		
    		@Override
    		public void run() {
    			super.run();
    			if (this.hasHttpResponse()) {
    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
    				if (statusCode != 200) {
    					// LOG THE ERROR AND DISPLAY AN ALERT DIALOG TO USER
    					try {
    						String responseText = GeneralMethods.getRawStringFromResponse(
									this.getHttpResponse().getEntity().getContent());
    						Log.e("UserStream[deleteEntry()] >> Wrong status code(" + statusCode
									+ ")", responseText);
    						mAlertMessage = "Failed to delete the entry...";
    						runOnUiThread(displayAlert);
						} 
    					catch (IllegalStateException ex) {
							Log.e("UserStream[deleteEntry()] >> Wrong status code(" + statusCode
									+ "), getRawStringFromResponse() failed.", 
									ex.getMessage());
						}
    					catch (IOException ex) {
    						Log.e("UserStream[deleteEntry()] >> Wrong status code(" + statusCode
									+ "), getRawStringFromResponse() failed.", 
									ex.getMessage());
						}
    					return;
    				}
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
    			else {
    				Log.e("UserStream[deleteEntry()] >> Uploading, checking HTTP response.", 
							(this.getExceptionMessage() != null)? 
									this.getExceptionMessage():"HTTP response is null.");
    				mAlertMessage = "Failed to delete the entry...";
					runOnUiThread(displayAlert);
    			}
    			
    			mProgressDialog.dismiss();
    		}
    	};
    	
    	httpDeleteRequest.addHeaderToHttpDelete("User-Agent", mPorter.getAppName());
		httpDeleteRequest.addHeaderToHttpDelete("Password", mPassword);
    	httpDeleteRequest.start();
    }
    
    private void editEntry(String endpointUri, String xmlActivityEntry,	final int postItemIndex) {
    	HttpTasks httpPutRequest = new HttpTasks(endpointUri, HttpTasks.HTTP_PUT) {
    		
    		@Override
    		public void run() {
    			super.run();
    			if (this.hasHttpResponse()) {
    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
    				if (statusCode != 200) {
    					try {
    						String responseText = GeneralMethods.getRawStringFromResponse(
									this.getHttpResponse().getEntity().getContent());
    						Log.e("UserStream[editEntry()] >> Wrong status code(" + statusCode
									+ ")", responseText);
    						mAlertMessage = "Failed to edit the entry...";
    						runOnUiThread(displayAlert);
						} 
    					catch (IllegalStateException ex) {
							Log.e("UserStream[editEntry()] >> Wrong status code(" + statusCode
									+ "), getRawStringFromResponse() failed.", 
									ex.getMessage());
						}
    					catch (IOException ex) {
    						Log.e("UserStream[editEntry()] >> Wrong status code(" + statusCode
									+ "), getRawStringFromResponse() failed.", 
									ex.getMessage());
						}
    					return;
    				}
    				
    				AtomEntry atomEntry = null;
    				try {
						mProgDialogTitle = "Parsing Response";
			        	runOnUiThread(mChangeProgDialogTitle);
			        	atomEntry = mXppActReader.parseEntry(mPorter.prepareXppWithInputStream(
			        			this.getHttpResponse().getEntity().getContent()));
				    }
			        catch(XmlPullParserException ex) {
			        	Log.e("UserStream[editEntry()] >> After PUT request, parsing response entry failed.", 
			        			ex.getMessage());
			        }
			        catch(IOException ex) {
			        	Log.e("UserStream[editEntry()] >> After PUT request, parsing response entry failed.", 
			        			ex.getMessage());
			        }
			        
			        if (atomEntry != null) {
			        	mProgDialogTitle = "Updating Stream"; 
			        	runOnUiThread(mChangeProgDialogTitle);
			        	
			        	if (postItemIndex == -1) {
			        		/*
			        		 *  THE EDITING PROCESS IS DIRECTED TO MEDIA LINK ENTRY
			        		 *  WHICH HASN'T BEEN ADDED TO THE LIST-VIEW BEFORE
			        		 */
			        		
			        		mPorter.getFeed().addEntry(atomEntry);
			        		
			        		// TODO: determine the source of the post-item
			        		int sourceToDisplay = PostItem.NOT_SPECIFIED;
			        		
			        		// PUT THE ENTRY INTO THE LIST-VIEW
			        		if (atomEntry instanceof ActivityEntry) { 
			        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			        			List<ActivityObject> objects = activityEntry.getObjects();
			        			int objectCount = 0;
			        			
			        			// EACH OBJECT IN THE ACTIVITY-ENTRY REPRESENTS A LIST ITEM			        			
			        			for (ActivityObject object : objects) {
			        				mPostItems.add(0, new PostItem(
			        						generateTitle(object, activityEntry.getVerbs()),
					        				generateContent(object),
					        				mPorter.generateImagePreview(object),
					        				sourceToDisplay,
					        				PostItem.getTypeByObjectType(object.getType()),
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
			        		else { 
			        			// THE ENTRY IS A REGULAR ATOM-ENTRY
			        			mPostItems.add(0, new PostItem(
			        					generateTitle(atomEntry, null),
				        				generateContent(atomEntry),
				        				null,
				        				sourceToDisplay,
				        				PostItem.NOT_SPECIFIED,
				        				atomEntry.getId(),
				        				-1));
			        			runOnUiThread(new Runnable() {
						    		@Override
						    		public void run() {
						    			// INSERT THE ENTRY TO THE TOP OF THE LIST
						    			mPostItemAdapter.insert(mPostItems.get(0), 0);
						    		}		
						    	});
			        		} 
			        		
			        		runOnUiThread(mRefreshAdapter);
			        		runOnUiThread(new Runnable() {
					    		@Override
					    		public void run() {
					    			// SET THE SCREEN VIEW TO THE FIRST ITEM IN THE LIST
					        		setSelection(0);
					    		}		
					    	});
			        	}
			        	else {
			        		// THE TARGET ENTRY HAS BEEN ALREADY ADDED TO THE LIST BEFORE
			        		
			        		final PostItem postItem = mPostItems.get(postItemIndex);
			        		mPorter.replaceEntry(atomEntry, postItem.getEntryId());
			        		
			        		// RESET THE PREVIEW OF THE LIST ITEMS
			        		if (atomEntry instanceof ActivityEntry) {
			        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			        			ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex()); 
			        			postItem.setImagePreview(mPorter.generateImagePreview(object));
			        			postItem.setTitle(generateTitle(object, activityEntry.getVerbs()));
			        			postItem.setContent(generateContent(object));
			        			postItem.setSource(PostItem.SOURCE_STORYTLR);
			        			
			        		}
			        		else { 
			        			// THE ENTRY IS A REGULAR ATOM-ENTRY
			        			postItem.setTitle(generateTitle(atomEntry, null));
			        			postItem.setContent(generateContent(atomEntry));
			        			postItem.setSource(PostItem.SOURCE_STORYTLR);
			        		} 
			        	}
		        		
		        		runOnUiThread(mRefreshAdapter);
			        }
			        else {
			        	// RESPONSE ENTRY IS NULL
			        	Log.e("UserStream[editEntry()] >> After PUT request, reading the entry response.", 
	        					"Response entry is null.");
			        	mAlertMessage = "Failed to edit the entry...";
			        	runOnUiThread(displayAlert);
			        }
    			}
    			else {
    				// HTTP RESPONSE IS NULL
    				Log.e("UserStream[editEntry()] >> Sending PUT request, checking HTTP response.", 
							(this.getExceptionMessage() != null)? 
									this.getExceptionMessage():"HTTP response is null.");
		        	mAlertMessage = "Failed to edit the entry...";
		        	runOnUiThread(displayAlert);
    			}
    			
    			mProgressDialog.dismiss();
    		}
    	};
    	
    	try {
    		httpPutRequest.getHttpPut().setEntity(new StringEntity(xmlActivityEntry));
    		httpPutRequest.addHeaderToHttpPut("User-Agent", mPorter.getAppName());
    		httpPutRequest.addHeaderToHttpPut("Content-Type", "application/atom+xml;type=entry");
    		httpPutRequest.addHeaderToHttpPut("Password", mPassword);
	    	httpPutRequest.start();
		} 
    	catch (UnsupportedEncodingException ex) {
    		Log.e("UserStream[editEntry()] >> Constructing StringEntity failed", ex.getMessage());
    		mAlertMessage = "Failed to edit the entry...";
    		runOnUiThread(displayAlert);
		}
    }
    
    private void postEntry(String endpointUri, String xmlActivityEntry)	{
    	HttpTasks httpPostRequest = new HttpTasks(endpointUri, HttpTasks.HTTP_POST) {
    		
    		@Override
    		public void run() {
    			super.run();
    			if (this.hasHttpResponse()) {
    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
    				if (statusCode != 200 && statusCode != 201)  {
    					try {
    						String responseText = GeneralMethods.getRawStringFromResponse(
									this.getHttpResponse().getEntity().getContent());
    						Log.e("UserStream[postEntry()] >> Wrong status code(" + statusCode
									+ ")", responseText);
    						mAlertMessage = "Failed to post the entry...";
    						runOnUiThread(displayAlert);
						} 
    					catch (IllegalStateException ex) {
							Log.e("UserStream[postEntry()] >> Wrong status code(" + statusCode
									+ "), getRawStringFromResponse() failed.", 
									ex.getMessage());
						}
    					catch (IOException ex) {
    						Log.e("UserStream[postEntry()] >> Wrong status code(" + statusCode
									+ "), getRawStringFromResponse() failed.", 
									ex.getMessage());
						}
    					return;
    				}
    				
    				AtomEntry atomEntry = null;
    				try {
						mProgDialogTitle = "Parsing Response";
			        	runOnUiThread(mChangeProgDialogTitle);		
			        	atomEntry = mXppActReader.parseEntry(mPorter.prepareXppWithInputStream(
			        			this.getHttpResponse().getEntity().getContent()));
			        }
			        catch(XmlPullParserException ex) {
			        	Log.e("UserStream[postEntry()] >> After POST request, parsing response entry failed.", 
			        			ex.getMessage());
			        }
			        catch(IOException ex) {
			        	Log.e("UserStream[postEntry()] >> After POST request, parsing response entry failed.", 
			        			ex.getMessage());
			        }
			        
			        if (atomEntry != null) {
			        	mProgDialogTitle = "Updating Stream";
			        	runOnUiThread(mChangeProgDialogTitle);
		        		mPorter.getFeed().addEntry(atomEntry);
		        		
		        		// PUT THE ENTRY INTO THE LIST-VIEW
		        		if (atomEntry instanceof ActivityEntry) { 
		        			
		        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
		        			List<ActivityObject> objects = activityEntry.getObjects();
		        			int objectCount = 0;
		        			
		        			// EACH OBJECT IN THE ACTIVITY-ENTRY REPRESENTS A LIST ITEM	
		        			for (ActivityObject object : objects) {
		        				mPostItems.add(0, new PostItem(
		        						generateTitle(object, activityEntry.getVerbs()),
				        				generateContent(object),
				        				mPorter.generateImagePreview(object),
				        				PostItem.SOURCE_STORYTLR,
				        				PostItem.getTypeByObjectType(object.getType()),
				        				activityEntry.getId(),
				        				objectCount));
		        				runOnUiThread(new Runnable() {
						    		@Override
						    		public void run() {
						    			// INSERT THE ENTRY TO THE TOP OF THE LIST
						    			mPostItemAdapter.insert(mPostItems.get(0), 0);
						    		}		
						    	});
		        				objectCount++;
		        			}
		        		}
		        		else { 
		        			// THE ENTRY IS A REGULAR ATOM-ENTRY
		        			mPostItems.add(0, new PostItem(
		        					generateTitle(atomEntry, null),
			        				generateContent(atomEntry),
			        				null,
			        				PostItem.SOURCE_STORYTLR,
			        				PostItem.NOT_SPECIFIED,
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
				    			// SET THE SCREEN VIEW TO THE FIRST ITEM IN THE LIST
				        		setSelection(0);
				    		}		
				    	});
			        }
			        else {
			        	// RESPONSE ENTRY IS NULL
			        	Log.e("UserStream[postEntry()] >> After POST request, reading the entry response.", 
	        					"Response entry is null.");
			        	mAlertMessage = "Failed to post the entry...";
			        	runOnUiThread(displayAlert);
			        }
    			}
    			else {
    				// HTTP RESPONSE IS NULL
    				Log.e("UserStream[postEntry()] >> Sending POST request, checking HTTP response.", 
							(this.getExceptionMessage() != null)? 
									this.getExceptionMessage():"HTTP response is null.");
		        	mAlertMessage = "Failed to post the entry...";
		        	runOnUiThread(displayAlert);
    			}
    			
    			mProgressDialog.dismiss();
    		}
    		
    	};
    	
    	try {
			httpPostRequest.getHttpPost().setEntity(new StringEntity(xmlActivityEntry));
	    	httpPostRequest.addHeaderToHttpPost("User-Agent", mPorter.getAppName());
	    	httpPostRequest.addHeaderToHttpPost("Content-Type", "application/atom+xml;type=entry");
    		httpPostRequest.addHeaderToHttpPost("Password", mPassword);
	    	httpPostRequest.start();
		} 
    	catch (UnsupportedEncodingException ex) {
    		Log.e("UserStream[postEntry()] >> Constructing StringEntity failed", ex.getMessage());
    		mAlertMessage = "Failed to post the entry...";
    		runOnUiThread(displayAlert);
		}
    }
    
    /*
     *  REQUEST A FEED OF THE USER'S STREAM
     *  AND
     *  REFRESH THE LIST-VIEW
     */
    private void requestFeeds(String endpointUri) {
    	mProgressDialog = ProgressDialog.show(this, "Retrieving Stream of " + mUsername, 
				"Please wait...");
    	HttpTasks httpGetRequest = new HttpTasks(endpointUri, HttpTasks.HTTP_GET) {
			
			@Override
			public void run() {
				long runtime = 0; // TO DEBUG THE EXECUTION TIME
				super.run();
				
				if (this.hasHttpResponse()) {
					int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
    				if (statusCode != 200)  {
    					try {
    						String responseText = GeneralMethods.getRawStringFromResponse(
									this.getHttpResponse().getEntity().getContent());
    						Log.e("UserStream[requestFeeds()] >> Wrong status code(" + statusCode
									+ ")", responseText);
    						mAlertMessage = "Failed to request the feed...";
    						runOnUiThread(displayAlert);
						} 
    					catch (IllegalStateException ex) {
							Log.e("UserStream[requestFeeds()] >> Wrong status code(" + statusCode
									+ "), getRawStringFromResponse() failed.", 
									ex.getMessage());
						}
    					catch (IOException ex) {
    						Log.e("UserStream[requestFeeds()] >> Wrong status code(" + statusCode
									+ "), getRawStringFromResponse() failed.", 
									ex.getMessage());
						}
    					return;
    				}
    				
					AtomFeed feed = null;
		        	try {
						mProgDialogTitle = "Parsing Stream";
			        	runOnUiThread(mChangeProgDialogTitle);
			        	
			        	runtime = System.currentTimeMillis();
			        	
			        	feed = mXppActReader.parse(mPorter.prepareXppWithInputStream(
			        			this.getHttpResponse().getEntity().getContent()));
			        	
			        	runtime = System.currentTimeMillis() - runtime;
			        	Log.d("UserStream[requestFeeds()]", "FEED-PARSING execution time: " + runtime + "ms.");
			        }
			        catch(XmlPullParserException ex) {
			        	Log.e("UserStream[requestFeeds()] >> After GET request, parsing response feed failed.", 
			        			ex.getMessage());
			        }
			        catch(IOException ex) {
			        	Log.e("UserStream[requestFeeds()] >> After GET request, parsing response feed failed.", 
			        			ex.getMessage());
			        }
			        
			        if (feed != null) {
			        	mPorter.setFeed(feed);			        
			        	
			        	mProgDialogTitle = "Displaying Stream";
			        	runOnUiThread(mChangeProgDialogTitle);
			        	
			        	runtime = System.currentTimeMillis();
			        	
			        	mPostItems = new ArrayList<PostItem>();
			        	List<AtomEntry> atomEntries = feed.getEntries();
			        	int totalEntries = atomEntries.size();
			        	
			        	AtomEntry atomEntry = null;
			        	
			        	// PUT THE ENTRIES INTO THE LIST-VIEW
			        	for (int i=0; (i<mMaxEntries) && (i<totalEntries); i++) {
			        		atomEntry = atomEntries.get(i);
			             	
			        		// TODO: determine the source of the post-item
			        		int sourceToDisplay = PostItem.NOT_SPECIFIED;
			        		
			        		if (atomEntry instanceof ActivityEntry) { 
			        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			        			List<ActivityObject> objects = activityEntry.getObjects();
			        			int objectCount = 0;
			        			
			        			// EACH OBJECT IN THE ACTIVITY-ENTRY REPRESENTS A LIST ITEM
			        			for (ActivityObject object : objects) {
			        				mPostItems.add(new PostItem(
			        						generateTitle(object, activityEntry.getVerbs()),
					        				generateContent(object),
					        				mPorter.generateImagePreview(object),
					        				sourceToDisplay,
					        				PostItem.getTypeByObjectType(object.getType()),
					        				activityEntry.getId(),
					        				objectCount));
			        				objectCount++;
			        			}
			        		}
			        		else { 
			        			// THE ENTRY IS A REGULAR ATOM-ENTRY
			        			mPostItems.add(new PostItem(
			        					generateTitle(atomEntry, null),
				        				generateContent(atomEntry),
				        				null,
				        				sourceToDisplay,
				        				PostItem.NOT_SPECIFIED,
				        				atomEntry.getId(),
				        				-1));
			        		} 
			        		
			        	}
			        	mPorter.setPostItems(mPostItems);
			        	mIsFeedRetrieved = true;
			        	
			        	runtime = System.currentTimeMillis() - runtime;
			        	Log.d("UserStream[requestFeeds()]", "ADDING ENTRIES INTO LIST-VIEW execution time: " + runtime + "ms.");
			        }
			        else {
			        	// RESPONSE FEED IS NULL
			        	Log.e("UserStream[requestFeeds()] >> After GET request, reading the feed response.", 
	        					"Response feed is null.");
			        	mAlertMessage = "Failed to request the feed...";
			        	runOnUiThread(displayAlert);
			        }
				}
				else{
    				// HTTP RESPONSE IS NULL
    				Log.e("UserStream[requestFeeds()] >> Sending GET request, checking HTTP response.", 
							(this.getExceptionMessage() != null)? 
									this.getExceptionMessage():"HTTP response is null.");
		        	mAlertMessage = "Failed to request the feed...";
		        	runOnUiThread(displayAlert);
    			}
		    		
		    	runOnUiThread(mUpdateUi);
			};
			
		};
		
		httpGetRequest.addHeaderToHttpGet("Accept", "text/atom+xml");
		httpGetRequest.start();
    }

    /*
     *  GENERATE THE TITLE FOR THE PREVIEW
     *  BASED ON THE VERB IN THE ENTRY AND THE OBJECT-TYPE IN THE OBJECT
     */
    private String generateTitle(AtomEntry atomEntry, List<ActivityVerb> verbs) {
    	String titleToDisplay = null;
    	
    	if (atomEntry instanceof ActivityObject) {
    		ActivityObject object = (ActivityObject) atomEntry;
    		for (ActivityVerb verb : verbs) {
    			String verbValue = verb.getValue();
    			if (verbValue.equals(ActivityVerb.POST)) {
    				titleToDisplay = mUsername + " posted";
    				break;
    			}
    			else if (verbValue.equals(ActivityVerb.SHARE)) {
    				titleToDisplay = mUsername + " shared";
    				break;
    			}
    		}
    		
    		if (titleToDisplay == null) {
    			// UNHANDLED activity:verb VALUE
    			titleToDisplay = mUsername + " posted";
    		}
    		
			String objectType = object.getType();
		
    		if (objectType.equals(ActivityObject.STATUS)) {
    			titleToDisplay += " a Status update.";
    		}
    		else if (objectType.equals(ActivityObject.ARTICLE)) {
    			titleToDisplay += " a Blog entry '" + mPorter.extractTitleFromObject(object) + "'.";
    		}
    		else if (objectType.equals(ActivityObject.BOOKMARK)) {
    			titleToDisplay += " a Link '" + mPorter.extractTitleFromObject(object) + "'.";
    		}
    		else if (objectType.equals(ActivityObject.PHOTO)) {
    			titleToDisplay += " a Picture '" + mPorter.extractTitleFromObject(object) + "'.";
    		}
    		else if (objectType.equals(ActivityObject.AUDIO)) {
    			titleToDisplay += " an Audio '" + mPorter.extractTitleFromObject(object) + "'.";
    		}
    		else if (objectType.equals(ActivityObject.VIDEO)) {
    			titleToDisplay += " a Video '" + mPorter.extractTitleFromObject(object) + "'.";
    		}	    		
    		else {
    			// UNHANDLED activity:object-type VALUE
    			titleToDisplay += " a new entry '" + mPorter.extractTitleFromObject(object) + "'.";
    		}    			
    	}
    	else 
    		// AN ATOM-ENTRY (NOT AN activity:object)
    		if (atomEntry.hasTitle())
        		titleToDisplay = GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getTitle());
    	
    	// SHORTENED THE STRING IF ITS LENGTH EXCEED THE MAXIMUM CHARACTERS
        titleToDisplay = GeneralMethods.getShortVersionString(titleToDisplay, mMaxCharsTitle);
    	return (titleToDisplay == null)? "(untitled)":titleToDisplay;
    }
    
    /*
     *  GENERATE THE CONTENT FOR THE PREVIEW
     *  BASED ON THE OBJECT-TYPE
     */
    private String generateContent(AtomEntry atomEntry) {
    	String contentToDisplay = null;
    	if (atomEntry instanceof ActivityObject) {
    		ActivityObject activityObject = (ActivityObject) atomEntry;
    		String objectType = activityObject.getType();
    		
    		if (objectType.equals(ActivityObject.STATUS)) {
    			contentToDisplay = mPorter.extractContentFromObject(activityObject);
    			if (contentToDisplay == null)
    				contentToDisplay = mPorter.extractTitleFromObject(activityObject);
    			
    			if (contentToDisplay == null)
    				contentToDisplay = "";
    			else
    				contentToDisplay = "\"" + GeneralMethods.getShortVersionString(
    					contentToDisplay, mMaxCharsContent - 6) + "\"";
    		}
    		else if (objectType.equals(ActivityObject.ARTICLE)) {
    			contentToDisplay = mPorter.extractSummaryFromObject(activityObject);
    			if (contentToDisplay == null)
    				contentToDisplay = mPorter.extractContentFromObject(activityObject);
			}
    		else if (objectType.equals(ActivityObject.BOOKMARK)) {
    			contentToDisplay = "Link: " + 
    				mPorter.extractHrefFromLinks(activityObject.getLinks(), AtomLink.REL_RELATED);
    			contentToDisplay += "\n" + mPorter.extractSummaryFromObject(activityObject);
    		}
    		else if (objectType.equals(ActivityObject.PHOTO)) {
    			boolean isPreviewable = (mPorter.extractHrefFromLinks(
    					activityObject.getLinks(), AtomLink.REL_PREVIEW) == null)? false:true;
    	
    			if (isPreviewable)
					contentToDisplay = mPorter.extractSummaryFromObject(activityObject);
    			else
    				contentToDisplay  = "\n[no preview due to the big size]";
    		}
    		else if (objectType.equals(ActivityObject.AUDIO)) {
    			contentToDisplay = "[Click to go to detailed view and Listen]\n\n" +
    					mPorter.extractSummaryFromObject(activityObject);
        	}
    		else if (objectType.equals(ActivityObject.VIDEO)) {
    			contentToDisplay = "[Video entry handling is not implemented yet]\n\n" +
						mPorter.extractSummaryFromObject(activityObject);
    		}
    		else 
    			// UNHANDLED activity:object-type VALUE
    			contentToDisplay = mPorter.extractContentFromObject(activityObject);
    		
    	}
    	else {
    		// AN ATOM-ENTRY (NOT AN activity:object)
    		if (atomEntry.hasContent())
				if (!atomEntry.getContent().hasSrc())
					contentToDisplay = GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getContent());
    	}

    	if (contentToDisplay != null)
    		contentToDisplay = GeneralMethods.getShortVersionString(contentToDisplay, mMaxCharsContent);
    	return contentToDisplay;
    }
    
    // CHANGE THE TITLE OF THE PROGRESS DIALOG
    private Runnable mChangeProgDialogTitle = new Runnable() {
    	@Override
    	public void run() {
    		mProgressDialog.setTitle(mProgDialogTitle);
    	};
    };
    
    // REFRESH THE LIST-VIEW
    private Runnable mRefreshAdapter = new Runnable() {
		@Override
		public void run() {
			mPostItemAdapter.notifyDataSetChanged();
		}
	};
	
	// UPDATE THE LIST-VIEW BY SYNCHRONIZING THE TabActivity's ListView AND ITS ADAPTER'S LIST ITEM
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
    		}
    		else {
    			Toast.makeText(getApplicationContext(), 
    					"Failed to retrieve post stream..", 
    					Toast.LENGTH_SHORT).show();
    			mProgressDialog.dismiss();
    		}
    	};
    };   
    
    private Runnable displayAlert = new Runnable() {

		@Override
		public void run() {
			if (mProgressDialog != null) mProgressDialog.dismiss();
			new AlertDialog.Builder(UserStream.this)
			.setTitle("Sharing new Audio")
			.setMessage(mAlertMessage)
			.setCancelable(true)
			.setPositiveButton("OK", null)
			.show();
		}
    	
    };
 
	private void loadSetup() {
		mUsername = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, "nobody");
		mPassword = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_PASSWORD, "");
    	mMaxEntries = mPorter.loadPreferenceInt(Porter.PREFERENCES_KEY_MAX_ENTRIES, 0);
        mMaxCharsTitle = mPorter.loadPreferenceInt(Porter.PREFERENCES_KEY_MAX_CHARS_TITLE, 0);
        mMaxCharsContent = mPorter.loadPreferenceInt(Porter.PREFERENCES_KEY_MAX_CHARS_CONTENT, 0);
	}
       
	// DEFINE A CUSTOM ADAPTER CLASS FOR THE CUSTOM LIST-VIEW'S ITEM LAYOUT (post_item.xml)
    private class PostItemAdapter extends ArrayAdapter<PostItem> {
    	private List<PostItem> mPostItems;
    	
		public PostItemAdapter(Context context, int textViewResourceId,	List<PostItem> postItems) {
			super(context, textViewResourceId, postItems);
			
			mPostItems = postItems;
		}
		
		// OVERRIDE THE getView() METHOD TO CUSTOMIZE THE LIST ITEM LAYOUT
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
					else { 
						// CHECK WHETHER THERE IS A REUSEABLE VIEW, IF SO, REMOVE IT FROM contentFrame
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
					else { 
						// CHECK WHETHER THERE IS A REUSEABLE VIEW, IF SO, REMOVE IT FROM contentFrame
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