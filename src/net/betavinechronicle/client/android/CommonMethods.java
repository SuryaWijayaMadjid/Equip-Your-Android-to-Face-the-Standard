package net.betavinechronicle.client.android;

public class CommonMethods {

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
}
