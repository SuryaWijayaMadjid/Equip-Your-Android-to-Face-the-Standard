package net.betavinechronicle.client.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

public class SharePicture extends Activity {

	private Bitmap mImageCache = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.share_picture_tabwidget);

		final RadioButton byUploadRadio = (RadioButton) findViewById (R.id.postOrShare_picture_byUpload);
		final RadioButton byUrlRadio = (RadioButton) findViewById (R.id.postOrShare_picture_byUrl);
		final FrameLayout uploadFrame = (FrameLayout) findViewById (R.id.postOrShare_picture_uploadFrame);
		final EditText urlEditText = (EditText) findViewById (R.id.postOrShare_picture_url);
		final Button choosePictureButton = (Button) findViewById (R.id.postOrShare_picture_chooseButton);
		
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
				if (mImageCache != null && mImageCache.isRecycled() == false)
					mImageCache.recycle(); // to prevent the application from out of memory
				startActivityForResult(getImageIntent, 1543);
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == 1543 && resultCode == Activity.RESULT_OK) {
			final ImageView displayPictureImage = (ImageView) findViewById(R.id.postOrShare_picture_display);
			final TextView displayPictureFileName = (TextView) findViewById(R.id.postOrShare_picture_displayFileName);
			String returnedImageFilePath = this.getRealPathFromUri(data.getData(), MediaStore.Images.Media.DATA);
			
			/*BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
			decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565; // RGB_565 = 16-bit color
			decodeOptions.inDither = true; // dithering improves decoding to RGB_565 a bit
			decodeOptions.inSampleSize = CommonMethods.recommendedSampleSize(
					viewWidth, viewHeight, imageWidth, imageHeight);*/
			
			mImageCache = BitmapFactory.decodeFile(returnedImageFilePath);
			displayPictureImage.setImageBitmap(mImageCache);
			displayPictureFileName.setText(this.getRealPathFromUri(data.getData(), 
					MediaStore.Images.Media.DISPLAY_NAME));			
		}
	}
	
	public String getRealPathFromUri (Uri contentUri, String whatToRetrieve) {
		
		String[] projection = { whatToRetrieve };  
	    Cursor cursor = managedQuery( contentUri, projection, null, null, null);
	    
	    int column_index = cursor.getColumnIndexOrThrow(whatToRetrieve);  
	    cursor.moveToFirst();  
	  
	    return cursor.getString(column_index);
	}
}
