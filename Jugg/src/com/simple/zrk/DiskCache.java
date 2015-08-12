package com.simple.zrk;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.simple.zrk.DiskLruCache.Editor;
import com.simple.zrk.DiskLruCache.Snapshot;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class DiskCache {
	DiskLruCache cache = null;
	private Context context;
	private int maxSize = 100 * 1024 * 1024;

	private DiskCache(Context context) {
		this.context = context;

		initDiskCache(context);
	}

	private void initDiskCache(Context context2) {
		// TODO Auto-generated method stub
		File cacheDir = getCacheDir(context);
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		try {
			cache = DiskLruCache.open(getCacheDir(context),
					getAppVersion(context), 1, maxSize);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int getAppVersion(Context con) {
		// TODO Auto-generated method stub
		PackageManager manager = con.getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(con.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 1;
	}

	public synchronized void put(String key, Bitmap value) {
		if (cache == null || cache.isClosed()) {
			initDiskCache(context);
		}

		OutputStream os=null;
		try {
			Editor edit = cache.edit(key);
			os = edit.newOutputStream(0);
			boolean b = value.compress(CompressFormat.JPEG, 100, os);
			if (b) {
				Log.i("disk cache ", "commit a cache file key:" + key);
				edit.commit();
			} else {
				Log.w("disk cache ", "abort a cache file key:" + key);
				edit.abort();
			}
			cache.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	public Bitmap get(String key) {
		if (cache == null || cache.isClosed()) {
			initDiskCache(context);
		}
		try {
			Snapshot snapshot = cache.get(key);
			if (snapshot != null) {
				Bitmap bmp = BitmapFactory.decodeStream(snapshot
						.getInputStream(0));
				snapshot.close();
				return bmp;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void clearCache() {
		if (cache == null || cache.isClosed()) {
			initDiskCache(context);
		}
		try {
			cache.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/***
	 * 
	 * @return size in kb
	 */
	public int size() {
		return (int) cache.size() / 1024;
	}

	private File getCacheDir(Context context) {
		// TODO Auto-generated method stub
		String cachePath = "";
		if (!Environment.isExternalStorageRemovable()
				|| Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + "bmp");
	}

	public static DiskCache getCache(Context context) {
		return new DiskCache(context);
	}
}
