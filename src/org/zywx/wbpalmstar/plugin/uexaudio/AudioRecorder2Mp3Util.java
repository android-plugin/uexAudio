package org.zywx.wbpalmstar.plugin.uexaudio;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.pocketdigi.utils.FLameUtils;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecorder2Mp3Util {

    private Activity activity;

	public static final int RAW = 0X00000001;
	public static final int MP3 = 0X00000002;

	private String rawPath = null;
	private String mp3Path = null;

	private static final int SAMPLE_RATE = 8000;

	private short[] mBuffer;
	private AudioRecord mRecorder;

	public AudioRecorder2Mp3Util(Activity activity) {
		this.activity = activity;
	}

	private boolean isRecording = false;

	private boolean convertOk = false;

	public AudioRecorder2Mp3Util(Activity activity, String rawPath,
			String mp3Path) {
		this.activity = activity;
		this.rawPath = rawPath;
		this.mp3Path = mp3Path;
	}

	public boolean startRecording() {

		if (isRecording) {
			return isRecording;
		}

		if (mRecorder == null) {
			initRecorder();
		}

		getFilePath();
		mRecorder.startRecording();
		startBufferedWrite(new File(rawPath));

		isRecording = true;
		return isRecording;
	}

	public boolean stopRecordingAndConvertFile() {
		if (!isRecording) {
			return isRecording;
		}

		mRecorder.stop();
		isRecording = false;

		FLameUtils lameUtils = new FLameUtils(1, SAMPLE_RATE, 96);
		convertOk = lameUtils.raw2mp3(rawPath, mp3Path);

		return isRecording ^ convertOk;// convertOk==true,return true
	}

	public String getFilePath(int fileAlias) {
		if (fileAlias == RAW) {
			return rawPath;
		} else if (fileAlias == MP3) {
			return mp3Path;
		} else
			return null;
	}

	public void cleanFile(int cleanFlag) {
		File f = null;
		try {
			switch (cleanFlag) {
			case MP3:
				f = new File(mp3Path);
				if (f.exists())
					f.delete();
				break;
			case RAW:
				f = new File(rawPath);
				if (f.exists())
					f.delete();
				break;
			case RAW | MP3:
				f = new File(rawPath);
				if (f.exists())
					f.delete();
				f = new File(mp3Path);
				if (f.exists())
					f.delete();
				break;
			}
			f = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		if (mRecorder != null)
			mRecorder.release();
		activity = null;
	}

	private void initRecorder() {
		int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		mBuffer = new short[bufferSize];
		mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				bufferSize);
	}

	private void getFilePath() {
		try {
			String folder = "audio_recorder_2_mp3";
			String fileName = String.valueOf(System.currentTimeMillis());
			if (rawPath == null) {
				File raw = new File(activity.getDir(folder,
						activity.MODE_PRIVATE), fileName + ".raw");
				raw.createNewFile();
				rawPath = raw.getAbsolutePath();
				raw = null;
			}
			if (mp3Path == null) {
				File mp3 = new File(activity.getDir(folder,
						activity.MODE_PRIVATE), fileName + ".mp3");
				mp3.createNewFile();
				mp3Path = mp3.getAbsolutePath();
				mp3 = null;
			}

			Log.d("rawPath", rawPath);
			Log.d("mp3Path", mp3Path);

			runCommand("chmod 777 " + rawPath);
			runCommand("chmod 777 " + mp3Path);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean runCommand(String command) {
		boolean ret = false;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
			process.waitFor();
			ret = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				process.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	private void startBufferedWrite(final File file) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				DataOutputStream output = null;
				try {
					output = new DataOutputStream(new BufferedOutputStream(
							new FileOutputStream(file)));
					while (isRecording) {
						int readSize = mRecorder.read(mBuffer, 0,
								mBuffer.length);
						for (int i = 0; i < readSize; i++) {
							output.writeShort(mBuffer[i]);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (output != null) {
						try {
							output.flush();
						} catch (IOException e) {
							e.printStackTrace();

						} finally {
							try {
								output.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}).start();
	}

	class Sample {
		AudioRecorder2Mp3Util util = null;
		String mp3 = null;
		String raw = null;

		void start() {
			if (util == null)
				util = new AudioRecorder2Mp3Util(null, "/sdcard/sample.raw",
						"/sdcard/sample.mp3");
			if (mp3 != null || raw != null) {
				util.cleanFile(RAW | MP3);
			}
			util.startRecording();
		}

		void stop() {
			raw = util.getFilePath(RAW);
			mp3 = util.getFilePath(MP3);
			util.cleanFile(RAW);
			util.close();
		}
	}
}