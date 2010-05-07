package net.betavinechronicle.client.android;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomLink;
import org.onesocialweb.model.atom.AtomText;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

public class EntryDetail extends Activity {

	static final int PREVIEW_BLOCK_WEB_ID = 2999;
	static final int PREVIEW_BLOCK_TEXT_ID = 2998;
	static final int PREVIEW_BLOCK_BUTTON_ID = 2997;
	
	private Porter mPorter;
	private long execTime;
	private MediaPlayer mediaPlayer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_detail);
		execTime = System.currentTimeMillis();
		mPorter = (Porter) this.getApplication();
		int targetIndex = this.getIntent().getIntExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
		if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1))
			this.displayEntryDetail(targetIndex);		
	}
	
	private void displayEntryDetail(int index) {
		final ImageView sourceImageView = (ImageView) findViewById(R.id.entry_detail_sourceIcon);
		final ImageView typeImageView = (ImageView) findViewById(R.id.entry_detail_typeIcon);
		final TextView titleTextView = (TextView) findViewById(R.id.entry_detail_title);
		final TextView updatedTextView = (TextView) findViewById(R.id.entry_detail_updated);
		final Button editPostButton = (Button) findViewById(R.id.entry_detail_editPost);
		final Button addCommentButton = (Button) findViewById(R.id.entry_detail_addComment);
		final Button deletePostButton = (Button) findViewById(R.id.entry_detail_deletePost);
		this.clearPreviewBlock();	 
		final int targetIndex = index;
		final List<PostItem> postItems = mPorter.getPostItems();
		final PostItem postItem = postItems.get(targetIndex);
		final AtomEntry atomEntry = mPorter.getEntryById(postItem.getEntryId());
		execTime = System.currentTimeMillis() - execTime;
		Log.d("INSIDE displayEntryDetail()", "Initialization: " + execTime + " ms");
		Log.d("INSIDE displayEntryDetail()", "Initialization completed");
		execTime = System.currentTimeMillis();
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
		
		if (titleTextView != null) {
			titleTextView.setText(GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getTitle()));
		}
		
		if (updatedTextView != null) {
			// TODO: format date
			/*updatedTextView.setText(DateFormat.getMediumDateFormat(
					getApplicationContext()).format(atomEntry.getUpdated()));*/
		}
		
		if (postItem.isTypeRecognized()) {
			ActivityEntry activityEntry = (ActivityEntry) atomEntry;
			ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex());
			AtomText title = object.getTitle();
			AtomContent content = object.getContent(); // null if the object doesn't have it
			AtomText summary = object.getSummary(); // null if the object doesn't have it
			List<AtomLink> links = null;
			String relAlternate = null;
			String relEnclosure = null;
			String previewData = "";
			WebView webPreview = null;
			TextView textPreview = null;
			switch(postItem.getType()) {
			case PostItem.TYPE_STATUS:			
				if (object.hasContent()) {
					if (!content.hasSrc()) {
						if (content.getType().equals("html")) {
							webPreview = this.insertWebPreview(PREVIEW_BLOCK_WEB_ID);
							webPreview.loadData(StringEscapeUtils.unescapeHtml(content.getValue()), 
									"text/html", "utf-8");
						}
						else if (content.getType().equals("text")) {
							textPreview = this.insertTextPreview(PREVIEW_BLOCK_TEXT_ID);
							textPreview.setText("\"" + content.getValue() + "\"");
						}
					}
					else {
						// TODO: contains SRC attribute, so the content value is empty
						// need to handle the object referenced by SRC
					}
				}
				break;						
			case PostItem.TYPE_BLOG:
				if (object.hasContent()) {
					if (!content.hasSrc()) {
						if (content.getType().equals("html")) {
							webPreview = this.insertWebPreview(PREVIEW_BLOCK_WEB_ID);
							previewData = StringEscapeUtils.unescapeHtml(content.getValue());
							if (title.getType().equals("html"))
								previewData = StringEscapeUtils.unescapeHtml(title.getValue()) 
													+ "<br/>" + previewData;
							else
								previewData = "<h4>" + title.getValue() + "</h4>" + previewData;
							webPreview.loadData(previewData, "text/html", "utf-8");
						}
						else if (content.getType().equals("text")) {
							textPreview = this.insertTextPreview(PREVIEW_BLOCK_TEXT_ID);
							previewData = "\n\n" + content.getValue();
							if (title.getType().equals("html"))
								previewData = StringEscapeUtils.unescapeHtml(
										GeneralMethods.ifHtmlRemoveMarkups(title)) + previewData;
							else
								previewData = title.getValue() + previewData;
							textPreview.setText(previewData);
						}
					}
					else {
						// TODO: contains SRC attribute, so the content value is empty
						// need to handle the object referenced by SRC
					}
				}
				break;					
			case PostItem.TYPE_LINK:
				// TODO: take the link not from the content, but from the link rel=related
				if (object.hasContent()) {
					if (!content.hasSrc()) {
						if (content.getType().equals("html")) {
							webPreview = this.insertWebPreview(PREVIEW_BLOCK_WEB_ID);
							previewData = StringEscapeUtils.unescapeHtml(content.getValue());
							if (title.getType().equals("html"))
								previewData = StringEscapeUtils.unescapeHtml(title.getValue()) 
													+ "<br/>" + previewData;
							else
								previewData = "<h4>" + title.getValue() + "</h4>" + previewData;
							if (object.hasSummary()) {
								if (summary.getType().equals("html"))
									previewData += "<br/>" + StringEscapeUtils.unescapeHtml(summary.getValue());
								else
									previewData += "<p>" + summary.getValue() + "</p>";
							}
							webPreview.loadData(previewData, "text/html", "utf-8");
						}
						else if (content.getType().equals("text")) {
							textPreview = this.insertTextPreview(PREVIEW_BLOCK_TEXT_ID);
							previewData = "\n\n" + content.getValue();
							if (title.getType().equals("html"))
								previewData = StringEscapeUtils.unescapeHtml(
										GeneralMethods.ifHtmlRemoveMarkups(title)) + previewData;
							else
								previewData = title.getValue() + previewData;
							if (object.hasSummary()) {
								if (summary.getType().equals("html"))
									previewData += "\n\n" + StringEscapeUtils.unescapeHtml(
											GeneralMethods.ifHtmlRemoveMarkups(title));
								else
									previewData += "\n\n" + summary.getValue();
							}
							textPreview.setText(previewData);
						}
					}
					else {
						// TODO: contains SRC attribute, so the content value is empty
						// need to handle the object referenced by SRC
					}
				}
				break;
			case PostItem.TYPE_PICTURE:
				// <h4>title</h4><a href="link alternate"><img src="link enclosure"/></a><p>summary</p>
				webPreview = this.insertWebPreview(PREVIEW_BLOCK_WEB_ID);
				previewData = "";
				if (title.getType().equals("html"))
					previewData = StringEscapeUtils.unescapeHtml(title.getValue());
				else
					previewData = "<h4>" + title.getValue() + "</h4>";
				links = object.getLinks();
				relAlternate = null;
				relEnclosure = null;
				for (AtomLink link : links) {
					if (link.hasRel() && link.hasHref()) {
						if (link.getRel().equals(AtomLink.REL_ALTERNATE))
							relAlternate = link.getHref();
						else if (link.getRel().equals(AtomLink.REL_ENCLOSURE))
							relEnclosure = link.getHref();
					}
				}
				if (relAlternate != null && relEnclosure != null)
					previewData += "<a href=\"" + relAlternate + "\"><img src=\"" + relEnclosure
									+ "\"/></a>";
				else if (relEnclosure != null)
					previewData += "<img src=\"" + relEnclosure + "\"/>";
				if (object.hasSummary()) {
					if (summary.getType().equals("html"))
						previewData += "<br/>" + StringEscapeUtils.unescapeHtml(summary.getValue());
					else
						previewData += "<p>" + summary.getValue() + "</p>";
				}
				webPreview.loadData(previewData, "text/html", "utf-8");				
				break;
			case PostItem.TYPE_AUDIO:
				mediaPlayer = new MediaPlayer();
				textPreview = this.insertTextPreview(PREVIEW_BLOCK_TEXT_ID);
				previewData = StringEscapeUtils.unescapeHtml(
						GeneralMethods.ifHtmlRemoveMarkups(title));
				if (object.hasSummary()) {
					if (summary.getType().equals("html"))
						previewData += "\n\n" + StringEscapeUtils.unescapeHtml(
								GeneralMethods.ifHtmlRemoveMarkups(title));
					else
						previewData += "\n\n" + summary.getValue();
				}
				textPreview.setText(previewData);
				final Button listenButton = this.insertButton(PREVIEW_BLOCK_BUTTON_ID, "Listen");
				links = object.getLinks();
				relEnclosure = null;
				for (AtomLink link : links) {
					if (link.hasRel() && link.hasHref()) {
						if (link.getRel().equals(AtomLink.REL_ENCLOSURE))
							relEnclosure = link.getHref();
					}
				}
				// debug
				relEnclosure = "http://dl.dropbox.com/u/4412045/Supercell%20-%20Kimi%20no%20Shiranai%20Monogatari.mp3";
				if (relEnclosure != null) {
					try {
						mediaPlayer.setDataSource(relEnclosure);
					}
					catch (IOException ex) {
						Log.e("EntryDetail-Setting data source to media player", ex.getMessage());
						mediaPlayer = null;
					}
					catch (Exception ex) {
						Log.e("EntryDetail-Setting data source to media player", ex.getMessage());
						mediaPlayer = null;
					}
					
					listenButton.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							if (mediaPlayer != null) {
								if (!mediaPlayer.isPlaying()) {
									try {
										mediaPlayer.prepare();
										mediaPlayer.start();
									}
									catch (IOException ex) {
										Log.e("EntryDetail-Setting data source to media player", ex.getMessage());
									}
									listenButton.setText("Stop listening");
								}
								else {
									mediaPlayer.stop();
									listenButton.setText("Listen");
								}
							}
						}
					});
				}
				break;
			case PostItem.TYPE_VIDEO:
				webPreview = this.insertWebPreview(PREVIEW_BLOCK_WEB_ID);
				previewData = "";
				if (title.getType().equals("html"))
					previewData = StringEscapeUtils.unescapeHtml(title.getValue());
				else
					previewData = "<h4>" + title.getValue() + "</h4>";
				if (object.hasSummary()) {
					if (summary.getType().equals("html"))
						previewData += "<br/>" + StringEscapeUtils.unescapeHtml(summary.getValue()) + "<br/>";
					else
						previewData += "<p>" + summary.getValue() + "</p>";
				}
				if (object.hasContent()) {
					if (!content.hasSrc()) {
						previewData += StringEscapeUtils.unescapeHtml(content.getValue());
					}
					else {
						// TODO: contains SRC attribute, so the content value is empty
						// need to handle the object referenced by SRC
					}
				}
				break;				
			}
		}
		else { // unrecognized object-type of activity:object
			
		}
		
		execTime = System.currentTimeMillis() - execTime;
		Log.d("INSIDE displayEntryDetail()", "Set UI: " + execTime + " ms");
		Log.d("INSIDE displayEntryDetail()", "Set UI completed");

		editPostButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				
				switch(postItem.getType()) {
				case PostItem.TYPE_STATUS:
					intent.setClass(getApplicationContext(), 
							net.betavinechronicle.client.android.PostStatus.class);
					break;						
				case PostItem.TYPE_BLOG:
					intent.setClass(getApplicationContext(), 
							net.betavinechronicle.client.android.PostBlog.class);
					break;					
				case PostItem.TYPE_LINK:
					intent.setClass(getApplicationContext(), 
							net.betavinechronicle.client.android.ShareLink.class);
					break;
				case PostItem.TYPE_PICTURE:
					intent.setClass(getApplicationContext(), 
							net.betavinechronicle.client.android.SharePicture.class);
					break;
				case PostItem.TYPE_AUDIO:
					intent.setClass(getApplicationContext(), 
							net.betavinechronicle.client.android.ShareAudio.class);
					break;
				case PostItem.TYPE_VIDEO:
					intent.setClass(getApplicationContext(), 
							net.betavinechronicle.client.android.ShareVideo.class);
					break;
				default: // a regular atom-entry (not atom-activity)
					intent.setClass(getApplicationContext(), 
							net.betavinechronicle.client.android.PostBlog.class);
				}
				
				intent.putExtra(Porter.EXTRA_KEY_REQUESTCODE, Porter.REQUESTCODE_EDIT_ENTRY);
				intent.putExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, targetIndex);
				startActivityForResult(intent, Porter.REQUESTCODE_EDIT_ENTRY);
			}
		});
		
		addCommentButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO: not implemented yet :)
				
			}
		});
		
		deletePostButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(EntryDetail.this)
				.setMessage("Are you sure?")
				.setTitle("Deleting Entry")
				.setCancelable(true)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String targetUri = null;
						List<AtomLink> links = atomEntry.getLinks();
						for (AtomLink link : links) {
							if (link.hasRel() && link.hasHref())
								if (link.getRel().equals(AtomLink.REL_EDIT))
									targetUri = link.getHref();
						}
						if (targetUri == null) {
							// TODO: show warning to user that the entry isn't allowed to be edited
							return;
						}
						Intent data = new Intent();
						data.putExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, targetIndex);
						data.putExtra(Porter.EXTRA_KEY_TARGET_URI, targetUri);
						data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Deleting Status");
						setResult(Porter.RESULTCODE_DELETING_ENTRY, data);
						finish();
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do nothing
					}
				})
				.show();
			}
		});
	}
	
	private WebView insertWebPreview(int id) {
		final LinearLayout previewBlock = (LinearLayout) findViewById(R.id.entry_detail_preview_block);
		final WebView webPreview = new WebView(getApplicationContext());
		webPreview.setId(id);
		WebClient previewWebClient = new WebClient();
		webPreview.setWebViewClient(previewWebClient);
		webPreview.getSettings().setJavaScriptEnabled(true);
		webPreview.getSettings().setPluginsEnabled(true);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		previewBlock.addView(webPreview, layoutParams);
		
		return webPreview;
	}
	
	private TextView insertTextPreview(int id) {
		final LinearLayout previewBlock = (LinearLayout) findViewById(R.id.entry_detail_preview_block);
		final TextView textPreview = new TextView(getApplicationContext());
		textPreview.setId(id);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		previewBlock.addView(textPreview, layoutParams);
		
		return textPreview;
	}
	
	private Button insertButton(int id, String text) {
		final LinearLayout previewBlock = (LinearLayout) findViewById(R.id.entry_detail_preview_block);
		final Button button = new Button(getApplicationContext());
		button.setId(id);
		button.setText(text);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		previewBlock.addView(button, layoutParams);
		return button;
	}
	
	private void clearPreviewBlock() {
		final LinearLayout previewBlock = (LinearLayout) findViewById(R.id.entry_detail_preview_block);
		previewBlock.removeAllViews();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Porter.REQUESTCODE_EDIT_ENTRY) {
			
			switch(resultCode) {
			case Porter.RESULTCODE_EDITING_ENTRY:
				/*int targetIndex = data.getIntExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
				if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1))
					this.displayEntryDetail(targetIndex);*/
				this.setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
				finish();
				break;
			}
		}
	}
	
	private class WebClient extends WebViewClient {
		
		private boolean mIsOverrideUrlLoading = false;
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// TODO Auto-generated method stub
			if (mIsOverrideUrlLoading) {
				view.loadUrl(url);
			}
			else {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
			}

			return true;
		}
		
		/*public void setIsOverrideUrlLoading(boolean isOverrideUrlLoading) {
			mIsOverrideUrlLoading = isOverrideUrlLoading;
		}*/
	}
	
}
