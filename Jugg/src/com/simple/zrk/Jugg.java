package com.simple.zrk;

import android.content.Context;

public class Jugg {
	 public static final int DEFAULT_CONNECT_TIME_OUT=5*1000;

	protected static Context sContext;
	private boolean loggable = false;
	Dispatcher dispatcher;

	private Jugg() {
		super();

	}

	public static Jugg withContext(Context context) {
		sContext = context;
		INSTANCE=getInstance();
		return INSTANCE;
	}

	public void finishJugg() {
		if (INSTANCE != null) {
			INSTANCE.dispatcher.clean();
			INSTANCE.dispatcher = null;
		}
		sContext = null;
	}

	public void cleanCacheIfNeed(){
		if (INSTANCE!=null) {
			INSTANCE.dispatcher.cleanCache();
		}
	}
	public Jugg loggable(boolean loggable) {
		this.loggable = loggable;
		return this;
	}

	public boolean isLoggable() {
		return this.loggable;
	}

	public RequestControl withUrl(String urlString) {
		RequestControl requestControl = new RequestControl.Builder()
				.url(urlString).key(urlString).requestType(Type.HTTP)
				.context(sContext).build();
		return requestControl;
	}

	public RequestControl withAsset(String assetName) {
		RequestControl requestControl = new RequestControl.Builder()
				.asset(assetName).key(assetName).requestType(Type.ASSET)
				.context(sContext).build();
		return requestControl;
	}

	public RequestControl withResource(int resId) {
		RequestControl requestControl = new RequestControl.Builder()
				.resource(resId).key(String.valueOf(resId))
				.requestType(Type.RESOURCE).context(sContext).build();
		return requestControl;
	}

	public RequestControl withFile(String absoluteFilePath) {
		RequestControl requestControl = new RequestControl.Builder()
				.file(absoluteFilePath).key(absoluteFilePath)
				.requestType(Type.FILE).context(sContext).build();
		return requestControl;
	}

	private static Jugg INSTANCE;

	static Jugg getInstance() {
		if (INSTANCE == null) {
			synchronized (Jugg.class) {
				if (INSTANCE == null) {
					INSTANCE = new Jugg();
				}
			}
		}
		if (INSTANCE.dispatcher == null) {
			INSTANCE.dispatcher = Dispatcher.getInstance(sContext);
		}
		return INSTANCE;
	}
}
