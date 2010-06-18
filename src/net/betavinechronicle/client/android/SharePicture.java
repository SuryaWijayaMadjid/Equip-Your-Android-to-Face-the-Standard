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
import android.widget.Toast;

public class SharePicture extends Activity {
	
	static final int REQUESTCODE_GET_IMAGE = 999;

	private Bitmap mImageCache = null;
	private Porter mPorter;
	private String mImageFilePath = null;
	private String mImageFileExt = null;
	private ActivityXmlWriter mActivityWriter;	
	
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
							showToastMessage("Please fill in the title and the note box.");
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
								showToastMessage("This entry and the picture are not allowed to be edited...");
								return;
							}
						}
						
						if (byUploadRadio.isChecked() && 
								(mImageFilePath != null) && (mImageFileExt != null)) {
							// UPLOAD IMAGE RADIO BUTTON IS CHECKED AND A FILE IS CHOSEN
																			
							if (targetMediaUri == null) {
								// NO URI PROVIDED FOR THE EDIT PROCESS
								showToastMessage("This picture is not allowed to be edited...");
								return;
							}
							
							HttpTasks uploadImageRequest = createUploadImageRequest(targetMediaUri, 
									HttpTasks.HTTP_PUT, targetIndex, newTitle, newNote, "Editing Picture Entry");
							
							try {
								uploadImageRequest.getHttpPut().setEntity((new FileEntity(
										new File(mImageFilePath), mImageFileExt)));
								uploadImageRequest.addHeaderToHttpPut("User-Agent", "AMC_BV/0.1");
						    	uploadImageRequest.addHeaderToHttpPut("Content-Type", mImageFileExt);
						    	uploadImageRequest.addHeaderToHttpPut("Slug", newTitle);
						    	uploadImageRequest.addHeaderToHttpPut("Password", "storytlr");
							}
							catch(Exception ex) {
								// TODO: exception handling
							}
							uploadImageRequest.start();
						}
						else {
							// USING URL RADIO BUTTON IS CHECKED
							
							AtomText title = mPorter.getAtomFactory().text("text", newTitle);
			        		object.setTitle(title);
			        		
			        		AtomText summary = mPorter.getAtomFactory().text("text", newNote);
							object.setSummary(summary);
							
							if (byUrlRadio.isChecked()) {
								// TODO: edit the value of necessary elements
							}
							
							Intent data = mPorter.prepareIntentForEditing(targetIndex, 
										mActivityWriter.toXml(entry), targetUri, "Editing Picture Entry");
							if (getParent() == null) 
								setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
							else
								getParent().setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
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
			
			postButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final String newTitle = StringEscapeUtils.escapeHtml(titleEditText.getText()
							.toString()).trim();
					final String newNote = StringEscapeUtils.escapeHtml(noteEditText.getText()
							.toString()).trim();
					
					if (newTitle.equals("") || newNote.equals("")) {
						showToastMessage("Please fill in the title and the note box.");
						return;
					}
					
					String username = mPorter.loadPreferenceString(Porter.PREFERENCES_KEY_USERNAME, 
							"Anonymous");
			
					if (byUploadRadio.isChecked() && (mImageFilePath != null) && (mImageFileExt != null)) {
						// UPLOAD IMAGE RADIO BUTTON IS CHECKED AND A FILE IS CHOSEN
																		
						HttpTasks uploadImageRequest = createUploadImageRequest(
								getString(R.string.service_endpoint_uri) + "?username=" + username, 
								HttpTasks.HTTP_POST, -1, newTitle, newNote, "Sharing new Picture");
						
						try {
							uploadImageRequest.getHttpPost().setEntity((new FileEntity(
									new File(mImageFilePath), mImageFileExt)));
							uploadImageRequest.addHeaderToHttpPost("User-Agent", "AMC_BV/0.1");
					    	uploadImageRequest.addHeaderToHttpPost("Content-Type", mImageFileExt);
					    	uploadImageRequest.addHeaderToHttpPost("Slug", newTitle);
					    	uploadImageRequest.addHeaderToHttpPost("Password", "storytlr");
						}
						catch(Exception ex) {
							// TODO: exception handling
						}
						uploadImageRequest.start();
					}
					else {
						// USING URL RADIO BUTTON IS CHECKED
						
						final String newPictUrl = StringEscapeUtils.escapeHtml(urlEditText.getText()
								.toString()).trim();
						
						if (newPictUrl.equals("")) {
							showToastMessage("Please fill in the URL box.");
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
						
						/*final TextView debugTextView = (TextView) findViewById(R.id.debug);
						debugTextView.setText(mActivityWriter.toXml(entry));*/

						// INCREMENT NEXT LINK ENTRY'S ID
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
		return new HttpTasks(targetMediaUri, httpMode) {
		
				@Override
				public void run() {
					// EXECUTE THE HTTP POST REQUEST TO UPLOAD THE PICTURE FILE FIRST
					super.run();
					
					Log.d("INSIDE onClickPostButton_SharePict", "Finished executing POST/PUT request");
	    			if (this.hasHttpResponse()) {
	    				int statusCode = this.getHttpResponse().getStatusLine().getStatusCode();
	    				Log.d("INSIDE onClickPostButton_SharePict", "Has HttpResponse, Status Code:" + statusCode);
	    				if (statusCode != 201 && statusCode != 200)
	    					return;
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
				        	Log.e("INSIDE onClickPostButton_SharePict XPP", ex.getMessage());
				        }
				        catch(IOException ex) {
				        	Log.e("INSIDE onClickPostButton_SharePict XPP", ex.getMessage());
				        }
				        catch(Exception ex) {
				        	Log.e("INSIDE onClickPostButton_SharePict XPP", ex.getMessage());
				        }
				        
				        if (atomEntry != null) {
				        	/*
				        	 *  EDIT THE RETRIEVED ENTRY WITH THE INPUT FROM USER (TITLE & NOTE)
				        	 *  AND
				        	 *  SEND BACK THE EDITED ENTRY THROUGH UserStream ACTIVITY
				        	 */
				        	
				        	String username = mPorter.loadPreferenceString(
				        			Porter.PREFERENCES_KEY_USERNAME, "Anonymous");
				        	if (atomEntry instanceof ActivityEntry) {
				        		ActivityEntry activityEntry = (ActivityEntry) atomEntry;
				        		ActivityObject object = activityEntry.getObjects().get(0);
				        		
				        		AtomText title = mPorter.getAtomFactory().text("text", newTitle);
				        		object.setTitle(title);
				        		
				        		title = mPorter.getAtomFactory().text("text", 
										username + " shared a link");
				        		activityEntry.setTitle(title);
				        		
				        		AtomText summary = mPorter.getAtomFactory().text("text", newNote);
				        		activityEntry.setSummary(summary);
				        		object.setSummary(summary);
				        		
				        		String targetUri = mPorter.extractHrefFromLinks(activityEntry.getLinks(), 
				        				AtomLink.REL_EDIT);
								if (targetUri == null) {
									showToastMessage("This entry is not allowed to be edited...");
									return;
								}
								
								// INCREMENT NEXT LINK ENTRY'S ID
								mPorter.incrementPreferenceInt(ActivityObject.PHOTO);
				        		
								/*
								 *  SET AN INTENT IN ACTIVITY'S RESULT 
								 *  AND 
								 *  GO BACK TO UserStream ACTIVITY WITH THE RESULT
								 */
				        		Intent data = mPorter.prepareIntentForEditing(targetItemIndex, 
										mActivityWriter.toXml(activityEntry), targetUri, dialogTitle);
								if (getParent() == null) 
									setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
								else
									getParent().setResult(Porter.RESULTCODE_EDITING_ENTRY, data);
								finish();
			        		}
			        		else { 
			        			// THE ENTRY IS A REGULAR ATOM-ENTRY
			        		} 
			        		
				        }
				        else 
				        	Log.e("INSIDE onClickPostButton_SharePict", "Response entry is null");
	    			}
	    			else
	    				Log.e("INSIDE onClickPostButton_SharePict", 
							(this.getExceptionMessage() != null)? 
									this.getExceptionMessage():"Http response is null");
				}
				
			};
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == REQUESTCODE_GET_IMAGE) {
			final ImageView displayPictureImage = (ImageView) findViewById(R.id.postOrShare_picture_display);
			
			if (resultCode == Activity.RESULT_OK) {
				final TextView displayPictureFileName = (TextView) findViewById(R.id.postOrShare_picture_displayFileName);
				String[] imageInfo = this.getFileInfoFromUri(data.getData());
				mImageFilePath = imageInfo[0];
				mImageFileExt = imageInfo[1];
				displayPictureFileName.setText(imageInfo[2]);
				
				/*BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
				decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565; // RGB_565 = 16-bit color
				decodeOptions.inDither = true; // dithering improves decoding to RGB_565 a bit
				decodeOptions.inSampleSize = CommonMethods.recommendedSampleSize(
						viewWidth, viewHeight, imageWidth, imageHeight);*/

				this.clearBitmapCache();
				mImageCache = BitmapFactory.decodeFile(mImageFilePath);
				displayPictureImage.setImageBitmap(mImageCache);
			}
			else if (resultCode == Activity.RESULT_CANCELED) {
				displayPictureImage.setImageBitmap(mImageCache);
			}
		}
	}
	
	public String[] getFileInfoFromUri (Uri contentUri) {
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
	
	private void showToastMessage(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
}
