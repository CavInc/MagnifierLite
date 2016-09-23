package cav.magnifierlite;

import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private final String TAG = "MAGNIFER";
    private  final int CAMERA_ID = 0;
    private  final boolean FULL_SCREEN = true;

    private SurfaceView sv;
    private SurfaceHolder holder;
    private Camera camera;
    private HolderCallback holderCallback;

    private TextView zoomText;
    private ImageView flashImgBtn;
    private ImageView zoomPlusBtn;
    private ImageView zoomMinusBtn;

    private boolean flashMode = false;

    private boolean isZoom = false;
    private boolean isFlashMode = false;
    private int maxZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // если хотим, чтобы приложение постоянно имело портретную ориентацию
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // запрет перехода в ждущий режим ?
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        zoomText = (TextView) findViewById(R.id.zoom_text);
        flashImgBtn = (ImageView) findViewById(R.id.flash_img);
        zoomPlusBtn = (ImageView) findViewById(R.id.zoomPlus);
        zoomMinusBtn = (ImageView) findViewById(R.id.zoomMunus);

        flashImgBtn.setOnClickListener(this);
        zoomPlusBtn.setOnClickListener(this);
        zoomMinusBtn.setOnClickListener(this);

        sv = (SurfaceView) findViewById(R.id.surfaceView);
        holder = sv.getHolder();

        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT>Build.VERSION_CODES.ECLAIR) {
            camera = Camera.open(CAMERA_ID);
        }else {
            Toast.makeText(this,"Старый ведройд",Toast.LENGTH_LONG).show();
        }
        checkPreferns();
        setStartFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) camera.release();
        camera = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.flash_img:
                changeFlash();
                break;
            case R.id.zoomPlus:
                setLensSize(MODE_PLUS);
                break;
            case R.id.zoomMunus:
                setLensSize(MODE_MINUS);
                break;

        }
    }

    private void checkPreferns(){
        Parameters params = camera.getParameters();
        isZoom = params.isZoomSupported();
        if (params.isZoomSupported()) {
           maxZoom = params.getMaxZoom();
        }
        Log.d(TAG," "+isZoom);

        if (params.getFlashMode()!=null) {
            isFlashMode = true;
        }

    }

    // включает выключает вспышку
    private void changeFlash(){
        if (isFlashMode) {
            Parameters params = camera.getParameters();
            if (flashMode) {
                // выспышка включена выключаем
                flashMode = false;
                flashImgBtn.setImageResource(R.drawable.ic_flash_on_black_24dp);
                params.setFlashMode(Parameters.FLASH_MODE_OFF);
            }else {
                // вспышка выключена включаем
                flashMode = true;
                flashImgBtn.setImageResource(R.drawable.ic_flash_off_black_24dp);
                params.setFlashMode(Parameters.FLASH_MODE_TORCH);
            }
            camera.setParameters(params);
        }

    }

    // Установка фокуса при старте приложения
    private void setStartFocus(){
        Parameters params = camera.getParameters();
        params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
    }

    private final int MODE_PLUS = 0;
    private final int MODE_MINUS = 1;
    // Работа с зумом
    private void setLensSize(int mode){
        if (isZoom) {
            Parameters params = camera.getParameters();
            int lastZoom = params.getZoom();
            if (mode==MODE_PLUS) {
                lastZoom +=1;
                if (lastZoom>maxZoom) lastZoom=maxZoom;
            }
            if (mode==MODE_MINUS) {
                lastZoom -=1;
                if (lastZoom<0) lastZoom=0;
            }
            zoomText.setText("x "+Integer.toString(lastZoom));
            params.setZoom(lastZoom);
            camera.setParameters(params);
        }
    }

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try {
                camera.setPreviewDisplay(holder); // сказали камере прослойку слушателя
                camera.startPreview(); // начало трансляции
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            camera.stopPreview();// остановили трансляцию
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }
}
