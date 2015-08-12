package com.simple.zrk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import android.R.integer;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.BitmapFactory.Options;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;

public class JuggAsyncTask extends AsyncTask<RequestControl, Integer, Bitmap> {
	private SoftReference<RequestControl> requestReference;

	private volatile boolean conlict;

	private static final int MIN_DECODE_WIDTH = 50;
	private static final int MIN_DECODE_HEIGHT = 50;

	private boolean isRunning = false;

	public JuggAsyncTask(RequestControl requestControl) {
		this.requestReference = new SoftReference<RequestControl>(
				requestControl);
	}

	public void exec(RequestControl request) {
		if (!isRunning) {
			this.execute(request);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		isRunning = true;
			
		if (requestReference.get().getPlaceholder()!=0) {
			requestReference.get().getTargetView()
			.setImageResource(requestReference.get().getPlaceholder());
		}
		
		if (this.requestReference.get() != null
				&& !this.requestReference.get().canceled
				&& this.requestReference.get().getTargetView() != null) {

			JuggCallback callback = requestReference.get().callback;
			if (callback != null) {
				callback.onPreLoad(requestReference.get().getTargetView(),
						requestReference.get().getKey());
			}
		}
		doResize(requestReference.get());
		doScaleType(requestReference.get());
		doRotate(requestReference.get());
	}

	private void doResize(final RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (!requestControl.needResizeWidth && !requestControl.needResizeHeight) {
			return;
		}
		Point size = requestControl.getTranformSize();
		final ImageView targetview = requestControl.getTargetView();
		if (targetview == null) {
			return;
		}
		LayoutParams params = targetview.getLayoutParams();
		if (size.x > 0) {
			requestControl.targetWidth = size.x;
		}
		if (size.y > 0) {
			requestControl.targetHeight = size.y;
		}
		params.width = size.x;
		params.height = size.y;
		targetview.setLayoutParams(params);
	}
	private void doRotate(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestControl.needRotate) {
			if (requestControl.getTargetView() != null) {
				requestControl.getTargetView().setRotation(
						requestControl.getRotationDegree());
			}
		}
	}

	private void doScaleType(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestControl.hasScaleType) {
			if (requestControl.getTargetView() != null) {
				ImageView targetView = requestControl.getTargetView();
				targetView.setScaleType(requestControl.getScaleType());
			}
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
		if (values[0] != null) {
			Utils.log("onProgressUpdate :" + values[0]);
			if (this.requestReference != null
					&& this.requestReference.get() != null
					&& this.requestReference.get().getTargetView() != null) {
				JuggCallback callback = this.requestReference.get().callback;
				if (callback != null) {
					int amount = -1;
					if (values.length > 1) {
						amount = values[1];
					}
					callback.onLoading(this.requestReference.get()
							.getTargetView(), requestReference.get().getKey(),
							values[0], amount);
				}
			}
		}
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		// TODO Auto-generated method stub
		if (result != null) {
			Utils.log("load bitmap finished at 1 ; "
					+ requestReference.get().getHashKey());
			if (requestReference.get() != null
					&& !requestReference.get().canceled) {
				Utils.log("load bitmap finished at 2; "
						+ requestReference.get().getHashKey());
				RequestControl request = requestReference.get();
				if (request.getTargetView() != null) {
					request.getTargetView().setImageBitmap(result);
				}
				request.getDispatcher().dispatchComplete(request);
				if (requestReference.get().callback != null) {
					requestReference.get().callback.onComplete(requestReference
							.get().getTargetView(), requestReference.get()
							.getKey());
				}
			} else {
				Utils.logE("SoftReference targetview has been cleaned!");
			}
		}
		super.onPostExecute(result);
	}

	@Override
	protected void onCancelled(Bitmap result) {
		// TODO Auto-generated method stub
		if (requestReference.get() != null) {
			if (requestReference.get().callback != null) {
				requestReference.get().callback
						.onLoadFailed(requestReference.get().getTargetView(),
								requestReference.get().getKey());
			}
			requestReference.get().getDispatcher()
					.dispatchCancel(requestReference.get());
		}
		super.onCancelled(result);
	}

	@Override
	protected Bitmap doInBackground(RequestControl... params) {
		// TODO Auto-generated method stub
		RequestControl requestControl = params[0];
		if (requestControl == null || requestControl.canceled) {
			cancel(true);
		}
		if (requestControl.getTargetView() == null) {
			cancel(true);
		}
		

		Bitmap bmp = requestControl.cache.get(requestControl.getKey());
		if (bmp != null) {
			requestControl.loadFrom = "cache";
			return bmp;
		}

		if (!Utils.judgeNetConnected(requestControl.networkInfo)) {
			Utils.logE("net work disconnect!!!");
			requestControl.getDispatcher().dispatchDisConnect(requestControl);
			if (requestControl.callback != null) {
				requestControl.callback
						.onLoadFailed(requestControl.getTargetView(),
								requestControl.getKey());
			}
			return null;
		}

		switch (requestControl.getType()) {
		case HTTP:
			requestControl.loadFrom = "http";
			bmp = loadFromHttp(requestControl);
			if (bmp == null) {
				Utils.logE("load from http get null  bitmap result");
				doError(requestControl);
			}
			break;
		case ASSET:
			requestControl.loadFrom = "asset";
			bmp = loadFromAsset(requestControl);
			if (bmp == null) {
				Utils.logE("load from asset get null bitmap result");
				doError(requestControl);
			}
			break;
		case FILE:
			requestControl.loadFrom = "file";
			bmp = loadFromFile(requestControl);
			if (bmp == null) {
				Utils.logE("load from file get null bitmap result");
				doError(requestControl);
			}
			break;
		case RESOURCE:
			requestControl.loadFrom = "resource";
			bmp = loadFromResource(requestControl);
			if (bmp == null) {
				Utils.logE("load from resource get null bitmap result");
				doError(requestControl);
			}
			break;
		default:
			throw new IllegalAccessError("unknown request type !!!!");
		}

		if (bmp != null) {
			requestControl.cache.put(requestControl.getKey(), bmp);
		}

		return bmp;
	}


	

	private Bitmap loadFromAsset(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestControl == null) {
			throw new NullPointerException("null request");
		}
		if (requestControl.getAssetUrl() == null
				|| "".equals(requestControl.getAssetUrl())) {
			throw new IllegalStateException(
					"asset path haven't been set yet before load a file image");
		}
		AssetManager asset = requestControl.getContext().getAssets();
		try {
			InputStream is = asset.open(requestControl.getAssetUrl());
			byte[] data = getBytesFromStream(is);
			Options opts = new Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, opts);
			int pw = requestControl.targetWidth;
			int ph = requestControl.targetHeight;
			int sampleRate = decodeSampleSize(pw, ph, opts.outWidth,
					opts.outHeight);
			opts.inSampleSize = sampleRate;
			opts.inJustDecodeBounds = false;
			Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length,
					opts);
			return bmp;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private Bitmap loadFromFile(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestControl == null) {
			throw new NullPointerException("null request");
		}
		if (requestControl.getFileName() == null
				|| "".equals(requestControl.getFileName())) {
			throw new IllegalStateException(
					"file path haven't been set yet before load a file image");
		}
		String path = requestControl.getFileName();
		File file = new File(path);
		if (!file.exists()) {
			return null;
		}
		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, opts);
		int pw = requestControl.targetWidth;
		int ph = requestControl.targetHeight;
		int sampleRate = decodeSampleSize(pw, ph, opts.outWidth, opts.outHeight);
		opts.inJustDecodeBounds = false;
		opts.inSampleSize = sampleRate;
		return BitmapFactory.decodeFile(path, opts);
	}

	private Bitmap loadFromResource(RequestControl requestControl) {
		if (requestControl == null) {
			throw new NullPointerException("null request");
		}
		if (requestControl.getResId() == 0) {
			throw new IllegalStateException(
					"res id haven't been set yet before load a resource");
		}

		Options opts = new Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(
				requestControl.getContext().getResources(),
				requestControl.getResId(), opts);
		int pw = requestControl.targetWidth;
		int ph = requestControl.targetHeight;
		int sampleRate = decodeSampleSize(pw, ph, opts.outWidth, opts.outHeight);
		opts.inJustDecodeBounds = false;
		opts.inSampleSize = sampleRate;
		Bitmap bmp = BitmapFactory.decodeResource(requestControl.getContext()
				.getResources(), requestControl.getResId(), opts);
		return bmp;
	}

	private void doError(RequestControl requestControl) {
		// TODO Auto-generated method stub
		Utils.logE("get " + requestControl.loadFrom + " bitmap error ");
		if (requestControl != null) {
			requestControl.getDispatcher().dispatchError(requestControl);
		}
	}

	private Bitmap loadFromHttp(RequestControl request) {
		// TODO Auto-generated method stub
		InputStream is = getHttpStream(request.getHttpUrl());
		if (is == null) {
			Utils.logE(getClass().getName()
					+ " getHttpStream return null stream");
			return null;
		}
		Bitmap bmp = decodeBitmap(is, request);
		try {
			if (is != null) {
				is.close();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return bmp;
	}

	private Bitmap decodeBitmap(InputStream is, RequestControl request) {
		// TODO Auto-generated method stub
		int pw = request.targetWidth;
		int ph = request.targetHeight;

		
		
		byte[] data = getBytesFromStream(is);
		if (data == null) {
			Utils.logE(getClass().getName()
					+ " getBytesFromStream return null byte array");
			return null;
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeByteArray(data, 0, data.length, options);
		Utils.log("decode primary stream photo options: w=" + options.outWidth
				+ ";h=" + options.outHeight);

		int sampleSize = decodeSampleSize(pw, ph, options.outWidth,
				options.outHeight);

		if (sampleSize == 0) {
			return BitmapFactory.decodeByteArray(data, 0, data.length);
		}

		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		return BitmapFactory.decodeByteArray(data, 0, data.length, options);
	}

	private int decodeSampleSize(int pw, int ph, int outWidth, int outHeight) {
		// TODO Auto-generated method stub

		if (pw >= outWidth && ph >= outHeight) {
			return 0;
		}

		pw = pw > MIN_DECODE_WIDTH ? pw : MIN_DECODE_WIDTH;
		ph = ph > MIN_DECODE_HEIGHT ? ph : MIN_DECODE_HEIGHT;

		int side = Math.max(pw, ph);

		int sample = 1;
		while (side < outWidth || side < outHeight) {
			outWidth = outHeight >> 1;
			outHeight = outHeight >> 1;
			sample = sample << 1;
			Utils.log("side:" + side + "; sample:" + sample + "; " + outWidth
					+ ":" + outHeight + ";");
		}
		return sample;
	}

	private byte[] getBytesFromStream(InputStream is) {
		// TODO Auto-generated method stub
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		byte[] data = null;
		int len = 0;
		int count = 0;

		try {
			while ((len = is.read(buffer)) != -1) {
				os.write(buffer, 0, len);
				count += len;
				int amount = is.available();
				if (requestReference.get() != null) {
					if (requestReference.get().callback != null) {
						publishProgress(count, amount);
					}
				}
			}
			data = os.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				is.close();
				os.close();
			} catch (Exception e2) {
				// TODO: handle exception
				e2.printStackTrace();
			}
		}
		return data;
	}

	private InputStream getHttpStream(String httpUrl) {
		// TODO Auto-generated method stub
		if (httpUrl == null || "".equals(httpUrl)) {
			throw new IllegalStateException(
					"get nul url in excute http request task");
		}
		try {
			URL url = new URL(httpUrl);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			// connection.setConnectTimeout(Jugg.DEFAULT_CONNECT_TIME_OUT);
			connection.setDoInput(true);
			connection.setRequestMethod("GET");
			if (checkResponseCode(connection)) {
				return connection.getInputStream();
			}
		} catch (MalformedURLException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private boolean checkResponseCode(HttpURLConnection connection) {
		// TODO Auto-generated method stub
		boolean res = false;
		try {
			switch (connection.getResponseCode()) {
			case HttpURLConnection.HTTP_ACCEPTED:
			case HttpURLConnection.HTTP_CREATED:
			case HttpURLConnection.HTTP_OK:
			case HttpURLConnection.HTTP_PARTIAL:
				res = true;
				break;

			default:
				res = false;
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}

}
