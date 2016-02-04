package cn.ljx.listviewdemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by liangjinxin on 2016/1/13.
 */
public class ImageAdapter extends ArrayAdapter<String> {

	/**
	 * 图片缓存的核心类，缓存下载图片，如果程序内存到达设定值会将最近最少使用的图片移除掉
	 *
	 * @param context
	 * @param resource
	 * @param objects
	 */

	private LruCache<String, BitmapDrawable> mMemoryCache;

	private ListView mListview;

	private Bitmap mLoadingBitmap;

	public ImageAdapter(Context context, int resource, String[] objects) {
		super(context, resource, objects);
		mLoadingBitmap = BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher);
		//获取应用程序的最大使用内存
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemory / 4;
		mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
			@Override
			protected int sizeOf(String key, BitmapDrawable drawable) {
				return drawable.getBitmap().getByteCount();
			}
		};
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (mListview==null){
			mListview= (ListView) parent;
		}
		String url = getItem(position);
//		Log.i("getView",url);
		View view;
		if (convertView == null) {
			view= LayoutInflater.from(getContext()).inflate(R.layout.image_item,null);
		} else {
			view = convertView;
		}
		ImageView iv = (ImageView) view.findViewById(R.id.iv);
//		iv.setTag(url);
		BitmapDrawable bitmapDrawable = getBitmapFromMemoryCache(url);

		if (bitmapDrawable!=null){

			iv.setImageDrawable(bitmapDrawable);
		}else if (canclePotentialWork(iv,url)){
			BitmapWorkerTask task = new BitmapWorkerTask(iv);
			AsyncDrawable asyncDrawable = new AsyncDrawable(getContext().getResources(),mLoadingBitmap,task);
			iv.setImageDrawable(asyncDrawable);
			task.execute(url);
		}
		return view;
	}

	/**
	 * 自定义一个Drawable 让drawable持有bitmapWorkerTask的弱引用
	 * @param url
	 * @return
	 */
	class AsyncDrawable extends BitmapDrawable{

		private WeakReference<BitmapWorkerTask> bitmapWorkerTaskWeakReference;
		public AsyncDrawable(Resources res, Bitmap bitmap,BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskWeakReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
		}


		public BitmapWorkerTask getBitmapWorkerTask(){
			return bitmapWorkerTaskWeakReference.get();
		}

	}

	/**
	 * 获取传入的imageview获取对应的bitmapWorkerTask
	 * @param url
	 * @return
	 *
	 */
	public BitmapWorkerTask getBitmapWorkerTask(ImageView iv){

		if (iv!=null){
			Drawable drawable = iv.getDrawable();
			if (drawable instanceof AsyncDrawable){
				AsyncDrawable asyncDrawable =(AsyncDrawable)drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	/**
	 * 取消掉后台潜在的任务，如果当前的ImageVIew存在另一个图片请求，那就把它取消掉
	 * 并返回 true。否则返回false
	 * @param url
	 * @return
	 */
	public boolean canclePotentialWork(ImageView iv,String url){
		BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(iv);
		if(bitmapWorkerTask!=null){
			String imageUrl = bitmapWorkerTask.imageUrl;
			if (imageUrl==null||!imageUrl.equals(url)){
				bitmapWorkerTask.cancel(true);
			}else{
				return false;
			}
		}

		return true;
	}




	private BitmapDrawable getBitmapFromMemoryCache(String url) {
		return mMemoryCache.get(url);
	}

	private void addBitmapToMemoryCache(String url, BitmapDrawable bitmapDrawable) {
		if (getBitmapFromMemoryCache(url) == null) {

			mMemoryCache.put(url, bitmapDrawable);
		}
	}

	private class BitmapWorkerTask extends AsyncTask<String,Void,BitmapDrawable>{
//		private  ImageView iv;
		String imageUrl;

//		public BitmapWorkerTask(ImageView iv) {
//			super();
//			this.iv=iv;
//		}
		private WeakReference<ImageView> imageViewWeakReference;

		public BitmapWorkerTask(ImageView iv){
			imageViewWeakReference = new WeakReference<ImageView>(iv);
		}


		@Override
		protected void onPostExecute(BitmapDrawable bitmapDrawable) {
//			ImageView iv = (ImageView) mListview.findViewWithTag(imageUrl);
			 ImageView iv = getAttachedImageView();
			if (iv!=null&&bitmapDrawable!=null){
				iv.setImageDrawable(bitmapDrawable);
			}
		
		}

		@Override
		protected BitmapDrawable doInBackground(String... params) {
			 imageUrl = params[0];
//			Log.i("IMageAdapter",imageUrl);
			Bitmap bitmap = downloadBitmap(imageUrl);
			BitmapDrawable bitmapDrawable = new BitmapDrawable(getContext().getResources(),bitmap);
			addBitmapToMemoryCache(imageUrl,bitmapDrawable);
			return bitmapDrawable;
		}

		/**
		 * 获取bitmapWorkerTask所关联的ImageView
		 * @param imageUrl
		 * @return
		 */
		public ImageView getAttachedImageView(){
			ImageView imageView = imageViewWeakReference.get();

			BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
			if (bitmapWorkerTask!=null&&this==bitmapWorkerTask){
				return imageView;
			}
			return null;
		}

		/**
		 * 联网下载图片
		 * @param imageUrl
		 * @return
		 */
		private Bitmap downloadBitmap(String imageUrl) {
			Bitmap bitmap=null;
			HttpURLConnection urlConnection=null;
			try {
				URL url = new URL(imageUrl);
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setConnectTimeout(4000);
				urlConnection.setReadTimeout(10000);
				bitmap = BitmapFactory.decodeStream(urlConnection.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
				Log.i("AsyncTask",e.getMessage());
			}finally {
				urlConnection.disconnect();
			}
			return bitmap;

		}
	}
}
