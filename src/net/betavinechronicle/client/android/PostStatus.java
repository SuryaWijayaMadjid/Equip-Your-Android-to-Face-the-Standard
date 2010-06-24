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
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PostStatus extends Activity {

	private Porter mPorter;
	private ActivityXmlWriter mActivityWriter;	
	private String mAlertMessage = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_status_tabwidget);
		mPorter = (Porter) this.getApplication();
		mActivityWriter = new ActivityXmlWriter();
		
		final EditText statusEditText = (EditText) findViewById(R.id.postOrShare_status_status);
		final Button postButton = (Button) findViewById(R.id.postOrShare_status_postButton);
		
		if (this.getIntent().getIntExtra(Porter.EXTRA_KEY_REQUESTCODE, 0) 
				== Porter.REQUESTCODE_EDIT_ENTRY) {			
			/*
			 * EDIT MODE
			 */
			
			postButton.setText("Confirm Edit");
			this.setTitle("Edit Status - Betavine Chronicle Client");
			
			// GET THE INDEX OF THE CLICKED ITEM IN THE LIST-VIEW
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
			
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final ActivityEntry entry = (ActivityEntry) mPorter.getEntryById(postItem.getEntryId());		
				final ActivityObject object = entry.getObjects().get(postItem.getObjectIndex());			
				
				// DISPLAY THE ORIGINAL STATUS
				statusEditText.setText(mPorter.extractContentFromObject(object));				
				if (statusEditText.getText().toString().equals(""))
					statusEditText.setText(mPorter.extractTitleFromObject(object));
				
				// POST-BUTTON CLICKED
				postButton.setOnClickListener(new View.OnClickListener() {					
					@Override
					public void onClick(View v) {
						String newStatus = StringEscapeUtils.escapeHtml(statusEditText.getText()
								.toString()).trim();
						
						if (newStatus.equals("")) {
							mAlertMessage = "Please fill in the status box.";
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
						
						AtomContent content = mPorter.getAtomFactory().content(
								newStatus, "text", null);
						AtomContent oldEntryContent = entry.getContent();
						AtomContent oldObjectContent = object.getContent();
						entry.setContent(content);
						object.setContent(content);
						
						Date currentDateTime = Calendar.getInstance().getTime();
						Date oldEntryUpdated = entry.getUpdated();
						Date oldObjectUpdated = object.getUpdated();
						object.setUpdated(currentDateTime);
						entry.setUpdated(currentDateTime);	
						
						setResult(Porter.RESULTCODE_EDITING_ENTRY,
								mPorter.prepareIntentForEditing(targetIndex, 
										mActivityWriter.toXml(entry), targetUri, "Editing Status Entry"));
						
						// ROLL BACK THE CHANGES (JUST IN CASE THE EDIT IS NOT SUCCESSFULL)
						entry.setContent(oldEntryContent);
						object.setContent(oldObjectContent);
						entry.setUpdated(oldEntryUpdated);
						object.setUpdated(oldObjectUpdated);	
						
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
					String newStatus = StringEscapeUtils.escapeHtml(statusEditText.getText()
							.toString()).trim();
					
					if (newStatus.equals("")) {
						mAlertMessage = "Please fill in the status box.";
						displayAlert.run();
						return;
					}
					
					String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, 
							"Anonymous");					
					Date currentDateTime = Calendar.getInstance().getTime();
					
					AtomText title = mPorter.getAtomFactory().text("text", 
							"Status update on " + DateFormat.getMediumDateFormat(
									getApplicationContext()).format(currentDateTime)
									);
					
					AtomContent content = mPorter.getAtomFactory().content(
							newStatus, "text", null);
					
					ActivityObject object = mPorter.constructObject(currentDateTime, title, 
							ActivityObject.STATUS, content);
					
					title = mPorter.getAtomFactory().text("text", 
							username + " posted a new status update");
					
					ActivityEntry entry = mPorter.constructEntry(currentDateTime, title, 
							content, mPorter.getActivityFactory().verb(ActivityVerb.POST), object);
					
					/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
					debugTextView.setText(mActivityWriter.toXml(entry));*/
					
					// INCREMENT NEXT STATUS ENTRY'S ID
					mPorter.incrementPreferenceInt(ActivityObject.STATUS);
					
					/*
					 *  SET AN INTENT IN ACTIVITY'S RESULT 
					 *  AND 
					 *  GO BACK TO UserStream ACTIVITY WITH THE RESULT
					 */
					Intent data = new Intent();
					data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
					data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Posting new Status");					
					if (getParent() == null) 
						setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					else
						getParent().setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					finish();
				}
			});
		}
	}
    
    private Runnable displayAlert = new Runnable() {

		@Override
		public void run() {
			new AlertDialog.Builder(PostStatus.this)
			.setTitle("Posting new Status")
			.setMessage(mAlertMessage)
			.setCancelable(true)
			.setPositiveButton("OK", null)
			.show();
		}
    	
    };
}
