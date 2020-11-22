package com.example.pepe.Recognition;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pepe.PreferenceManager;
import com.example.pepe.R;
import com.example.pepe.Recognition.CameraActivity;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.example.pepe.kakao.Ocr;
import com.example.pepe.kakao.Result;
import com.google.gson.Gson;
import cz.msebera.android.httpclient.Header;

public class ImageAnalysisActivity extends AppCompatActivity {

    //다른 Activity에서 접근할 수 있도록 context 변수 선언
    public static Context context_main; // context 변수 선언
    final private static String TAG = "PEPE";
    TextToSpeech tts;
    Float VolumeNo,Speed;
    TextView view_result;

    //이미지 분석에 사용할 가장 최근에 촬영한 사진의 이름
    public String targetPhoto = ((CameraActivity)CameraActivity.context_main).mCurrentPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageanalysis);
        Intent intent = getIntent();
        String imgs = intent.getStringExtra("img");

        Bitmap srcBmp = BitmapFactory.decodeFile(imgs);

        if(String.valueOf(srcBmp) == null){
            Intent error = new Intent(ImageAnalysisActivity.this, CameraActivity.class);
            startActivity(error);
            tts.speak("저장된 사진이 없습니다. 앱을 다시 시작해주세요", TextToSpeech.QUEUE_FLUSH, null);
        ImageAnalysisActivity.this.finish();
        }

        Context context = getApplicationContext();
        final Vibrator vibrator = (Vibrator)getSystemService(context.VIBRATOR_SERVICE);

        VolumeNo = PreferenceManager.getFloat(context,"Volume");
        Speed = PreferenceManager.getFloat(context, "Speed");

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(Speed);
                }
            }
        });

        view_result = (TextView)findViewById(R.id.textView_analy);
        Button recam = (Button)findViewById(R.id.button_recam);
        Button easy = (Button)findViewById(R.id.button_easy);

        recam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vibrator.vibrate(1000);
                Intent intent_recog = new Intent(ImageAnalysisActivity.this, CameraActivity.class);
                startActivity(intent_recog);
                ImageAnalysisActivity.this.finish();
            }
        });

        easy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long[] pattern = {0,700,200,700};
                vibrator.vibrate(pattern, -1);
                ComponentName componentName = new ComponentName("com.kakao.talk","com.kakao.talk.activity.SplashActivity");
                Intent intent_open = new Intent(Intent.ACTION_MAIN);
                intent_open.addCategory(Intent.CATEGORY_LAUNCHER);
                intent_open.setComponent(componentName);
                tts.speak("이지터치가 열렸습니다.",TextToSpeech.QUEUE_FLUSH,null);
                startActivity(intent_open);
                ImageAnalysisActivity.this.finish();
            }
        });
        int iWidth   = 520;   // 축소시킬 너비

        int iHeight  = 520;   // 축소시킬 높이


        float fWidth  = srcBmp.getWidth();

        float fHeight = srcBmp.getHeight();



// 원하는 널이보다 클 경우의 설정

        if(fWidth > iWidth) {

            float mWidth = (float) (fWidth / 100);

            float fScale = (float) (iWidth / mWidth);

            fWidth *= (fScale / 100);

            fHeight *= (fScale / 100);

// 원하는 높이보다 클 경우의 설정

        }else if (fHeight > iHeight) {

            float mHeight = (float) (fHeight / 100);

            float fScale = (float) (iHeight / mHeight);

            fWidth *= (fScale / 100);

            fHeight *= (fScale / 100);

        }



        FileOutputStream fosObj = null;

        try {

            // 리사이즈 이미지 동일파일명 덮어 씌우기 작업

            Bitmap resizedBmp = Bitmap.createScaledBitmap(srcBmp, (int)fWidth, (int)fHeight, true);

            fosObj = new FileOutputStream(imgs);

            resizedBmp.compress(Bitmap.CompressFormat.JPEG, 100, fosObj);

        } catch (Exception e){

            ;

        } finally {

            try {
                fosObj.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                fosObj.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }




        Log.d("path",imgs);





//        Intent intent_c_next = new Intent(ImageAnalysisActivity.this, C_NextAcitivity.class);

        //다른 Activity에서 접근할 수 있도록 선언
        context_main = this;


        //GET_naver("https://dapi.kakao.com/v2/vision/text/ocr");
        POSTUpload("https://dapi.kakao.com/v2/vision/text/ocr",imgs);
    }



    private void POSTUpload(String url_upload, String filepath) {
        RequestParams params = new RequestParams();

        //File sdcard = Environment.getExternalStorageDirectory();
// to this path add a new directory path
        //File dir = new File(sdcard.getAbsolutePath() + "/download/");
// create this directory if not already created
// create the file in which we will write the contents
        File files = new File(filepath);
        Log.d("log",files.getAbsolutePath());
        Log.d("log:size = ", String.valueOf(files.length()));



        try {
            params.put("image", files);
            params.setForceMultipartEntityContentType(true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setMaxConnections(100);
        client.addHeader("Authorization", "KakaoAK bdd55a1090f22ceea0b8fc66da6b3073"); // NCP Maps User Key ID

        client.post(url_upload, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d("Succ", String.valueOf(statusCode));
                Log.d("Succ", String.valueOf(responseBody));

                Gson gson = new Gson();
                Ocr ocrresult = new Ocr();
                ocrresult = gson.fromJson(new String(responseBody), Ocr.class);

                List<Result> result = ocrresult.getResult();

                String text= "";
                for(int i = 0 ; i < result.size();i++) {
                    Result res = new Result();
                    res = result.get(i);
                    List<String> recognitionWords = res.getRecognitionWords();
                    for (int j = 0; j < recognitionWords.size(); j++) {
                        String te = recognitionWords.get(j);
                        Log.d("result", te); //te에 결과 저장됨 이거 tts로 읽어주면 됩니다.
                        text += te;
                    }
                    tts.speak(text,TextToSpeech.QUEUE_FLUSH,null);
                    view_result.setText(text);
                }

                tts.speak("볼륨 높임 버튼을 누르면 재인식을 볼륨 낯춤 버튼을 누르면 이지터치로 이동합니다.",TextToSpeech.QUEUE_ADD,null);

            }


            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("Fail", String.valueOf(statusCode));

                Log.d("Fail", String.valueOf(responseBody));
            }
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        switch (keyCode){
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                double temp = VolumeNo + 0.15;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*temp), AudioManager.FLAG_SHOW_UI);
                ComponentName componentName = new ComponentName("com.kakao.talk","com.kakao.talk.activity.SplashActivity");
                Intent intent_open = new Intent(Intent.ACTION_MAIN);
                intent_open.addCategory(Intent.CATEGORY_LAUNCHER);
                intent_open.setComponent(componentName);
                tts.speak("이지터치가 열렸습니다.",TextToSpeech.QUEUE_FLUSH,null);
                startActivity(intent_open);
                ImageAnalysisActivity.this.finish();
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*VolumeNo), AudioManager.FLAG_SHOW_UI);
                Intent intent_recog = new Intent(ImageAnalysisActivity.this, CameraActivity.class);
                tts.speak("재인식",TextToSpeech.QUEUE_FLUSH,null);
                startActivity(intent_recog);
                ImageAnalysisActivity.this.finish();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

}
