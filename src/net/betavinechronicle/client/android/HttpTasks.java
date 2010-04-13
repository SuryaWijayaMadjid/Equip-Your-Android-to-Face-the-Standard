package net.betavinechronicle.client.android;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpTasks extends Thread {

	static final int HTTP_GET = 1;
	static final int HTTP_POST = 2;
	
	private HttpPost mHttpPost;
	private HttpGet mHttpGet;
	private HttpClient mHttpClient;
	private HttpResponse mHttpResponse;
	
	private StringEntity xmlEntity_;	
	private int httpMode_;
	
	public HttpTasks (String targetUri, int httpMode) {
		
		httpMode_ = httpMode;
		xmlEntity_ = null;
		mHttpResponse = null;
		mHttpGet = null;
		mHttpPost = null;
		mHttpClient = new DefaultHttpClient();
		
		if (httpMode == this.HTTP_GET) mHttpGet = new HttpGet(targetUri);
		else if (httpMode == this.HTTP_POST) mHttpPost = new HttpPost(targetUri);		
	}
	
	public void setStringEntity (StringEntity stringEntity, String contentType) {
		xmlEntity_ = stringEntity;
		xmlEntity_.setContentType(contentType);
	}
	
	@Override
	public void run() {
		
		switch (httpMode_) {
		
		case HTTP_GET:
			//mHttpGet.addHeader("Accept", "text/atom+xml");
			
			try {
				mHttpResponse = mHttpClient.execute(mHttpGet);
			}
			catch (IOException ex) {
				//TODO: exception handling
				
			}
			catch (Exception ex) {
				//TODO: exception handling
			}
			
			break;
			
		case HTTP_POST:
			mHttpPost.addHeader("Accept", "text/atom+xml");
			mHttpPost.addHeader("Authorization", "Basic authorization");
			mHttpPost.addHeader("Content-Type", "application/atom+xml");
			    			
			mHttpPost.setEntity(xmlEntity_);
	    			
			try {
				mHttpResponse = mHttpClient.execute(mHttpPost);

			}
			catch (IOException ex) {
				//TODO: exception handling

			}
			catch (Exception ex) {
				//TODO: exception handling
			}
			
			break;
		}
	}
	
	public HttpResponse getHttpResponse() {
		return mHttpResponse;
	}
}
