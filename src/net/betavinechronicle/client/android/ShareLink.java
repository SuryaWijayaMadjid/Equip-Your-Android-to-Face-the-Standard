package net.betavinechronicle.client.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.entity.StringEntity;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.activity.ActivityVerb;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomLink;
import org.onesocialweb.model.atom.AtomText;
import org.onesocialweb.xml.writer.ActivityXmlWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ShareLink extends Activity {

	private Porter mPorter;
	private ActivityXmlWriter mActivityWriter;
	private ProgressDialog mProgressDialog = null;
	private String mAlertMessage = "";
	
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
							mAlertMessage = "Please fill in the title, link, and note box.";
							displayAlert.run();
							return;
						}
						
						String targetUri = mPorter.extractHrefFromLinks(object.getLinks(), AtomLink.REL_EDIT);
						
						if (targetUri == null) { 
							targetUri = mPorter.extractHrefFromLinks(entry.getLinks(), AtomLink.REL_EDIT);
							if (targetUri == null) {
								// NO URI PROVIDED FOR THE EDIT PROCESS
								mAlertMessage = "This entry is not allowed to be edited...";
								displayAlert.run();
								return;
							}
						}
						
						Date currentDateTime = Calendar.getInstance().getTime();
						Date oldEntryUpdated = entry.getUpdated();
						Date oldObjectUpdated = object.getUpdated();
						object.setUpdated(currentDateTime);
						entry.setUpdated(currentDateTime);	
												
						AtomText title = mPorter.getAtomFactory().text("text", newTitle);
						AtomText oldObjectTitle = object.getTitle();
						object.setTitle(title);
						
						/*  
						 *  GET THE LINK WHICH HAS REL ATTRIBUTE WITH VALUE "RELATED"
						 *  THAT REPRESENTS THE SHARED BOOKMARK
						 */
						AtomLink linkRelated = null;
						AtomLink oldLinkRelated = null;
						linkRelated = oldLinkRelated = mPorter.getLinkByRelValue(object.getLinks(), 
								AtomLink.REL_RELATED);
						
						// NO LINK WITH REL VALUE OF "RELATED" FOUND
						if (linkRelated == null) {
							linkRelated = mPorter.getAtomFactory().link();
							linkRelated.setRel(AtomLink.REL_RELATED);
							object.addLink(linkRelated);
						}
						
						String oldHrefLinkRelated = linkRelated.getHref();
						String oldTitleLinkRelated = linkRelated.getTitle();
						linkRelated.setHref(newLink);
						linkRelated.setTitle(newTitle);
						
						AtomText summary = mPorter.getAtomFactory().text("text", newNote);
						AtomText oldObjectSummary = object.getSummary();
						AtomText oldEntrySummary = entry.getSummary();
						object.setSummary(summary);
						entry.setSummary(summary);
						
						AtomContent content = mPorter.getAtomFactory().content(
								"<a href=\"" + newLink + "\">" + newLink + "</a>", "html", null);
						AtomContent oldEntryContent = entry.getContent();
						AtomContent oldObjectContent = object.getContent();
						entry.setContent(content);
						object.setContent(content);
													
						setResult(Porter.RESULTCODE_EDITING_ENTRY, 
								mPorter.prepareIntentForEditing(targetIndex, 
										mActivityWriter.toXml(entry), targetUri, "Editing Link Entry"));
						
						// ROLL BACK THE CHANGES (JUST IN CASE THE EDIT IS NOT SUCCESSFULL)
						entry.setContent(oldEntryContent);
						object.setContent(oldObjectContent);
						entry.setUpdated(oldEntryUpdated);
						object.setUpdated(oldObjectUpdated);	
						object.setTitle(oldObjectTitle);
						entry.setSummary(oldEntrySummary);
						object.setSummary(oldObjectSummary);
						if (oldLinkRelated == null)
							object.removeLink(linkRelated);
						else {
							linkRelated.setHref(oldHrefLinkRelated);
							linkRelated.setTitle(oldTitleLinkRelated);
						}
						
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
			String sharedLink = this.getIntent().getStringExtra(Intent.EXTRA_TEXT);
			if (sharedLink != null)
				linkEditText.setText(sharedLink);
			
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
						mAlertMessage = "Please fill in the title, link, and note box.";
						displayAlert.run();
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
					
					// INCREMENT NEXT LINK ENTRY'S ID
					mPorter.incrementPreferenceInt(ActivityObject.BOOKMARK);
					
					if (getIntent().getAction() != null) {
						if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
							/*
							 *  THIS ACTIVITY WAS CALLED BY ANOTHER APPLICATION'S ACTIVITY
							 *  THUS, SEND THE POST REQUEST INDEPENDENTLY (W/O GOING BACK TO UserStream)
							 */							
							HttpTasks httpPostRequest = new HttpTasks(
									mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_ENDPOINT, "")
									+ "?username="
									+ mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, "nobody"), 
									HttpTasks.HTTP_POST) {
								
								@Override
								public void run() {
									super.run();
									if (this.hasHttpResponse()) {
										int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
					    				if (statusCode == 200 || statusCode == 201) 
					    					runOnUiThread(notifySuccess);
					    				else {
					    					try {
					    						String responseText = GeneralMethods.getRawStringFromResponse(
														this.getHttpResponse().getEntity().getContent());
					    						Log.e("ShareLink >> Wrong status code(" + statusCode
														+ ")", responseText);
					    						mAlertMessage = "Failed to post the Link entry...";
					    						runOnUiThread(displayAlert);
											} 
					    					catch (IllegalStateException ex) {
												Log.e("ShareLink >> Wrong status code(" + statusCode
														+ "), getRawStringFromResponse() failed.", 
														ex.getMessage());
											}
					    					catch (IOException ex) {
					    						Log.e("ShareLink >> Wrong status code(" + statusCode
														+ "), getRawStringFromResponse() failed.", 
														ex.getMessage());
											}
					    				}
									}		
								}
							};
							
							try {
								httpPostRequest.getHttpPost().setEntity(
										new StringEntity(mActivityWriter.toXml(entry)));
						    	httpPostRequest.addHeaderToHttpPost("User-Agent", mPorter.getAppName());
						    	httpPostRequest.addHeaderToHttpPost("Content-Type", "application/atom+xml;type=entry");
					    		httpPostRequest.addHeaderToHttpPost("Password", 
					    				mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_PASSWORD, ""));
						    	mProgressDialog = ProgressDialog.show(ShareLink.this, "Sharing new Link", 
										"Please wait...");
						    	httpPostRequest.start(); 
					    	}
							catch (UnsupportedEncodingException ex) {
					    		Log.e("ShareLink >> Constructing StringEntity failed", ex.getMessage());
					    		mAlertMessage = "Failed to post the Link entry...";
					    		displayAlert.run();
							}
						}
					}
					else {
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
					}
				}
			});			
		}
	}

    private Runnable notifySuccess = new Runnable() {

		@Override
		public void run() {
			if (mProgressDialog != null) mProgressDialog.dismiss();
			new AlertDialog.Builder(ShareLink.this)
			.setTitle("Sharing new Link")
			.setMessage("The Link entry has been posted successfully.")
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.show();
		}
    	
    };
    
    private Runnable displayAlert = new Runnable() {

		@Override
		public void run() {
			if (mProgressDialog != null) mProgressDialog.dismiss();
			new AlertDialog.Builder(ShareLink.this)
			.setTitle("Sharing new Link")
			.setMessage(mAlertMessage)
			.setCancelable(true)
			.setPositiveButton("OK", null)
			.show();
		}
    	
    };
}
