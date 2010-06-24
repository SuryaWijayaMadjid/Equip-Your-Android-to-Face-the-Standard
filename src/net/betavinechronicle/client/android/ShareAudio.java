package net.betavinechronicle.client.android;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.entity.FileEntity;
import org.onesocialweb.model.activity.ActivityEntry;
import org.onesocialweb.model.activity.ActivityObject;
import org.onesocialweb.model.activity.ActivityVerb;
import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomEntry;
import org.onesocialweb.model.atom.AtomLink;
import org.onesocialweb.model.atom.AtomText;
import org.onesocialweb.xml.writer.ActivityXmlWriter;
import org.onesocialweb.xml.xpp.imp.DefaultXppActivityReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

public class ShareAudio extends Activity {

	static final int REQUESTCODE_GET_AUDIO = 888;
	
	private Porter mPorter;
	private String mAudioFilePath = null;
	private String mAudioFileExt = null;
	private ActivityXmlWriter mActivityWriter;	
	private ProgressDialog mProgressDialog = null;
	private String mAlertMessage = "";
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.share_audio_tabwidget);
		mPorter = (Porter) this.getApplication();
		mActivityWriter = new ActivityXmlWriter();
		
		final RadioButton byUploadRadio = (RadioButton) findViewById (R.id.postOrShare_audio_byUpload);
		final RadioButton byUrlRadio = (RadioButton) findViewById (R.id.postOrShare_audio_byUrl);
		final Button chooseAudioButton = (Button) findViewById (R.id.postOrShare_audio_chooseButton);
		final Button postButton = (Button) findViewById(R.id.postOrShare_audio_postButton);
		
		final FrameLayout uploadFrame = (FrameLayout) findViewById (R.id.postOrShare_audio_uploadFrame);
		final EditText urlEditText = (EditText) findViewById (R.id.postOrShare_audio_url);
		
		final EditText titleEditText = (EditText) findViewById(R.id.postOrShare_audio_title);
		final EditText noteEditText = (EditText) findViewById(R.id.postOrShare_audio_note);
		
		byUrlRadio.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				uploadFrame.setVisibility(View.GONE);
				urlEditText.setVisibility(View.VISIBLE);
			}
		});
		
		byUploadRadio.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				uploadFrame.setVisibility(View.VISIBLE);
				urlEditText.setVisibility(View.GONE);
			}
		});
		
		chooseAudioButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent getAudioIntent = new Intent(Intent.ACTION_GET_CONTENT);
				getAudioIntent.setType("audio/mp3");
				startActivityForResult(getAudioIntent, REQUESTCODE_GET_AUDIO);
			}
		});
		
		if (this.getIntent().getIntExtra(Porter.EXTRA_KEY_REQUESTCODE, 0)
				== Porter.REQUESTCODE_EDIT_ENTRY) {
			/*
			 * EDIT MODE
			 */
			
			postButton.setText("Confirm Edit");
			this.setTitle("Edit Audio - Betavine Chronicle Client");
			
			// GET THE INDEX OF THE CLICKED ITEM IN THE LIST-VIEW
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
			
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final ActivityEntry entry = (ActivityEntry) mPorter.getEntryById(postItem.getEntryId());								
				final ActivityObject object = entry.getObjects().get(postItem.getObjectIndex());
				final TextView displayAudioFileName = (TextView) findViewById(R.id.postOrShare_audio_displayFileName);
				
				// DISPLAY THE ORIGINAL TITLE AND FILE NAME
				titleEditText.setText(mPorter.extractTitleFromObject(object));
				displayAudioFileName.setText(titleEditText.getText().toString());
				
				// TODO: detect audio file name
				
				// DISPLAY THE ORIGINAL NOTE
				noteEditText.setText(mPorter.extractSummaryFromObject(object));
				
				// POST-BUTTON CLICKED
				postButton.setOnClickListener(new View.OnClickListener() {					
					@Override
					public void onClick(View v) {
						
						String newTitle = StringEscapeUtils.escapeHtml(titleEditText.getText()
								.toString()).trim();
						String newNote = StringEscapeUtils.escapeHtml(noteEditText.getText()
								.toString()).trim();

						if (newTitle.equals("") || newNote.equals("")) {
							mAlertMessage = "Please fill in the title and the note box.";
							displayAlert.run();
							return;
						}
						
						String targetUri = null;
						String targetMediaUri = null;
						/*
						 *  mPorter.extractHrefFromObject() METHOD IS NOT CALLED TWICE
						 *  INSTEAD, THE LOOP BELOW IS USED
						 *  IN CONSIDERATION OF THE PROCESS EFFICIENCY 
						 */
						List<AtomLink> links = entry.getLinks();
						for (AtomLink link : links) {
							if (link.hasRel() && link.hasHref()) {
								if (link.getRel().equals(AtomLink.REL_EDIT))
									targetUri = link.getHref();
								else if (link.getRel().equals(AtomLink.REL_EDIT_MEDIA))
									targetMediaUri = link.getHref();
							}
						}
						
						if (targetUri == null && targetMediaUri == null) {
							links = object.getLinks();
							for (AtomLink link : links) {
								if (link.hasRel() && link.hasHref()) {
									if (link.getRel().equals(AtomLink.REL_EDIT))
										targetUri = link.getHref();
									else if (link.getRel().equals(AtomLink.REL_EDIT_MEDIA))
										targetMediaUri = link.getHref();
								}
							}
							
							if (targetUri == null && targetMediaUri == null) {
								// NO URI PROVIDED FOR THE EDIT PROCESS
								mAlertMessage = "This entry and the audio are not allowed to be edited...";
								displayAlert.run();
								return;
							}
						}
						
						if (byUploadRadio.isChecked() && 
								(mAudioFilePath != null) && (mAudioFileExt != null)) {
							// UPLOAD AUDIO RADIO BUTTON IS CHECKED AND A FILE IS CHOSEN
																			
							if (targetMediaUri == null) {
								// NO URI PROVIDED FOR THE EDIT PROCESS
								mAlertMessage = "This audio is not allowed to be edited...";
								displayAlert.run();
								return;
							}
							
							HttpTasks uploadAudioRequest = createUploadAudioRequest(targetMediaUri, 
									HttpTasks.HTTP_PUT, targetIndex, newTitle, newNote, "Editing Audio Entry");
							
							uploadAudioRequest.getHttpPut().setEntity((new FileEntity(
									new File(mAudioFilePath), mAudioFileExt)));
							uploadAudioRequest.addHeaderToHttpPut("User-Agent", mPorter.getAppName());
					    	uploadAudioRequest.addHeaderToHttpPut("Content-Type", mAudioFileExt);
					    	uploadAudioRequest.addHeaderToHttpPut("Slug", newTitle);
					    	uploadAudioRequest.addHeaderToHttpPut("Password", 
				    				mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_PASSWORD, ""));
							
							uploadAudioRequest.start();
						}
						else {
							// EITHER URL RADIO BUTTON IS CHECKED OR THE USER DIDN'T CHANGE THE AUDIO
							
							AtomText title = mPorter.getAtomFactory().text("text", newTitle);
			        		AtomText oldObjectTitle = object.getTitle();
			        		object.setTitle(title);
			        		
			        		AtomText summary = mPorter.getAtomFactory().text("text", newNote);
			        		AtomText oldObjectSummary = object.getSummary();
							AtomText oldEntrySummary = entry.getSummary();
							object.setSummary(summary);
							
							String xmlEntry = null;
							
							if (byUrlRadio.isChecked()) {
								// USING URL RADIO BUTTON IS CHECKED, ASSUME NEW AUDIO HAS PROVIDED
								
								String newAudioUrl = StringEscapeUtils.escapeHtml(urlEditText.getText()
										.toString()).trim();
								
								if (newAudioUrl.equals("")) {
									mAlertMessage = "Please fill in the URL box.";
									displayAlert.run();
									return;
								}
								
				        		// TODO: detect the audio type
								String audioType = "audio/*";
				        		
				        		AtomContent content = mPorter.getAtomFactory().content(
				        				null, audioType, newAudioUrl);
				        		AtomContent oldEntryContent = entry.getContent();
				        		entry.setContent(content);
							
								AtomLink linkEnclosure = null;
								AtomLink oldLinkEnclosure = null;
								linkEnclosure = oldLinkEnclosure = mPorter.getLinkByRelValue(
										object.getLinks(), AtomLink.REL_ENCLOSURE);
								if (linkEnclosure == null) {
									linkEnclosure = mPorter.getAtomFactory().link();
									linkEnclosure.setRel(AtomLink.REL_ENCLOSURE);
									object.addLink(linkEnclosure);
								}
								
								String oldHrefLinkEnclosure = linkEnclosure.getHref();
								String oldTitleLinkEnclosure = linkEnclosure.getTitle();
								String oldTypeLinkEnclosure = linkEnclosure.getType();
								linkEnclosure.setHref(newAudioUrl);
								linkEnclosure.setTitle(newTitle);
								linkEnclosure.setType(audioType);
																
								xmlEntry = mActivityWriter.toXml(entry);
								
								entry.setContent(oldEntryContent);
								if (oldLinkEnclosure == null)
									object.removeLink(linkEnclosure);
								else {
									linkEnclosure.setHref(oldHrefLinkEnclosure);
									linkEnclosure.setTitle(oldTitleLinkEnclosure);
									linkEnclosure.setType(oldTypeLinkEnclosure);
								}
							}
							else
								xmlEntry = mActivityWriter.toXml(entry);
							
							Intent data = mPorter.prepareIntentForEditing(targetIndex, 
									xmlEntry, targetUri, "Editing Audio Entry");
							if (getParent() == null) 
								setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
							else
								getParent().setResult(Porter.RESULTCODE_EDITING_ENTRY, data);

							object.setTitle(oldObjectTitle);
							entry.setSummary(oldEntrySummary);
							object.setSummary(oldObjectSummary);
							
							finish();
						}
					} // END OF onClick
				});		
			}
		}
		else {

			/*
			 *  POST MODE
			 */
			
			postButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final String newTitle = StringEscapeUtils.escapeHtml(titleEditText.getText()
							.toString()).trim();
					final String newNote = StringEscapeUtils.escapeHtml(noteEditText.getText()
							.toString()).trim();

					if (newTitle.equals("") || newNote.equals("")) {
						mAlertMessage = "Please fill in the title and the note box.";
						displayAlert.run();
						return;
					}
					
					String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, 
							"nobody");
					
					if (byUploadRadio.isChecked()) {
						// UPLOAD AUDIO RADIO BUTTON IS CHECKED
						
						if ((mAudioFilePath == null) && (mAudioFileExt == null)) {
							mAlertMessage = "Please select an audio file.";
							displayAlert.run();
							return;
						}
						HttpTasks uploadAudioRequest = createUploadAudioRequest(
								mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_ENDPOINT, "")
								+ "?username=" + username, HttpTasks.HTTP_POST, -1, 
								newTitle, newNote, "Sharing new Audio");
						
						uploadAudioRequest.getHttpPost().setEntity((new FileEntity(
								new File(mAudioFilePath), mAudioFileExt)));
						uploadAudioRequest.addHeaderToHttpPost("User-Agent", mPorter.getAppName());
						uploadAudioRequest.addHeaderToHttpPost("Content-Type", mAudioFileExt);
						uploadAudioRequest.addHeaderToHttpPost("Slug", newTitle);
						uploadAudioRequest.addHeaderToHttpPost("Password", 
			    				mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_PASSWORD, ""));
						
						uploadAudioRequest.start();
					}
					else {
						// USING URL RADIO BUTTON IS CHECKED
						
						final String newAudioUrl = StringEscapeUtils.escapeHtml(urlEditText.getText()
								.toString()).trim();
						
						if (newAudioUrl.equals("")) {
							mAlertMessage = "Please fill in the URL box.";
							displayAlert.run();
							return;
						}
						
						Date currentDateTime = Calendar.getInstance().getTime();
						
						AtomText title = mPorter.getAtomFactory().text("text", newTitle);
						
						// TODO: detect the audio type
						String audioType = "audio/mpeg";
						
						AtomContent content = mPorter.getAtomFactory().content(null, 
								audioType, newAudioUrl);
						
						ActivityObject object = mPorter.constructObject(currentDateTime, title, 
								ActivityObject.AUDIO, content);
						
						title = mPorter.getAtomFactory().text("text", 
								username + " shared an audio file");
						
						ActivityEntry entry = mPorter.constructEntry(currentDateTime, title, 
								content, mPorter.getActivityFactory().verb(ActivityVerb.SHARE), object);

						AtomLink link = mPorter.getAtomFactory().link(newAudioUrl, AtomLink.REL_ENCLOSURE, 
								newTitle, audioType);
						object.addLink(link);						
						entry.addLink(link);

						AtomText summary = mPorter.getAtomFactory().text("text", newNote);
						object.setSummary(summary);
						entry.setSummary(summary);
												
						// INCREMENT NEXT LINK ENTRY'S ID
						mPorter.incrementPreferenceInt(ActivityObject.AUDIO);
						
						/*
						 *  SET AN INTENT IN ACTIVITY'S RESULT 
						 *  AND 
						 *  GO BACK TO UserStream ACTIVITY WITH THE RESULT
						 */
						Intent data = new Intent();
						data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
						data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Sharing new Audio");
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
	
	private HttpTasks createUploadAudioRequest(String targetMediaUri, int httpMode, final int targetItemIndex,
			final String newTitle, final String newNote, final String dialogTitle) {
		
		mProgressDialog = ProgressDialog.show(ShareAudio.this, "Uploading Audio file", 
				"Please wait...");
		return new HttpTasks(targetMediaUri, httpMode) {
		
				@Override
				public void run() {
					// EXECUTE THE HTTP REQUEST TO UPLOAD THE AUDIO FILE FIRST
					super.run();
					
					if (this.hasHttpResponse()) {
	    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
	    				if (statusCode != 201 && statusCode != 200) {
	    					try {
	    						String responseText = GeneralMethods.getRawStringFromResponse(
										this.getHttpResponse().getEntity().getContent());
	    						Log.e("ShareAudio >> Upload failed, wrong status code(" + statusCode
										+ ")", responseText);
	    						mAlertMessage = "Failed to upload the audio file...";
	    						runOnUiThread(displayAlert);
							} 
	    					catch (IllegalStateException ex) {
	    						Log.e("ShareAudio >> Upload failed, wrong status code(" + statusCode
										+ "), getRawStringFromResponse() failed.", 
										ex.getMessage());
							}
	    					catch (IOException ex) {
	    						Log.e("ShareAudio >> Upload failed, wrong status code(" + statusCode
										+ "), getRawStringFromResponse() failed.", 
										ex.getMessage());
							}
	    					return;
	    				}
	    				
	    				AtomEntry atomEntry = null;
	    				try {
	    					// GET THE ENTRY RESPONSE AND PARSE IT	    					
							XmlPullParserFactory xppFactory = XmlPullParserFactory.newInstance();
				        	xppFactory.setNamespaceAware(true);
				        	XmlPullParser xpp = xppFactory.newPullParser();
				        	xpp.setInput(this.getHttpResponse().getEntity().getContent(), "UTF-8");
				        	DefaultXppActivityReader xppActivityReader = new DefaultXppActivityReader();
				        	xpp.next();
				        	atomEntry = xppActivityReader.parseEntry(xpp);
				        }
				        catch(XmlPullParserException ex) {
				        	Log.e("ShareAudio >> After upload, parsing response entry failed.", 
				        			ex.getMessage());
				        }
				        catch(IOException ex) {
				        	Log.e("ShareAudio >> After upload, parsing response entry failed.", 
				        			ex.getMessage());
				        }
	    				
				        if (atomEntry != null) {
				        	/*
				        	 *  EDIT THE RETRIEVED ENTRY WITH THE INPUT FROM USER (TITLE & NOTE)
				        	 *  AND
				        	 *  SEND BACK THE EDITED ENTRY THROUGH UserStream ACTIVITY
				        	 */
				        	
				        	String username = mPorter.loadPreferenceString(
				        			Porter.PREFERENCES_KEY_USERNAME, "nobody");
				        	String targetUri = null;
				        	String xmlEntry = null;
				        	
				        	if (atomEntry instanceof ActivityEntry) {
				        		ActivityEntry activityEntry = (ActivityEntry) atomEntry;
				        		ActivityObject object = activityEntry.getObjects().get(0);
				        		
				        		targetUri = mPorter.extractHrefFromLinks(activityEntry.getLinks(), 
				        				AtomLink.REL_EDIT);
								if (targetUri == null) {
									mAlertMessage = "This entry is not allowed to be edited...";
									runOnUiThread(displayAlert);
									return;
								}
				        		
				        		AtomText title = mPorter.getAtomFactory().text("text", newTitle);
				        		AtomText oldObjectTitle = object.getTitle();
				        		object.setTitle(title);
				        		
				        		title = mPorter.getAtomFactory().text("text", 
										username + " shared a link");
				        		AtomText oldActEntryTitle = activityEntry.getTitle();
				        		activityEntry.setTitle(title);
				        		
				        		AtomText summary = mPorter.getAtomFactory().text("text", newNote);
				        		AtomText oldActEntrySummary = activityEntry.getSummary();
				        		AtomText oldObjectSummary = object.getSummary();
				        		activityEntry.setSummary(summary);
				        		object.setSummary(summary);
				        		
								// INCREMENT NEXT AUDIO ENTRY'S ID
								mPorter.incrementPreferenceInt(ActivityObject.AUDIO);
				        										
								xmlEntry = mActivityWriter.toXml(activityEntry);
								
								activityEntry.setTitle(oldActEntryTitle);
								object.setTitle(oldObjectTitle);
								activityEntry.setSummary(oldActEntrySummary);
				        		object.setSummary(oldObjectSummary);								
			        		}
			        		else { 
			        			// THE ENTRY IS A REGULAR ATOM-ENTRY
			        			targetUri = mPorter.extractHrefFromLinks(atomEntry.getLinks(), 
				        				AtomLink.REL_EDIT);
								if (targetUri == null) {
									mAlertMessage = "This entry is not allowed to be edited...";
									runOnUiThread(displayAlert);
									return;
								}
								
			        			AtomText title = mPorter.getAtomFactory().text("text", newTitle);
				        		AtomText oldEntryTitle = atomEntry.getTitle();
				        		atomEntry.setTitle(title);
				        		
				        		AtomText summary = mPorter.getAtomFactory().text("text", newNote);
				        		AtomText oldEntrySummary = atomEntry.getSummary();
				        		atomEntry.setSummary(summary);
				        		
				        		// TODO: atom-entry -> activity-entry (object entry)
				        		//xmlEntry = mActivityWriter.toXml(activityEntry);
				        		
				        		atomEntry.setTitle(oldEntryTitle);
				        		atomEntry.setSummary(oldEntrySummary);
			        		} 

							/*
							 *  SET AN INTENT IN ACTIVITY'S RESULT 
							 *  AND 
							 *  GO BACK TO UserStream ACTIVITY WITH THE RESULT
							 */
			        		Intent data = mPorter.prepareIntentForEditing(targetItemIndex, 
									xmlEntry, targetUri, dialogTitle);
							if (getParent() == null) 
								setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
							else
								getParent().setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
							finish();			        		
				        }
				        else {
				        	Log.e("ShareAudio >> After upload, reading the entry response.", 
		        					"Response entry is null.");
				        	mAlertMessage = "Failed to edit the Audio entry's metadata...";
				        	runOnUiThread(displayAlert);
				        }
	    			}
	    			else {
	    				Log.e("ShareAudio >> Uploading, checking HTTP response.", 
								(this.getExceptionMessage() != null)? 
										this.getExceptionMessage():"HTTP response is null.");
	    				mAlertMessage = "Failed to edit the Audio entry's metadata...";
			        	runOnUiThread(displayAlert);
	    			}
					
					mProgressDialog.dismiss();
				}
				
			};
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == REQUESTCODE_GET_AUDIO && resultCode == Activity.RESULT_OK) {
			final TextView displayAudioFileName = (TextView) findViewById(R.id.postOrShare_audio_displayFileName);
			String[] audioInfo = this.getFileInfoFromUri(data.getData());
			mAudioFilePath = audioInfo[0];
			mAudioFileExt = audioInfo[1];
			displayAudioFileName.setText(audioInfo[2]);
		}
	}
	
	private String[] getFileInfoFromUri (Uri contentUri) {
		String[] fileInfo = new String[3];
		
		String[] projection = { MediaStore.Audio.Media.DATA, // THE REAL PATH NAME OF THE AUDIO FILE
								MediaStore.Audio.Media.MIME_TYPE, // THE MIME TYPE
								MediaStore.Audio.Media.DISPLAY_NAME }; // THE FILE NAME 
	    Cursor cursor = managedQuery(contentUri, projection, null, null, null);
	    
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);  
	    cursor.moveToFirst();  
	    fileInfo[0] = cursor.getString(column_index);
	    
	    column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);  
	    cursor.moveToFirst();  
	    fileInfo[1] = cursor.getString(column_index);
	    
	    column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);  
	    cursor.moveToFirst();  
	    fileInfo[2] = cursor.getString(column_index);
	    
	    return fileInfo;
	}	

    private Runnable displayAlert = new Runnable() {

		@Override
		public void run() {
			if (mProgressDialog != null) mProgressDialog.dismiss();
			new AlertDialog.Builder(ShareAudio.this)
			.setTitle("Sharing new Audio")
			.setMessage(mAlertMessage)
			.setCancelable(true)
			.setPositiveButton("OK", null)
			.show();
		}
    	
    };
}
