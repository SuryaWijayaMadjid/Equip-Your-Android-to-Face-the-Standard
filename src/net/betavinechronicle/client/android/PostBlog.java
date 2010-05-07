package net.betavinechronicle.client.android;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.activity.ActivityVerb;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomLink;
import org.onesocialweb.model.atom.AtomText;
import org.onesocialweb.xml.writer.ActivityXmlWriter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PostBlog extends Activity {

	private Porter mPorter;
	private ActivityXmlWriter mActivityWriter;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_blog_tabwidget);
		mPorter = (Porter) this.getApplication();
		mActivityWriter = new ActivityXmlWriter();
		
		final EditText titleEditText = (EditText) findViewById(R.id.postOrShare_blog_title);
		final EditText contentEditText = (EditText) findViewById(R.id.postOrShare_blog_content);
		final Button postButton = (Button) findViewById(R.id.postOrShare_blog_postButton);
		
		if (this.getIntent().getIntExtra(Porter.EXTRA_KEY_REQUESTCODE, 0)
				== Porter.REQUESTCODE_EDIT_ENTRY) {
			postButton.setText("Confirm Edit");
			
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final AtomEntry atomEntry = mPorter.getEntryById(postItem.getEntryId());
				//titleEditText.setText(GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getTitle()));
				
				AtomContent content = null;
				if (atomEntry instanceof ActivityEntry && postItem.hasObjectIndex()) {
					this.setTitle("Edit Blog - Betavine Chronicle Client");
					ActivityEntry activityEntry = (ActivityEntry) atomEntry;
					ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex());
					if (object.hasTitle())
						titleEditText.setText(StringEscapeUtils.unescapeHtml(
								GeneralMethods.ifHtmlRemoveMarkups(object.getTitle())));
					content = object.getContent();
				}
				else {
					this.setTitle("Edit Entry - Betavine Chronicle Client");
					if (atomEntry.hasTitle())
						titleEditText.setText(StringEscapeUtils.unescapeHtml(
								GeneralMethods.ifHtmlRemoveMarkups(atomEntry.getTitle())));
					content = atomEntry.getContent();
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
						String newTitle = titleEditText.getText().toString().trim();
						String newContent = contentEditText.getText().toString().trim();
						if (newTitle.equals("") || newContent.equals("")) {
							// TODO: display alert dialog
							return;
						}
						// TODO: updated?
						//Date currentDateTime = Calendar.getInstance().getTime();
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
						AtomText title = null;
						AtomContent content = null;
						String xmlEntry = "";
						if (atomEntry instanceof ActivityEntry && postItem.hasObjectIndex()) {
							ActivityEntry activityEntry = (ActivityEntry) atomEntry;
							ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex());
							title = object.getTitle();
							content = object.getContent();
							title.setType("text");
							title.setValue(StringEscapeUtils.escapeHtml(newTitle));
							content.setType("text");
							content.setValue(StringEscapeUtils.escapeHtml(newContent));
							activityEntry.setContent(content);
							/*object.setUpdated(currentDateTime);
							activityEntry.setUpdated(currentDateTime);*/
							xmlEntry = mActivityWriter.toXml(activityEntry);
						}
						else {
							title = atomEntry.getTitle();
							content = atomEntry.getContent();
							//atomEntry.setUpdated(currentDateTime);
							
							// TODO: atom-entry => activity-entry (object-entry)
						}
						/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
						debugTextView.setText(xmlEntry);*/
						Intent data = new Intent();
						data.putExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, targetIndex);
						data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, xmlEntry);
						data.putExtra(Porter.EXTRA_KEY_TARGET_URI, targetUri);
						data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Editing Status");
						setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
						finish();
					}
				});
			}
		}
		else {
			postButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					String newTitle = titleEditText.getText().toString().trim();
					String newContent = contentEditText.getText().toString().trim();
					if (newTitle.equals("") || newContent.equals("")) {
						// TODO: display alert dialog
						return;
					}
					String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, 
							"Anonymous");
					Date currentDateTime = Calendar.getInstance().getTime();
					AtomText title = mPorter.getAtomFactory().text("text", 
							StringEscapeUtils.escapeHtml(newTitle));
					AtomContent content = mPorter.getAtomFactory().content();
					content.setType("text");
					content.setValue(StringEscapeUtils.escapeHtml(newContent));
					
					ActivityObject object = mPorter.constructObject(currentDateTime, title, 
							ActivityObject.ARTICLE, content);
					
					title = mPorter.getAtomFactory().text("text", 
							username + " posted a new blog entry");
					
					ActivityEntry entry = mPorter.constructEntry(currentDateTime, title, 
							content, mPorter.getActivityFactory().verb(ActivityVerb.POST), object);
					
					/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
					debugTextView.setText(mActivityWriter.toXml(entry));*/
					Intent data = new Intent();
					data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
					data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Posting New Blog");
					if (getParent() == null) 
						setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					else
						getParent().setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					String preferenceKey = mPorter.getPreferenceKeyFromObjectType(ActivityObject.ARTICLE);
					mPorter.savePreferenceInt(preferenceKey, 
							mPorter.loadPreferenceInt(preferenceKey, 0) + 1);
					finish();
				}
			});
		}
	}
}
