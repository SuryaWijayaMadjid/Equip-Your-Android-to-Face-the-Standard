package net.betavinechronicle.client.android;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.activity.ActivityVerb;
import org.onesocialweb.model.atom.AtomContent;
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

public class ShareLink extends Activity {

	private Porter mPorter;
	private ActivityXmlWriter mActivityWriter;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.share_link_tabwidget);
		mPorter = (Porter) this.getApplication();
		mActivityWriter = new ActivityXmlWriter();
		
		final EditText titleEditText = (EditText) findViewById(R.id.postOrShare_link_title);
		final EditText linkEditText = (EditText) findViewById(R.id.postOrShare_link_link);
		final EditText noteEditText = (EditText) findViewById(R.id.postOrShare_link_note);
		final Button postButton = (Button) findViewById(R.id.postOrShare_link_postButton);
		
		if (this.getIntent().getIntExtra(Porter.EXTRA_KEY_REQUESTCODE, 0)
				== Porter.REQUESTCODE_EDIT_ENTRY) {			
			/*
			 *  EDIT MODE
			 */
			
			postButton.setText("Confirm Edit");
			this.setTitle("Edit Link - Betavine Chronicle Client");
			
			// GET THE INDEX OF THE CLICKED ITEM IN THE LIST-VIEW
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
			
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final ActivityEntry entry = (ActivityEntry) mPorter.getEntryById(postItem.getEntryId());								
				final ActivityObject object = entry.getObjects().get(postItem.getObjectIndex());
				
				// DISPLAY THE ORIGINAL TITLE, LINK, AND NOTE
				titleEditText.setText(mPorter.extractTitleFromObject(object));				
				linkEditText.setText(mPorter.extractHrefFromLinks(object.getLinks(), AtomLink.REL_RELATED));
				noteEditText.setText(mPorter.extractSummaryFromObject(object));
			
				// POST-BUTTON CLICKED
				postButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						
						String newTitle = StringEscapeUtils.escapeHtml(titleEditText.getText()
								.toString()).trim();
						String newLink = StringEscapeUtils.escapeHtml(linkEditText.getText()
								.toString()).trim();
						String newNote = StringEscapeUtils.escapeHtml(noteEditText.getText()
								.toString()).trim();
						
						if (newTitle.equals("") || newLink.equals("") || newNote.equals("")) {
							showToastMessage("Please fill in the title, link, and note box.");
							return;
						}
						
						String targetUri = mPorter.extractHrefFromLinks(object.getLinks(), AtomLink.REL_EDIT);
						
						if (targetUri == null) { 
							targetUri = mPorter.extractHrefFromLinks(entry.getLinks(), AtomLink.REL_EDIT);
							if (targetUri == null) {
								// NO URI PROVIDED FOR THE EDIT PROCESS
								showToastMessage("This entry is not allowed to be edited...");
								return;
							}
						}
						
						AtomText title = mPorter.getAtomFactory().text("text", newTitle);
						object.setTitle(title);
						
						/*  
						 *  GET THE LINK WHICH HAS REL ATTRIBUTE WITH VALUE "RELATED"
						 *  THAT REPRESENTS THE SHARED BOOKMARK
						 */
						AtomLink linkRelated = null;
						List<AtomLink> links = object.getLinks();
						for (AtomLink link : links) {
							if (link.hasRel() && link.hasHref()) {
								if (link.getRel().equals(AtomLink.REL_RELATED))
									linkRelated = link;
							}
						}
						
						// NO LINK WITH REL VALUE OF "RELATED" FOUND
						if (linkRelated == null) {
							linkRelated = mPorter.getAtomFactory().link();
							linkRelated.setRel(AtomLink.REL_RELATED);
							object.addLink(linkRelated);
						}
						
						linkRelated.setHref(newLink);
						linkRelated.setTitle(newTitle);
						
						AtomText summary = mPorter.getAtomFactory().text("text", newNote);
						object.setSummary(summary);
						entry.setSummary(summary);
						
						AtomContent content = mPorter.getAtomFactory().content(
								"<a href=\"" + newLink + "\">" + newLink + "</a>", "html", null);
						entry.setContent(content);
						object.setContent(content);
													
						setResult(Porter.RESULTCODE_EDITING_ENTRY, 
								mPorter.prepareIntentForEditing(targetIndex, 
										mActivityWriter.toXml(entry), targetUri, "Editing Link Entry"));
						finish();
					}
				});			
			}
		}
		else {
			
			/*
			 *  POST MODE
			 */
			
			/*
			 *  IN CASE THIS ACTIVITY IS CALLED BY ANOTHER APPLICATION'S ACTIVITY
			 *  
			 *  e.g. BY "SHARE LINK" MENU OF BROWSER'S BOOKMARK
			 *  THUS, FILL THE LINK BOX VALUE BASED ON THE BOOKMARK  
			 */
			linkEditText.setText(this.getIntent().getCharSequenceExtra("android.intent.extra.TEXT"));
			
			// POST-BUTTON CLICKED
			postButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String newTitle = StringEscapeUtils.escapeHtml(titleEditText.getText()
							.toString()).trim();
					String newLink = StringEscapeUtils.escapeHtml(linkEditText.getText()
							.toString()).trim();
					String newNote = StringEscapeUtils.escapeHtml(noteEditText.getText()
							.toString()).trim();
					
					if (newTitle.equals("") || newLink.equals("") || newNote.equals("")) {
						showToastMessage("Please fill in the title, link, and note box.");
						return;
					}
					
					String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, 
							"Anonymous");
					Date currentDateTime = Calendar.getInstance().getTime();
					
					AtomText title = mPorter.getAtomFactory().text("text", newTitle);
					
					AtomContent content = mPorter.getAtomFactory().content(
							"<a href=\"" + newLink + "\">" + newLink + "</a>", "html", null);	
					
					ActivityObject object = mPorter.constructObject(currentDateTime, title, 
							ActivityObject.BOOKMARK, content);
					
					title = mPorter.getAtomFactory().text("text", 
							username + " shared a link");
					
					ActivityEntry entry = mPorter.constructEntry(currentDateTime, title, 
							content, mPorter.getActivityFactory().verb(ActivityVerb.SHARE), object);
					
					
					AtomText summary = mPorter.getAtomFactory().text("text", newNote);
					object.setSummary(summary);
					entry.setSummary(summary);
					
					AtomLink link = mPorter.getAtomFactory().link(newLink, 
							AtomLink.REL_RELATED, newTitle, null);
					object.addLink(link);
					entry.addLink(link);
					
					/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
					debugTextView.setText(mActivityWriter.toXml(entry));*/
					
					// INCREMENT NEXT LINK ENTRY'S ID
					mPorter.incrementPreferenceInt(ActivityObject.BOOKMARK);
					
					/*
					 *  SET AN INTENT IN ACTIVITY'S RESULT 
					 *  AND 
					 *  GO BACK TO UserStream ACTIVITY WITH THE RESULT
					 */
					Intent data = new Intent();
					data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
					data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Sharing new Link");
					if (getParent() == null) 
						setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					else
						getParent().setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					finish();
					
					// TODO: publish the entry directly 
					// (if this activity was not started by UserStream activity)
				}
			});			
		}
	}
	
	private void showToastMessage(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
}
