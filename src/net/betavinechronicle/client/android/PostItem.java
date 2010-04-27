package net.betavinechronicle.client.android;

import android.graphics.Bitmap;

public class PostItem {

	static final int SOURCE_STORYTLR = 1;
	static final int SOURCE_TWITTER = 2;
	static final int SOURCE_PICASA = 3;
	static final int SOURCE_YOUTUBE = 4;
	
	static final int TYPE_STATUS = -1;
	static final int TYPE_BLOG = -2;
	static final int TYPE_LINK = -3;
	static final int TYPE_PICTURE = -4;
	static final int TYPE_AUDIO = -5;
	static final int TYPE_VIDEO = -6;
	
	private String mTitle;
	private String mContent;
	private Bitmap mImagePreview;
	private int mSource;
	private int mType;
	private int mEntryIndex;
	private int mObjectIndex;
	
	public PostItem() {
		mSource = 0;
		mType = 0;
		mEntryIndex = -1;
		mObjectIndex = -1;
	}
	
	public PostItem(String title, String content, Bitmap imagePreview, int source, 
			int type, int entryIndex, int objectIndex) {
		mTitle = title;
		mContent = content;
		mImagePreview = imagePreview;
		mSource = source;
		mType = type;
		mEntryIndex = entryIndex;
		mObjectIndex = objectIndex;
	}
	
	public boolean hasImagePreview() {
		return (mImagePreview != null);
	}
	
	public boolean hasTitle() {
		return (mTitle != null);
	}
	
	public boolean hasContent() {
		return (mContent != null);
	}
	
	public boolean hasEntryIndex() {
		return (mEntryIndex > -1);
	}
	
	public boolean hasObjectIndex() {
		return (mObjectIndex > -1);
	}
	
	public void setTitle(String title) {
		mTitle = title;
	}
	
	public void setContent(String content) {
		mContent = content;
	}
	
	public void setSource(int source) {
		mSource = source;
	}
	
	public void setType(int type) {
		mType = type;
	}
	
	public void setEntryIndex(int entryIndex) {
		mEntryIndex = entryIndex;
	}
	
	public void setObjectIndex(int objectIndex) {
		mObjectIndex = objectIndex;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getContent() {
		return mContent;
	}
	
	public Bitmap getImagePreview() {
		return mImagePreview;
	}
	
	public int getSource() {
		return mSource;
	}
	
	public int getType() {
		return mType;
	}
	
	public int getEntryIndex() {
		return mEntryIndex;
	}
	
	public int getObjectIndex() {
		return mObjectIndex;
	}
	
	public int getSourceIconResId() {
		switch (mSource) {
		case PostItem.SOURCE_STORYTLR: return R.drawable.storytlr;
		case PostItem.SOURCE_TWITTER: return R.drawable.twitter;
		case PostItem.SOURCE_PICASA: return R.drawable.picasa;
		default: return -1;
		}
	}
	
	public int getTypeIconResId() {
		switch (mType) {
		case PostItem.TYPE_STATUS: return -1;
		case PostItem.TYPE_BLOG: return -1;
		case PostItem.TYPE_LINK: return -1;
		case PostItem.TYPE_PICTURE: return -1;
		case PostItem.TYPE_AUDIO: return -1;
		case PostItem.TYPE_VIDEO: return -1;
		default: return -1;
		}
	}
}
