package net.betavinechronicle.client.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringEscapeUtils;
import org.onesocialweb.model.atom.AtomText;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class GeneralMethods {

	public static int recommendedSampleSize(int viewWidth, int viewHeight, int imageWidth, int imageHeight) {
		int widthRatio = imageWidth / viewWidth;
		int heightRatio = imageHeight / viewHeight;
		if (widthRatio <= 1 && heightRatio <= 1) return 1;
		
		int scaleRatio = (widthRatio > heightRatio)? widthRatio : heightRatio;
		
		scaleRatio = closestPowerOf(scaleRatio, 2, 1);
		
		return scaleRatio;
	}
	
	public static int closestPowerOf(int number, int powerBy, int preference) {
		
		int resultNumber = powerBy;
		
		while (resultNumber < number) {
			resultNumber *= powerBy;
		}
		
		if (preference == 0) { // return the power value closest to the value of number variable
			if ((resultNumber/powerBy) % powerBy != 0) return resultNumber;
			return (resultNumber-number < number-resultNumber/powerBy)? resultNumber : resultNumber/powerBy;
		}
		else if (preference == 1) { // return the power value 1 level below the value of number variable
			if (resultNumber == number) return resultNumber;
			else return resultNumber/powerBy;
		}
		else return 1;
	}
	
	public static String removeContainedMarkups (String inputString, boolean unescapeHtmlFirst) {
		
		if (unescapeHtmlFirst) 
			inputString = StringEscapeUtils.unescapeHtml(inputString);
		
		boolean mOpeningDetected = false;
		int mOpeningAt = -1;
		
		for (int i = 0; i < inputString.length(); i++) {
			if (inputString.charAt(i) == '<') {
				mOpeningDetected = true;
				mOpeningAt = i;
			}
			else if (inputString.charAt(i) == '>' && mOpeningDetected == true) {
				inputString = inputString.substring(0, mOpeningAt) + inputString.substring(i+1);
				mOpeningDetected = false;
				i -= (i - mOpeningAt + 1);
			}
		}
		
		return inputString;
	}
	
	public static String getShortVersionString (String string, int maxLength) {
		if (string == null) 
			return null;
		if (string.length() > maxLength)
			string = string.substring(0, maxLength).trim() + "....";
		return string;
	}
	
    
    public static String ifHtmlRemoveMarkups(AtomText atomText) {
    	String text = atomText.getValue();
    	if (atomText.getType().equals("html"))
    		text = GeneralMethods.removeContainedMarkups(text, true);
    	return text;
    }
        
    public static Bitmap getImageBitmapFromUrlString(String url) {
    	Bitmap bitmapImage = null;
    	try {
			InputStream inStream = (InputStream) (new URL(url)).getContent();
			bitmapImage = BitmapFactory.decodeStream(inStream);
		}
		catch (MalformedURLException ex) {
			Log.e("INSIDE generateImagePreview()", ex.getMessage());
		}
		catch (IOException ex) {
			Log.e("INSIDE generateImagePreview()", ex.getMessage());
		}
		catch (Exception ex) {
			Log.e("INSIDE generateImagePreview()", ex.getMessage());
		}
    	return bitmapImage;
    }
    
    public static String getRawStringFromResponse(InputStream inputStream) {
    	StringBuilder stringBuilder = new StringBuilder();
		String line = "";
		String text = "";
		
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		try
		{
			while ( ( line = bufferedReader.readLine() ) != null)
			{
				stringBuilder.append(line + "\n");
			}
			text += stringBuilder.toString();
		}
		catch (IOException ex) {
			Log.e("INSIDE getRawStringFromResponse():", ex.getMessage());
		}
		
		return text;
    }
}
