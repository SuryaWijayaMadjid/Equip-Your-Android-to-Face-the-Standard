package net.betavinechronicle.client.android;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomFeed;
import org.onesocialweb.model.atom.AtomText;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PostBlog extends Activity {

	private Porter mPorter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_blog_tabwidget);
		mPorter = (Porter) this.getApplication();
		
		final EditText titleEditText = (EditText) findViewById(R.id.postOrShare_blog_title);
		final EditText contentEditText = (EditText) findViewById(R.id.postOrShare_blog_content);
		final Button postButton = (Button) findViewById(R.id.postOrShare_blog_postButton);
		
		if (this.getIntent().getIntExtra(Porter.EXTRAKEY_REQUESTCODE, 0)
				== Porter.REQUESTCODE_EDIT_ENTRY) {
			postButton.setText("Confirm Edit");
			
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRAKEY_TARGET_POSTITEM_INDEX, -1);
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final AtomFeed feed = mPorter.getFeed();
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final AtomEntry atomEntry = feed.getEntries().get(postItem.getEntryIndex());
				//titleEditText.setText(GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getTitle()));
				
				AtomContent content = null;
				if (atomEntry instanceof ActivityEntry && postItem.hasObjectIndex()) {
					this.setTitle("Edit Blog - Betavine Chronicle Client");
					ActivityEntry activityEntry = (ActivityEntry) atomEntry;
					ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex());
					titleEditText.setText(StringEscapeUtils.unescapeHtml(
							GeneralMethods.ifHtmlRemoveMarkups(object.getTitle())));
					content = object.getContent();
				}
				else {
					this.setTitle("Edit Entry - Betavine Chronicle Client");
					content = atomEntry.getContent();
					titleEditText.setText(StringEscapeUtils.unescapeHtml(
							GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getTitle())));
				}
				
				if (content != null) {
					if (!content.hasSrc()) {
						contentEditText.setText(StringEscapeUtils.unescapeHtml(
								GeneralMethods.ifHtmlRemoveMarkups(content)));
					}
					else {
						// TODO: not implemented yet :)
					}
				}
				
				postButton.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						String newTitle = titleEditText.getText().toString();
						String newContent = contentEditText.getText().toString();
						
						// TODO: send edit resource request
						
						AtomText title = null;
						AtomContent content = null;
						if (atomEntry instanceof ActivityEntry && postItem.hasObjectIndex()) {
							ActivityEntry activityEntry = (ActivityEntry) atomEntry;
							ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex());
							title = object.getTitle();
							content = object.getContent();
						}
						else {
							title = atomEntry.getTitle();
							content = atomEntry.getContent();
						}
						title.setType("text");
						title.setValue(newTitle);
						content.setType("text");
						content.setValue(newContent);
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
