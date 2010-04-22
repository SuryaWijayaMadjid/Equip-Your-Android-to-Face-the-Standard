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
	
	public PostItem() {
		mSource = 0;
		mType = 0;
	}
	
	public PostItem(String title, String content, Bitmap imagePreview, int source, int type) {
		mTitle = title;
		mContent = content;
		mImagePreview = imagePreview;
		mSource = source;
		mType = type;
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
}
