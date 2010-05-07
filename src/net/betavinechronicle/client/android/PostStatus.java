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
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PostStatus extends Activity {

	private Porter mPorter;
	private ActivityXmlWriter mActivityWriter;	
	
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
			postButton.setText("Confirm Edit");
			this.setTitle("Edit Status - Betavine Chronicle Client");
			
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final ActivityEntry entry = (ActivityEntry) mPorter.getEntryById(postItem.getEntryId());		
				final ActivityObject object = entry.getObjects().get(postItem.getObjectIndex());
				AtomContent content = object.getContent();
				
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
						String newStatus = statusEditText.getText().toString().trim();
						if (newStatus.equals("")) {
							// TODO: display alert dialog
							return;
						}
						String targetUri = null;
						List<AtomLink> links = entry.getLinks();
						for (AtomLink link : links) {
							if (link.hasRel() && link.hasHref())
								if (link.getRel().equals(AtomLink.REL_EDIT))
									targetUri = link.getHref();
						}
						if (targetUri == null) {
							// TODO: show warning to user that the entry isn't allowed to be edited
							return;
						}
						AtomContent content = object.getContent();
						content.setType("text");
						content.setValue(StringEscapeUtils.escapeHtml(newStatus));
						entry.setContent(content);
						// TODO: updated?
						/*Date currentDateTime = Calendar.getInstance().getTime();
						object.setUpdated(currentDateTime);
						entry.setUpdated(currentDateTime);	*/
						/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
						debugTextView.setText(mActivityWriter.toXml(entry));*/
						Intent data = new Intent();
						data.putExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, targetIndex);
						data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
						data.putExtra(Porter.EXTRA_KEY_TARGET_URI, targetUri);
						data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Editing Status");
						setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
						finish();
						// TODO: if editing failed then the entry should be rolled back
					}
				});
			}
		}
		else {
			postButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					String newStatus = statusEditText.getText().toString().trim();
					if (newStatus.equals("")) {
						// TODO: display alert dialog
						return;
					}
					String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, 
							"Anonymous");
					Date currentDateTime = Calendar.getInstance().getTime();
					AtomText title = mPorter.getAtomFactory().text("text", 
							"Status update on " + DateFormat.getMediumDateFormat(
									getApplicationContext()).format(currentDateTime)
									);
					AtomContent content = mPorter.getAtomFactory().content();
					content.setType("text");
					content.setValue(StringEscapeUtils.escapeHtml(newStatus));
					
					ActivityObject object = mPorter.constructObject(currentDateTime, title, 
							ActivityObject.STATUS, content);
					
					title = mPorter.getAtomFactory().text("text", 
							username + " posted a new status update");
					
					ActivityEntry entry = mPorter.constructEntry(currentDateTime, title, 
							content, mPorter.getActivityFactory().verb(ActivityVerb.POST), object);
					
					/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
					debugTextView.setText(mActivityWriter.toXml(entry));*/
					Intent data = new Intent();
					data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
					data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Posting New Status");
					if (getParent() == null) 
						setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					else
						getParent().setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					String preferenceKey = mPorter.getPreferenceKeyFromObjectType(ActivityObject.STATUS);
					mPorter.savePreferenceInt(preferenceKey, 
							mPorter.loadPreferenceInt(preferenceKey, 0) + 1);
					finish();
				}
			});
		}
	}
}
