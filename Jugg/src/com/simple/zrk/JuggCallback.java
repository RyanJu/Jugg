package com.simple.zrk;

import android.view.View;

public interface JuggCallback {
	public void onPreLoad(final View view,final String key);
	public void onLoading(final View view,final String key,int progress,int amount);
	public void onComplete(final View view,final String key);
	public void onLoadFailed(final View view,final String key);
}
