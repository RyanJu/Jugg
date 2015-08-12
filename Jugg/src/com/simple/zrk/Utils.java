package com.simple.zrk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.security.auth.login.LoginException;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.NetworkInfo;
import android.util.Log;

class Utils {
	private final static String TAG="Jugg";
	public static String httpStringKey(String http){
		String newString=http.replaceAll("/", "__");
		newString=newString.replaceAll(":", "___");
		return newString;
	}
	
	public static void log(String s){
		log(TAG, s);
	}
	
	public static void log(String tag,String s){
		if (Jugg.getInstance().isLoggable()) {
			Log.i(tag, s);
		}
	}
	
	public static void logE(String tag,String s){
		if (Jugg.getInstance().isLoggable()) {
			Log.e(tag, s);
		}
	}
	public static void logE(String s){
		logE(TAG, s);
	}
	
	public static int getBitmapSize(Bitmap bmp){
		return bmp.getByteCount()/1024;
	}
	
	public static int dpToPx(Context context,float dp){
		float denstiy=context.getResources().getDisplayMetrics().density;
		return (int) (dp*denstiy+0.5f);
	}
	
	public static int pxToDp(Context context,int px){
		float density=context.getResources().getDisplayMetrics().density;
		return (int) (px/density+0.5f);
	}
	
	public static String hashKey(String primaryString){
		try {
			MessageDigest digest=MessageDigest.getInstance("MD5");
			digest.update(primaryString.getBytes());
			byte[] bytes = digest.digest();
			StringBuilder sb=new StringBuilder();
			for (byte b : bytes) {
				String hex=Integer.toHexString(0xFF & b);
				if (hex.length()==1) {
					sb.append('0');
				}
				sb.append(hex);
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return primaryString.hashCode()+"";
	}
	
	public static String generateHashKey(){
		return UUID.randomUUID().toString();
	}
	public static boolean judgeNetConnected(NetworkInfo info){
		return info.isAvailable() || info.isConnectedOrConnecting();
	}
}
