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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

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
    private static SensorEventListener sensorEventListener;
    private boolean start_record_fail = false;
    private boolean testedPermission = false;
    private boolean startBackgroundRecord_singleton = true;

    private ResoureFinder finder = ResoureFinder.getInstance();
    private List<String> soundList = new ArrayList<String>();

    private int mRecordCallbackId=-1;

    public EUExAudio(Context context, EBrowserView inParent) {
        super(context, inParent);
        audioRecorder = new AudioRecorder();
    }

    public static void onActivityPause(Context context) {
        BDebug.i(tag, "onActivityPause");
        if (sensorEventListener != null) {
            SensorManager mSensorManager = (SensorManager) context
                    .getApplicationContext().getSystemService(
                            Context.SENSOR_SERVICE);
            mSensorManager.unregisterListener(sensorEventListener);
        }
    }

    public static void onActivityReStart(Context context) {
        BDebug.i(tag, "onActivityReStart");
        if (sensorEventListener != null) {
            SensorManager mSensorManager = (SensorManager) context
                    .getApplicationContext().getSystemService(
                            Context.SENSOR_SERVICE);
            Sensor mSensor = mSensorManager
                    .getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mSensorManager.registerListener(sensorEventListener, mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public static void onActivityDestroy(Context context) {
        BDebug.i(tag, "onActivityDestroy");
        AudioManager audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION || audioManager.getMode() == AudioManager.MODE_IN_CALL) {
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
                        String js = SCRIPT_HEADER + "if(" + FINISHED + "){" + FINISHED + "(" + index + ");}";
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
        if (parm.length != 0 && parm.length != 1)
            return;
        int loop = parm.length == 0 ? 0 : Integer.parseInt(parm[0]);//兼容以前没有参数（如果没有参数，默认为0）

        if (m_pfMusicPlayer != null) {
            m_pfMusicPlayer.play(m_mediaPath, loop);
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
        if (parm.length < 2) {
            return;
        }
        if (parm.length>2){
            mRecordCallbackId= Integer.parseInt(parm[2]);
        }else{
            mRecordCallbackId=-1;
        }
        final String path = mBrwView.getCurrentWidget().getWidgetPath() + BUtility.F_APP_AUDIO;
        if (path != null && path.length() > 0) {
            Intent intent = new Intent(mContext, AudioRecorderActivity.class);
            intent.putExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_SAVE_PATH, path);
            intent.putExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_TYPE, Integer.parseInt(parm[0]));
            intent.putExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_FILENAME, parm[1]);
            startActivityForResult(intent, F_ACT_REQ_CODE_UEX_AUDIO_RECORD);
        } else {
            if(mRecordCallbackId!=-1){
                callbackToJs(mRecordCallbackId,false);
            }else{
                jsCallback(F_CALLBACK_NAME_AUDIO_RECORD, 0, EUExCallback.F_C_INT, EUExCallback.F_C_FAILED);
            }
           }

    }

    /**
     * @author WangJingwei
     * @Data 2016/1/25
     * @description 对于录音权限被用户禁止的情况，打开app第一次录音时，先测试录音，测试后如果ok，则用户继续录音，否则提示用户，
     * 此测试对hybrid开发人员和用户是透明的。
     */
    public void startBackgroundRecord(String[] parm) {
        for (String par : parm) {
            BDebug.i(par);
        }
        if (parm.length != 2) {
            return;
        }
        final String audioFolder = mBrwView.getRootWidget().getWidgetPath() + BUtility.F_APP_AUDIO;
        if (!testedPermission) {
            try {
                BDebug.i("thread", Thread.currentThread() + "");
                TestBackgroundRecord(audioFolder);
            } catch (Exception e) {
                e.printStackTrace();
                start_record_fail = true;
                Toast.makeText(mContext, "请检查录音权限是否正常开启", Toast.LENGTH_SHORT).show();
                return;
            } finally {
                File file = new File(audioFolder + "testPermission.amr");
                file.delete();
            }
        }
        if (startBackgroundRecord_singleton) {
            // 开始正式录音
            start_record_fail = false;
            testedPermission = true;
            audioRecorder.startRecord(new File(audioFolder), Integer.valueOf(parm[0]), parm[1]);
            startBackgroundRecord_singleton = false;
        }
    }

    private void TestBackgroundRecord(String audioFolder) throws Exception {
        audioRecorder.startRecord(new File(audioFolder), 1, "testPermission");
        Thread.sleep(300);//模拟录音300毫秒，以用来判断是否可录音。
        audioRecorder.stopRecord();
        long size = 0;
        String recordFile = audioRecorder.getRecordFile();
        File file = new File(recordFile);
        if (file.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            size = fis.available();
            fis.close();
        }
        BDebug.i("test size", size + "");
        if ((recordFile == null) || (recordFile.endsWith(".amr") && size <= 30)) {
            throw new myAudioPermissionException("AudioPermission maybe denied, please accept audio permission");
        }
    }

    private class myAudioPermissionException extends Exception {
        private static final long serialVersionUID = 1L;

        public myAudioPermissionException(String detailMessage) {
            super(detailMessage);
        }
    }

    public void stopBackgroundRecord(String[] parm) {
        int callbackId=-1;
        if (parm.length>0){
            callbackId= Integer.parseInt(parm[0]);
        }
        if (start_record_fail) {
            start_record_fail = false;
            BDebug.i("已知无权限", "跳出");
            return;
        } else if (!startBackgroundRecord_singleton) {
            for (String par : parm) {
                BDebug.i(par);
            }
            long size = 0;
            audioRecorder.stopRecord();
            String recordFile = audioRecorder.getRecordFile();// !=""?audioRecorder.getRecordFile():"null";
            try {
                File file = new File(recordFile);
                if (file.exists()) {
                    FileInputStream fis = null;
                    fis = new FileInputStream(file);
                    size = fis.available();
                    fis.close();
                }
            } catch (Exception e) {
                BDebug.i("startRecord", "录音失败1");
                if(callbackId!=-1){
                    callbackToJs(callbackId,false);
                }else{
                    errorCallback(0, EUExCallback.F_E_AUDIO_SOUND_PLAY_NO_OPEN_ERROR_CODE,
                            finder.getString(mContext, "plugin_audio_no_open_error"));

                }
                return;
            }
            BDebug.i("size", size + "");
            if ((recordFile.endsWith(".amr") && size <= 100) || (recordFile.endsWith(".mp3") && size <= 2000)) {
                BDebug.i("startRecord", "录音失败2");
                if(callbackId!=-1){
                    callbackToJs(callbackId,false);
                }else{
                    errorCallback(0, EUExCallback.F_E_AUDIO_SOUND_PLAY_NO_OPEN_ERROR_CODE,
                            finder.getString(mContext, "plugin_audio_no_open_error"));
                }
             } else {
                if(callbackId!=-1){
                    callbackToJs(callbackId,false,recordFile);
                }else{
                    jsCallback(F_CALLBACK_NAME_AUDIO_BACKGROUND_RECORD, 0, EUExCallback.F_C_TEXT, recordFile);
                }
             }
            startBackgroundRecord_singleton = true;
        }
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

    public void addSound(String[] parm) {
        if (parm.length != 2)
            return;
        String soundid = parm[0];
        String soundurl = parm[1];
        String soundPath = BUtility.makeRealPath(soundurl, mBrwView.getCurrentWidget().m_widgetPath, mBrwView.getCurrentWidget().m_wgtType);
        int soundID = m_pfMusicPlayer.loadSound(soundPath);
        //用户添加id,音效池中对应id，播放返回id（默认为0，还没播放）
        soundList.add(soundid + "," + soundID + ",0");//即使返回为0，也要添加记录
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
                        soundID = item.substring(item.indexOf(',') + 1, item.lastIndexOf(','));
                        stopFromSoundPool(new String[]{inSoundID});
                        int inStreamID = m_pfMusicPlayer.playSound(Integer.parseInt(soundID.trim()));
                        StringBuffer sb = new StringBuffer(item.substring(0, item.lastIndexOf(",")));
                        sb.append("," + inStreamID);
                        int index = soundList.indexOf(item);
                        item = sb.toString();
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
                for (String item : soundList) {
                    String userID = item.substring(0, item.indexOf(','));
                    if (userID.equals(inSoundID)) {
                        String inStreamID = item.substring(item.lastIndexOf(',') + 1);
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

    public void closeSoundPool(String[] parm) {//关闭所有
        if (soundList.size() > 0) {
            for (String item : soundList) {
                String inStreamID = item.substring(item.lastIndexOf(',') + 1);
                m_pfMusicPlayer.stopSound(Integer.parseInt(inStreamID.trim()));
            }
            soundList.removeAll(soundList);
        }
    }

    public void setPlayMode(String[] params) {
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

    public void setProximityState(String[] params) {
        if (params == null || params.length < 1)
            return;
        boolean state = "1".equals(params[0]);
        SensorManager mSensorManager = (SensorManager) mContext.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        if (!state) {
            if (sensorEventListener != null)
                mSensorManager.unregisterListener(sensorEventListener);
            return;
        }
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (sensorEventListener == null) {
            if (m_pfMusicPlayer != null) {
                sensorEventListener = m_pfMusicPlayer.getSensorEventListener();
            } else {
                errorCallback(0,
                        EUExCallback.F_E_AUDIO_MUSIC_STOP_NO_OPEN_ERROR_CODE,
						/* "文件未打开错误" */finder.getString(mContext,
                                "plugin_audio_no_open_error"));
                return;
            }
        }
        mSensorManager.registerListener(sensorEventListener, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (F_ACT_REQ_CODE_UEX_AUDIO_RECORD == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                String returnPath = data.getStringExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_RETURN_PATH);
                if(mRecordCallbackId!=-1){
                    callbackToJs(mRecordCallbackId,false,returnPath);
                }else{
                    jsCallback(EUExAudio.F_CALLBACK_NAME_AUDIO_RECORD, 0, EUExCallback.F_C_TEXT, returnPath);
                }
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
        if (sensorEventListener != null) {
            SensorManager mSensorManager = (SensorManager) mContext.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            mSensorManager.unregisterListener(sensorEventListener);
            sensorEventListener = null;
        }
        return true;
    }
}
