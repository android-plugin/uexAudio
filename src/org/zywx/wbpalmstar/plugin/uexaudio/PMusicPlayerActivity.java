/*
 *  Copyright (C) 2014 The AppCan Open Source Project.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.zywx.wbpalmstar.plugin.uexaudio;


import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PMusicPlayerActivity extends Activity implements OnCompletionListener {
	
	public static final String F_INTENT_DATA_KEY_AUDIOMGR_AUDIO_LIST = "audioList";
	public final static String TAG="MUSIC";
	public final static String F_SDKARD_PATH = "file:///sdcard/";//sd卡
	public final static String F_RES_PATH = "file:///res/";//工程的assets文件夹下
	public final static String F_DATA_PATH = "file:///data/";
	public final static String F_HTTP_PATH = "http://";//网络地址
	public final static String F_RES_ROOT_PATH = "file:///res/widget/";//工程的assets文件夹下widget文件夹
	public final static String F_WGT_PATH = "wgt://";//工程的assets文件夹下widget文件夹
	public final static String F_WGTS_PATH = "wgts://";//工程的assets文件夹下widget文件夹
	public final static String F_ABSOLUTE_PATH = "/";//绝对路径
	public final static String F_OPPOSITE_PATH = "";//相对路径
	public final static String F_BASE_RES_PATH="res://";//工程res目录
	public final static String F_BASE_PATH="BASE_PATH";//基本路径
	private ImageView mImage_special_image=null;//音乐专辑图片
	private Button mButton_return=null;//返回按钮
	private Button mButton_list=null;//返回按钮
	private TextView mText_music_name=null;//歌曲名称
	private SeekBar mBar_music_progress=null;//歌曲进度条
	private TextView mText_play_time=null;//歌曲当前时间
	private TextView mText_total_time=null;//歌曲总体时间
	private ImageButton mButton_last=null;//上一首歌曲按钮
	private ImageButton mButton_play=null;//播放歌曲按钮
	private ImageButton mButton_next=null;//下一首歌曲按钮
	private SeekBar mBar_sound_progress=null;//声音大小波动
	private String[] mString_path_array=null;//歌曲路径字符串数组
	private MediaPlayer mMediaplayer=null;
	private Integer mInt_music_index=0;//歌曲路径序号
	private AudioManager audioManager=null; //音频
	private int mInt_cache_time=0;//时间缓存
	private Integer mInt_total_time=0;//歌曲总共的播放时间
	private int mInt_max_volume=0;//volume max
	private Message mMsg=null;
	private int mInt_screen_width;//屏幕宽度
	private String mString_file_Path;//音乐文件路径
	private ResoureFinder finder = ResoureFinder.getInstance();
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 100:
				mText_play_time.setText(getTime(msg.arg1));
				break;
			case 88:
				if (mMediaplayer != null && mBar_music_progress != null) {
					if (mMediaplayer.isPlaying()) {
						int currentTime = mMediaplayer.getCurrentPosition();// get music current position	
						if (currentTime != 0) {
							mText_play_time.setText(getTime(currentTime));//show current position
							int progress = (currentTime * 100 / mInt_total_time);// 计算当前音乐播放进度
							if (mInt_cache_time != progress)
								mBar_music_progress.setProgress(progress);// 设置当前进度
							mInt_cache_time = progress;// 将当前音乐进度缓存下来
						}
						handler.sendEmptyMessageDelayed(88, 1000);
					}
				}
				break;
			default:
				break;
			}
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		setContentView(finder.getLayoutId(this, "plugin_audio_style"));
		
		WindowManager manager=getWindowManager();
		Display display=manager.getDefaultDisplay();
		mInt_screen_width=display.getWidth();
 		ButtonOnClickListener buttonListener=new ButtonOnClickListener();
 		SeekBarOnClickListener seekBarListener=new SeekBarOnClickListener();
 		mImage_special_image=(ImageView)findViewById(finder.getId(this, "music_special_image"));
 		mButton_return=(Button)findViewById(finder.getId(this, "plugin_music_button_return"));
 		mButton_return.setOnClickListener(buttonListener);
 		mButton_list=(Button)findViewById(finder.getId(this, "plugin_music_button_list"));
 		mButton_list.setOnClickListener(buttonListener);
 		mText_music_name=(TextView)findViewById(finder.getId(this, "plugin_music_name"));
 		mBar_music_progress=(SeekBar)findViewById(finder.getId(this, "widget_top_progress_seekBar"));
 		mBar_music_progress.setOnSeekBarChangeListener(seekBarListener);
 		mText_play_time=(TextView)findViewById(finder.getId(this, "widget_top_play_time"));
 		mText_total_time=(TextView)findViewById(finder.getId(this, "widget_top_total_time"));
 		mButton_last=(ImageButton)findViewById(finder.getId(this, "widget_bottom_button_last"));
 		mButton_last.setOnClickListener(buttonListener);
 		mButton_play=(ImageButton)findViewById(finder.getId(this, "widget_bottom_button_play"));
 		mButton_play.setOnClickListener(buttonListener);
 		mButton_next=(ImageButton)findViewById(finder.getId(this, "widget_bottom_button_next"));
 		mButton_next.setOnClickListener(buttonListener);
 		mBar_sound_progress=(SeekBar)findViewById(finder.getId(this, "plugin_centre_bottom_seekBar"));
 		mBar_sound_progress.setOnSeekBarChangeListener(seekBarListener);
 		audioManager=(AudioManager)getApplicationContext().getSystemService(Service.AUDIO_SERVICE);
 		mInt_max_volume=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
 		mString_path_array=getIntent().getStringArrayExtra(F_INTENT_DATA_KEY_AUDIOMGR_AUDIO_LIST);
 		mInt_music_index=getIntent().getIntExtra("fileIndex", 0);
 		initMusic();
	}

	private void initMusic() {
		try {
			if (mString_path_array != null) {
				if(mMediaplayer != null){
					mMediaplayer.stop();
					mMediaplayer.reset();
				}else{
					mMediaplayer = new MediaPlayer();
					mMediaplayer.reset();
				}
				mString_file_Path=mString_path_array[mInt_music_index];
				if (mString_file_Path != null && mString_file_Path.length() > 0) {
					initMusicInfo();//初始化界面，跟mediaplayer无关
					if (loadFile(mString_file_Path)) {
						mMediaplayer.setOnCompletionListener(this);// 播放完监听
						mMediaplayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
							@Override
							public boolean onError(MediaPlayer mp, int what, int extra) {
								mMediaplayer.reset();
								errorPrompt();
								return true;
							}
						});
						mMediaplayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {//播放器prepare后start
							@Override
							public void onPrepared(MediaPlayer mp) {
								setSpecialImage();
								setMediaInfo();
								mMediaplayer.start();
								mButton_play.setImageResource(mMediaplayer.isPlaying()?/*R.drawable.plugin_audio_pause_selector*/finder.getDrawableId(PMusicPlayerActivity.this, "plugin_audio_pause_selector"):/*R.drawable.plugin_audio_play_selector*/ResoureFinder.getInstance().getDrawableId(PMusicPlayerActivity.this, "plugin_audio_play_selector"));
								handler.sendEmptyMessage(88);
							}
						});
					}
				}
			}
		} catch (Exception e) {
			errorPrompt();
		}
	}

	private void setSpecialImage() {
		try {
			String musicPath = mString_file_Path;
			Cursor currentCursor = getCursor(musicPath.substring(musicPath.lastIndexOf('/') + 1));
			int album_id = currentCursor.getInt(currentCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
			String albumArt = getAlbumArt(album_id);
			Bitmap bm = null;
			if (albumArt != null)
				bm = BitmapFactory.decodeFile(albumArt);
			if (albumArt != null && bm != null) {
				mImage_special_image.setImageBitmap(ZoomBitmap(bm));
			}
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	/*
	 * 缩放专辑图片
	 */
	private Bitmap ZoomBitmap(Bitmap bp){
		if(bp!=null){
			int width=(mInt_screen_width/5)*3;
			int bp_width=bp.getWidth();
			int bp_height=bp.getHeight();
			float scale=((float)width)/bp_width;
			Matrix matrix = new Matrix();
		    matrix.postScale(scale, scale);
			return Bitmap.createBitmap(bp, 0, 0, bp_width, bp_height, matrix, true);			
		}
		return null;
	}
	/*
	 * 初始化界面控件，和mediaplayer无关的
	 */
	private void initMusicInfo(){
		/*
		 * 初始化专辑图片(如果歌曲有专辑图片，下面再更换)
		 */
		Bitmap bm = BitmapFactory.decodeResource(getResources(), finder.getDrawableId(this, "plugin_audio_special"));
		if (bm != null)
			mImage_special_image.setImageBitmap(ZoomBitmap(bm));
		
		mButton_play.setImageResource(finder.getDrawableId(this, "plugin_audio_play_selector"));//初始化播放按钮图片
		//show music name
		mText_music_name.setText(mString_file_Path.substring(mString_file_Path.lastIndexOf('/') + 1, mString_file_Path.lastIndexOf('.')));
		mText_play_time.setText("00:00");//初始化歌曲时间
		mText_total_time.setText("00:00");//初始化歌曲时间总长
		mBar_music_progress.setProgress(0);//
		mBar_music_progress.setSecondaryProgress(0);
		int volume=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		mBar_sound_progress.setProgress(volume*100/mInt_max_volume);//设置声音进度条
	}
	/*
	 * 初始化mediaplayer相关变量
	 */
	private void setMediaInfo(){
		//select ImageButton image
		mInt_total_time=mMediaplayer.getDuration();//得到歌曲时间总长
		mText_total_time.setText(getTime(mInt_total_time));//show music time length
		mMediaplayer.seekTo(0);
	}
	/*
	 * Button Listener
	 */
	class ButtonOnClickListener implements OnClickListener{
		@Override
		public void onClick(View v) {
			if (v.getId() == ResoureFinder.getInstance().getId(PMusicPlayerActivity.this, "plugin_music_button_return")) {
				finishActivity();
			} else if (v.getId() == ResoureFinder.getInstance().getId(PMusicPlayerActivity.this, "plugin_music_button_list")) {
				Intent intent = new Intent();
				intent.putExtra(F_INTENT_DATA_KEY_AUDIOMGR_AUDIO_LIST, mString_path_array);
				intent.setClass(PMusicPlayerActivity.this, PMusicFileActivity.class);
				startActivityForResult(intent, 0);
			}
			if (v.getId() == ResoureFinder.getInstance().getId(PMusicPlayerActivity.this, "widget_bottom_button_last")/*getTrendsID("widget_bottom_button_last", "id")*/) {
				if (mMediaplayer != null)
					if (mString_path_array != null && mInt_music_index > 0) {
						mInt_music_index--;
						initMusic();
					}
			}
			if (v.getId() == ResoureFinder.getInstance().getId(PMusicPlayerActivity.this, "widget_bottom_button_play")) {
				if (mMediaplayer != null && mBar_music_progress.getProgress() != 100)
					if (mMediaplayer.isPlaying()) {
						mMediaplayer.pause();
						mButton_play.setImageResource(finder.getDrawableId(PMusicPlayerActivity.this, "plugin_audio_play_selector"));
					} else {
						mMediaplayer.start();
						handler.sendEmptyMessage(88);
						mButton_play.setImageResource(finder.getDrawableId(PMusicPlayerActivity.this, "plugin_audio_pause_selector"));
					}
			}
			if (v.getId() == ResoureFinder.getInstance().getId(PMusicPlayerActivity.this, "widget_bottom_button_next")) {
				if (mMediaplayer != null)
					if (mString_path_array != null && mInt_music_index < mString_path_array.length - 1) {
						mInt_music_index++;
						initMusic();
					}
			}
		}
	}
	/*
	 * 接受文件列表返回的选择序号
	 * (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			mInt_music_index=data.getIntExtra("listIndex", 0);
			initMusic();
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	private int startProgress=0;//缓存的音乐进度progress
	class SeekBarOnClickListener implements OnSeekBarChangeListener{
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			if(seekBar.getId()==ResoureFinder.getInstance().getId(PMusicPlayerActivity.this, "widget_top_progress_seekBar")){
				startProgress = seekBar.getProgress();// 缓存按下时的Progress值
			}
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			try{
				if(seekBar.getId()==ResoureFinder.getInstance().getId(PMusicPlayerActivity.this, "widget_top_progress_seekBar")){
					if(mMediaplayer!=null && startProgress!=100){//如果音乐没有播放完毕，可以设定播放进度
						mMediaplayer.pause();	
						mMediaplayer.seekTo(mInt_total_time/100*mBar_music_progress.getProgress());
						mMediaplayer.start();handler.sendEmptyMessage(88);
						mButton_play.setImageResource(finder.getDrawableId(PMusicPlayerActivity.this, "plugin_audio_pause_selector"));
					}else if(startProgress==100){//如果播放完毕就不许进度条再设定其他值
						mBar_music_progress.setProgress(100);
					}
				}else if(seekBar.getId()==ResoureFinder.getInstance().getId(PMusicPlayerActivity.this, "plugin_centre_bottom_seekBar")){
					int mediaPlayerVolume=mBar_sound_progress.getProgress();			
					int volume=mInt_max_volume*mediaPlayerVolume/100;//获取设置音量
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);

				}			
			}catch(Exception e){
			    e.printStackTrace();
			}		
		}
		
	}
	private AlertDialog alertDialog;

	// 弹出消息框
	private void alertMessage(String message, final boolean exitOnConfirm) {

		alertDialog = new AlertDialog.Builder(this).setTitle(finder.getString("prompt")).setMessage(message)
				.setCancelable(false)
				.setPositiveButton(finder.getString("confirm"), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (exitOnConfirm) {
							finishActivity();
							dialog.dismiss();
							PMusicPlayerActivity.this.finish();
						}
					}
				}).create();

		alertDialog.show();
	}
	/*
	 * 根据inPath导入路径的开头判断导入方式
	 */
	boolean isPrepare=false;
	public boolean loadFile(String inPath){
		try{
			if (inPath.indexOf(BUtility.F_Widget_RES_path) >= 0) {// (res://)
				final AssetFileDescriptor descriptor = this.getAssets().openFd(inPath.substring(inPath.indexOf(BUtility.F_Widget_RES_path)));
				if (descriptor == null) {
					alertMessage(finder.getString("plugin_audio_info_nofile"), true);
				} else {
					mMediaplayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
				}
			} else {
				Uri uri = Uri.parse(inPath);
				mMediaplayer.setDataSource(this.getApplicationContext(), uri);
			}
			mMediaplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			
			mMediaplayer.prepareAsync();
			mMediaplayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
				@Override
				public void onBufferingUpdate(MediaPlayer mp, int percent) {
					try {
						if (percent == 100 && !isPrepare) {
							mBar_music_progress.setSecondaryProgress(percent);
							isPrepare = true;
							handler.sendEmptyMessage(88);
						} else if (!isPrepare) {
							mBar_music_progress.setSecondaryProgress(percent);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}catch(Exception e){
		    e.printStackTrace();
			return false;	
		}
		return true;
	}

	/*
	 * SD卡目录
	 */
	public String getSDDirectory(){
		return Environment.getExternalStorageDirectory()+"/";
	}
	//得到小时
	private String getTime(Integer time) {
		int hours = time/3600>0?time/3600/1000:0;// 小时
		int minutes = (time / 1000 - hours * 3600) / 60;
		int seconds = time / 1000 % 60;
		return (minutes < 10 ? "0" + minutes : "" + minutes) +":"+ (seconds < 10 ? "0" + seconds : "" + seconds);
	}
/*
 * 播放完毕监听
 * @see android.media.MediaPlayer.OnCompletionListener#onCompletion(android.media.MediaPlayer)
 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		mBar_music_progress.setProgress(100);//音乐进度100%
		mMediaplayer.stop();//停止音乐
		/*将显示时间设为最大*/
		mMsg = handler.obtainMessage();
		mMsg.what = 100;
		mMsg.arg1 = mInt_total_time;
		handler.sendMessage(mMsg);
		/*设置播放图标*/
		mButton_play.setImageResource(finder.getDrawableId(this, "plugin_audio_play_selector"));
		
	}


	private Cursor getCursor(String filePath) {
		String path = null;
		Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		if (c.moveToFirst()) {
			do {
				path = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
				if (path.indexOf(filePath) != -1) {
					break;
				}
			} while (c.moveToNext());
		}
		return c;
	}

	private String getAlbumArt(int album_id) {
		String mUriAlbums = "content://media/external/audio/albums";
		String[] projection = new String[] { "album_art" };
		Cursor cur = this.getContentResolver().query(Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)), projection, null, null, null);
		String album_art = null;
		if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
			cur.moveToNext();
			album_art = cur.getString(0);
		}
		cur.close();
		cur = null;
		return album_art;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			finishActivity();
			break;
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		finishActivity();
		super.onDestroy();
	}
	/*
	 * 出错提示
	 */
	private void errorPrompt(){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(finder.getStringId(this, "prompt"));
		alert.setMessage(finder.getStringId(this, "plugin_audio_info_nofile"));
		alert.setPositiveButton(finder.getStringId(this, "confirm"), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finishActivity();
			}
		});
		alert.show();
	}
	/*
	 * finish activity code
	 */
	private void finishActivity() {
		if (mMediaplayer != null) {
			if (mMediaplayer.isPlaying())
				mMediaplayer.stop();
			mMediaplayer.release();
		}
		mMediaplayer = null;
		finish();
	}
}
