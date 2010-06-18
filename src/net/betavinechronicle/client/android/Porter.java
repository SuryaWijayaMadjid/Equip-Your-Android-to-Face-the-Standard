package net.betavinechronicle.client.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityFactory;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.activity.ActivityVerb;
import org.onesocialweb.model.activity.DefaultActivityFactory;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomFactory;
import org.onesocialweb.model.atom.AtomFeed;
import org.onesocialweb.model.atom.AtomLink;
import org.onesocialweb.model.atom.AtomPerson;
import org.onesocialweb.model.atom.AtomText;
import org.onesocialweb.model.atom.DefaultAtomFactory;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

public class Porter extends Application {
	
	static final int RESULTCODE_SUBACTIVITY_CHAINCLOSE = 99;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_PROFILE = 98;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE = 97;
	static final int RESULTCODE_DELETING_ENTRY = 96;
	static final int RESULTCODE_EDITING_ENTRY = 95;
	static final int RESULTCODE_POSTING_ENTRY = 94;
	static final int RESULTCODE_POSTING_ENTRY_WITH_MEDIA = 93;
	
	static final int REQUESTCODE_VIEW_ENTRY = 79;
	static final int REQUESTCODE_EDIT_ENTRY = 78;
	static final int REQUESTCODE_DELETE_ENTRY = 77;
	static final int REQUESTCODE_POST_OR_SHARE = 76;
	static final int REQUESTCODE_EDIT_PROFILE = 75;
	static final int REQUESTCODE_PROMPT_USERNAME = 74;
	
	static final String EXTRA_KEY_REQUESTCODE = "request-code";
	static final String EXTRA_KEY_TARGET_POSTITEM_INDEX = "alter-postitem-index";
	static final String EXTRA_KEY_XML_CONVERTED_ENTRY = "xml-converted-entry";
	static final String EXTRA_KEY_DIALOG_TITLE = "dialog-title";
	static final String EXTRA_KEY_TARGET_URI = "target-uri";
	static final String EXTRA_KEY_MEDIA_TYPE = "media-type";
	
	static final String PREFERENCES_NAME = "AMC-preferences";
	static final String PREFERENCES_KEY_USERNAME = "username";
	static final String PREFERENCES_KEY_POST_COUNT = "post-count";

	private AtomFeed mFeed;
	private List<PostItem> mPostItems;
	private boolean mIsRefreshNeeded = false;
	private ActivityFactory mActivityFactory = new DefaultActivityFactory();
	private AtomFactory mAtomFactory = new DefaultAtomFactory();
	
	public AtomEntry getEntryById(String id) {
		List<AtomEntry> entries = mFeed.getEntries();
		for (AtomEntry entry : entries) {
			if (entry.getId().equals(id)) return entry;
		}
		
		return null;
	}
	
	public void replaceEntry(AtomEntry newEntry, String oldEntryId) {
		List<AtomEntry> entries = mFeed.getEntries();
		int count = 0;
		for (AtomEntry entry : entries) {
			if (entry.getId().equals(oldEntryId)) break;
			count++;
		}
		mFeed.getEntries().remove(count);
		mFeed.getEntries().add(count, newEntry);
	}
	
	public ActivityFactory getActivityFactory() {
		return mActivityFactory;
	}
	
	public AtomFactory getAtomFactory() {
		return mAtomFactory;
	}
	
	public AtomFeed getFeed() {
		return mFeed;
	}
	
	public List<PostItem> getPostItems() {
		return mPostItems;
	}
	
	public boolean isRefreshNeeded() {
		return mIsRefreshNeeded;
	}
	
	public void setFeed(AtomFeed feed) {
		mFeed = feed;
	}
	
	public void setPostItems(List<PostItem> postItems) {
		mPostItems = postItems;
	}
	
	public void setIsRefreshNeeded(boolean isRefreshNeeded) {
		mIsRefreshNeeded = isRefreshNeeded;
	}
	
	public boolean hasFeed() {
		return (mFeed != null);
	}
	
	public boolean hasPostItems() {
		return (mPostItems != null);
	}
	
	public void savePreferenceInt(String key, int value) {
		SharedPreferences prefs = this.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.putInt(key, value);
		prefsEditor.commit();
	}
	
	public void incrementPreferenceInt(String key) {
		SharedPreferences prefs = this.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit();
		int value = prefs.getInt(key, 0);
		prefsEditor.putInt(key, value);
		prefsEditor.commit();
	}
	
	public void savePreferenceString(String key, String value) {
		SharedPreferences prefs = this.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.putString(key, value);
		prefsEditor.commit();
	}
	
	public int loadPreferenceInt(String key, int defaultValue) {
		SharedPreferences prefs = this.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		int value = prefs.getInt(key, defaultValue);
		
		return value;
	}
	
	public String loadPreferenceString(String key, String defaultValue) {
		SharedPreferences prefs = this.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		String value = prefs.getString(key, defaultValue);
		
		return value;
	}
	
	public ActivityEntry constructEntry(Date currentDateTime, AtomText title, 
			AtomContent content, ActivityVerb verb, ActivityObject object) {
		ActivityEntry entry = mActivityFactory.entry();
		SharedPreferences prefs = this.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		
		int postCount = prefs.getInt(PREFERENCES_KEY_POST_COUNT, 0) + 1;
		entry.setId("tag:net.betavinechronicle.client.android,"
				+ (new SimpleDateFormat("yyyy-MM-dd")).format(currentDateTime)
				+ ":/posts/" + postCount);
		entry.setTitle(title);
		entry.setUpdated(currentDateTime);
		entry.setPublished(currentDateTime);
		
		String username = prefs.getString(PREFERENCES_KEY_USERNAME, "Anonymous");
		List<AtomPerson> authors = new ArrayList<AtomPerson>();
		AtomPerson author = mAtomFactory.person();
		author.setName(username);
		authors.add(author);
		entry.setAuthors(authors);
		
		entry.setContent(content);
		entry.addVerb(verb);
		entry.addObject(object);		
		
		return entry;
	}
	
	public ActivityObject constructObject(Date currentDateTime, AtomText title, 
			String objectType, AtomContent content) {
		ActivityObject object = mActivityFactory.object(objectType);
		SharedPreferences prefs = this.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
		
		int typeCount = prefs.getInt(objectType, 0) + 1; 
		object.setId("tag:net.betavinechronicle.client.android,"
				+ (new SimpleDateFormat("yyyy-MM-dd")).format(currentDateTime)
				+ ":/" + objectType + "/" + typeCount);
		object.setTitle(title);
		object.setUpdated(currentDateTime);
		object.setContent(content);
		
		return object;
	}
	
    public Bitmap generateImagePreview(ActivityObject object) {
    	Bitmap imagePreview = null;
    	if (object.getType().equals(ActivityObject.PHOTO)) {
    		List<AtomLink> links = object.getLinks();
			for (AtomLink link : links) {
				if (link.hasRel() && link.hasHref()) {
					if (link.getRel().equals(AtomLink.REL_PREVIEW)) {
						imagePreview = GeneralMethods.getImageBitmapFromUrlString(link.getHref());
						break;
					}
				}
			}
    	}  
    	
    	return imagePreview;
    }
    
    public Intent prepareIntentForEditing(int targetIndex, String xmlEntry, 
    		String targetUri, String dialogTitle) {
    	Intent intent = new Intent();
    	
    	intent.putExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, targetIndex);
    	intent.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, xmlEntry);
    	intent.putExtra(Porter.EXTRA_KEY_TARGET_URI, targetUri);
    	intent.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, dialogTitle);
    	
    	return intent;
    }
    
    public String extractTitleFromObject(ActivityObject object) {
    	if (object.hasTitle())
			return StringEscapeUtils.unescapeHtml(GeneralMethods.ifHtmlRemoveMarkups(object.getTitle()));
    	
    	return null;
    }
    
    public String extractContentFromObject(ActivityObject object) {
    	if (object.hasContent()) {
    		AtomContent content = object.getContent();
    		if (content.hasSrc()) return null;
			if (!content.getType().equals("text") && !content.getType().equals("html")
					&& !content.getType().equals("xhtml")) return null;
    		return StringEscapeUtils.unescapeHtml(GeneralMethods.ifHtmlRemoveMarkups(object.getContent()));
    	}
    	
    	return null;
    }
    
    public String extractSummaryFromObject(ActivityObject object) {
    	if (object.hasSummary())
			return StringEscapeUtils.unescapeHtml(GeneralMethods.ifHtmlRemoveMarkups(object.getSummary()));
	    	
	    return null;
    }
    
    public String extractHrefFromLinks(List<AtomLink> links, String linkRelValue) {
    	String href = null;
    	
    	for (AtomLink link : links) {
			if (link.hasRel() && link.hasHref())
				if (link.getRel().equals(linkRelValue))
					return StringEscapeUtils.unescapeHtml(link.getHref());
		}
		
    	return href;
    }
}
