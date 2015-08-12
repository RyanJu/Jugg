package com.simple.zrk;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import android.R.drawable;
import android.R.integer;
import android.R.string;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.ImageView;

public class Dispatcher {
	private ConcurrentHashMap<String, RequestControl> requestMap;
	private Cache cache;
	private Context context;
	private NetWorkReciever netWorkReciever;
	public List<WeakReference<RequestControl> > reloadList;
	private DispatchHandler dispatchHandler;
	private DispatchThread dispatchThread;

	private Dispatcher(Context context) {
		dispatchThread = new DispatchThread();
		dispatchThread.start();
		requestMap = new ConcurrentHashMap<String, RequestControl>();
		reloadList = new ArrayList<WeakReference<RequestControl>>();
		this.cache = Cache.getCache(context);
		this.context = context;
		registNetReciever(context);
		this.dispatchHandler = new DispatchHandler(dispatchThread.getLooper(), this);
	}

	public Map<String, RequestControl> getRequestMap() {
		return requestMap;
	}
	
	private void registNetReciever(Context context) {
		// TODO Auto-generated method stub
		if (netWorkReciever!=null) {
			return ;
		}
		netWorkReciever=new NetWorkReciever();
		IntentFilter filter=new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(netWorkReciever, filter);
	}

	private static class DispatchHandler extends Handler{
		private Dispatcher dispatcher;
		public DispatchHandler(Looper looper,Dispatcher dispatcher){
			super(looper);
			this.dispatcher=dispatcher;
		}
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case MessageId.EXCUTE:
				dispatcher.handleExecute(msg);
				break;
			case MessageId.ERROR:
				dispatcher.handleError(msg);
				break;
			case MessageId.COMPLETE:
				dispatcher.handleComplete(msg);
				break;
			case MessageId.DISCONNECT:
				dispatcher.handleDisConnect(msg);
				break;
			case MessageId.RELOAD:
				dispatcher.handleReload(msg);
				break;
			case MessageId.MEASURE:
				dispatcher.handleMeasure(msg);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	}
	
	private Handler mainThreadHandler = new Handler(Looper.getMainLooper()){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MessageId.BIND_DRAWABLE:
				String key=(String) msg.obj;
				RequestControl reqeust = requestMap.get(key);
				if (reqeust!=null ) {
					if (reqeust.getTargetView()!=null && reqeust.juggDrawable!=null) {
						Drawable d=reqeust.getTargetView().getDrawable();
						if (d instanceof JuggDrawable) {
							JuggDrawable jd=(JuggDrawable) d;
							if (jd.getJuggAsyncTask()!=null && jd.getJuggAsyncTask()==reqeust.juggDrawable.getJuggAsyncTask()) {
								break;
							}
						}
						reqeust.getTargetView().setImageDrawable(reqeust.juggDrawable);
						if (reqeust.juggDrawable.getJuggAsyncTask()!=null) {
							reqeust.juggDrawable.getJuggAsyncTask().exec(reqeust);
						}
					}
				}
				break;
			case MessageId.ERROR:
				String hashkey=(String) msg.obj;
				RequestControl reqError=requestMap.get(hashkey);
				if (reqError!=null) {
					if (reqError.getTargetView()!=null && reqError.getErrorholder()!=0) {
						reqError.getTargetView().setImageResource(reqError.getErrorholder());
					}
					requestMap.remove(hashkey);
				}
				break;
			default:
				break;
			}
		};
	};
	

	//检查缓存在JuggAsyntask里做，不在这里检查
	//这里只检查map同一个key的冲突
	public void dispatchSubmit(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (checkConfict(requestControl)) {
			return;
		}
		requestMap.put(requestControl.getHashKey(), requestControl);
		JuggAsyncTask task = new JuggAsyncTask(requestControl);
		if (requestControl.getPlaceholder() != 0) {
			requestControl.juggDrawable = new JuggDrawable(
					requestControl.getContext(),
					null, task);
		} else {
			requestControl.juggDrawable = new JuggDrawable(
					requestControl.getContext(), null, task);
		}
		
		if (requestControl.needMeasureTarget()) {
			dispatchMeasure(requestControl);
		}
		dispatchExcute(requestControl);
	}


	public void handleMeasure(Message msg) {
		// TODO Auto-generated method stub
		String hashKey=(String) msg.obj;
		final RequestControl request = requestMap.get(hashKey);
		if (request.getTargetView()!=null) {
			final ImageView view=request.getTargetView();
			view.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
				@Override
				public boolean onPreDraw() {
					// TODO Auto-generated method stub
					request.targetWidth=view.getWidth();
					request.targetHeight=view.getHeight();
					Utils.log("handleMeasure", request.targetWidth+" ; "+request.targetHeight);
					view.getViewTreeObserver().removeOnPreDrawListener(this);
					return false;
				}
			});
		}
	}

	private void dispatchMeasure(RequestControl requestControl) {
		// TODO Auto-generated method stub
		dispatchHandler.obtainMessage(MessageId.MEASURE, requestControl.getHashKey()).sendToTarget();
	}

	protected void handleReload(Message msg) {
		// TODO Auto-generated method stub
		String hashkey=(String) msg.obj;
		Utils.log("handleReload "+hashkey);
		RequestControl request = requestMap.get(hashkey);
		if (request!=null && !request.canceled) {
			if (request.getTargetView()!=null) {
				Drawable d = request.getTargetView().getDrawable();
				if (d==null ) {
					return ;
				}
				if (d instanceof JuggDrawable) {
					 JuggDrawable juggDrawable=(JuggDrawable) d;
					 JuggAsyncTask task = juggDrawable.getJuggAsyncTask();
					 if (task!=null) {
						task.exec(request);
						Utils.log("handleReload "+hashkey+" ; task reExecute");
					}
				}
			}
		}
	}

	protected void handleDisConnect(Message msg) {
		// TODO Auto-generated method stub
		String hashkey=(String) msg.obj;
		RequestControl request=requestMap.get(hashkey);
		if (request!=null && !request.canceled) {
			reloadList.add(new WeakReference<RequestControl>(request));
		}
	}

	protected void handleExecute(Message msg) {
		// TODO Auto-generated method stub
		String hashkey=(String) msg.obj;
		if (!requestMap.containsKey(hashkey)) {
			return ;
		}
		RequestControl requst = requestMap.get(hashkey);
		if (requst.juggDrawable==null) {
			return;
		}
		if (requst.getTargetView()==null) {
			return ;
		}
		mainThreadHandler.obtainMessage(MessageId.BIND_DRAWABLE, hashkey).sendToTarget();
	}

	private void dispatchExcute(RequestControl requestControl) {
		// TODO Auto-generated method stub
		dispatchHandler.obtainMessage(MessageId.EXCUTE,requestControl.getHashKey()).sendToTarget();
	}

	// 解决同一个Url对应不同的imageview的冲突：原始的hashkey就是key，如果有冲突则更改hashkey
	private boolean checkConfict(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestMap==null) {
			throw new NullPointerException("checkConfict map is null");
		}
		if (requestControl==null) {
			throw new NullPointerException("checkConfict request is null");
		}
		if (requestMap.containsKey(requestControl.getHashKey())) {
			if (requestControl.getTargetView() == requestMap.get(
					requestControl.getHashKey()).getTargetView()) {
				Utils.log("checkConfict same request ,ignore it");
				return false;
			}
			Utils.log("checkConfict same key with different request , set it with a new hashkey");
			requestControl.setHashKey(Utils.generateHashKey());
		}
		return false;
	}


	public void dispatchComplete(RequestControl requestControl) {
		// TODO Auto-generated method stub
		String hashkey = requestControl.getHashKey();
		if (requestMap.containsKey(hashkey)) {
			dispatchHandler.obtainMessage(MessageId.COMPLETE, hashkey).sendToTarget();
			Utils.log("dispatchComplete "+requestControl.getHashKey());
		}
	}

	protected void handleComplete(Message msg) {
		// TODO Auto-generated method stub
		String hashkey = (String) msg.obj;
		RequestControl requestControl = requestMap.get(hashkey);
		if (requestControl != null) {
			RequestControl reques = requestMap.remove(hashkey);
			reques=null;
			Utils.log("handleComplete remove "+requestControl.getHashKey()+ " from map");
		}
	}




	public void dispatchError(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestControl==null || requestControl.canceled) {
			return;
		}
		String hashkey = requestControl.getHashKey();
		if (requestControl.getErrorholder()!=0 && requestControl.getTargetView()!=null) {
			Utils.logE("dispatch error at key "+requestControl.getHashKey() );
			dispatchHandler.obtainMessage(MessageId.ERROR, hashkey).sendToTarget();
		}
	}

	protected void handleError(Message msg) {
		// TODO Auto-generated method stub
		String hashkey = (String) msg.obj;
		RequestControl requestControl = requestMap.get(hashkey);
		if (requestControl!=null ) {
			if (requestControl.retryTimes>0) {
				requestControl.retryTimes--;
				dispatchReload(requestControl);
			}else {
				if (requestControl.getTargetView()!=null) {
//					requestControl.getTargetView().setImageResource(requestControl.getErrorholder());
//					requestMap.remove(hashkey);
					mainThreadHandler.obtainMessage(MessageId.ERROR, hashkey).sendToTarget();
				}
			}
		}
	}

	public void dispatchReload(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestControl ==null || requestControl.canceled) {
			return ;
		}
		Utils.log("dispatchReload "+requestControl.getHashKey());
		String hashkey =requestControl.getHashKey();
		dispatchHandler.obtainMessage(MessageId.RELOAD, hashkey).sendToTarget();
	}
	


	public void dispatchDisConnect(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestControl!=null && !requestControl.canceled) {
			Utils.log("dispatchDisConnect "+requestControl.getHashKey());
			dispatchHandler.obtainMessage(MessageId.DISCONNECT,requestControl.getHashKey()).sendToTarget();
		}
	}
	
	
	public void dispatchCancel(RequestControl requestControl) {
		// TODO Auto-generated method stub
		if (requestControl==null) {
			return ;
		}
		if (requestMap.containsKey(requestControl.getHashKey())) {
			requestMap.remove(requestControl.getHashKey());
		}
	}
	
	
	private static Dispatcher singleton;

	public static Dispatcher getInstance(Context context) {
		if (singleton == null) {
			synchronized (Dispatcher.class) {
				if (singleton == null) {
					singleton = new Dispatcher(context);
				}
			}
		}
		return singleton;
	}

	public static class MessageId {
		static final int MEASURE = 0x1007;
		final static int BIND_DRAWABLE = 0x1008;
		final static int EXCUTE = 0x1009;
		final static int ERROR = 0x1100;
		final static int COMPLETE = 0x1108;
		final static int DISCONNECT= 0x1109;
		final static int RELOAD= 0x1110;
	}

	private class NetWorkReciever extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action=intent.getAction();
			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
				Utils.log("net reciever "+action);
				ConnectivityManager manager=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo netInfo = manager.getActiveNetworkInfo();
				if (Utils.judgeNetConnected(netInfo)) {
					if (reloadList!=null && !reloadList.isEmpty()) {
						for (WeakReference<RequestControl> requestReference : reloadList) {
							if (requestReference!=null && requestReference.get()!=null) {
								dispatchReload(requestReference.get());
							}
						}
					}
				}
			}
		}
	}

	public void clean() {
		// TODO Auto-generated method stub
		Log.e("clean ", "clean");
		unregistNetReciever();
		for (WeakReference<RequestControl> requestReference : reloadList) {
			if (requestReference!=null && requestReference.get()!=null) {
				requestReference.clear();
			}
		}
		for (Entry<String, RequestControl> entry : requestMap.entrySet()) {
			RequestControl req=entry.getValue();
			if(req!=null && !req.canceled){
				req.cancel();
			}
			requestMap.remove(entry.getKey());
			req=null;
		}
	}

	public void cleanCache(){
		if (cache!=null) {
			cache.cleanDiskCache();
			cache.cleanMemroyCache();
		}
	}
	private void unregistNetReciever() {
		// TODO Auto-generated method stub
		if (netWorkReciever!=null) {
			context.unregisterReceiver(netWorkReciever);
			netWorkReciever=null;
		}
	}

	
	
	private static class DispatchThread extends HandlerThread{
		private static String DISPATCH_THREAD_NAME="dispatcher_thread";
		public DispatchThread() {
			super(DISPATCH_THREAD_NAME);
		}
	}


}
