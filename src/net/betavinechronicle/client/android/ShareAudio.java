package net.betavinechronicle.client.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;

public class ShareAudio extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.share_audio_tabwidget);
		
		final RadioButton byUploadRadio = (RadioButton) findViewById (R.id.postOrShare_audio_byUpload);
		final RadioButton byUrlRadio = (RadioButton) findViewById (R.id.postOrShare_audio_byUrl);
		final FrameLayout uploadFrame = (FrameLayout) findViewById (R.id.postOrShare_audio_uploadFrame);
		final EditText urlEditText = (EditText) findViewById (R.id.postOrShare_audio_url);
		final Button chooseAudioButton = (Button) findViewById (R.id.postOrShare_audio_chooseButton);
		
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
				
				Intent getImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
				getImageIntent.setType("audio/mp3");
				startActivityForResult(getImageIntent, 1543);
			}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == 1543 && resultCode == Activity.RESULT_OK) {
			final TextView displayAudioFileName = (TextView) findViewById(R.id.postOrShare_audio_displayFileName);
			Uri returnedImageUri = data.getData();
			displayAudioFileName.setText(
					getRealPathFromUri(returnedImageUri));
		}
	}
	
	//Function made by BigRedPimp -- http://www.androidsnippets.org/snippets/130/
	public String getRealPathFromUri (Uri contentUri) {
		
		String [] projection = {MediaStore.Audio.Media.DISPLAY_NAME};  
	    Cursor cursor = managedQuery( contentUri,  
	            projection, // Which columns to return  
	            null,       // WHERE clause; which rows to return (all rows)  
	            null,       // WHERE clause selection arguments (none)  
	            null); // Order-by clause (ascending by name)
	    
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);  
	    cursor.moveToFirst();  
	  
	    return cursor.getString(column_index);
	}
}
