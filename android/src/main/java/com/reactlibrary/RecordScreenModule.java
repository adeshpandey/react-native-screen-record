package com.reactlibrary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.telecom.Call;

import com.facebook.common.activitylistener.BaseActivityListener;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableNativeMap;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.File;
import java.io.IOException;

public class RecordScreenModule extends ReactContextBaseJavaModule implements HBRecorderListener {

    private final ReactApplicationContext reactContext;
    private Boolean mic= false;
    private HBRecorder hbRecorder;
    private File outputUri;
    private int SCREEN_RECORD_REQUEST_CODE = 1000;
    private Promise startPromise;
    private Promise stopPromise;
    public RecordScreenModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        outputUri = reactContext.getExternalFilesDir("ReactNativeRecordScreen");
    }


    private final ActivityEventListener mActivityEventListener = new ActivityEventListener() {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_CANCELED) {
                    startPromise.reject("403","Permission denied");
                } else if (resultCode == Activity.RESULT_OK) {
                    hbRecorder.startScreenRecording(data, resultCode, getCurrentActivity());
                }
                startPromise.resolve(data);
            }
        }
    };

    @Override
    public String getName() {
        return "RnScreenRecord";
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @ReactMethod
    public void setup(Boolean mic) {
        // TODO: Implement some actually useful functionality
//        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
        hbRecorder= new HBRecorder(reactContext,this);
        hbRecorder.isAudioEnabled(mic);
        hbRecorder.setOutputPath(outputUri.toString());
        if(doesSupportEncoder("h264")){
            hbRecorder.setVideoEncoder("H264");
        }else{
            hbRecorder.setVideoEncoder("DEFAULT");
        }
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startRecordingScreen(){
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) reactContext.getSystemService (Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        getCurrentActivity().startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
    }

    @ReactMethod
    public void record(Promise promise){
        startPromise = promise;
        try {
            startRecordingScreen();
        } catch (IllegalStateException e) {
            startPromise.reject("404",e.getMessage());
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @ReactMethod
    public void stop(Promise promise){
        stopPromise=promise;
        hbRecorder.stopScreenRecording();
    }

    @TargetApi(Build.VERSION_CODES.N)
    @ReactMethod
    public void pause(Callback callback){
        hbRecorder.pauseScreenRecording();
    }

    @Override
    public void HBRecorderOnStart() {

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void HBRecorderOnComplete() {
        String uri = hbRecorder.getFilePath();
        WritableNativeMap response = new WritableNativeMap();
        WritableNativeMap result =  new WritableNativeMap();
        result.putString("outputURL", uri);
        response.putString("status", "success");
        response.putMap("result", result);
        stopPromise.resolve(response);
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        startPromise.resolve(reason);
    }

    private boolean doesSupportEncoder(String encoder) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i=0; i<numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder()) {
                if (codecInfo.getName() != null) {
                    if (codecInfo.getName().contains(encoder)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
