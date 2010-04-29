package net.betavinechronicle.client.android;

import java.util.List;

import org.onesocialweb.model.atom.AtomFeed;

import android.app.Application;

public class Porter extends Application {
	
	static final int RESULTCODE_SUBACTIVITY_CHAINCLOSE = 99;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_PROFILE = 98;
	static final int RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE = 97;
	static final int RESULTCODE_ENTRY_DELETED = 96;
	static final int RESULTCODE_ENTRY_EDITED = 95;
	
	static final int REQUESTCODE_VIEW_ENTRY = 79;
	static final int REQUESTCODE_EDIT_ENTRY = 78;
	static final int REQUESTCODE_DELETE_ENTRY = 77;
	static final int REQUESTCODE_POST_OR_SHARE = 76;
	static final int REQUESTCODE_EDIT_PROFILE = 75;
	static final int REQUESTCODE_PROMPT_USERNAME = 74;
	
	static final String EXTRAKEY_REQUESTCODE = "request-code";
	static final String EXTRAKEY_TARGET_POSTITEM_INDEX = "alter-postitem-index";

	private AtomFeed mFeed;
	private List<PostItem> mPostItems;
	private boolean mIsRefreshNeeded = false;
	
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
	
}
