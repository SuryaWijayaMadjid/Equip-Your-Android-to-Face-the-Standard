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
import android.widget.Toast;

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
			/*
			 * EDIT MODE
			 * 
			 * PostBlog ACTIVITY HANDLES EDITING ACTIVITY-ENTRY AND REGULAR ATOM-ENTRY			 * 
			 */
			
			postButton.setText("Confirm Edit");
			
			// GET THE INDEX OF THE CLICKED ITEM IN THE LIST-VIEW
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
			
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final AtomEntry atomEntry = mPorter.getEntryById(postItem.getEntryId());
				
				// DISPLAY THE ORIGINAL TITLE AND BLOG CONTENT				
				AtomContent content = null;				
				if (atomEntry instanceof ActivityEntry && postItem.hasObjectIndex()) {
					this.setTitle("Edit Blog - Betavine Chronicle Client");
					ActivityEntry activityEntry = (ActivityEntry) atomEntry;
					ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex());
					
					titleEditText.setText(mPorter.extractTitleFromObject(object));
					
					content = object.getContent();
				}
				else { 
					// THE ENTRY IS A REGULAR ATOM-ENTRY
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
						// IF THE CONTENT HAVE THE SRC VALUE (NO CONTENT VALUE)
					}
				}
				
				// POST-BUTTON CLICKED
				postButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String newTitle = StringEscapeUtils.escapeHtml(titleEditText.getText()
								.toString()).trim();
						String newContent = StringEscapeUtils.escapeHtml(contentEditText.getText()
								.toString()).trim();
						
						if (newTitle.equals("") || newContent.equals("")) {
							showToastMessage("Please fill in the title and content box.");
							return;
						}

						Date currentDateTime = Calendar.getInstance().getTime();
												
						AtomText title = mPorter.getAtomFactory().text("text", newTitle);						
						AtomContent content = mPorter.getAtomFactory().content(newContent, "text", null);
						
						String xmlEntry = "";
						String targetUri = null;
						if (atomEntry instanceof ActivityEntry && postItem.hasObjectIndex()) {
							ActivityEntry activityEntry = (ActivityEntry) atomEntry;
							ActivityObject object = activityEntry.getObjects().get(postItem.getObjectIndex());
							
							object.setTitle(title);
							object.setContent(content);
							activityEntry.setContent(content);
							object.setUpdated(currentDateTime);
							activityEntry.setUpdated(currentDateTime);
							
							targetUri = mPorter.extractHrefFromLinks(object.getLinks(), AtomLink.REL_EDIT);
							xmlEntry = mActivityWriter.toXml(activityEntry);
						}
						else {
							atomEntry.setTitle(title);
							atomEntry.setContent(content);
							atomEntry.setUpdated(currentDateTime);							
							// TODO: atom-entry => activity-entry (object-entry)
						}
						
						if (targetUri == null) { 
							targetUri = mPorter.extractHrefFromLinks(atomEntry.getLinks(), AtomLink.REL_EDIT);
							if (targetUri == null) {
								// NO URI PROVIDED FOR THE EDIT PROCESS
								showToastMessage("This entry is not allowed to be edited...");
								return;
							}
						}
						
						/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
						debugTextView.setText(xmlEntry);*/
					
						setResult(Porter.RESULTCODE_EDITING_ENTRY, 
								mPorter.prepareIntentForEditing(targetIndex, xmlEntry, 
										targetUri, "Editing Blog Entry"));
						finish();
					}
				});
			}
		}
		else {

			/*
			 * POST MODE
			 */
			
			// POST-BUTTON CLICKED
			postButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String newTitle = StringEscapeUtils.escapeHtml(titleEditText.getText()
							.toString()).trim();
					String newContent = StringEscapeUtils.escapeHtml(contentEditText.getText()
							.toString()).trim();
					
					if (newTitle.equals("") || newContent.equals("")) {
						showToastMessage("Please fill in the title and content box.");
						return;
					}
					
					String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, 
							"Anonymous");
					Date currentDateTime = Calendar.getInstance().getTime();
					
					AtomText title = mPorter.getAtomFactory().text("text", newTitle);
					
					AtomContent content = mPorter.getAtomFactory().content(newContent, "text", null);
					
					ActivityObject object = mPorter.constructObject(currentDateTime, title, 
							ActivityObject.ARTICLE, content);
					
					title = mPorter.getAtomFactory().text("text", 
							username + " posted a new blog entry");
					
					ActivityEntry entry = mPorter.constructEntry(currentDateTime, title, 
							content, mPorter.getActivityFactory().verb(ActivityVerb.POST), object);
					
					/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
					debugTextView.setText(mActivityWriter.toXml(entry));*/

					// INCREMENT NEXT BLOG ENTRY'S ID
					mPorter.incrementPreferenceInt(ActivityObject.ARTICLE);
					
					/*
					 *  SET AN INTENT IN ACTIVITY'S RESULT 
					 *  AND 
					 *  GO BACK TO UserStream ACTIVITY WITH THE RESULT
					 */
					Intent data = new Intent();
					data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
					data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Posting new Blog");					
					if (getParent() == null) 
						setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					else
						getParent().setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					finish();
				}
			});
		}
	}
	
	private void showToastMessage(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
}
