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
			postButton.setText("Confirm Edit");
			this.setTitle("Edit Link - Betavine Chronicle Client");
			
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final ActivityEntry entry = (ActivityEntry) mPorter.getEntryById(postItem.getEntryId());
								
				final ActivityObject object = entry.getObjects().get(postItem.getObjectIndex());
				AtomText title = object.getTitle();
				titleEditText.setText(StringEscapeUtils.unescapeHtml(
						GeneralMethods.ifHtmlRemoveMarkups(title)));
				List<AtomLink> links = object.getLinks();
				for (AtomLink link : links) {
					if (link.hasRel() && link.hasHref())
						if (link.getRel().equals(AtomLink.REL_RELATED)) {
							linkEditText.setText(link.getHref());
							break;
						}
				}
				
				if (object.hasSummary()) {
					AtomText summary = object.getSummary();
					noteEditText.setText(StringEscapeUtils.unescapeHtml(
							GeneralMethods.ifHtmlRemoveMarkups(summary)));	
				}
			
				postButton.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO: send edit resource request
						String newTitle = titleEditText.getText().toString().trim();
						String newLink = linkEditText.getText().toString().trim();
						String newNote = noteEditText.getText().toString().trim();
						if (newTitle.equals("") || newLink.equals("") || newNote.equals("")) {
							// TODO: display alert dialog
							return;
						}
						String targetUri = null;
						AtomLink atomLink = null;
						List<AtomLink> links = entry.getLinks();
						for (AtomLink link : links) {
							if (link.hasRel() && link.hasHref()) {
								if (link.getRel().equals(AtomLink.REL_EDIT))
									targetUri = link.getHref();
								else if (link.getRel().equals(AtomLink.REL_RELATED))
									atomLink = link;
							}
						}
						if (targetUri == null) {
							// TODO: show warning to user that the entry isn't allowed to be edited
							return;
						}
						
						AtomText title = mPorter.getAtomFactory().text();
						title.setType("text");
						title.setValue(StringEscapeUtils.escapeHtml(newTitle));
						object.setTitle(title);
						entry.setTitle(title);
						
						AtomLink objectLink = null;
						links = object.getLinks();
						for (AtomLink link : links) {
							if (link.hasRel() && link.hasHref()) {
								if (link.getRel().equals(AtomLink.REL_RELATED))
									objectLink = link;
							}
						}
						if (objectLink == null) {
							objectLink = mPorter.getAtomFactory().link();
							objectLink.setRel(AtomLink.REL_RELATED);
							object.addLink(objectLink);
						}
						objectLink.setHref(StringEscapeUtils.escapeHtml(newLink));
						if (atomLink == null) {
							atomLink = mPorter.getAtomFactory().link();
							atomLink.setRel(AtomLink.REL_RELATED);
							entry.addLink(atomLink);
						}
						atomLink.setHref(StringEscapeUtils.escapeHtml(newLink));
						
						AtomText summary = mPorter.getAtomFactory().text();
						summary.setType("text");
						summary.setValue(StringEscapeUtils.escapeHtml(newNote));
						object.setSummary(summary);
						entry.setSummary(summary);
						
						AtomContent content = mPorter.getAtomFactory().content();
						content.setType("html");
						content.setValue("<a href=\"" + newLink + "\">" + newLink + "</a>");
						entry.setContent(content);
						object.setContent(content);
													
						Intent data = new Intent();
						data.putExtra(Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, targetIndex);
						data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
						data.putExtra(Porter.EXTRA_KEY_TARGET_URI, targetUri);
						data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Editing Link");
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
					String newLink = linkEditText.getText().toString().trim();
					String newNote = noteEditText.getText().toString().trim();
					if (newTitle.equals("") || newLink.equals("") || newNote.equals("")) {
						// TODO: display alert dialog
						return;
					}
					String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, 
							"Anonymous");
					Date currentDateTime = Calendar.getInstance().getTime();
					AtomLink link = mPorter.getAtomFactory().link();
					link.setRel(AtomLink.REL_RELATED);
					link.setHref(StringEscapeUtils.escapeHtml(newLink));
					AtomText title = mPorter.getAtomFactory().text("text",
							StringEscapeUtils.escapeHtml(newTitle));
					AtomContent content = mPorter.getAtomFactory().content();
					content.setType("html");
					content.setValue(StringEscapeUtils.escapeHtml(
							"<a href=\"" + newLink + "\">" + newLink + "</a>"));
					
					ActivityObject object = mPorter.constructObject(currentDateTime, title, 
							ActivityObject.BOOKMARK, content);
					AtomText summary = mPorter.getAtomFactory().text("text", 
							StringEscapeUtils.escapeHtml(newNote));
					object.setSummary(summary);
					object.addLink(link);
					
					title = mPorter.getAtomFactory().text("text", 
							username + " shared a link");
					
					ActivityEntry entry = mPorter.constructEntry(currentDateTime, title, 
							content, mPorter.getActivityFactory().verb(ActivityVerb.SHARE), object);
					entry.addLink(link);
					entry.setSummary(summary);
					
					/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
					debugTextView.setText(mActivityWriter.toXml(entry));*/
					Intent data = new Intent();
					data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
					data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Sharing a Link");
					if (getParent() == null) 
						setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					else
						getParent().setResult(Porter.RESULTCODE_POSTING_ENTRY, data);
					String preferenceKey = mPorter.getPreferenceKeyFromObjectType(ActivityObject.BOOKMARK);
					mPorter.savePreferenceInt(preferenceKey, 
							mPorter.loadPreferenceInt(preferenceKey, 0) + 1);
					finish();
				}
			});
		}
	}
}
