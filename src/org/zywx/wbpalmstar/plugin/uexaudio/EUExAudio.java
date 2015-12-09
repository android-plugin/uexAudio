package org.zywx.wbpalmstar.plugin.uexaudio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public class EUExAudio extends EUExBase {
	public static final String tag = "uexAudio_";
	public static final int F_ACT_REQ_CODE_UEX_AUDIO_RECORD = 4;
	public static final String F_CALLBACK_NAME_AUDIO_RECORD = "uexAudio.cbRecord";
	public static final String F_CALLBACK_NAME_AUDIO_BACKGROUND_RECORD = "uexAudio.cbBackgroundRecord";
	public static final String FINISHED = "uexAudio.onPlayFinished";
	private PFMusicPlayer m_pfMusicPlayer = null;
	private String m_mediaPath;
	private ArrayList<Integer> IdsList = new ArrayList<Integer>();
	private AudioRecorder audioRecorder;

	private ResoureFinder finder = ResoureFinder.getInstance();
	private List<String> soundList = new ArrayList<String>();
	public EUExAudio(Context context, EBrowserView inParent) {
		super(context, inParent);
		audioRecorder = new AudioRecorder();
	}
	
	public static void onActivityDestroy(Context context){
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if(audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION || audioManager.getMode() == AudioManager.MODE_IN_CALL){
			audioManager.setMode(AudioManager.MODE_NORMAL);
		}
	}

	/*
	 * 创建音乐播放对象,为播放音乐准备
	 */
	public void open(String[] parm) {
		if (parm == null || parm.length != 1)
			return;
		String inPath = parm[0];
		if (inPath != null && inPath.length() > 0) {
			m_mediaPath = BUtility.makeRealPath(inPath, mBrwView.getCurrentWidget().m_widgetPath, mBrwView.getCurrentWidget().m_wgtType);
			if (m_pfMusicPlayer == null)
				m_pfMusicPlayer = new PFMusicPlayer(mContext) {

					@Override
					public void onPlayFinished(int index) {
						String js = SCRIPT_HEADER + "if(" + FINISHED + "){" + FINISHED + "("+index+");}";
						onCallback(js);
					}
				};
			if (m_pfMusicPlayer != null) {
				m_pfMusicPlayer.basePath = mBrwView.getCurrentUrl();
				m_pfMusicPlayer.open();
			} else {
				errorCallback(0, EUExCallback.F_E_AUDIO_MUSIC_OPEN_NO_OPEN_ERROR_CODE,
				/* "文件未打开错误" */finder.getString(mContext, "plugin_audio_no_open_error"));
			}
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_MUSIC_OPEN_PARAMETER_ERROR_CODE,
			/* "参数错误" */finder.getString(mContext, "plugin_audio_parameter_error"));
		}

	}

	/*
	 * 播放音乐接口
	 */
	public void play(String[] parm) {
		if(parm.length != 0 && parm.length != 1)
			return ;
		int loop = parm.length == 0 ? 0 : Integer.parseInt(parm[0]);//兼容以前没有参数（如果没有参数，默认为0）
			
		if (m_pfMusicPlayer != null) {
			m_pfMusicPlayer.play(m_mediaPath,loop);
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_MUSIC_PLAY_NO_OPEN_ERROR_CODE,
			/* "文件未打开错误" */finder.getString(mContext, "plugin_audio_no_open_error"));
		}
	}

	/*
	 * 暂停音乐接口
	 */
	public void pause(String[] parm) {
		if (m_pfMusicPlayer != null) {
			m_pfMusicPlayer.pause();
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_MUSIC_PAUSE_NO_OPEN_ERROR_CODE,
			/* "文件未打开错误" */finder.getString(mContext, "plugin_audio_no_open_error"));
		}
	}

	/*
	 * 停止播放音乐
	 */
	public void stop(String[] parm) {
		if (m_pfMusicPlayer != null) {
			m_pfMusicPlayer.stop();
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_MUSIC_STOP_NO_OPEN_ERROR_CODE,
			/* "文件未打开错误" */finder.getString(mContext, "plugin_audio_no_open_error"));
		}
	}

	/*
	 * 调高音乐音量接口
	 */
	public void volumeUp(String[] parm) {
		if (m_pfMusicPlayer != null) {
			m_pfMusicPlayer.volumeUp();
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_MUSIC_STOP_NO_OPEN_ERROR_CODE,
			/* "文件未打开错误" */finder.getString(mContext, "plugin_audio_no_open_error"));
		}
	}

	/*
	 * 调低音乐音量接口
	 */
	public void volumeDown(String[] parm) {
		if (m_pfMusicPlayer != null) {
			m_pfMusicPlayer.volumeDown();
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_VOLUMEUP_NO_OPEN_ERROR_CODE,
			/* "文件未打开错误" */finder.getString(mContext, "plugin_audio_no_open_error"));
		}
	}

	/*
	 * 打开一个音乐播放的activity界面接口,参数urlSet是一个地址字符串数组
	 */
	public void openPlayer(String[] parm) {
		if (parm.length < 1) {
			return;
		}
		String[] inUrlStrs = parm[0].split(",");
		String activeIndex = null;
		if (parm.length == 2) {
			activeIndex = parm[1];
		}
		if (inUrlStrs != null && inUrlStrs.length > 0) {
			for (int i = 0; i < inUrlStrs.length; i++) {
				inUrlStrs[i] = BUtility.makeRealPath(inUrlStrs[i], mBrwView.getCurrentWidget().m_widgetPath, mBrwView.getCurrentWidget().m_wgtType);
			}
			Intent intent = new Intent();
			intent.putExtra(PMusicPlayerActivity.F_INTENT_DATA_KEY_AUDIOMGR_AUDIO_LIST, inUrlStrs);
			int index = 0;
			if (activeIndex != null && activeIndex.length() > 0 && !activeIndex.equals("undefined")) {
				index = Integer.parseInt(activeIndex);
			}
			intent.putExtra("fileIndex", index);
			intent.setClass(mContext, PMusicPlayerActivity.class);
			mContext.startActivity(intent);
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_SOUND_OPENS_PARAMETER_ERROR_CODE,
			/* "参数出错" */finder.getString(mContext, "plugin_audio_parameter_error"));
		}
	}

	public void record(String[] parm) {
		if(parm.length != 2) {
			return;
		}
		final String path = mBrwView.getCurrentWidget().getWidgetPath() + BUtility.F_APP_AUDIO;
		if (path != null && path.length() > 0) {
			Intent intent = new Intent(mContext, AudioRecorderActivity.class);
			intent.putExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_SAVE_PATH, path);
			intent.putExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_TYPE, Integer.parseInt(parm[0]));
			intent.putExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_FILENAME, parm[1]);
			startActivityForResult(intent, F_ACT_REQ_CODE_UEX_AUDIO_RECORD);
		} else {
			jsCallback(F_CALLBACK_NAME_AUDIO_RECORD, 0, EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
		}

	}
	
	public void startBackgroundRecord(String[] parm) {for(String par:parm){Log.i("log", par);}
		if(parm.length != 2) {
			return;
		}
		final String audioFolder = mBrwView.getRootWidget().getWidgetPath() + BUtility.F_APP_AUDIO;
		if (!audioRecorder.startRecord(new File(audioFolder),Integer.valueOf(parm[0]), parm[1])) {
			errorCallback(0, EUExCallback.F_E_AUDIO_SOUND_PLAY_NO_OPEN_ERROR_CODE, finder.getString(mContext, "plugin_audio_no_open_error"));
		}
	}

	public void stopBackgroundRecord(String[] parm) {for(String par:parm){Log.i("log", par);}
		audioRecorder.stopRecord();
		String recordFile = audioRecorder.getRecordFile();
		jsCallback(F_CALLBACK_NAME_AUDIO_BACKGROUND_RECORD, 0, EUExCallback.F_C_TEXT, recordFile);
	}

	public void openSoundPool(String[] parm) {
			if (m_pfMusicPlayer == null)
				m_pfMusicPlayer = new PFMusicPlayer(mContext) {

					@Override
					public void onPlayFinished(int index) {
					}
				};
			if (m_pfMusicPlayer != null) {
				m_pfMusicPlayer.openSound(Integer.parseInt("50"), Integer.parseInt("3"), Integer.parseInt("0"));
			}
	}

	public void addSound(String[] parm){
		if(parm.length != 2)
			return;
		String soundid  = parm[0];
		String soundurl = parm[1];
		String soundPath = BUtility.makeRealPath(soundurl, mBrwView.getCurrentWidget().m_widgetPath, mBrwView.getCurrentWidget().m_wgtType);
		int soundID = m_pfMusicPlayer.loadSound(soundPath);
		//用户添加id,音效池中对应id，播放返回id（默认为0，还没播放）
		soundList.add(soundid+","+soundID+",0");//即使返回为0，也要添加记录
	}

	public void playFromSoundPool(String[] parm) {
		if (parm == null || parm.length != 1)
			return;
		String inSoundID = parm[0];
		if (inSoundID != null && inSoundID.length() > 0) {
			if (m_pfMusicPlayer != null) {
				String soundID = "";
				for (String item : soundList) {
					String tag = item.substring(0, item.indexOf(','));
					if (tag.equals(inSoundID)) {
						soundID = item.substring(item.indexOf(',') + 1,item.lastIndexOf(','));
						stopFromSoundPool(new String[]{inSoundID});
						int inStreamID=m_pfMusicPlayer.playSound(Integer.parseInt(soundID.trim()));
						StringBuffer sb=new StringBuffer(item.substring(0,item.lastIndexOf(",")));
						sb.append(","+inStreamID);
						int index=soundList.indexOf(item);
						item=sb.toString();
						soundList.set(index, item);
						return;
					}
				}
			} else {
				errorCallback(0, EUExCallback.F_E_AUDIO_SOUND_PLAY_NO_OPEN_ERROR_CODE,
				/* "文件未打开错误" */finder.getString(mContext, "plugin_audio_no_open_error"));
			}
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_SOUND_PLAY_PARAMETER_ERROR_CODE,
			/* "参数错误" */finder.getString(mContext, "plugin_audio_parameter_error"));
		}
	}

	public void stopFromSoundPool(String[] parm) {//停止某个sound
		if (parm == null || parm.length != 1)
			return;
		String inSoundID = parm[0];
		if (inSoundID != null && inSoundID.length() > 0 && Integer.parseInt(inSoundID) > 0) {
			if (m_pfMusicPlayer != null) {
				for(String item:soundList){
					String userID = item.substring(0,item.indexOf(','));
					if(userID.equals(inSoundID)){
						String inStreamID = item.substring(item.lastIndexOf(',')+1);
						m_pfMusicPlayer.stopSound(Integer.parseInt(inStreamID.trim()));
						return;
					}
				}
			} else {
				errorCallback(0, EUExCallback.F_E_AUDIO_SOUND_STOP_NO_OPEN_ERROR_CODE,
				/* "文件未打开错误" */finder.getString(mContext, "plugin_audio_no_open_error"));
			}
		} else {
			errorCallback(0, EUExCallback.F_E_AUDIO_SOUND_STOP_PARAMETER_ERROR_CODE,
			/* "参数错误" */finder.getString(mContext, "plugin_audio_parameter_error"));
		}
	}
	
	public void closeSoundPool(String[] parm){//关闭所有
		if(soundList.size()>0){
			for(String item:soundList){
				String inStreamID = item.substring(item.lastIndexOf(',')+1);
				m_pfMusicPlayer.stopSound(Integer.parseInt(inStreamID.trim()));
			}
			soundList.removeAll(soundList);
		}
	}
	
	public void setPlayMode(String[] params){
		if (params == null || params.length != 1)
			return;
		String playMode = "0";
		try {
			JSONObject json = new JSONObject(params[0]);
			playMode = json.getString("playMode");
		} catch (JSONException e) {
		}
		if (m_pfMusicPlayer != null) {
			m_pfMusicPlayer.setModeInCall("1".equals(playMode));
		} else {
			errorCallback(0,
					EUExCallback.F_E_AUDIO_MUSIC_STOP_NO_OPEN_ERROR_CODE,
					/* "文件未打开错误" */finder.getString(mContext,
							"plugin_audio_no_open_error"));
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (F_ACT_REQ_CODE_UEX_AUDIO_RECORD == requestCode) {
			if (resultCode == Activity.RESULT_OK) {
				String returnPath = data.getStringExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_RETURN_PATH);
				jsCallback(EUExAudio.F_CALLBACK_NAME_AUDIO_RECORD, 0, EUExCallback.F_C_TEXT, returnPath);
			}
		}
	}

	@Override
	public boolean clean() {
		if (m_pfMusicPlayer != null) {
			m_pfMusicPlayer.stop();
			for (Integer id : IdsList) {
				m_pfMusicPlayer.stopSound(id);
			}
			IdsList.clear();
			m_pfMusicPlayer = null;
		}
		return true;
	}
}
