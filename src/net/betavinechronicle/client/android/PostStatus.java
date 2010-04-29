package net.betavinechronicle.client.android;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomFeed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PostStatus extends Activity {

	private Porter mPorter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_status_tabwidget);
		mPorter = (Porter) this.getApplication();
		
		final EditText statusEditText = (EditText) findViewById(R.id.postOrShare_status_status);
		final Button postButton = (Button) findViewById(R.id.postOrShare_status_postButton);
		
		if (this.getIntent().getIntExtra(Porter.EXTRAKEY_REQUESTCODE, 0) 
				== Porter.REQUESTCODE_EDIT_ENTRY) {
			postButton.setText("Confirm Edit");
			this.setTitle("Edit Status - Betavine Chronicle Client");
			
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRAKEY_TARGET_POSTITEM_INDEX, -1);
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final AtomFeed feed = mPorter.getFeed();
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final ActivityEntry entry = (ActivityEntry) feed.getEntries().get(postItem.getEntryIndex());		
				final ActivityObject object = entry.getObjects().get(postItem.getObjectIndex());
				final AtomContent content = object.getContent();
				
				if (content != null) {
					if (!content.hasSrc()) {
						statusEditText.setText(StringEscapeUtils.unescapeHtml(
								GeneralMethods.ifHtmlRemoveMarkups(content)));
					}
					else {
						// TODO: not implemented yet :)
					}
				}
				else {
					statusEditText.setText(StringEscapeUtils.unescapeHtml(
							GeneralMethods.ifHtmlRemoveMarkups(object.getTitle())));
				}
				
				postButton.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						String newStatus = statusEditText.getText().toString();
						// TODO: send edit resource request
						content.setType("text");
						content.setValue(newStatus);
						Intent data = new Intent();
						data.putExtra(Porter.EXTRAKEY_TARGET_POSTITEM_INDEX, targetIndex);
						setResult(Porter.RESULTCODE_ENTRY_EDITED, data);
						finish();
					}
				});
			}
		}
	}
}
