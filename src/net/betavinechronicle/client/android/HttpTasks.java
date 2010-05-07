package net.betavinechronicle.client.android;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;


public class HttpTasks extends Thread {

	static final int HTTP_GET = 1;
	static final int HTTP_POST = 2;
	static final int HTTP_PUT = 3;
	static final int HTTP_DELETE = 4;
	
	private HttpPost mHttpPost;
	private HttpGet mHttpGet;
	private HttpPut mHttpPut;
	private HttpDelete mHttpDelete;
	private HttpClient mHttpClient;
	private HttpResponse mHttpResponse;

	private int mHttpMode;
	private String mExceptionMessage = "";
	
	public HttpTasks (String targetUri, int httpMode) {
		
		mHttpMode = httpMode;
		mHttpClient = new DefaultHttpClient();
		
		if (httpMode == HTTP_GET) mHttpGet = new HttpGet(targetUri);
		else if (httpMode == HTTP_POST) mHttpPost = new HttpPost(targetUri);	
		else if (httpMode == HTTP_PUT) mHttpPut = new HttpPut(targetUri);
		else if (httpMode == HTTP_DELETE) mHttpDelete = new HttpDelete(targetUri);
	}
	
	@Override
	public void run() {
		
		try {
			if (mHttpMode == HTTP_GET) mHttpResponse = mHttpClient.execute(mHttpGet);
			else if (mHttpMode == HTTP_POST) mHttpResponse = mHttpClient.execute(mHttpPost);
			else if (mHttpMode == HTTP_PUT) mHttpResponse = mHttpClient.execute(mHttpPut);
			else if (mHttpMode == HTTP_DELETE) mHttpResponse = mHttpClient.execute(mHttpDelete);
		}
		catch (IOException ex) {
			mExceptionMessage = ex.getMessage();
		}
		catch (Exception ex) {
			mExceptionMessage = ex.getMessage();
		}
	}
	
	public void addHeaderToHttpPost(String name, String value) {
		if (mHttpPost != null) mHttpPost.addHeader(name, value);
	}
	
	public void addHeaderToHttpGet(String name, String value) {
		if (mHttpGet != null) mHttpGet.addHeader(name, value);
	}
	
	public void addHeaderToHttpPut(String name, String value) {
		if (mHttpPut != null) mHttpPut.addHeader(name, value);
	}
	
	public void addHeaderToHttpDelete(String name, String value) {
		if (mHttpDelete != null) mHttpDelete.addHeader(name, value);
	}
	
	public HttpResponse getHttpResponse() {
		return mHttpResponse;
	}
	
	public HttpGet getHttpGet() {
		return mHttpGet;
	}
	
	public HttpPost getHttpPost() {
		return mHttpPost;
	}
	
	public HttpPut getHttpPut() {
		return mHttpPut;
	}
	
	public HttpDelete getHttpDelete() {
		return mHttpDelete;
	}
	
	public boolean hasHttpResponse() {
		return (mHttpResponse != null);
	}
	
	public String getExceptionMessage() {
		return mExceptionMessage;
	}
}
