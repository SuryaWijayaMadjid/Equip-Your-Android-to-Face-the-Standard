package net.betavinechronicle.client.android;

import org.apache.commons.lang.StringEscapeUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Settings extends Activity {
	
	private boolean mIsSet = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		final SharedPreferences prefs = getSharedPreferences(Porter.PREFERENCES_NAME, 
				Application.MODE_PRIVATE);
		mIsSet = (prefs.getString(Porter.PREFERENCES_KEY_USERNAME, "").equals(""))? false:true;
		
		final EditText usernameEditText = (EditText) findViewById(R.id.settings_username);
		final EditText passwordEditText = (EditText) findViewById(R.id.settings_password);
		final EditText endpointEditText = (EditText) findViewById(R.id.settings_endpoint);
		final EditText maxEntriesEditText = (EditText) findViewById(R.id.settings_maxEntries);
		final EditText maxCharsTitleEditText = (EditText) findViewById(R.id.settings_maxCharsTitle);
		final EditText maxCharsContentEditText = (EditText) findViewById(R.id.settings_maxCharsContent);
		final Button saveButton = (Button) findViewById(R.id.settings_save);
		final Button quitButton = (Button) findViewById(R.id.settings_quit);
		
		if (mIsSet) {
			usernameEditText.setText(prefs.getString(Porter.PREFERENCES_KEY_USERNAME, ""));
			passwordEditText.setText(prefs.getString(Porter.PREFERENCES_KEY_PASSWORD, ""));
			endpointEditText.setText(prefs.getString(Porter.PREFERENCES_KEY_ENDPOINT, ""));
			maxEntriesEditText.setText(String.valueOf(prefs.getInt(Porter.PREFERENCES_KEY_MAX_ENTRIES, 10)));
			maxCharsTitleEditText.setText(String.valueOf(prefs.getInt(Porter.PREFERENCES_KEY_MAX_CHARS_TITLE, 50)));
			maxCharsContentEditText.setText(String.valueOf(prefs.getInt(Porter.PREFERENCES_KEY_MAX_CHARS_CONTENT, 85)));
		}
		else {
			new AlertDialog.Builder(Settings.this)
			.setMessage("Before you can use the apps, you need to fill all of the settings values. Password is optional.")
			.setTitle("First time configuration")
			.setCancelable(true)
			.setPositiveButton("OK", null)
			.show();
		}
		
		saveButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String username = StringEscapeUtils.escapeHtml(usernameEditText.getText()
						.toString()).trim();
				String password = StringEscapeUtils.escapeHtml(passwordEditText.getText()
						.toString()).trim();
				String endpoint = StringEscapeUtils.escapeHtml(endpointEditText.getText()
						.toString()).trim();
				String maxEntries = maxEntriesEditText.getText().toString();
				String maxCharsTitle = maxCharsTitleEditText.getText().toString();
				String maxCharsContent = maxCharsContentEditText.getText().toString();
				
				if (username.equals("") || password.equals("") || endpoint.equals("")
						|| maxEntries.equals("") || maxCharsTitle.equals("")
						|| maxCharsContent.equals("")) {
					Toast.makeText(getApplicationContext(), 
							"Please fill all the required values. All the values except Password are required.", 
							Toast.LENGTH_LONG).show();
				}
				else {
					final SharedPreferences.Editor prefsEditor = prefs.edit();
					prefsEditor.putString(Porter.PREFERENCES_KEY_USERNAME, username);
					prefsEditor.putString(Porter.PREFERENCES_KEY_PASSWORD, password);
					prefsEditor.putString(Porter.PREFERENCES_KEY_ENDPOINT, endpoint);
					prefsEditor.putInt(Porter.PREFERENCES_KEY_MAX_ENTRIES, 
							Integer.parseInt(maxEntries));
					prefsEditor.putInt(Porter.PREFERENCES_KEY_MAX_CHARS_TITLE, 
							Integer.parseInt(maxCharsTitle));
					prefsEditor.putInt(Porter.PREFERENCES_KEY_MAX_CHARS_CONTENT, 
							Integer.parseInt(maxCharsContent));
					prefsEditor.commit();
					mIsSet = true;
					Toast.makeText(getApplicationContext(), "Settings saved.", Toast.LENGTH_LONG).show();
					finish();
				}
			}
		});
		
		quitButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mIsSet == false)
					promptQuit();
				else
					finish();
			}
		});
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mIsSet == false)
				promptQuit();
			else
				finish();
			return true;
		}
		else
			return super.onKeyDown(keyCode, event);
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	Log.d("onConfigChanged", "right before changing the configuration!!");
    	super.onConfigurationChanged(newConfig);
    }
    
    private void promptQuit() {
    	new AlertDialog.Builder(Settings.this)
		.setTitle("Aborting settings")
		.setMessage("The settings haven't been configured yet. You can't use this apps without the settings.\nQuit anyway?")
		.setCancelable(true)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				setResult(Porter.RESULTCODE_SUBACTIVITY_CHAINCLOSE);
				finish();
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
    }
}
