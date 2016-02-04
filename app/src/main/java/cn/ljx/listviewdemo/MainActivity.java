package cn.ljx.listviewdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ListView lv= (ListView) findViewById(R.id.lv);
//		ImageAdapter adapter = new ImageAdapter(this,0,Image.imageUrls);
		ImageAdapter2 adapter = new ImageAdapter2(this,0,Image.imageUrls);
		lv.setAdapter(adapter);
	}
}
