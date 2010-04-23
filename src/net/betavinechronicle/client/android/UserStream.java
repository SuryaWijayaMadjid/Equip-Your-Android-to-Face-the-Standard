package net.betavinechronicle.client.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomFeed;
import org.onesocialweb.model.atom.AtomLink;
import org.onesocialweb.model.atom.AtomText;
import org.onesocialweb.xml.xpp.imp.DefaultXppActivityReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class UserStream extends ListActivity {
	static final int RESULTCODE_SUBACTIVITY_CHAINCLOSE = 99;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_PROFILE = 98;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE = 97;
	
	static final int LIST_ITEM_PREVIEW_IMAGE_ID = 9876;
	static final int LIST_ITEM_CONTENT_TEXT_ID = 9877;
	
	private List<PostItem> mPostItems;
	private PostItemAdapter mPostItemAdapter;
	private ProgressDialog mProgressDialog = null;
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
        mPostItems = new ArrayList<PostItem>();
        mPostItemAdapter = new PostItemAdapter(this, R.layout.post_item, mPostItems);
        
        // load setup values
        mMaxEntriesCount = Integer.parseInt(getString(R.string.max_entries));
        mMaxTitleLength = Integer.parseInt(getString(R.string.max_char_title));
        mMaxContentLength = Integer.parseInt(getString(R.string.max_char_content));
        
        // add a circling progress loading display feature to the top 3right of our application
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
    				null,
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
			        	int totalEntries = atomEntries.size();
			        	AtomEntry atomEntry = null;
			        	for (int i=0; (i<mMaxEntriesCount) && (i<totalEntries); i++) {
			        		atomEntry = atomEntries.get(i);
			        		String titleToDisplay = "";
			        		int sourceToDisplay = i%4;
			        		int typeToDisplay = i%4;
			        		// TODO: if there's no title (invalid atom-feed)			        			        		
			        		
			        		// the title to be displayed will always be only one
			        		titleToDisplay = generateTitle(atomEntry);
			        		
			        		// TODO: determine the source and type of the post-item
			        		
			        		// make the content of the post-item to be displayed
			        		if (atomEntry instanceof ActivityEntry) { 
			        			// each object will represent a post-item
			        			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			        			List<ActivityObject> objects = activityEntry.getObjects();
			        			
			        			for (ActivityObject object : objects) {
			        				mPostItems.add(new PostItem(
					        				titleToDisplay,
					        				generateContent(object),
					        				generateImagePreview(object),
					        				sourceToDisplay,
					        				typeToDisplay));
			        			}
			        		}
			        		else { // The entry is a regular atom-entry        			
			        			mPostItems.add(new PostItem(
				        				titleToDisplay,
				        				generateContent(atomEntry),
				        				null,
				        				sourceToDisplay,
				        				typeToDisplay));
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
    	
    	titleToDisplay = CommonMethods.getShortVersionString(titleToDisplay, mMaxTitleLength);
    	return titleToDisplay;
    }
    
    private String generateContent(AtomEntry atomEntry) {
    	String contentToDisplay = null;
    	if (atomEntry instanceof ActivityObject) {
    		ActivityObject activityObject = (ActivityObject) atomEntry;
    		String objectType = activityObject.getType();
    		if (objectType.equals(ActivityObject.ARTICLE)) {
    			if (activityObject.hasSummary())
    				contentToDisplay = this.ifHtmlRemoveMarkups(activityObject.getSummary());
    			else if (activityObject.hasContent())
    				if (!activityObject.getContent().hasSrc())
    					contentToDisplay = this.ifHtmlRemoveMarkups(activityObject.getContent());
    		}
    		else if (objectType.equals(ActivityObject.AUDIO)) {
    			if (activityObject.hasSummary())
    				contentToDisplay = this.ifHtmlRemoveMarkups(activityObject.getSummary());
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
    				contentToDisplay += "\n" + this.ifHtmlRemoveMarkups(activityObject.getSummary());
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
    					contentToDisplay = this.ifHtmlRemoveMarkups(activityObject.getTitle());
    				}
    			}
    			if (!isPreviewable) 
    				contentToDisplay  += " [no preview due to the big size]";
    		}
    		else if (objectType.equals(ActivityObject.STATUS)) {
    			contentToDisplay = this.ifHtmlRemoveMarkups(activityObject.getContent());
    			contentToDisplay = "\"" + CommonMethods.getShortVersionString(
    					activityObject.getContent().getValue(), mMaxContentLength - 6) + "\"";
    		}
    		else if (objectType.equals(ActivityObject.VIDEO)) {
    			if (activityObject.hasSummary())
    				contentToDisplay = this.ifHtmlRemoveMarkups(activityObject.getSummary());
    		}
    		else { // unknown object-type of activity:object
    			if (activityObject.hasContent())
    				if (!activityObject.getContent().hasSrc())
    					contentToDisplay = this.ifHtmlRemoveMarkups(activityObject.getContent());
    		}
    	}
    	else {
    		if (atomEntry.hasContent())
				if (!atomEntry.getContent().hasSrc())
					contentToDisplay = this.ifHtmlRemoveMarkups(atomEntry.getContent());
    	}

    	if (contentToDisplay != null)
    		contentToDisplay = CommonMethods.getShortVersionString(contentToDisplay, mMaxContentLength);
    	return contentToDisplay;
    }
    
    private String ifHtmlRemoveMarkups(AtomText atomText) {
    	String text = atomText.getValue();
    	if (atomText.getType().equals("html"))
    		text = CommonMethods.removeContainedMarkups(text, true);
    	return text;
    }
    
    private Bitmap generateImagePreview(ActivityObject object) {
    	Bitmap imagePreview = null;
    	if (object.getType().equals(ActivityObject.PHOTO)) {
    		List<AtomLink> links = object.getLinks();
			for (AtomLink link : links) {
				if (link.hasRel() && link.hasHref()) {
					if (link.getRel().equals(AtomLink.REL_PREVIEW)) {
						try {
							InputStream inStream = (InputStream) (new URL(link.getHref())).getContent();
							imagePreview = BitmapFactory.decodeStream(inStream);
						}
						catch (MalformedURLException ex) {
							Log.e("INSIDE generateImagePreview()", ex.getMessage());
						}
						catch (IOException ex) {
							Log.e("INSIDE generateImagePreview()", ex.getMessage());
						}
						catch (Exception ex) {
							Log.e("INSIDE generateImagePreview()", ex.getMessage());
						}
						break;
					}
				}
			}
    	}
    	
    	// for debugging...
    	/*
    	try {
			InputStream inStream = (InputStream) (new URL("http://a1.typepad.com/6a010535617444970b0133ecc20b29970b-120si")).getContent();
			imagePreview = Drawable.createFromStream(inStream, "linkHref");
		}
		catch (MalformedURLException ex) {
			Log.e("INSIDE generateImagePreview()", ex.getMessage());
		}
		catch (IOException ex) {
			Log.e("INSIDE generateImagePreview()", ex.getMessage());
		}
		catch (Exception ex) {
			Log.e("INSIDE generateImagePreview()", ex.getMessage());
		}*/
    	
    	return imagePreview;
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
				final FrameLayout contentFrame = (FrameLayout) view.findViewById(R.id.userStream_listItem_frameContent);
				final TextView titleTextView = (TextView) view.findViewById(R.id.userStream_listItem_title);
				final ImageView sourceImageView = (ImageView) view.findViewById(R.id.userStream_listItem_sourceIcon);
				final ImageView typeImageView = (ImageView) view.findViewById(R.id.userStream_listItem_typeIcon);
				
				if (titleTextView != null) {
					titleTextView.setText(postItem.getTitle());
				}
				if (contentFrame != null) {
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
							contentFrame.addView(previewImageView, layoutParams);
						}
						else {
							previewImageView.setImageBitmap(postItem.getImagePreview());
						}
					}
					else { // check whether there is a reusable view, if so remove it from contentFrame
						if (previewImageView != null)
							contentFrame.removeView(previewImageView);
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
							contentFrame.addView(contentTextView, layoutParams);
						}
						else {
							contentTextView.setText(postItem.getContent());
						}
					}
					else { // check whether there is a reusable view, if so remove it from contentFrame
						if (contentTextView != null)
							contentFrame.removeView(contentTextView);
					}
				}
				
				if (sourceImageView != null) {
					switch (postItem.getSource()) {
					case PostItem.SOURCE_STORYTLR: sourceImageView.setImageResource(R.drawable.storytlr); break;
					case PostItem.SOURCE_TWITTER: sourceImageView.setImageResource(R.drawable.twitter); break;
					case PostItem.SOURCE_PICASA: sourceImageView.setImageResource(R.drawable.picasa); break;
					default: sourceImageView.setImageBitmap(null);
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
					default: typeImageView.setImageBitmap(null);
					}
				}
			}
			
			return view;
		}
    }
}