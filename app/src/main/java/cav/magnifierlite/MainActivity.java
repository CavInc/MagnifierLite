package cav.magnifierlite;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener,View.OnTouchListener,Camera.AutoFocusCallback {
    private static final int PERMISOPN_REQUEST_SETTING_CODE = 101;
    private static final String ZOOM_STATE = "ZOOM_STATE";
    private final String TAG = "MAGNIFER";
    private  final int CAMERA_ID = 0;
    private  final boolean FULL_SCREEN = true;

    private SurfaceView sv;
    private SurfaceHolder holder;
    private Camera camera;
    private HolderCallback holderCallback;

    private FrameLayout mFrameLayout;

    private TextView zoomText;
    private ImageView flashImgBtn;
    private ImageView zoomPlusBtn;
    private ImageView zoomMinusBtn;

    private ImageView frezzeBtn;
    private ImageView photoBtn;

    private ScaleGestureDetector scaleGestureDetector;

    private boolean flashMode = false;

    private boolean isZoom = false;
    private boolean isFlashMode = false;
    private int maxZoom;

    private List<String> colorEffect;
    private List<Integer> zoomRatio;

    private List <String> supportFocusMode;

    private int zoomOffset=1;

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

        frezzeBtn = (ImageView) findViewById(R.id.frezee_img);
        photoBtn = (ImageView) findViewById(R.id.photo_img);

        frezzeBtn.setOnClickListener(this);
        photoBtn.setOnClickListener(this);

        mFrameLayout = (FrameLayout) findViewById(R.id.fLayout);

        sv = (SurfaceView) findViewById(R.id.surfaceView);
        holder = sv.getHolder();

        sv.setOnClickListener(this);
        //sv.setOnTouchListener(this);


        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);


        if (savedInstanceState == null) {
            // актифить прервый раз
            Log.d(TAG,"FIRST START");
        }else {
            Log.d(TAG, "GET SAVE VALUE");
            lastZoom = savedInstanceState.getInt(ZOOM_STATE,0);
            Log.d(TAG,Integer.toString(lastZoom));
        }
    }

    private void openApplicationSetting(){
        Intent appSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName()));
        startActivityForResult(appSettingIntent,PERMISOPN_REQUEST_SETTING_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG," ON REQUEST PERMISSION");
        if (requestCode == 102){
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                initalizeCamera();
            }
            if (grantResults[1]==PackageManager.PERMISSION_GRANTED){
                // обработка разрешения на запись
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"RESUME");

        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"A6+");

            ActivityCompat.requestPermissions(this, new String[] {
                    android.Manifest.permission.CAMERA,android.Manifest.permission.WRITE_EXTERNAL_STORAGE},102);//  102 -число с потолка

            Snackbar.make(mFrameLayout, R.string.permision_str,Snackbar.LENGTH_LONG).
                    setAction(R.string.give_permision, new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            openApplicationSetting();
                        }
                    }).show();

        } else {
            initalizeCamera();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"PAUSE");
        if (camera != null) camera.release();
        camera = null;
    }

    private void initalizeCamera(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            camera = Camera.open(CAMERA_ID);
        } else {
            camera = Camera.open();
        }
        setPreviewSize(FULL_SCREEN);
        checkPreferns();
        setStartFocus();
        if (lastZoom != 0) {
            //TODO усановить сохраненный зум
            setZoom(lastZoom);
        }

        if (stop) {
            camera.stopPreview();// остановили трансляцию
            setCameraDisplayOrientation(CAMERA_ID);
            startCamera();
        }

    }

    private boolean stop=false;

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"STOP");
        stop=true;
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
            case R.id.surfaceView:
                Log.d(TAG," SV CLICK");
                setFocusManual();
                break;
            case R.id.frezee_img:
                frezzeOnOff();
                break;
            case R.id.photo_img:
                Log.d(TAG,"PHOTO");
                takePhoto();
                break;
        }
    }

    private boolean frezzeFlg = false;

    private void frezzeOnOff() {
        if (frezzeFlg) {
            frezzeFlg = false;
            frezzeBtn.setImageResource(R.drawable.ic_pause_black_24dp);
            startCamera();

        } else {
            frezzeFlg = true;
            frezzeBtn.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            camera.stopPreview();
        }
    }

    /**
     * Сохраняем состояние активити
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        outState.putInt(ZOOM_STATE,lastZoom);
    }

    private void checkPreferns(){
        Parameters params = camera.getParameters();
        isZoom = params.isZoomSupported();
        if (params.isZoomSupported()) {
            maxZoom = params.getMaxZoom();
            //zoomRatio
            zoomRatio = params.getZoomRatios();
            /*
            коэффициенты масштабирования в 1/100 с шагом.
            x: а зум 3.2x возвращается как 320.
            Число элементов getMaxZoom () + 1. Список сортируется от мала до велика.
            Первый элемент всегда 100.
            Последним элементом является коэффициент увеличения максимального значения масштабирования.
             */
            if (maxZoom>10) {
                if (maxZoom % 2==0){
                    zoomOffset=4;
                }else {
                    zoomOffset = 5;
                }
            }
        }

        Log.d(TAG," -ZOOM "+isZoom);

        if (params.getFlashMode()!=null) {
            isFlashMode = true;
       }else {
            flashImgBtn.setImageResource(R.drawable.ic_flash_on_gray_24dp1);
        }
       supportFocusMode = params.getSupportedFocusModes();
        /*
        for (String l :supportFocusMode){
            Log.d(TAG+" focus Mode:",l);
        }
        */

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
        String focus = params.getFocusMode();
        for (int i=0;i<supportFocusMode.size();i++){
             if (supportFocusMode.get(i).equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                focus = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
            }
        }
        params.setFocusMode(focus);
        camera.setParameters(params);
    }

    private final int MODE_PLUS = 0;
    private final int MODE_MINUS = 1;
    private int lastZoom = 0;
    // Работа с зумом
    private void setLensSize(int mode){
        if (isZoom) {
            Parameters params = camera.getParameters();
            //lastZoom = params.getZoom();
            if (mode==MODE_PLUS) {
                lastZoom +=zoomOffset;
                if (lastZoom>maxZoom) lastZoom=maxZoom;
            }
            if (mode==MODE_MINUS) {
                lastZoom -=zoomOffset;
                if (lastZoom<0) lastZoom=0;
            }
            Log.d(TAG,Integer.toString(lastZoom));
            zoomText.setText("x "+Float.toString((float) (zoomRatio.get(lastZoom)/100.0)));
            params.setZoom(lastZoom);
            camera.setParameters(params);
        }
    }
    // установка значения зума
    private void setZoom(int zoomValue){
        if (isZoom){
            Parameters params = camera.getParameters();
            params.setZoom(zoomValue);
            camera.setParameters(params);
            zoomText.setText("x "+Float.toString((float) (zoomRatio.get(lastZoom)/100.0)));
        }
    }

    // Установка фокуса по касанию экрана
    private void setFocusManual(){
        Parameters params = camera.getParameters();
        if (params.getMaxNumFocusAreas() > 0){
            List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
            Rect areaRect1 = new Rect(-100, -100, 100, 100); // центр экрана
            focusAreas.add(new Camera.Area(areaRect1, 600));
            params.setMeteringAreas(focusAreas);
            camera.setParameters(params);
        }
    }

    private void showToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    private boolean isAutoFocus (){
        for (int i=0;i<supportFocusMode.size();i++){
            if (supportFocusMode.get(i).equals(Parameters.FOCUS_MODE_AUTO)) {
                return true;
            }
        }
        return false;
    }
    // делаем снимок
    private void takePhoto(){
        if (!frezzeFlg) {
            Log.d(TAG,"NO FREEZE");
            //Log.d(TAG,camera.getParameters().getFocusMode());
            if (isAutoFocus()) {
                camera.autoFocus(this);
            }else {
                camera.takePicture(mShutterCallback,null,null,mPictureCallback);
            }
        }
    }

    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            Log.d(TAG,"SHUTTER");
            //Log.d(TAG,camera.getParameters().getFocusMode());
        }
    };

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback(){

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Uri pictureFile = generateUri();
            try {
                savePhotoInFile(data, pictureFile);
                showToast(getString(R.string.save_file) + pictureFile);
                galleryAddPic(pictureFile.toString());
            }catch (Exception e){
                showToast(getString(R.string.error_file));
            }
            camera.startPreview();
        }
    };

    private void savePhotoInFile(byte[] data, Uri pictureFile) throws Exception {
        if (pictureFile == null) throw new Exception();
        OutputStream os = getContentResolver().openOutputStream(pictureFile);
        os.write(data);
        os.close();
    }
    private String mPath;

    // путь к каталогу для сохранения
    private Uri generateUri(){
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;
        File path = new File (Environment.getExternalStorageDirectory(), "MagnifierLite");
        // для общего каталога Pictures/
        //File path = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
        if (! path.exists()) {
            if (!path.mkdirs()){
                return null;
            }
        }
        mPath = path.getPath();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        File newFile = new File(path.getPath() + File.separator +"MF" +timeStamp + ".jpg");
        return Uri.fromFile(newFile);
    }

    // Региструем в галерее
    private void galleryAddPic(String pathPhoto){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(pathPhoto);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
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

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.d(TAG,"TOUCH");
        Log.d(TAG,motionEvent.toString());
        return false;
    }

    private void startCamera(){
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            showToast(e.getLocalizedMessage());
        }
    }

    @Override
    public void onAutoFocus(boolean success, Camera lCamera) {
        if (success) {
            lCamera.takePicture(mShutterCallback, null, null, mPictureCallback);
            camera.cancelAutoFocus();
        }
    }

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            Log.d(TAG,"sufraceCreated");
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
            Log.d(TAG,"sufraceChanged");
            if (camera!=null) {
                camera.stopPreview();// остановили трансляцию
                setCameraDisplayOrientation(CAMERA_ID);
                startCamera();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }
}
