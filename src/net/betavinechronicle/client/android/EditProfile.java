package net.betavinechronicle.client.android;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class EditProfile extends Activity {

	private Bitmap mImageCache = null;
	private boolean mIsOpeningNewActivity = false;
	private boolean mRotateCancelled = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_profile);
		
		final Button choosePictureButton = (Button) findViewById(R.id.edit_profile_chooseButton);
		final ImageView displayPictureImage = (ImageView) findViewById(R.id.edit_profile_display);
		final EditText firstNameEditText = (EditText) findViewById(R.id.edit_profile_firstName);
		final EditText lastNameEditText = (EditText) findViewById(R.id.edit_profile_lastName);
		final EditText shortBioEditText = (EditText) findViewById(R.id.edit_profile_shortBio);
		final EditText locationEditText = (EditText) findViewById(R.id.edit_profile_location);
		final Button saveButton = (Button) findViewById(R.id.edit_profile_save);
		Log.d("OnCreate", "inside the onCreate~!!");
		
		choosePictureButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				mIsOpeningNewActivity = true;
				Log.d("Choose Pict onClick event", "right before opening new activity!!");
				Intent getImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
				getImageIntent.setType("image/*");
				if (mImageCache != null && mImageCache.isRecycled() == false)
					mImageCache.recycle(); // to prevent the application from out of memory
				startActivityForResult(getImageIntent, 1543);
			}
		});
		
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
    	MenuInflater menuInflater = this.getMenuInflater();
    	menuInflater.inflate(R.menu.profile_options_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId()) {
    	case R.id.profile_options_userStream: 
			this.setResult(UserStream.RESULT_OK);
    		finish();
    		return true;
    	
    	case R.id.profile_options_postOrShare: 
    		this.setResult(UserStream.RESULTCODE_SWITCH_ACTIVITY_TO_POST_OR_SHARE);
    		this.finish();
    		return true;
    	
    	case R.id.profile_options_exit: 
    		this.setResult(UserStream.RESULTCODE_SUBACTIVITY_CHAINCLOSE);
    		this.finish();
    		return true;
    	}
    	
    	return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	Log.d("onConfigChanged", "right before changing the configuration!!");
    	super.onConfigurationChanged(newConfig);
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 1543 && resultCode == Activity.RESULT_OK) {
			final ImageView displayPictureImage = (ImageView) findViewById(R.id.edit_profile_display);
			final TextView displayPictureFileName = (TextView) findViewById(R.id.edit_profile_displayFileName);
			String returnedImageFilePath = this.getRealPathFromUri(data.getData(), MediaStore.Images.Media.DATA);
			
			mImageCache = BitmapFactory.decodeFile(returnedImageFilePath);
			displayPictureImage.setImageBitmap(mImageCache);
			displayPictureFileName.setText(this.getRealPathFromUri(data.getData(), 
					MediaStore.Images.Media.DISPLAY_NAME));
		}
		
		mIsOpeningNewActivity = false;
	}
	
	public String getRealPathFromUri (Uri contentUri, String whatToRetrieve) {
		
		String[] projection = { whatToRetrieve };  
	    Cursor cursor = managedQuery( contentUri, projection, null, null, null);
	    
	    int column_index = cursor.getColumnIndexOrThrow(whatToRetrieve);  
	    cursor.moveToFirst();  
	  
	    return cursor.getString(column_index);
	}
}
