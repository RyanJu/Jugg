package com.simple.zrk;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class RequestControl {
	private String httpUrl;
	private String assetUrl;
	private int resId;
	private String fileName;
	private Context context;
	private WeakReference<ImageView> target;
	private Dispatcher dispatcher;
	private int placeholder;
	private int errorholder;
	private String key;// use it to save in cache
	private String hashKey;// use it to be identification in dispatcher's request map
	private Type requestType;
	int retryTimes=0;
	
	String loadFrom;
	volatile int targetWidth;
	volatile int targetHeight;
	JuggDrawable juggDrawable;
	Cache cache;
	boolean canceled = false;
	boolean needResizeWidth = false;
	boolean needResizeHeight = false;

	private Point size;

	private ScaleType scaleType;
	boolean hasScaleType;
	private float rotationDegree;
	boolean needRotate;
	NetworkInfo networkInfo;
	
	JuggCallback callback;
	

	private RequestControl(String httpUrl, String assetUrl, int resId,
			String fileName, String key, String hashKey, Type type,
			Context context, Dispatcher dispatcher) {
		super();
		this.httpUrl = httpUrl;
		this.assetUrl = assetUrl;
		this.resId = resId;
		this.fileName = fileName;
		this.key = key;
		this.hashKey = hashKey;
		this.requestType = type;
		this.context = context;

		this.retryTimes=retryTimes;
		if (dispatcher == null) {
			dispatcher = Dispatcher.getInstance(context);
		}
		this.dispatcher = dispatcher;
		this.cache = Cache.getCache(context);
		ConnectivityManager manage=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		this.networkInfo=manage.getActiveNetworkInfo();
	}

	boolean needMeasureTarget(){
		if (target.get()!=null) {
			if (targetHeight==0||targetWidth==0) {
				return true;
			}
		}
		return false;
	}
	
	private void getTargetDimension() {
		// TODO Auto-generated method stub
		if (target == null || target.get() == null) {
			throw new IllegalArgumentException("target view is null !!");
		}
		final ImageView targetView = target.get();

		if (targetView != null) {
			targetView.getViewTreeObserver().addOnPreDrawListener(
					new OnPreDrawListener() {

						@Override
						public boolean onPreDraw() {
							// TODO Auto-generated method stub
							targetView.getViewTreeObserver()
									.removeOnPreDrawListener(this);
							targetWidth = targetView.getWidth();
							targetHeight = targetView.getHeight();
							return false;
						}
					});
		}
	}

	public ScaleType getScaleType() {
		return this.scaleType;
	}

	private RequestControl scaleType(ScaleType scaleType) {
		if (scaleType == null) {
			this.scaleType = null;
			this.hasScaleType = false;
			return this;
		}
		this.scaleType = scaleType;
		this.hasScaleType = true;
		return this;
	}

	public RequestControl centerCrop() {
		return scaleType(ScaleType.CENTER_CROP);
	}

	public RequestControl centerInside() {
		return scaleType(ScaleType.CENTER_INSIDE);
	}

	public RequestControl fitXY() {
		return scaleType(ScaleType.FIT_XY);
	}

	public RequestControl noScaleType() {
		return scaleType(null);
	}


	public RequestControl placeholder(int placeholderResId) {
		this.placeholder = placeholderResId;
		return this;
	}

	public RequestControl errorholder(int errorResId) {
		this.errorholder = errorResId;
		return this;
	}

	public RequestControl retryIfFailed(int times){
		this.retryTimes=times;
		return this;
	}
	
	public int getRetryTimes(){
		return this.retryTimes;
	}
	/***
	 * 
	 * @param width
	 *            :ViewGroup.LayoutParams.MATCH_PARENT or
	 *            ViewGroup.LayoutParams.WRAP_CONTENT or number>0 0 will be
	 *            ignored
	 * @param height
	 *            :like width
	 * @return
	 */
	public RequestControl resize(int width, int height) {
		size = new Point();
		if (width > 0 || width == ViewGroup.LayoutParams.MATCH_PARENT
				|| width == ViewGroup.LayoutParams.WRAP_CONTENT) {
			targetWidth = width;
			needResizeWidth = true;
			size.x = width;
		} else {
			needResizeWidth = false;
		}

		if (height > 0 || height == ViewGroup.LayoutParams.MATCH_PARENT
				|| height == ViewGroup.LayoutParams.WRAP_CONTENT) {
			targetHeight = height;
			needResizeHeight = true;
			size.y = height;
		} else {
			needResizeHeight = false;
		}
		return this;
	}

	public Point getTranformSize() {
		return this.size;
	}

	public RequestControl resizeByDp(int widthDp, int heightDp) {
		return resize(Utils.pxToDp(context, widthDp),
				Utils.pxToDp(context, heightDp));
	}

	public RequestControl rotate(float rotationDegree) {
		this.rotationDegree = rotationDegree;
		this.needRotate = true;
		return this;
	}

	public float getRotationDegree() {
		return this.rotationDegree;
	}

	
	public void show(ImageView targetView) {
		show(targetView, null);
	}
	
	public void show(ImageView targetview,JuggCallback callback){
		if (key == null || "".equals(key)) {
			throw new IllegalStateException("error by get key");
		}

		this.target = new WeakReference<ImageView>(targetview);
		this.callback=callback;
		
//		getTargetDimension();

		dispatcher.dispatchSubmit(this);
	}

	private boolean hasContent(String url) {
		// TODO Auto-generated method stub
		if (url == null || "".equals(url)) {
			return false;
		}
		return true;
	}

	public void setHashKey(String newKey) {
		this.hashKey = newKey;
	}

	public String getHashKey() {
		return this.hashKey;
	}

	public String getKey() {
		return key;
	}

	public String getHttpUrl() {
		return httpUrl;
	}

	public String getAssetUrl() {
		return assetUrl;
	}

	public int getResId() {
		return resId;
	}

	public String getFileName() {
		return fileName;
	}

	public Context getContext() {
		return context;
	}

	public ImageView getTargetView() {
		return target.get();
	}

	public Dispatcher getDispatcher() {
		return dispatcher;
	}


	public int getPlaceholder() {
		return placeholder;
	}

	public int getErrorholder() {
		return errorholder;
	}

	public Type getType() {
		return requestType;
	}

	static class Builder {
		private String httpUrl;
		private String assetUrl;
		private int resId;
		private String fileName;
		private Context context;
		private String key;
		private Type type;
		private Dispatcher dispatcher;
		private String hashKey;

		public RequestControl build() {
			checkType();
			checkNullPointer();
			return new RequestControl(httpUrl, assetUrl, resId, fileName, key,
					hashKey, type, context,  dispatcher);
		}

		private void checkType() {
			// TODO Auto-generated method stub
			if (type == null) {
				throw new NullPointerException(" request type cannot be null ");
			}
			switch (type) {
			case HTTP:
				if (httpUrl == null || "".equals(httpUrl)) {
					throw new NullPointerException(
							"http url mismatch request type HTTP or http url haven's been setted");
				}
				break;
			case FILE:
				if (fileName == null || "".equals(fileName)) {
					throw new NullPointerException(
							"file path mismatch request type FILE or file path haven's been setted");
				}
				break;
			case ASSET:
				if (assetUrl == null || "".equals(assetUrl)) {
					throw new NullPointerException(
							"asset path mismatch request type ASSET or asset url haven's been setted");
				}
				break;
			case RESOURCE:
				if (resId == 0) {
					throw new NullPointerException(
							"resource id mismatch request type RESOURCE or resource id haven's been setted");
				}
				break;
			default:
				break;
			}
		}

		private void checkNullPointer() {
			// TODO Auto-generated method stub
			if (context == null) {
				throw new NullPointerException("null context");
			}
			if (key == null || "".equals(key)) {
				throw new NullPointerException("null key or hashkey");
			}
			if (hashKey == null || "".equals(hashKey)) {
				hashKey = key;
			} else {
				Utils.logE("request has been reload : hashkey " + hashKey);
			}
		}

		
		public Builder url(String httpUrl) {
			this.httpUrl = httpUrl;
			return this;
		}

		public Builder asset(String assetUrl) {
			this.assetUrl = assetUrl;
			return this;
		}

		public Builder resource(int resId) {
			this.resId = resId;
			return this;
		}

		public Builder file(String fileName) {
			this.fileName = fileName;
			return this;
		}

		public Builder context(Context context) {
			this.context = context;
			return this;
		}

		public Builder key(String key) {
			key = Utils.hashKey(key);
			this.key = key;
			return this;
		}

		public Builder hashKey(String hashkey) {
			this.hashKey = hashkey;
			return this;
		}

		public Builder dispatcher(Dispatcher dispatcher) {
			if (dispatcher == null) {
				throw new IllegalArgumentException("dispatcher null");
			}
			this.dispatcher = dispatcher;
			return this;
		}

		public Builder requestType(Type type) {
			// TODO Auto-generated method stub
			this.type = type;
			return this;
		}
	}

	public void cancel() {
		// TODO Auto-generated method stub
		canceled = true;
		httpUrl=null;
		assetUrl=null;
		resId=0;
		fileName=null;
		context=null;
		target.clear();
		dispatcher=null;
		placeholder=0;
		errorholder=0;
		key=null;
		hashKey=null;
		requestType=null;
		loadFrom=null;
		targetWidth=0;
		targetHeight=0;
		juggDrawable=null;
		cache=null;
		needResizeHeight=false;
		needResizeWidth=false;
		size=null;
		scaleType=null;
		hasScaleType=false;
		rotationDegree=0;
		needRotate=false;
	}

}
