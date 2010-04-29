package net.betavinechronicle.client.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class EnterUsername extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.enter_username);
		
		final EditText usernameEditText = (EditText) findViewById(R.id.enter_username_username);
		final Button getFeedButton = (Button) findViewById(R.id.enter_username_getFeed);
		
		getFeedButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String username = usernameEditText.getText().toString();
				if (!username.equals("")) {
					Intent data = new Intent();
					data.putExtra("username", username);
					setResult(RESULT_OK, data);
					finish();
				}
			}
		});
	}
}
