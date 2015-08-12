package com.simple.zrk;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

public class JuggDrawable extends BitmapDrawable {
	private Context context;
	private WeakReference<JuggAsyncTask> taskReference=null;
	
	
	public JuggDrawable(Context context,Bitmap bitmap,JuggAsyncTask task) {
		// TODO Auto-generated constructor stub
		super(context.getResources(),bitmap);
		this.context=context;
		this.taskReference=new WeakReference<JuggAsyncTask>(task);
	}
	
	public JuggAsyncTask getJuggAsyncTask(){
		return taskReference.get();
	}

	public void setJuggAsyncTask(JuggAsyncTask task) {
		// TODO Auto-generated method stub
		this.taskReference=new WeakReference<JuggAsyncTask>(task);
	}
	
}
