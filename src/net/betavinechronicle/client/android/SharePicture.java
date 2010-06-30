package net.betavinechronicle.client.android;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

public class SharePicture extends Activity {
	
	static final int REQUESTCODE_GET_IMAGE = 999;

	private Bitmap mImageCache = null;
	private Porter mPorter;
	private String mImageFilePath = null;
	private String mImageFileExt = null;
	private ActivityXmlWriter mActivityWriter;	
	private ProgressDialog mProgressDialog = null;
	private String mProgDialogTitle = "";
	private String mAlertMessage = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.share_picture_tabwidget);
		mPorter = (Porter) this.getApplication();
		mActivityWriter = new ActivityXmlWriter();

		final RadioButton byUploadRadio = (RadioButton) findViewById (R.id.postOrShare_picture_byUpload);
		final RadioButton byUrlRadio = (RadioButton) findViewById (R.id.postOrShare_picture_byUrl);
		final Button choosePictureButton = (Button) findViewById (R.id.postOrShare_picture_chooseButton);
		final Button postButton = (Button) findViewById(R.id.postOrShare_picture_postButton);
		
		final ImageView displayPictureImage = (ImageView) findViewById(R.id.postOrShare_picture_display);
		final FrameLayout uploadFrame = (FrameLayout) findViewById (R.id.postOrShare_picture_uploadFrame);
		final EditText urlEditText = (EditText) findViewById (R.id.postOrShare_picture_url);				
		
		final EditText titleEditText = (EditText) findViewById(R.id.postOrShare_picture_title);
		final EditText noteEditText = (EditText) findViewById(R.id.postOrShare_picture_note);
		
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
		
		choosePictureButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent getImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
				getImageIntent.setType("image/*");
				displayPictureImage.setImageBitmap(null);
				startActivityForResult(getImageIntent, REQUESTCODE_GET_IMAGE);
			}
		});
		
		if (this.getIntent().getIntExtra(Porter.EXTRA_KEY_REQUESTCODE, 0)
				== Porter.REQUESTCODE_EDIT_ENTRY) {
			/*
			 * EDIT MODE
			 */
			
			postButton.setText("Confirm Edit");
			this.setTitle("Edit Picture - Betavine Chronicle Client");
			
			// GET THE INDEX OF THE CLICKED ITEM IN THE LIST-VIEW
			final int targetIndex = this.getIntent().getIntExtra(
					Porter.EXTRA_KEY_TARGET_POSTITEM_INDEX, -1);
			
			if (mPorter.hasFeed() && mPorter.hasPostItems() && (targetIndex > -1)) {
				final List<PostItem> postItems = mPorter.getPostItems();
				final PostItem postItem = postItems.get(targetIndex);
				final ActivityEntry entry = (ActivityEntry) mPorter.getEntryById(postItem.getEntryId());								
				final ActivityObject object = entry.getObjects().get(postItem.getObjectIndex());
				
				// DISPLAY THE ORIGINAL TITLE
				titleEditText.setText(mPorter.extractTitleFromObject(object));
				
				this.clearBitmapCache();
				
				/*
				 *  DISPLAY THE PICTURE FROM THE LINK 
				 *  WHICH HAS REL ATTRIBUTE WITH VALUE "PREVIEW"
				 */
				mImageCache = mPorter.generateImagePreview(object);				
				if (mImageCache != null)
					displayPictureImage.setImageBitmap(mImageCache);
				else { 
					// NO IMAGE CAN BE RETRIEVED, THUS SWITCH TO IMAGE-URL MODE
					uploadFrame.setVisibility(View.GONE);
					urlEditText.setVisibility(View.VISIBLE);
					byUploadRadio.setChecked(false);
					byUrlRadio.setChecked(true);
					
					/*
					 *  mPorter.extractHrefFromObject() METHOD IS NOT CALLED TWICE
					 *  INSTEAD, THE LOOP BELOW IS USED
					 *  IN CONSIDERATION OF THE PROCESS EFFICIENCY 
					 */
					List<AtomLink> links = object.getLinks();
					for (AtomLink link : links) {
						if (link.hasRel() && link.hasHref()) {
							if (link.getRel().equals(AtomLink.REL_PREVIEW)) {
								urlEditText.setText(link.getHref());
								break;
							}
							else if (link.getRel().equals(AtomLink.REL_ENCLOSURE))
								urlEditText.setText(link.getHref());
						}
					}
				}
				
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
								mAlertMessage = "This entry and the picture are not allowed to be edited...";
								displayAlert.run();
								return;
							}
						}
						
						if (byUploadRadio.isChecked() && 
								(mImageFilePath != null) && (mImageFileExt != null)) {
							// UPLOAD IMAGE RADIO BUTTON IS CHECKED AND A FILE IS CHOSEN
																			
							if (targetMediaUri == null) {
								// NO URI PROVIDED FOR THE EDIT PROCESS
								mAlertMessage = "This picture is not allowed to be edited...";
								displayAlert.run();
								return;
							}
							
							HttpTasks uploadImageRequest = createUploadImageRequest(targetMediaUri, 
									HttpTasks.HTTP_PUT, targetIndex, newTitle, newNote, "Editing Picture Entry");
														
							uploadImageRequest.getHttpPut().setEntity((new FileEntity(
									new File(mImageFilePath), mImageFileExt)));
							uploadImageRequest.addHeaderToHttpPut("User-Agent", mPorter.getAppName());
					    	uploadImageRequest.addHeaderToHttpPut("Content-Type", mImageFileExt);
					    	uploadImageRequest.addHeaderToHttpPut("Slug", newTitle);
					    	uploadImageRequest.addHeaderToHttpPut("Password", 
				    				mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_PASSWORD, ""));
							
							uploadImageRequest.start();
						}
						else {
							// EITHER URL RADIO BUTTON IS CHECKED OR THE USER DIDN'T CHANGE THE PICTURE
							
							AtomText title = mPorter.getAtomFactory().text("text", newTitle);
			        		AtomText oldObjectTitle = object.getTitle();
							object.setTitle(title);
			        		
			        		AtomText summary = mPorter.getAtomFactory().text("text", newNote);
			        		AtomText oldObjectSummary = object.getSummary();
							AtomText oldEntrySummary = entry.getSummary();
							entry.setSummary(summary);
			        		object.setSummary(summary);
			        		
			        		String xmlEntry = null;
			        		
							if (byUrlRadio.isChecked()) {
								// USING URL RADIO BUTTON IS CHECKED, ASSUME NEW PICTURE HAS PROVIDED
								
								String newPictUrl = StringEscapeUtils.escapeHtml(urlEditText.getText()
										.toString()).trim();
								
								if (newPictUrl.equals("")) {
									mAlertMessage = "Please fill in the URL box.";
									displayAlert.run();
									return;
								}
								
				        		// TODO: detect the image type
								String imageType = "image/*";
				        		
				        		AtomContent content = mPorter.getAtomFactory().content(
				        				null, imageType, newPictUrl);
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
								linkEnclosure.setHref(newPictUrl);
								linkEnclosure.setTitle(newTitle);
								linkEnclosure.setType(imageType);
																
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
										xmlEntry, targetUri, "Editing Picture Entry");
							if (getParent() == null) 
								setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
							else
								getParent().setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
							
							object.setTitle(oldObjectTitle);
							entry.setSummary(oldEntrySummary);
							object.setSummary(oldObjectSummary);
							
							finish();
						}
					}  // END OF onClick
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
			 *  e.g. BY "SHARE" MENU OF GALLERY APPLICATION
			 *  THUS, SHOW THE PICTURE BASED ON THE "SHARE"-ED PICTURE
			 */
			Uri sharedPictUriString = this.getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
			if (sharedPictUriString != null) {
				byUrlRadio.setVisibility(View.GONE);
				this.processSelectedPicture(sharedPictUriString);
			}
			
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
						// UPLOAD IMAGE RADIO BUTTON IS CHECKED
																
						if ((mImageFilePath == null) && (mImageFileExt == null)) {
							mAlertMessage = "Please select a picture.";
							displayAlert.run();
							return;
						}
						
						HttpTasks uploadImageRequest = createUploadImageRequest(
								mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_ENDPOINT, "") 
								+ "?username=" + username, 
								HttpTasks.HTTP_POST, -1, newTitle, newNote, "Sharing new Picture");
												
						uploadImageRequest.getHttpPost().setEntity((new FileEntity(
								new File(mImageFilePath), mImageFileExt)));
						uploadImageRequest.addHeaderToHttpPost("User-Agent", mPorter.getAppName());
				    	uploadImageRequest.addHeaderToHttpPost("Content-Type", mImageFileExt);
				    	uploadImageRequest.addHeaderToHttpPost("Slug", newTitle);
				    	uploadImageRequest.addHeaderToHttpPost("Password", 
			    				mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_PASSWORD, ""));
						
						uploadImageRequest.start();
					}
					else {
						// USING URL RADIO BUTTON IS CHECKED
						
						final String newPictUrl = StringEscapeUtils.escapeHtml(urlEditText.getText()
								.toString()).trim();
						
						if (newPictUrl.equals("")) {
							mAlertMessage = "Please fill in the URL box.";
							displayAlert.run();
							return;
						}
						
						Date currentDateTime = Calendar.getInstance().getTime();
												
						AtomText title = mPorter.getAtomFactory().text("text", newTitle);
						
						// TODO: detect the image type
						String imageType = "image/*";
						
						AtomContent content = mPorter.getAtomFactory().content(null, 
								imageType, newPictUrl);
						
						ActivityObject object = mPorter.constructObject(currentDateTime, title, 
								ActivityObject.PHOTO, content);
						
						title = mPorter.getAtomFactory().text("text", 
								username + " shared a picture");
						
						ActivityEntry entry = mPorter.constructEntry(currentDateTime, title, 
								content, mPorter.getActivityFactory().verb(ActivityVerb.SHARE), object);
						
						AtomLink link = mPorter.getAtomFactory().link(newPictUrl, AtomLink.REL_ENCLOSURE, 
								newTitle, imageType);
						object.addLink(link);
						entry.addLink(link);
						
						AtomText summary = mPorter.getAtomFactory().text("text", newNote);
						object.setSummary(summary);
						entry.setSummary(summary);
						
						// INCREMENT NEXT PICTURE ENTRY'S ID
						mPorter.incrementPreferenceInt(ActivityObject.PHOTO);
						
						/*
						 *  SET AN INTENT IN ACTIVITY'S RESULT 
						 *  AND 
						 *  GO BACK TO UserStream ACTIVITY WITH THE RESULT
						 */
						Intent data = new Intent();
						data.putExtra(Porter.EXTRA_KEY_XML_CONVERTED_ENTRY, mActivityWriter.toXml(entry));
						data.putExtra(Porter.EXTRA_KEY_DIALOG_TITLE, "Sharing new Picture");
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
	
	private HttpTasks createUploadImageRequest(String targetMediaUri, int httpMode, final int targetItemIndex,
			final String newTitle, final String newNote, final String dialogTitle) {

    	mProgressDialog = ProgressDialog.show(SharePicture.this, "Uploading Picture", 
				"Please wait...");
		return new HttpTasks(targetMediaUri, httpMode) {
		
				@Override
				public void run() {
					// EXECUTE THE HTTP POST REQUEST TO UPLOAD THE PICTURE FILE FIRST
					super.run();
					
					if (this.hasHttpResponse()) {
	    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
	    				if (statusCode != 201 && statusCode != 200) {
	    					try {
	    						String responseText = GeneralMethods.getRawStringFromResponse(
										this.getHttpResponse().getEntity().getContent());
	    						Log.e("SharePicture >> Upload failed, wrong status code(" + statusCode
										+ ")", responseText);
	    						mAlertMessage = "Failed to upload the picture...";
	    						runOnUiThread(displayAlert);
							} 
	    					catch (IllegalStateException ex) {
	    						Log.e("SharePicture >> Upload failed, wrong status code(" + statusCode
										+ "), getRawStringFromResponse() failed.", 
										ex.getMessage());
							}
	    					catch (IOException ex) {
	    						Log.e("SharePicture >> Upload failed, wrong status code(" + statusCode
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
				        	Log.e("SharePicture >> After upload, parsing response entry failed.", 
				        			ex.getMessage());
				        }
				        catch(IOException ex) {
				        	Log.e("SharePicture >> After upload, parsing response entry failed.", 
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
				        		AtomText oldObjectTitle = activityEntry.getTitle();
				        		object.setTitle(title);
				        		
				        		title = mPorter.getAtomFactory().text("text", 
										username + " shared a picture");
				        		AtomText oldActEntryTitle = activityEntry.getTitle();
				        		activityEntry.setTitle(title);
				        		
				        		AtomText summary = mPorter.getAtomFactory().text("text", newNote);
				        		AtomText oldActEntrySummary = activityEntry.getSummary();
				        		AtomText oldObjectSummary = object.getSummary();
				        		activityEntry.setSummary(summary);
				        		object.setSummary(summary);
				        		
								// INCREMENT NEXT PICTURE ENTRY'S ID
								mPorter.incrementPreferenceInt(ActivityObject.PHOTO);
				        		
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
				        	
				        	if (getIntent().getAction() != null) {
					        	if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
									/*
									 *  THIS ACTIVITY WAS CALLED BY ANOTHER APPLICATION'S ACTIVITY
									 *  THUS, SEND THE PUT REQUEST TO EDIT THE METADATA 
									 *  IN MEDIA LINK ENTRY INDEPENDENTLY (W/O GOING BACK TO UserStream)
									 */									
									HttpTasks httpPutRequest = new HttpTasks(targetUri, HttpTasks.HTTP_PUT) {
										
										@Override
										public void run() {
											super.run();
											if (this.hasHttpResponse()) {
												int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
							    				if (statusCode == 200) 
							    					runOnUiThread(notifySuccess);
							    				else {
							    					try {
							    						String responseText = GeneralMethods.getRawStringFromResponse(
																this.getHttpResponse().getEntity().getContent());
							    						Log.e("SharePicture >> Updating entry metadata failed, wrong status code(" + statusCode
																+ ")", responseText);
							    						mAlertMessage = "Failed to edit the Picture entry's metadata...";
							    						runOnUiThread(displayAlert);
													} 
							    					catch (IllegalStateException ex) {
							    						Log.e("SharePicture >> Updating entry metadata failed, wrong status code(" + statusCode
																+ "), getRawStringFromResponse() failed.", 
																ex.getMessage());
													}
							    					catch (IOException ex) {
							    						Log.e("SharePicture >> Updating entry metadata failed, wrong status code(" + statusCode
																+ "), getRawStringFromResponse() failed.", 
																ex.getMessage());
													}
							    				}
											}		
										}
									};
									
									try {
										httpPutRequest.getHttpPut().setEntity(
												new StringEntity(xmlEntry));
										httpPutRequest.addHeaderToHttpPut("User-Agent", mPorter.getAppName());
										httpPutRequest.addHeaderToHttpPut("Content-Type", "application/atom+xml;type=entry");
										httpPutRequest.addHeaderToHttpPut("Password", 
							    				mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_PASSWORD, ""));
								    	mProgDialogTitle = "Posting the entry";
										runOnUiThread(mChangeProgDialogTitle);
								    	httpPutRequest.start(); 
							    	}
									catch (UnsupportedEncodingException ex) {
										Log.e("SharePicture >> Constructing StringEntity failed", ex.getMessage());
							    		mAlertMessage = "Failed to edit the Picture entry's metadata...";
							    		runOnUiThread(displayAlert);
									}
								}
				        	}
							else {
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
				        }
				        else {
				        	Log.e("SharePicture >> After upload, reading the entry response.", 
				        			"Response entry is null.");
				        	mAlertMessage = "Failed to edit the Picture entry's metadata...";
				        	runOnUiThread(displayAlert);
				        }
	    			}
	    			else {
	    				Log.e("SharePicture >> Uploading, checking HTTP response.", 
								(this.getExceptionMessage() != null)? 
										this.getExceptionMessage():"HTTP response is null.");
	    				mAlertMessage = "Failed to edit the Picture entry's metadata...";
			        	runOnUiThread(displayAlert);
	    			}
	    				
					mProgressDialog.dismiss();
				}
				
			};
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == REQUESTCODE_GET_IMAGE) {			
			if (resultCode == Activity.RESULT_OK) {
				this.processSelectedPicture(data.getData());
			}
			else if (resultCode == Activity.RESULT_CANCELED) {
				final ImageView displayPictureImage = (ImageView) findViewById(R.id.postOrShare_picture_display);				
				displayPictureImage.setImageBitmap(mImageCache);
			}
		}
	}
	
	private void processSelectedPicture(Uri sharedPictUriString) {
		final ImageView displayPictureImage = (ImageView) findViewById(R.id.postOrShare_picture_display);
		final TextView displayPictureFileName = (TextView) findViewById(R.id.postOrShare_picture_displayFileName);
		String[] imageInfo = this.getFileInfoFromUri(sharedPictUriString);
		mImageFilePath = imageInfo[0];
		mImageFileExt = imageInfo[1];
		displayPictureFileName.setText(imageInfo[2]);
		this.clearBitmapCache();
		mImageCache = BitmapFactory.decodeFile(mImageFilePath);
		displayPictureImage.setImageBitmap(mImageCache);
	}
	
	private String[] getFileInfoFromUri (Uri contentUri) {
		String[] fileInfo = new String[3];
		
		String[] projection = { MediaStore.Images.Media.DATA, // THE REAL PATH NAME OF THE IMAGE FILE
								MediaStore.Images.Media.MIME_TYPE, // THE MIME TYPE
								MediaStore.Images.Media.DISPLAY_NAME }; // THE FILE NAME
	    Cursor cursor = managedQuery(contentUri, projection, null, null, null);
	    
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);  
	    cursor.moveToFirst();  
	    fileInfo[0] = cursor.getString(column_index);
	    
	    column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);  
	    cursor.moveToFirst();  
	    fileInfo[1] = cursor.getString(column_index);
	    
	    column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);  
	    cursor.moveToFirst();  
	    fileInfo[2] = cursor.getString(column_index);
	    
	    return fileInfo;
	}
	
	// PREVENTS OUT OF MEMORY PROBLEM
	private void clearBitmapCache() {
		if (mImageCache != null && mImageCache.isRecycled() == false)
			mImageCache.recycle();
	}

    private Runnable mChangeProgDialogTitle = new Runnable() {
    	@Override
    	public void run() {
    		mProgressDialog.setTitle(mProgDialogTitle);
    	};
    };
    
    private Runnable notifySuccess = new Runnable() {

		@Override
		public void run() {
			if (mProgressDialog != null) mProgressDialog.dismiss();
			new AlertDialog.Builder(SharePicture.this)
			.setTitle("Sharing new Picture")
			.setMessage("The Picture entry has been posted successfully.")
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
			new AlertDialog.Builder(SharePicture.this)
			.setTitle("Sharing new Picture")
			.setMessage(mAlertMessage)
			.setCancelable(true)
			.setPositiveButton("OK", null)
			.show();
		}
    	
    };
}
