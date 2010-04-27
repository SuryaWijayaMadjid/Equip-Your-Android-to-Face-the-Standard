package net.betavinechronicle.client.android;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomFeed;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class EntryDetail extends Activity {

	private Porter mPorter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_detail);
		
		mPorter = (Porter) this.getApplication();
		
		final ImageView sourceImageView = (ImageView) findViewById(R.id.entry_detail_sourceIcon);
		final ImageView typeImageView = (ImageView) findViewById(R.id.entry_detail_typeIcon);
		final TextView titleTextView = (TextView) findViewById(R.id.entry_detail_title);
		final TextView publishedTextView = (TextView) findViewById(R.id.entry_detail_published);
		final TextView previewTextView = (TextView) findViewById(R.id.entry_detail_textPreview);
		final WebView previewWebView = (WebView) findViewById(R.id.entry_detail_webPreview);
		WebClient previewWebClient = new WebClient();
		previewWebView.setWebViewClient(previewWebClient);
		previewWebView.getSettings().setJavaScriptEnabled(true);
		previewWebView.getSettings().setPluginsEnabled(true);
		final Button editPostButton = (Button) findViewById(R.id.entry_detail_editPost);
		final Button addCommentButton = (Button) findViewById(R.id.entry_detail_addComment);
		final Button deletePostButton = (Button) findViewById(R.id.entry_detail_deletePost);
		
		final int targetIndex = this.getIntent().getIntExtra(Porter.EXTRAKEY_TARGET_POSTITEM_INDEX, -1);
		if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
			final AtomFeed feed = mPorter.getFeed();
			final List<PostItem> postItems = mPorter.getPostItems();
			final PostItem postItem = postItems.get(targetIndex);
			final AtomEntry atomEntry = feed.getEntries().get(postItem.getEntryIndex());
			 
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
			
			if (publishedTextView != null) {
				publishedTextView.setText(atomEntry.getPublished().toString());
			}
			
			AtomContent content = atomEntry.getContent();
			if (!content.hasSrc()) {
				if (content.getType().equals("html")) {
					previewWebView.setVisibility(View.VISIBLE);					
					previewWebView.loadData(StringEscapeUtils.unescapeHtml(content.getValue()), 
							"text/html", "utf-8");
				}
				else if (content.getType().equals("text")) {
					previewTextView.setVisibility(View.VISIBLE);
					previewTextView.setText(StringEscapeUtils.unescapeHtml(content.getValue()));
				}
			}
			else {
				previewWebView.setVisibility(View.VISIBLE);
				previewWebClient.setIsOverrideUrlLoading(true);
				previewWebView.loadUrl(content.getSrc());
			}
			

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
					default:
						intent.setClass(getApplicationContext(), 
								net.betavinechronicle.client.android.PostBlog.class);
					}
					
					intent.putExtra(Porter.EXTRAKEY_REQUESTCODE, Porter.REQUESTCODE_EDIT_ENTRY);
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
					initiateEntryDeletion();
				}
			});
		}
	}
	
	private void initiateEntryDeletion() {
		new AlertDialog.Builder(this)
		.setMessage("Are you sure?")
		.setTitle("Deleting Entry")
		.setCancelable(true)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				sendDeleteRequest();
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
	
	private void sendDeleteRequest() {
		// TODO: send delete resource request
		setResult(Porter.RESULTCODE_ENTRY_DELETED);
		finish();
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
		
		public void setIsOverrideUrlLoading(boolean isOverrideUrlLoading) {
			mIsOverrideUrlLoading = isOverrideUrlLoading;
		}
	}
	
}
