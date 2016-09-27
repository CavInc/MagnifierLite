package cav.magnifierlite;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener{
    private static final int PERMISOPN_REQUEST_SETTING_CODE = 101;
    private static final String ZOOM_STATE = "ZOOM_STATE";
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

    private List<String> colorEffect;

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
/*
        // разрешения для A6+
        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"No A6+");
        }else {
            showToast("А тут надо поставить разрешения для A6+");
            /*
            ActivityCompat.requestPermissions(this, new String[] {
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            },102);//  102 -число с потолка
            Snackbar.make(mCoordinatorLayout,"Для корректной работы необходимо дать требуемые разрешения ",Snackbar.LENGTH_LONG).
                    setAction(R.string.solve_txt, new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            openApplicationSetting();
                        }
                    }).show();
           */
      /*  }
    */
        if (savedInstanceState == null) {
            // актифить прервый раз
        }else {
            lastZoom = savedInstanceState.getInt(ZOOM_STATE,0);
        }
    }

    private void openApplicationSetting(){
        Intent appSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName()));
        startActivityForResult(appSettingIntent,PERMISOPN_REQUEST_SETTING_CODE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.FROYO) {
            camera = Camera.open(CAMERA_ID);
        }else {
            Toast.makeText(this,"Старый ведройд",Toast.LENGTH_LONG).show();
            camera = Camera.open();
        }
        setPreviewSize(FULL_SCREEN);
        checkPreferns();
        setStartFocus();
        if (lastZoom!=0) {
            //TODO усановить сохраненный зум
        }
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

    /**
     * Сохраняем состояние активити
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ZOOM_STATE,lastZoom);
    }

    private void checkPreferns(){
        Parameters params = camera.getParameters();
        isZoom = params.isZoomSupported();
        if (params.isZoomSupported()) {
           maxZoom = params.getMaxZoom();
        }
        Log.d(TAG," -ZOOM "+isZoom);

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
        }else {
            showToast(getResources().getString(R.string.no_flash));
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
    private int lastZoom = 0;
    // Работа с зумом
    private void setLensSize(int mode){
        if (isZoom) {
            Parameters params = camera.getParameters();
            lastZoom = params.getZoom();
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

    // Установка фокуса по касанию экрана
    private void setFocusManual(){
        Parameters params = camera.getParameters();
        if (params.getMaxNumFocusAreas() > 0){

        }
    }

    private void showToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }


    private void setResizeViewPort(){
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        float aspect = (float) previewSize.width / previewSize.height;

        int previewSurfaceWidth = sv.getWidth();
        int previewSurfaceHeight = sv.getHeight();

        LayoutParams lp = sv.getLayoutParams();

        // здесь корректируем размер отображаемого preview, чтобы не было искажений
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
            // портретный вид
            camera.setDisplayOrientation(90);
            lp.height = previewSurfaceHeight;
            lp.width = (int) (previewSurfaceHeight / aspect);
        }else {
            // ландшафтный
            camera.setDisplayOrientation(0);
            lp.width = previewSurfaceWidth;
            lp.height = (int) (previewSurfaceWidth / aspect);
        }
        sv.setLayoutParams(lp);
        camera.startPreview();
    }


    // поворот экрана
    private void setCameraDisplayOrientation(int cameraId) {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // задняя камера
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);
    }

    private void setPreviewSize(boolean fullScreen) {
        // получаем размеры экрана
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        Camera.Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF первью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // превью в вертикальной ориентации
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        // преобразование
        matrix.mapRect(rectPreview);

        // установка размеров surface из получившегося преобразования
        sv.getLayoutParams().height = (int) (rectPreview.bottom);
        sv.getLayoutParams().width = (int) (rectPreview.right);
    }

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            /*
            try {
                camera.setPreviewDisplay(holder); // сказали камере прослойку слушателя
                camera.startPreview(); // начало трансляции
            } catch (IOException e) {
                e.printStackTrace();
                showToast(e.getLocalizedMessage());
            }
           // setResizeViewPort();
            */
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

            camera.stopPreview();// остановили трансляцию
            setCameraDisplayOrientation(CAMERA_ID);
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                showToast(e.getLocalizedMessage());
            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }
}
