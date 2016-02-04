package cn.ljx.listviewdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.ImageLoader.ImageCache;

/**
 * 使用Volley加载图片
 * Created by liangjinxin on 2016/1/14.
 */
public class ImageAdapter2 extends ArrayAdapter<String> {

	private  ImageLoader mImageLoader;

	public ImageAdapter2(Context context, int resource, String[] objects) {
		super(context, resource, objects);
		RequestQueue requestQueue = Volley.newRequestQueue(context);
		mImageLoader = new ImageLoader(requestQueue,new BitmapCache());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String url = getItem(position);
		View view;
		if (convertView==null){
			view=View.inflate(parent.getContext(),R.layout.image_item,null);
		}else{
			view=convertView;
		}
		NetworkImageView iv = (NetworkImageView) view.findViewById(R.id.iv);
		iv.setDefaultImageResId(R.mipmap.ic_launcher);
		iv.setErrorImageResId(R.mipmap.ic_launcher);
		iv.setImageUrl(url,mImageLoader);
		return view;
	}

	public class BitmapCache implements ImageCache {

		private  LruCache<String, Bitmap> mLruCache;

		public BitmapCache(){
			int maxMemory = (int) Runtime.getRuntime().maxMemory();
			mLruCache = new LruCache<String,Bitmap>(maxMemory/8){
				@Override
				protected int sizeOf(String key, Bitmap value) {
					return value.getRowBytes()*value.getHeight();
				}
			};
		}
		@Override
		public Bitmap getBitmap(String url) {
			return mLruCache.get(url);
		}

		@Override
		public void putBitmap(String url, Bitmap bitmap) {
			mLruCache.put(url,bitmap);
		}


		//Volley
	}
}
