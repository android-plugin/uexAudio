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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.zywx.wbpalmstar.base.FileHelper;
import org.zywx.wbpalmstar.base.ResoureFinder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AudioRecorderActivity extends Activity implements OnClickListener, OnSeekBarChangeListener,
		OnCompletionListener, OnErrorListener {

	public static final String INTENT_KEY_AUDIO_RECORD_SAVE_PATH = "AudioRecordPath";
	public static final String INTENT_KEY_AUDIO_RECORD_FILENAME = "AudioRecordFileName";
	public static final String INTENT_KEY_AUDIO_RECORD_RETURN_PATH = "AudioRecordReturnPath";
	public static final String INTENT_KEY_AUDIO_RECORD_TYPE = "AudioRecordType"; //Android暂时只支持.amr, .mp3格式。对应的数值2代表MP3格式，其它数字，对应amr格式
	private static final int ACTION_START_RECORD_AUDIO = 1;
	private static final int ACTION_UPDATE_RECORD_TIME = 2;
	private static final int ACTION_FINISH_RECORD_AUDIO = 3;
	private static final int ACTION_UPDATE_PLAY_TIME = 5;
	private static final int ACTION_FINISH_PLAY_AUDIO = 6;
	private static final int PLAY_STATE_PREPARED = 12;
	private static final int PLAY_STATE_PLAYING = 13;
	private static final int PLAY_STATE_PAUSE = 14;
	private static final int PLAY_STATE_PLAY_COMPLETED = 15;
	private static final int PLAY_STATE_RELEASED = 16;
	private static final int RECORD_STATE_INIT = 21;
	private static final int RECORD_STATE_RECORDING = 22;
	private static final int RECORD_STATE_STOP = 23;
	private static final int STATE_OCCUR_ERROR = 25;

	 private ResoureFinder finder = null;
	private MediaRecorder mediaRecorder;
	private int currentRecordState = RECORD_STATE_INIT;
	private long audioRecordStartTime;
	// 录音状态指示器
	private ImageView ivRecordStateIndicator;
	// 录音已录制时间
	private TextView tvRecordPassedTime;

	// 录制按钮
	private ImageView btnRecord;
	// 录制文件存放路径
	private File recordFolder;
	// 当前录制文件的引用
	private File currentRecordFile;
	// 录音播放器
	private MediaPlayer mediaPlayer;
	// 拖动条
	private Button btnBack;
	private SeekBar playerSeekBar;
	private TextView tvPlayerPassedTime;
	private TextView tvPlayerTotalTime;
	private ImageView btnPlay;
	private ImageView btnUse;
	private RelativeLayout layoutRecorder;
	private RelativeLayout layoutPlayer;
	private VisualFrequencyView frequencyView;
	private ProgressDialog progressDialog;
	private int currentPlayState;
	private boolean isUserSeekingBar = false;
	private Handler handler = new Handler() {
		boolean indicatorState = false;

		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case ACTION_START_RECORD_AUDIO:
				btnPlay.setEnabled(false);
				btnUse.setEnabled(false);
				ivRecordStateIndicator.setBackgroundResource(finder.getDrawableId("plugin_audio_recorder_turn_on"));
				btnRecord.setBackgroundResource(finder.getDrawableId("plugin_audio_recorder_stop_selector"));
				frequencyView.startResponse();
				indicatorState = true;
				handler.sendEmptyMessageDelayed(ACTION_UPDATE_RECORD_TIME, 500);
				break;
			case ACTION_UPDATE_RECORD_TIME:
				if (mediaRecorder != null && currentRecordState == RECORD_STATE_RECORDING) {
					final int duration = (int) (System.currentTimeMillis() - audioRecordStartTime);
					tvRecordPassedTime.setText(formatTime(duration));
					// 不停切换录制图片指示状态
					indicatorState = !indicatorState;
					if (indicatorState) {
						ivRecordStateIndicator.setBackgroundResource(finder.getDrawableId("plugin_audio_recorder_turn_on"));
					} else {
						ivRecordStateIndicator.setBackgroundResource(finder.getDrawableId("plugin_audio_recorder_turn_off"));
					}
					handler.sendEmptyMessageDelayed(ACTION_UPDATE_RECORD_TIME, 500);
				}
				break;
			case ACTION_FINISH_RECORD_AUDIO:
				tvRecordPassedTime.setText(formatTime(0));
				ivRecordStateIndicator.setBackgroundResource(finder.getDrawableId("plugin_audio_recorder_turn_off"));
				indicatorState = false;
				audioRecordStartTime = 0;
				btnRecord.setBackgroundResource(finder.getDrawableId("plugin_audio_recorder_record_selector"));
				frequencyView.stopResponse();
				btnPlay.setEnabled(true);
				btnUse.setEnabled(true);
				layoutPlayer.setVisibility(View.VISIBLE);
				layoutRecorder.setVisibility(View.GONE);
				break;
			case ACTION_UPDATE_PLAY_TIME:
				if (mediaPlayer != null && currentPlayState == PLAY_STATE_PLAYING) {
					if (!isUserSeekingBar) {
						final int passTime = mediaPlayer.getCurrentPosition();
						tvPlayerPassedTime.setText(formatTime(passTime));
						playerSeekBar.setProgress(passTime);
					}
					handler.sendEmptyMessageDelayed(ACTION_UPDATE_PLAY_TIME, 500);
				}
				break;
			case PLAY_STATE_PREPARED:
				dismissProgressDialog();
				// 初始化播放器UI界面
				if (mediaPlayer != null) {
					final int playTotalTime = mediaPlayer.getDuration();
					playerSeekBar.setMax(playTotalTime);
					playerSeekBar.setProgress(0);
					tvPlayerTotalTime.setText(formatTime(playTotalTime));
					tvPlayerPassedTime.setText(formatTime(0));
					btnPlay.setBackgroundResource(finder.getDrawableId("plugin_audio_play_selector"));
					layoutRecorder.setVisibility(View.GONE);
					layoutPlayer.setVisibility(View.VISIBLE);
				}
				break;
			case PLAY_STATE_PLAYING:
				if (mediaPlayer != null) {
					frequencyView.startResponse();
					btnPlay.setBackgroundResource(finder.getDrawableId("plugin_audio_pause_selector"));
					handler.sendEmptyMessage(ACTION_UPDATE_PLAY_TIME);
				}
				break;
			case PLAY_STATE_PAUSE:
				frequencyView.stopResponse();
				btnPlay.setBackgroundResource(finder.getDrawableId("plugin_audio_play_selector"));
				break;
			case PLAY_STATE_PLAY_COMPLETED:
				frequencyView.stopResponse();
				tvPlayerPassedTime.setText(formatTime(0));
				playerSeekBar.setProgress(0);
				btnPlay.setBackgroundResource(finder.getDrawableId("plugin_audio_play_selector"));
				break;
			case STATE_OCCUR_ERROR:
				new AlertDialog.Builder(AudioRecorderActivity.this).setTitle(getString(finder.getStringId("prompt")))
						.setMessage((String) msg.obj)
						.setPositiveButton(finder.getStringId("confirm"), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (msg.arg1 == 1) {
									AudioRecorderActivity.this.finish();
								}
							}
						}).show();
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		finder = ResoureFinder.getInstance(this);
		setContentView(finder.getLayoutId("plugin_audio_recorder_main"));
		initViews();
	}

	/**
	 * 初始化录音界面
	 */
	private void initViews() {
		btnBack = (Button) findViewById(finder.getId("plugin_audio_recorder_back"));
		btnBack.setOnClickListener(this);
		ivRecordStateIndicator = (ImageView) findViewById(finder.getId("plugin_audio_recorder_iv_state_indicator"));
		tvRecordPassedTime = (TextView) findViewById(finder.getId("plugin_audio_recorder_tv_time_passed"));
		tvRecordPassedTime.setText(formatTime(0));
		frequencyView = (VisualFrequencyView) findViewById(finder.getId("plugin_audio_recorder_visual_freqency"));
		btnRecord = (ImageView) findViewById(finder.getId("plugin_audio_recorder_btn_record"));
		btnRecord.setOnClickListener(this);
		btnPlay = (ImageView) findViewById(finder.getId("plugin_audio_recorder_play"));
		btnPlay.setOnClickListener(this);
		btnPlay.setEnabled(false);
		btnUse = (ImageView) findViewById(finder.getId("plugin_audio_recorder_use"));
		btnUse.setOnClickListener(this);
		btnUse.setEnabled(false);
		layoutRecorder = (RelativeLayout) findViewById(finder.getId("plugin_audio_recorder_layout_timeline"));
		layoutPlayer = (RelativeLayout) findViewById(finder.getId("plugin_audio_recorder_player_layout"));
		playerSeekBar = (SeekBar) findViewById(finder.getId("plugin_audio_recorder_player_sb_progress"));
		playerSeekBar.setOnSeekBarChangeListener(this);
		tvPlayerPassedTime = (TextView) findViewById(finder.getId("plugin_audio_recorder_player_pass_time"));
		tvPlayerTotalTime = (TextView) findViewById(finder.getId("plugin_audio_recorder_player_total_time"));
	}

	// 初始化和开始MediaRecorder
	private void initAndStartMediaRecorder(String filePath) {

		try {
			mediaRecorder = new MediaRecorder();
			mediaRecorder.reset();
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mediaRecorder.setOutputFile(filePath);
			mediaRecorder.prepare();
			mediaRecorder.start();
			audioRecordStartTime = System.currentTimeMillis();
			currentRecordState = RECORD_STATE_RECORDING;
			handler.sendEmptyMessage(ACTION_START_RECORD_AUDIO);
		} catch (Exception e) {
			alertMessage(getString(finder.getStringId("plugin_audio_recorder_start_record_fail")), true);
		}
	}

	// 释放媒体播放器资源
	private void stopMediaRecorder() {
		if (mediaRecorder != null) {
			if (currentRecordState == RECORD_STATE_RECORDING) {
				mediaRecorder.stop();
			}
			mediaRecorder.release();
			currentRecordState = RECORD_STATE_STOP;
			mediaRecorder = null;
		}
	}

	// 初始化媒体播放器
	private void initMediaPlayer(String file) {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setLooping(false);
		try {
			mediaPlayer.setDataSource(file);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.prepare();
			currentPlayState = PLAY_STATE_PREPARED;
			handler.sendEmptyMessage(PLAY_STATE_PREPARED);
		} catch (Exception e) {
			alertMessage(getString(finder.getStringId("plugin_audio_recorder_can_not_play_this_audio_file")), true);
		}
	}

	// 启动媒体播放器
	private void startMediaPlayer() {
		if (mediaPlayer != null
				&& (currentPlayState == PLAY_STATE_PAUSE || currentPlayState == PLAY_STATE_PREPARED || currentPlayState == PLAY_STATE_PLAY_COMPLETED)) {
			mediaPlayer.start();
			currentPlayState = PLAY_STATE_PLAYING;
			handler.sendEmptyMessage(PLAY_STATE_PLAYING);
		}
	}

	private void pauseMediaPlayer() {
		if (mediaPlayer != null && currentPlayState == PLAY_STATE_PLAYING) {
			mediaPlayer.pause();
			currentPlayState = PLAY_STATE_PAUSE;
			handler.sendEmptyMessage(PLAY_STATE_PAUSE);
		}
	}

	// 释放媒体播放器占用的资源
	private void releaseMediaPlayer() {
		if (mediaPlayer != null) {
			if (currentPlayState == PLAY_STATE_PLAYING || currentPlayState == PLAY_STATE_PAUSE
					|| currentPlayState == PLAY_STATE_PLAY_COMPLETED) {
				mediaPlayer.stop();
			}
			mediaPlayer.release();
			currentPlayState = PLAY_STATE_RELEASED;
			handler.sendEmptyMessage(ACTION_FINISH_PLAY_AUDIO);
			mediaPlayer = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onClick(View v) {
		
		if (v == btnBack) {
			this.finish();
		} else if (v == btnRecord) {
			new Thread("SoTowerMobile-uexAudioReleaseMediaPlayer") {
				public void run() {
					releaseMediaPlayer();
				};
			}.start();
			switch (currentRecordState) {
			case RECORD_STATE_INIT:
			case RECORD_STATE_STOP:
				layoutRecorder.setVisibility(View.VISIBLE);
				layoutPlayer.setVisibility(View.GONE);
				if (ensureFolderCreated()) {
					if (FileHelper.getSDcardFreeSpace() > 1048576L) {// 大于1MB
						String fileName = getIntent().getStringExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_FILENAME);
                        int type = getIntent().getIntExtra(INTENT_KEY_AUDIO_RECORD_TYPE, 0);
						if(fileName == null || "".equals(fileName)) {
							currentRecordFile = new File(recordFolder.getAbsolutePath(), formatDateToFileName(System.currentTimeMillis(), type));
						}else {
							currentRecordFile = new File(recordFolder.getAbsolutePath(), formatStringToFileName(fileName, type));
						}
						initAndStartMediaRecorder(currentRecordFile.getAbsolutePath());
					} else {
						alertMessage(getString(finder.getStringId("plugin_audio_recorder_sdcard_free_space_not_enough")), true);
					}
				} else {
					alertMessage(getString(finder.getStringId("plugin_audio_can_not_mount_sdcard")), true);
				}
				break;
			case RECORD_STATE_RECORDING:
				showProgressDialog(getString(finder.getStringId("plugin_audio_recorder_now_saving_record")));
				new Thread("SoTowerMobile-uexAudioStopMediaRecorder") {
					public void run() {
						stopMediaRecorder();
						handler.sendEmptyMessage(ACTION_FINISH_RECORD_AUDIO);
						initMediaPlayer(currentRecordFile.getAbsolutePath());
					};
				}.start();
				break;
			}
		} else if (v == btnPlay) {
			switch (currentPlayState) {
			case PLAY_STATE_PREPARED:
			case PLAY_STATE_PAUSE:
			case PLAY_STATE_PLAY_COMPLETED:
				startMediaPlayer();
				break;
			case PLAY_STATE_PLAYING:
				pauseMediaPlayer();
				break;
			}
		} else if (v == btnUse) {
			Intent intent = new Intent();
			intent.putExtra(INTENT_KEY_AUDIO_RECORD_RETURN_PATH, currentRecordFile.getAbsolutePath());
			setResult(Activity.RESULT_OK, intent);
			AudioRecorderActivity.this.finish();
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		isUserSeekingBar = true;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (fromUser) {
			tvPlayerPassedTime.setText(formatTime(progress));
			playerSeekBar.setProgress(progress);
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (mediaPlayer != null) {
			mediaPlayer.seekTo(playerSeekBar.getProgress());
		}
		isUserSeekingBar = false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		currentPlayState = PLAY_STATE_PLAY_COMPLETED;
		handler.sendEmptyMessage(PLAY_STATE_PLAY_COMPLETED);
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopMediaRecorder();
		releaseMediaPlayer();
	}

	private String formatDateToFileName(long milliSeconds, int type) {
		final SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss");
		return sdf.format(new Date(milliSeconds)) + getAudioFileSuffixByType(type);
	}
	
	private String formatStringToFileName(String fileName, int type) {
		return fileName + getAudioFileSuffixByType(type);
	}

	private String formatTime(int ms) {
		if (ms >= 0) {
			final int totalSeconds = ms / 1000;
			final int hours = totalSeconds / 3600;
			final int minutes = (totalSeconds % 3600) / 60;
			final int second = ((totalSeconds % 3600) % 60);
			StringBuffer sb = new StringBuffer();
			if (hours <= 10) {
				sb.append("0");
			}
			sb.append(hours).append(":");
			if (minutes < 10) {
				sb.append("0");
			}
			sb.append(minutes).append(":");
			if (second < 10) {
				sb.append("0");
			}
			sb.append(second);
			return sb.toString();
		}
		return "";
	}

	// 显示进度框
	private void showProgressDialog(String msg) {
		dismissProgressDialog();
		progressDialog = ProgressDialog.show(this, null, msg, false, false);
	}

	// 关闭进度框
	private void dismissProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

	private void alertMessage(String message, final boolean confirmToExit) {
		Message msg = handler.obtainMessage(STATE_OCCUR_ERROR);
		msg.obj = message;
		msg.arg1 = confirmToExit ? 1 : 0;
		handler.sendMessage(msg);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		alertMessage(getString(finder.getStringId("plugin_audio_recorder_can_not_play_this_audio_file")), true);
		return false;
	}

	private boolean ensureFolderCreated() {
		boolean isCreated = false;
		String sdcard = FileHelper.getSDcardPath();
		if (sdcard == null) {
			return false;
		}
		try {
			final String audioPath = getIntent()
					.getStringExtra(AudioRecorderActivity.INTENT_KEY_AUDIO_RECORD_SAVE_PATH);
			if (audioPath == null) {
				return false;
			}
			final File file = new File(audioPath);
			if (!file.exists()) {
				if (isCreated = file.mkdirs()) {
					recordFolder = file;
				}
			} else {
				isCreated = true;
				recordFolder = file;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isCreated;
	}
    //type: 2 对应的是mp3格式，其它数字对应amr格式
    private String getAudioFileSuffixByType(int type) {
        if (type != 2) {
            return ".amr";
        }
        return ".mp3";
    }
}
