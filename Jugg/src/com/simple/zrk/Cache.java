package com.simple.zrk;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.Context;
import android.graphics.Bitmap;

public class Cache {
	private MemoryCache memoryCache;
	private DiskCache diskCache;
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public Cache(Context context) {
		int maxSize = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
		memoryCache = new MemoryCache(maxSize);
		diskCache = DiskCache.getCache(context);
	}

	public void put(String key, Bitmap value) {
		try {
			lock.writeLock().tryLock(Jugg.DEFAULT_CONNECT_TIME_OUT,TimeUnit.MILLISECONDS);
			memoryCache.put(key, value);
			diskCache.put(key, value);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally{
			lock.writeLock().unlock();
		}
	}

	public Bitmap get(String key) {
		Bitmap output = null;
		try {
			lock.readLock().tryLock(Jugg.DEFAULT_CONNECT_TIME_OUT,TimeUnit.MILLISECONDS);
			output = memoryCache.get(key);
			if (output == null || output.isRecycled()) {
				output = diskCache.get(key);
				if (output != null) {
					memoryCache.put(key, output);
					Utils.log("load from disk cache");
				}
				return output;
			}
			Utils.log("load from memory cache");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			lock.readLock().unlock();
		}
		return output;
	}

	public void cleanMemroyCache(){
		if (memoryCache!=null) {
			memoryCache.clear();
		}
	}
	
	public void cleanDiskCache(){
		if (diskCache!=null) {
			diskCache.clearCache();
		}
	}
	
	private static Cache singleton = null;

	public static Cache getCache(Context context) {
		if (singleton == null) {
			synchronized (Cache.class) {
				if (singleton == null) {
					singleton = new Cache(context);
				}
			}
		}
		return singleton;
	}

	private class MemoryCache implements ICache {
		private int size;
		private final int maxSize;
		private int hitCount;
		private int missCount;
		private int putCount;
		private int evictCount;
		private LinkedHashMap<String, Bitmap> map;
		private ReentrantReadWriteLock lock=new ReentrantReadWriteLock();
		
		public MemoryCache(final int maxSize) {
			this.maxSize = maxSize;
			size = 0;
			hitCount = 0;
			missCount = 0;
			putCount = 0;
			evictCount = 0;
			map = new LinkedHashMap<String, Bitmap>(0, 0.75f, true);
		}

		@Override
		public Bitmap get(String key) {
			// TODO Auto-generated method stub
			if (key == null) {
				throw new NullPointerException(getClass().getName() + " " + key);
			}
			Bitmap val = null;
			try {
				lock.readLock().lock();
				val = map.get(key);
				if (val != null && !val.isRecycled()) {
					hitCount++;
				} else {
					missCount++;
				}
				
			} catch (Exception e) {
				// TODO: handle exception
			}finally{
				lock.readLock().unlock();
			}
			return val;
		}

		@Override
		public void put(String key, Bitmap value) {
			// TODO Auto-generated method stub
			Bitmap previous = null;
			try {
				lock.writeLock().lock();
				previous = map.put(key, value);
				putCount++;
				size += Utils.getBitmapSize(value);
				if (previous != null) {
					size -= Utils.getBitmapSize(previous);
				}
				trimToSize(maxSize);
			} catch (Exception e) {
				// TODO: handle exception
			}finally{
				lock.writeLock().unlock();
			}
		}

		private synchronized void trimToSize(int s) {
			// TODO Auto-generated method stub
			while (true) {
				if (size < 0 || (size > 0 && map.isEmpty())) {
					throw new IllegalStateException(getClass().getName()
							+ "memory cache internal error");
				}
				if (size <= s || map.isEmpty()) {
					return;
				}
				Entry<String, Bitmap> toEvict = map.entrySet().iterator()
						.next();
				String key = toEvict.getKey();
				Bitmap val = toEvict.getValue();
				map.remove(key);

				size -= Utils.getBitmapSize(val);
				evictCount++;
				val.recycle();
				val = null;
			}
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return size;
		}

		@Override
		public int maxSize() {
			// TODO Auto-generated method stub
			return maxSize;
		}

		@Override
		public void clear() {
			// TODO Auto-generated method stub
			trimToSize(0);
		}

	}

	interface ICache {
		public Bitmap get(String key);

		public void put(String key, Bitmap value);

		public int size();// the current size in kb

		public int maxSize();// the max size in kb

		public void clear();
	}

}
