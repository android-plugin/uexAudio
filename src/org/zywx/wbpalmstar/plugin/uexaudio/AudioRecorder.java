package org.zywx.wbpalmstar.plugin.uexaudio;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.media.MediaRecorder;

public class AudioRecorder {

	public static final String TAG = "AudioRecorder";
	private static final int STATE_IDLE = 0;
	private static final int STATE_STARTED = 1;
	private MediaRecorder mediaRecorder;
	private int currentState = STATE_IDLE;
	private String recordFile;
	AudioRecorder2Mp3Util util = null;
	int backRecordType=0;//得到的录音文件类型，2-为MP3格式，其他为amr格式
	/**
	 * 开始后台录音
	 * 
	 * @param folder
	 *            指定录音的文件夹
	 * @return 录音是否成功启动
	 */
	public boolean startRecord(File folder,int type, String fileName) {
		boolean isSuc = false;
		File file = null;
		if (currentState == STATE_IDLE) {
			try {
				backRecordType=type;
				currentState = STATE_STARTED;
				if(fileName == null || "".equals(fileName)) {
					file = new File(folder, formatDateToFileName(System.currentTimeMillis()));//文件路径
				} else {
					file = new File(folder, formatStringToFileName(fileName));
					if(file.exists()){
						file.delete();
					}
				}
				if (type != 2) {
					startMediaRecord(file);
				} else {
					startAudioRecord(file);
				}
				isSuc = true;
			} catch (Exception e) {
				currentState = STATE_IDLE;
				if(file.exists()){
					file.delete();
					file = null;
				}
				return false;
			}
		}
		return isSuc;
	}
	/**
	 * 开始media录音
	 * @param file
	 * @throws IOException
	 */
	private void startMediaRecord(File file) throws IOException {
		mediaRecorder = new MediaRecorder();
		mediaRecorder.reset();
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mediaRecorder.setOutputFile(file.getAbsolutePath());
		mediaRecorder.prepare();
		mediaRecorder.start();
		recordFile = file.getAbsolutePath();
	}
	/**
	 * 开始audio录音
	 * @param file
	 */
	private void startAudioRecord(File file) {
		if (util == null) {
			util = new AudioRecorder2Mp3Util(null, file.getAbsolutePath(), file.getAbsolutePath().replaceAll(".raw", ".mp3"));
		}
		util.cleanFile(AudioRecorder2Mp3Util.MP3 | AudioRecorder2Mp3Util.RAW);//清除MP3和raw文件
		util.startRecording();//开始录音
		recordFile = util.getFilePath(0X00000002);
	}

	private String formatDateToFileName(long milliSeconds) {
		final SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-ss");
		return sdf.format(new Date(milliSeconds)) + (backRecordType==2 ? ".raw":".amr");
	}
	
	private String formatStringToFileName(String fileName) {
		return fileName + (backRecordType==2 ? ".raw":".amr");
	}
	
	public void stopRecord() {
		if (currentState == STATE_STARTED) {
			currentState = STATE_IDLE;
			try {
				if (backRecordType != 2) {
					stopMediaRecord();
				} else {
					stopAudioRecord();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 停止media录音停止
	 */
	private void stopMediaRecord() {
		mediaRecorder.stop();
		mediaRecorder.release();
		mediaRecorder = null;
	}

	/**
	 * 停止audio录音停止
	 */
	private void stopAudioRecord() {
		util.stopRecordingAndConvertFile();// 停止录音
		util.cleanFile(AudioRecorder2Mp3Util.RAW);// 清除中间录音raw文件，保留MP3文件
		// 如果要关闭可以
		util.close();
		util = null;
	}
	
	public String getRecordFile() {
		return recordFile;
	}
}
