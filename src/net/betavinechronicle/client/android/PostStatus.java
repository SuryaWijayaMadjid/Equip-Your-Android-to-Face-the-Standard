package net.betavinechronicle.client.android;

import org.onesocialweb.model.atom.AtomContent;
import org.onesocialweb.model.atom.AtomEntry;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PostStatus extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_status_tabwidget);
		Porter mPorter = (Porter) this.getApplication();
		
		final EditText statusEditText = (EditText) findViewById(R.id.postOrShare_status_status);
		final Button postButton = (Button) findViewById(R.id.postOrShare_status_postButton);
		
		if (this.getIntent().getIntExtra(Porter.EXTRAKEY_REQUESTCODE, 0) == Porter.REQUESTCODE_EDIT_ENTRY) {
			if (mPorter.hasEntry() && mPorter.hasPostItemIndex() && mPorter.hasPostItem()) {
				AtomEntry entry = mPorter.getEntry();				
				AtomContent content = entry.getContent();
				if (!content.hasSrc()) {
					statusEditText.setText(GeneralMethods.ifHtmlRemoveMarkups(content));
				}
				else {
					// TODO: not implemented yet :)
				}
			}
			
			postButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO: send edit resource request
					
				}
			});
		}
	}
}
