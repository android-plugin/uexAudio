package org.zywx.wbpalmstar.plugin.uexaudio;

import org.zywx.wbpalmstar.base.ResoureFinder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

public class PMusicFileActivity extends Activity implements OnItemClickListener, OnClickListener/*, onTextItemListener*/ {
	private ListView m_file_list=null;
	private Button m_file_back_button=null;
	private String[] list;
	private MusicFileAdapter adapter=null;
	private ResoureFinder finder = ResoureFinder.getInstance();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		list=getIntent().getStringArrayExtra(PMusicPlayerActivity.F_INTENT_DATA_KEY_AUDIOMGR_AUDIO_LIST);
		setContentView(finder.getLayoutId(this, "plugin_audio_file"));
		m_file_back_button=(Button)findViewById(finder.getId(this, "music_file_play_button"));
		m_file_back_button.setOnClickListener(this);			
		
		m_file_list=(ListView)findViewById(finder.getId(this, "music_file_play_list"));
		adapter=new MusicFileAdapter(this,list);
		m_file_list.setAdapter(adapter);
		m_file_list.setOnItemClickListener(this);
	
		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Intent intent = new Intent();
		intent.putExtra("listIndex", arg2);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onClick(View arg0) {
		if(arg0.getId()==finder.getId(this, "music_file_play_button")){
			finish();
		}
	}
}
