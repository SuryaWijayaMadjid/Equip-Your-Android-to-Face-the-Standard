package net.betavinechronicle.client.android;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;



public class HttpTasks extends Thread {

	static final int HTTP_GET = 1;
	static final int HTTP_POST = 2;
	
	private HttpPost mHttpPost;
	private HttpGet mHttpGet;
	private HttpClient mHttpClient;
	private HttpResponse mHttpResponse;
	
	private StringEntity mStringEntity;	
	private FileEntity mFileEntity;
	private int mHttpMode;
	private String mExceptionMessage = "";
	
	public HttpTasks (String targetUri, int httpMode) {
		
		mHttpMode = httpMode;
		mHttpClient = new DefaultHttpClient();
		
		if (httpMode == HttpTasks.HTTP_GET) mHttpGet = new HttpGet(targetUri);
		else if (httpMode == HttpTasks.HTTP_POST) mHttpPost = new HttpPost(targetUri);		
	}
	
	// Set a string entity that we want to send along with the post method
	public void setStringEntity (StringEntity stringEntity, String contentType) {
		mStringEntity = stringEntity;
		mStringEntity.setContentType(contentType);
	}
	
	// Set a file entity that we want to send along with the post method
	public void setFileEntity(FileEntity fileEntity) {
		mFileEntity = fileEntity;
	}
	
	public boolean hasStringEntity() {
		return (mStringEntity != null);
	}
	
	public boolean hasFileEntity() {
		return (mFileEntity != null);
	}
	
	public boolean hasAvailableEntity() {
		if (mStringEntity != null) return true;
		if (mFileEntity != null) return true;
		return false;
	}
	
	public HttpEntity getAvailableEntity() {
		if (mStringEntity != null) return mStringEntity;
		if (mFileEntity != null) return mFileEntity;
		return null;
	}
	
	@Override
	public void run() {
		
		switch (mHttpMode) {
		
		case HTTP_GET:
			mHttpGet.addHeader("Accept", "text/atom+xml");
			
			try {
				mHttpResponse = mHttpClient.execute(mHttpGet);
			}
			catch (IOException ex) {
				mExceptionMessage = ex.getMessage();
			}
			catch (Exception ex) {
				mExceptionMessage = ex.getMessage();
			}
			
			break;
			
		case HTTP_POST:
			HttpEntity httpEntity = this.getAvailableEntity();
			mHttpPost.addHeader("Accept", "text/atom+xml");
			mHttpPost.addHeader("Authorization", "Basic authorization");
			mHttpPost.addHeader(httpEntity.getContentType());
			
			mHttpPost.setEntity(httpEntity);
	    			
			try {
				mHttpResponse = mHttpClient.execute(mHttpPost);
			}
			catch (IOException ex) {
				mExceptionMessage = ex.getMessage();
			}
			catch (Exception ex) {
				mExceptionMessage = ex.getMessage();
			}
			
			break;
		}
	}
	
	public HttpResponse getHttpResponse() {
		return mHttpResponse;
	}
	
	public boolean hasHttpResponse() {
		return (mHttpResponse != null);
	}
	
	public String getExceptionMessage() {
		return mExceptionMessage;
	}
}
