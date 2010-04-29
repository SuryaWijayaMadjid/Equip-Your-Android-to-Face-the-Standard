package net.betavinechronicle.client.android;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomFeed;
import org.onesocialweb.model.atom.AtomLink;
import org.onesocialweb.model.atom.AtomText;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ShareLink extends Activity {

	private Porter mPorter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.share_link_tabwidget);
		mPorter = (Porter) this.getApplication();
		
		final EditText titleEditText = (EditText) findViewById(R.id.postOrShare_link_title);
		final EditText linkEditText = (EditText) findViewById(R.id.postOrShare_link_link);
		final EditText noteEditText = (EditText) findViewById(R.id.postOrShare_link_note);
		final Button postButton = (Button) findViewById(R.id.postOrShare_link_postButton);
		
		if (this.getIntent().getIntExtra(Porter.EXTRAKEY_REQUESTCODE, 0)
				== Porter.REQUESTCODE_EDIT_ENTRY) {
			postButton.setText("Confirm Edit");
			this.setTitle("Edit Link - Betavine Chronicle Client");
			
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRAKEY_TARGET_POSTITEM_INDEX, -1);
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final AtomFeed feed = mPorter.getFeed();
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final ActivityEntry entry = (ActivityEntry) feed.getEntries().get(postItem.getEntryIndex());
								
				ActivityObject object = entry.getObjects().get(postItem.getObjectIndex());
				final AtomText title = object.getTitle();
				final AtomContent content = object.getContent();
				titleEditText.setText(StringEscapeUtils.unescapeHtml(
						GeneralMethods.ifHtmlRemoveMarkups(title)));
				final AtomText summary = object.getSummary();
				List<AtomLink> links = object.getLinks();
				AtomLink tempLink = null;				
				for (AtomLink loopLink : links) {
					if (loopLink.hasRel() && loopLink.hasHref())
						if (loopLink.getRel().equals(AtomLink.REL_RELATED)) {
							tempLink = loopLink;
							break;
						}
				}
				final AtomLink link = tempLink;
				linkEditText.setText(link.getHref());
				noteEditText.setText(StringEscapeUtils.unescapeHtml(
						GeneralMethods.ifHtmlRemoveMarkups(summary)));	
			
				postButton.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO: send edit resource request
						String newTitle = titleEditText.getText().toString();
						String newLink = linkEditText.getText().toString();
						String newNote = noteEditText.getText().toString();
						title.setType("text");
						title.setValue(newTitle);
						link.setHref(newLink);
						summary.setType("text");
						summary.setValue(newNote);
						if (content != null) {
							content.setType("html");
							content.setValue("<a href=\"" + newLink + "\">" + newLink + "</a>");
						}
							
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
