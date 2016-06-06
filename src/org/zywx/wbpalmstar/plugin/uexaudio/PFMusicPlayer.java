package org.zywx.wbpalmstar.plugin.uexaudio;

import java.io.File;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public abstract class PFMusicPlayer {
	
	public final static String F_SDKARD_PATH = "/sdcard/";//SD卡
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
	public String basePath="";
	
	private MediaPlayer m_mediaPlayer=null;
	private SoundPool m_soundPool=null;
	private Context m_context=null;
	private final static int MEDIAPLAY_STATE_PLAYING=0;//播放状态
	private final static int MEDIAPLAY_STATE_PAUSEING=1;//暂停状态
	private final static int MEDIAPLAY_STATE_STOPING=2;//停止状态
	private int playState=0;
	public static int soundID=0;
	private ResoureFinder finder = ResoureFinder.getInstance();
	
	private int loopCount = 0;
	private int loopIndex = 0;
	
	private boolean isModeInCall = false;

	public PFMusicPlayer(Context inContext){
		m_context=inContext;
	}

	public int getLoopIndex(){
		return loopIndex;
	}
	
	public void setModeInCall(boolean isModeInCall){
		this.isModeInCall = isModeInCall;
	}

	/*
	 * SD卡目录
	 */
	public String getSDDirectory(){
		return Environment.getExternalStorageDirectory()+"/";
	}

	public void openSound(int maxStream, int streamType, int srcQuality) {
		if (m_soundPool == null){
			m_soundPool = new SoundPool(maxStream, streamType, srcQuality);
		}
	}

	public int loadSound(String inPath) {
		soundID = 0;
		try {
			if(inPath.startsWith(PFMusicPlayer.F_SDKARD_PATH) || inPath.startsWith(PFMusicPlayer.F_HTTP_PATH) || inPath.startsWith("/mnt/sdcard/")){//(wgt://和wgts://和sd卡开头一致)(网络)
				soundID = m_soundPool.load(inPath, 1);
			}else if(inPath.startsWith(BUtility.F_Widget_RES_path)){//(res://)
				final AssetFileDescriptor descriptor = this.m_context.getAssets().openFd(inPath);
				if (descriptor == null) {
					alertMessage(finder.getString("plugin_audio_info_nofile"), true);
				} else {
					soundID = m_soundPool.load(descriptor, 1);	
				}
			}else if(inPath.startsWith("/data/data/")){//(box://)
				soundID = m_soundPool.load(inPath, 1);
			}else if(inPath.startsWith("/")){
				File myFile = new File(inPath);
				soundID = m_soundPool.load(myFile.getAbsolutePath(), 1);
			}else{
				alertMessage(finder.getString("plugin_audio_info_nofile") + inPath, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return soundID;
	}

	//因为每次打开返回的id不同，停止方法中需要用到该id
	public int playSound(int soundID) {
		if(m_soundPool!=null){
			int inStreamID = m_soundPool.play( soundID, 3, 3, 0, -1, 1);
			
			if (inStreamID == 0) {
				stopSound(soundID);
			} else {
				
			}
			return inStreamID;
		}
		return 0;		
	}

	public void stopSound(int inStreamID) {
		if (m_soundPool != null) {
			m_soundPool.stop( inStreamID );
			checkModeEnd();
		}
	}

	/*
	 * 创建meidaplayer对象,提供调用
	 */
	public void open() {
		if (m_mediaPlayer == null) {
			m_mediaPlayer = new MediaPlayer();
			m_mediaPlayer.setOnCompletionListener(new mediaPlayerCompletionListener());
			m_mediaPlayer.setOnErrorListener(new mediaPlayerErrorListener());
			playState=MEDIAPLAY_STATE_STOPING;
		}
	}

	/*
	 * 播放完音乐监听
	 */
	class mediaPlayerCompletionListener implements OnCompletionListener {
		@Override
		public void onCompletion(MediaPlayer mp) {
			if (loopCount > 0 && loopIndex < loopCount) {
				checkModeStart();
				m_mediaPlayer.start();
				playState = MEDIAPLAY_STATE_PLAYING;
				onPlayFinished(loopIndex);
				loopIndex++;
			} else {
				m_mediaPlayer.stop();
				playState = MEDIAPLAY_STATE_STOPING;
				onPlayFinished(loopIndex);
				checkModeEnd();
			}
		}
	}

	/*
	 * 播放完音乐（提供给EUEX对象回调）
	 */
	public abstract void onPlayFinished(int index);
	/*
	 * 音乐出错监听
	 */
	class mediaPlayerErrorListener implements OnErrorListener{
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			m_mediaPlayer.release();
			playState=MEDIAPLAY_STATE_STOPING;
			return false;
		}
	}

	/*
	 * 根据inPath路径导入播放音乐路径
	 */
	public void play(String inPath,int loopType) {
		if (m_mediaPlayer != null) {
			try {
				checkModeStart();
				switch(playState){
				case MEDIAPLAY_STATE_STOPING:
					m_mediaPlayer.reset();
					if (loadMediaPlayerFile(inPath)) {// 读取文件成功才执行
						m_mediaPlayer.prepare();
						switch (loopType) {
						case -1:
							m_mediaPlayer.setLooping(true);
							loopCount = 0;
							loopIndex = 0;
							break;
						case 0:
							m_mediaPlayer.setLooping(false);
							loopCount = 0;
							loopIndex = 0;
							break;
						default:
							if(loopType>0){
								m_mediaPlayer.setLooping(false);
								loopCount = loopType-1;
								loopIndex = 0;
							}
							break;
						}
						m_mediaPlayer.start();
						playState = MEDIAPLAY_STATE_PLAYING;
					}
					break;
				case MEDIAPLAY_STATE_PAUSEING:
					m_mediaPlayer.start();
					playState=MEDIAPLAY_STATE_PLAYING;
					break;
				}
			} catch (Exception e) {
				Toast.makeText(m_context, ResoureFinder.getInstance().getStringId(m_context, "plugin_audio_info_nofile"), Toast.LENGTH_LONG).show();
				checkModeEnd();
			}
		}
	}
	
	private AlertDialog alertDialog;

	// 弹出消息框
	private void alertMessage(String message, final boolean exitOnConfirm) {

		alertDialog = new AlertDialog.Builder(m_context).setTitle(finder.getString("prompt")).setMessage(message)
				.setCancelable(false)
				.setPositiveButton(finder.getString("confirm"), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (exitOnConfirm) {
							dialog.dismiss();
							stop();
						}
					}
				}).create();

		alertDialog.show();
	}

	/*
	 * 根据inPath的内容判断导入方式
	 */
	public boolean loadMediaPlayerFile(String inPath) throws Exception{
		boolean isLoadMediaPlayerFileSucceed = true;
		try {
			if (inPath.indexOf(BUtility.F_Widget_RES_path) >= 0) {// (res://)
				final AssetFileDescriptor descriptor = this.m_context.getAssets().openFd(inPath.substring(inPath.indexOf(BUtility.F_Widget_RES_path)));
				if (descriptor == null) {
					alertMessage(finder.getString("plugin_audio_info_nofile"), true);
				} else {
					m_mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
				}
			} else {
				Uri uri = Uri.parse(inPath);
				if (uri != null)
					m_mediaPlayer.setDataSource(m_context, uri);
			}
			
		} catch (Exception e) {
			
			isLoadMediaPlayerFileSucceed = false;
			throw e;
		} catch (Error e){
			isLoadMediaPlayerFileSucceed = false;
			throw e;
		}
		return isLoadMediaPlayerFileSucceed;
	}

	/*
	 * 关闭当前正在播放的音乐
	 */
	public void pause() {
		try {
			if (m_mediaPlayer != null && m_mediaPlayer.isPlaying()) {
				m_mediaPlayer.pause();
				playState = MEDIAPLAY_STATE_PAUSEING;
			}
		} catch (Exception e) {

		}
		checkModeEnd();
	}

	/*
	 * 停止播放当前音乐,将时间滑块调整到音乐开始处,重新播放
	 */
	public void replay() {
		try {
			checkModeStart();
			if (m_mediaPlayer != null) {
				m_mediaPlayer.stop();
				m_mediaPlayer.prepare();
				m_mediaPlayer.seekTo(0);
				m_mediaPlayer.start();
				playState=MEDIAPLAY_STATE_PLAYING;
			}
		} catch (Exception e) {
			
		}
	}

	/*
	 * 停止播放当前的音乐，无论处于什么状态
	 */
	public void stop(){
		try {
			if(m_mediaPlayer!=null){
				m_mediaPlayer.stop();
				playState=MEDIAPLAY_STATE_STOPING;
			}			
		}catch(Exception e){
			
		}
		checkModeEnd();
	}

	/*
	 * 停止播放当前音乐,根据inPath提供的路径重新导入，并开始播放
	 */
	public void palyNext(String inPath) {
		if (m_mediaPlayer != null) {
			try {
				checkModeStart();
				switch (playState) {
				case MEDIAPLAY_STATE_PLAYING:
				case MEDIAPLAY_STATE_PAUSEING:
					m_mediaPlayer.stop();
					m_mediaPlayer.prepareAsync();
					break;
				case MEDIAPLAY_STATE_STOPING:
					m_mediaPlayer.prepareAsync();
					break;
				}
				m_mediaPlayer.reset();
				if (loadMediaPlayerFile(inPath)) {
					m_mediaPlayer.prepare();
					m_mediaPlayer.setLooping(false);
					m_mediaPlayer.start();
					playState = MEDIAPLAY_STATE_PLAYING;
				}
			} catch (Exception e) {
				
			}
		}
	}

	/*
	 * 调高音量
	 */
	public void volumeUp() {
		AudioManager audioManager = (AudioManager) m_context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		int maxVolum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);// 最大音量
		int currentVolum = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);// 当前音量
		if (currentVolum < maxVolum) {
			currentVolum++;
		}
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolum, AudioManager.FLAG_SHOW_UI);
	}

	/*
	 * 调低音量
	 */
	public void volumeDown() {
		AudioManager audioManager = (AudioManager) m_context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		int currentVolum = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);// 当前音量
		if (currentVolum > 0){
			currentVolum--;
		}
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolum, AudioManager.FLAG_SHOW_UI);
	}
	
	public void checkModeStart(){
		if(isModeInCall){
			playModeInCall();
		}
	}
	
	public void checkModeEnd() {
		playModeNormol();
	}
	
	/**
	 * 听筒模式
	 */
	private void playModeInCall(){
		AudioManager audioManager = (AudioManager) m_context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if(audioManager.getMode() == AudioManager.MODE_NORMAL){
			audioManager.setMode(AudioManager.MODE_IN_CALL);
			if(audioManager.getMode() == AudioManager.MODE_NORMAL){
				audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
			}
		}
	}
	
	/**
	 * 正常模式
	 */
	private void playModeNormol(){
		AudioManager audioManager = (AudioManager) m_context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if(audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION || audioManager.getMode() == AudioManager.MODE_IN_CALL){
			audioManager.setMode(AudioManager.MODE_NORMAL);
		}
	}
	
	/**
	 * make a SensorEventListener
	 * @return
	 */
	public SensorEventListener getSensorEventListener(){
		return new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				try {
					float range = event.values[0];
					SensorManager mSensorManager = (SensorManager) m_context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
					Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
					Log.i("uexAudio_onSensorChanged",range + "   max:" + mSensor.getMaximumRange());
					if (range == 0) {
						if(m_mediaPlayer != null){
							if(isModeInCall){
								return;
							}
							if(m_mediaPlayer.isPlaying()){
								m_mediaPlayer.pause();
								playModeInCall();
								m_mediaPlayer.start();
							}
						}
					} else {
						if(m_mediaPlayer != null){
							if(isModeInCall){
								return;
							}
							if(m_mediaPlayer.isPlaying()){
								m_mediaPlayer.pause();
								playModeNormol();
								m_mediaPlayer.start();
							}
						}
					}
				} catch (Exception e) {
				}
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}
	
}
