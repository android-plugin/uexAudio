var type = 0;

var soundid = 8;

function openMusicExplorer() {

  type = 0;

  uexFileMgr.explorer('');

}

function openListMusicExplorer() {

  type = 1;

  uexFileMgr.explorer('');

}

function record() {
  uexAudio.record(1, 'cui', function(data) {

    recordPath = data;
    document.getElementById('backrecord').innerHTML = data;

  });
}

function openSoundExplorer() {

  type = 2;

  uexFileMgr.explorer('');

}

// 音乐播放

function audioMgr(type, url) {

  if (type == -1) {

    uexAudio.open(url);

  } else if (type == 0) {

    uexAudio.play('4');

  } else if (type == 2) {

    uexAudio.stop();

  } else if (type == 3) {

    uexAudio.pause();

  } else if (type == 5) {

    uexAudio.volumeUp();

  } else if (type == 6) {

    uexAudio.volumeDown();

  } else if (type == 7) {

    var value = document.getElementById('musicList').value;

    if (value != '' && value.length > 0) {

      var mycars = value.split(";");

      uexAudio.openPlayer(mycars, "0");

    } else {

      alert("文件列表不能为空！");

    }

  } else if (type == 8) {

    uexAudio.record('recordCallBack', 'failedCallBack', function(opCode, dataType, data) {

      switch (dataType) {

        case cText:
          recordPath = data;
          document.getElementById('backrecord').innerHTML = data;

          break;

        case cJson:

          alert("uex.cJson");

          break;

        case cInt:

          alert("uex.cInt");

          break;

        default:

          alert("error");

      }

    });

  } else if (type == 9) {

    uexAudio.openSoundPool();

  } else if (type == 10) {

    if (soundid > 0)

      uexAudio.playFromSoundPool(soundid);

  } else if (type == 11) {

    if (soundid > 0) {

      uexAudio.stopFromSoundPool(soundid);

    }

  } else if (type == 12)

  {

    uexAudio.addSound(soundid, url);

  } else if (type == 13)

    uexAudio.closeSoundPool();

}

function recordCallBack(data) {

  var obj = eval('(' + data + ')');

  document.getElementById('videorecord').innerHTML = obj.audioRecorderPath;

}

function explorerSuccess(data) {

  var obj = eval('(' + data + ')');

  document.getElementById('hidText').value = obj.fileExplorerPath;

  document.getElementById('file').innerHTML = obj.fileExplorerPath;

}

var cText = 0;

var cJson = 1;

var cInt = 2;



function playBackRecord() {

  uexAudio.open(recordPath);

  uexAudio.play('2');

}

function playMode() {
  var param = {
    playMode: '0'
  };
  param = JSON.stringify(param);

  uexAudio.setPlayMode(param);

}

function playMode1() {
  var param = {
    playMode: '1'
  };
  param = JSON.stringify(param);

  uexAudio.setPlayMode(param);

}

function startBackgroundRecord() {
  uexAudio.startBackgroundRecord(1, 'hong.caf');
}

function stopBackgroundRecord() {
  uexAudio.stopBackgroundRecord(function(data) {

    alert("Path:" + data);

    recordPath = data;

    document.getElementById('backrecord').innerHTML = data;

  });
}

var recordPath;

window.uexOnload = function() {

  uexAudio.onPlayFinished = function(data) {

    alert("music finished" + data);

  }

  uexFileMgr.cbExplorer = function(opCode, dataType, data) {

    if (type == 0) {

      document.getElementById('hidText').value = data;

      document.getElementById('file').innerHTML = data;

    }

    if (type == 1) {

      var value = document.getElementById('musicList').value;

      if (value != '' && value.length > 0) {

        document.getElementById('musicList').value = value + ";" + data;

      } else {

        document.getElementById('musicList').value = data;

      }

    }

    if (type == 2) {

      document.getElementById('SoundhidText').value = data;

      document.getElementById('Soundfile').innerHTML = data;

      soundid = document.getElementById('dirPath').value;

    }

  }

  //			uexAudio.cbOpenSoundPool=function(opCode,dataType,data){

  //				switch(dataType){

  //					case cText:

  //					  alert("cText");

  //					  break;

  //					case cJson:

  //					  alert("cJson");

  //					  break;

  //					case cInt:

  //					alert("open 成功");

  //					  soundid=data;

  //					 	break;

  //					default:

  //			      alert("error");

  //					}

  //			}

  uexWidgetOne.cbError = function(opCode, errorCode, errorInfo) {

    alert("错误代码：" + errorCode + "\n" + "错误内容：" + errorInfo);

  }



}
