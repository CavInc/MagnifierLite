package cav.magnifierlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.LinearLayout;
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
    private static final int EXT_PANEL_VIEW = 0;
    private static final int MAIN_PANEL_VIEW = 1;
    private static final String CAMERA_SELECT_TYPE = "CAMERA_SELECT_TYPE";
    private static final int PERMISSION_REQUEST_CODE = 102;
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 103;
    private static final int PERMISSION_REQUEST_CODE_SD = 104;
    private final String TAG = "MAGNIFER";
    private  int CAMERA_ID = 0;
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

    private ImageView backBtn;
    private ImageView extBtn;
    private ImageView changeBtn;


    private boolean flashMode = false;

    private boolean isZoom = false;
    private boolean isFlashMode = false;
    private int maxZoom;

    private boolean isWritePermission = false;

    private int cam_count = 0;

    private List<String> colorEffect;
    private List<Integer> zoomRatio;

    private List <String> supportFocusMode;
    private List<Camera.Size> pictureSize;

    private ScaleGestureDetector mGestureDetector;

    private int zoomOffset=1;

    private LinearLayout upPanel;
    private LinearLayout downPanel;
    private LinearLayout twoPanel;
    private TextView mMinMax;

    private boolean lockRepeatPrivelege = false;

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
        mMinMax = (TextView) findViewById(R.id.max_min);

        flashImgBtn.setOnClickListener(this);
        zoomPlusBtn.setOnClickListener(this);
        zoomMinusBtn.setOnClickListener(this);

        frezzeBtn = (ImageView) findViewById(R.id.frezee_img);
        photoBtn = (ImageView) findViewById(R.id.photo_img);

        frezzeBtn.setOnClickListener(this);
        photoBtn.setOnClickListener(this);

        extBtn = (ImageView) findViewById(R.id.add_panel);
        backBtn = (ImageView) findViewById(R.id.back_img);

        extBtn.setOnClickListener(this);
        backBtn.setOnClickListener(this);

        mMinMax.setOnClickListener(this);

        changeBtn = (ImageView) findViewById(R.id.change_camera_img);
        changeBtn.setOnClickListener(this);

        mFrameLayout = (FrameLayout) findViewById(R.id.fLayout);

        sv = (SurfaceView) findViewById(R.id.surfaceView);
        holder = sv.getHolder();

        mGestureDetector =  new ScaleGestureDetector (sv.getContext(), new MyScaleGestureListener());

        //sv.setOnClickListener(this);
        sv.setOnTouchListener(this);


        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);


        if (savedInstanceState == null) {
            // актифить прервый раз
            Log.d(TAG,"FIRST START");
        }else {
            Log.d(TAG, "GET SAVE VALUE");
            lastZoom[CAMERA_ID] = savedInstanceState.getInt(ZOOM_STATE,0);
            Log.d(TAG,Integer.toString(lastZoom[CAMERA_ID]));
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            isWritePermission = true;
            changeStatusPhotoButton(isWritePermission);
        } else {
            changeStatusPhotoButton(isWritePermission);
        }

    }

    private void openApplicationSetting(){
        Intent appSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName()));
        startActivityForResult(appSettingIntent,PERMISOPN_REQUEST_SETTING_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // словили другое разрешение
        /*
        if (requestCode != PERMISSION_REQUEST_CODE  ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        */

        if (requestCode == PERMISSION_REQUEST_CODE_CAMERA && grantResults.length != 0) {
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                if (camera != null) camera.release();
                camera = null;
                initalizeCamera();
            }   else {
                lockRepeatPrivelege = true;
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.app_name)
                        .setMessage(R.string.no_camera_permission)
                        .setPositiveButton(R.string.ok, listener)
                        .show();
                return;
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE_SD && grantResults.length != 0) {
            if (grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                // обработка разрешения на запись
                lockRepeatPrivelege = true;
                Log.d(TAG,"NO SAVE PERMISION");
                isWritePermission = false;
                changeStatusPhotoButton(isWritePermission);
                final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.dialog_attention)
                        .setMessage(R.string.no_write_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                            }
                        }).create();
                dialog.show();
                return;
            } else {
                isWritePermission = true;
                changeStatusPhotoButton(isWritePermission);
            }
        }

        /*
        if (grantResults.length!=0){
            System.out.println(grantResults);
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                if (camera != null) camera.release();
                camera = null;
                initalizeCamera();
                //return;
            }
            if (grantResults[1]!=PackageManager.PERMISSION_GRANTED){
                // обработка разрешения на запись
                Log.d(TAG,"NO SAVE PERMISION");
                isWritePermission = false;
                changeStatusPhotoButton(isWritePermission);
                final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle(R.string.dialog_attention)
                        .setMessage(R.string.no_write_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).create();
                dialog.show();
                return;
            } else {
                isWritePermission = true;
                changeStatusPhotoButton(isWritePermission);
            }
        }
        */
        //changeStatusPhotoButton(isWritePermission);
        /*
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name)
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
        */
       // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }



    @Override
    protected void onResume() {
        super.onResume();

        if (lockRepeatPrivelege) {
            changeStatusPhotoButton(isWritePermission);
            return;
        }

        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"A6+");

            ActivityCompat.requestPermissions(this, new String[] {
                    android.Manifest.permission.CAMERA},PERMISSION_REQUEST_CODE_CAMERA);//  102 -число с потолка
        } else {
            initalizeCamera();
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_REQUEST_CODE_SD);//  102 -число с потолка
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) camera.release();
        camera = null;
    }

    private void changeStatusPhotoButton (boolean status) {
        if (status) {
            photoBtn.setImageResource(R.drawable.ic_camera_alt_black_24dp);
            photoBtn.setEnabled(true);
        } else {
            photoBtn.setImageResource(R.drawable.ic_photo_camera_black_24dp);
            photoBtn.setEnabled(false);
        }
    }

    private void initalizeCamera(){
        //определение количества камер на устройстве

        cam_count = Camera.getNumberOfCameras() ;
        Log.d(TAG+" CAMERA NUM :",Integer.toString(cam_count));
        if (cam_count<2) {
            // TODO здесь ставим неактивную иконку для поворота
            changeBtn.setImageResource(R.drawable.ic_camera_front_gray_24dp1);
        }
        if (cam_count == 0) return;

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            camera = Camera.open(CAMERA_ID);
        } else {
            camera = Camera.open();
        }
        */
        camera = Camera.open(CAMERA_ID);

        setPreviewSize(FULL_SCREEN);
        checkPreferns();
        setStartFocus();
        if (lastZoom[CAMERA_ID] != 0) {
            //TODO усановить сохраненный зум
            setZoom(lastZoom[CAMERA_ID]);
        }

        if (stop) {
            camera.stopPreview();// остановили трансляцию
            setCameraDisplayOrientation(CAMERA_ID);
            startCamera();
        }

    }

    private boolean stop = false;

    @Override
    protected void onStop() {
        super.onStop();
       // Log.d(TAG,"STOP");
        stop=true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.flash_img:
                changeFlash();
                break;
            case R.id.zoomPlus:
                setLensSize(MODE_PLUS,zoomOffset);
                break;
            case R.id.zoomMunus:
                setLensSize(MODE_MINUS,zoomOffset);
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
            case R.id.add_panel:
                Log.d(TAG,"EXT PANEL");
                changeViewPanel(EXT_PANEL_VIEW);
                break;
            case R.id.back_img:
                Log.d(TAG,"BACK PANEL");
                changeViewPanel(MAIN_PANEL_VIEW);
                break;
            case R.id.change_camera_img:
                // смена камеры фронт/задник
                if (cam_count>1) {
                    changeCamera();
                }
                break;
            case R.id.max_min:
                if (minMaxFlg) {
                    setZoom(maxZoom);
                    mMinMax.setText("MIN");
                    lastZoom[CAMERA_ID] = maxZoom;
                } else {
                    setZoom(0);
                    mMinMax.setText("MAX");
                    lastZoom[CAMERA_ID] = 0;
                }

                minMaxFlg = !minMaxFlg;
                zoomText.setText("x "+Float.toString((float) (zoomRatio.get(lastZoom[CAMERA_ID])/100.0)));
                break;
        }
    }

    private boolean minMaxFlg = true;

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
        outState.putInt(ZOOM_STATE,lastZoom[CAMERA_ID]);
        outState.putInt(CAMERA_SELECT_TYPE,CAMERA_ID);
    }

    private int maxWidth=0;
    private int maxHeight=0;

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
            flashImgBtn.setImageResource(R.drawable.ic_flash_on_black_24dp);
       }else {
            isFlashMode = false;
            flashImgBtn.setImageResource(R.drawable.ic_flash_on_gray_24dp1);
        }
       supportFocusMode = params.getSupportedFocusModes();
        /*
        for (String l :supportFocusMode){
            Log.d(TAG+" focus Mode:",l);
        }
        */
        // размеры подперживаемые камерой
        pictureSize =  params.getSupportedPictureSizes();
        for (Camera.Size l:pictureSize){
            maxWidth = 0;

            Log.d(TAG+" SIZE:","WIDTH:"+Integer.toString(l.width)+" HEIGHT: "+Integer.toString(l.height));
            if  (maxWidth<l.width) {
                maxWidth=l.width;
                maxHeight=l.height;
            }
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
    private int[] lastZoom = {0,0};
    // Работа с зумом
    private void setLensSize(int mode,int zoomOffset){
        if (isZoom) {
            Parameters params = camera.getParameters();
            //lastZoom = params.getZoom();
            if (mode==MODE_PLUS) {
                lastZoom[CAMERA_ID] +=zoomOffset;
                if (lastZoom[CAMERA_ID]>maxZoom) lastZoom[CAMERA_ID]=maxZoom;
            }
            if (mode==MODE_MINUS) {
                lastZoom[CAMERA_ID] -=zoomOffset;
                if (lastZoom[CAMERA_ID]<0) lastZoom[CAMERA_ID]=0;
            }
            Log.d(TAG,Integer.toString(lastZoom[CAMERA_ID]));
            zoomText.setText("x "+Float.toString((float) (zoomRatio.get(lastZoom[CAMERA_ID])/100.0)));
            params.setZoom(lastZoom[CAMERA_ID]);
            camera.setParameters(params);
        }
    }
    // установка значения зума
    private void setZoom(int zoomValue){
        if (isZoom){
            Parameters params = camera.getParameters();
            params.setZoom(zoomValue);
            camera.setParameters(params);
            zoomText.setText("x "+Float.toString((float) (zoomRatio.get(lastZoom[CAMERA_ID])/100.0)));
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

    // мега пихели
    // long pixelCountTemp = cameraParams.getSupportedPictureSizes().get(j).width * cameraParams.getSupportedPictureSizes().get(j).height; // Just changed i to j in this loop
    // maxResolution = ((float)pixelCountTemp) / (1024000.0f);
    // делаем снимок
    private void takePhoto(){
        if (!frezzeFlg) {
            Log.d(TAG,"NO FREEZE");
            //Log.d(TAG,camera.getParameters().getFocusMode());
            Parameters parameters = camera.getParameters();
            parameters.setPictureSize(maxWidth, maxHeight);
            camera.setParameters(parameters);


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
        //Log.d(TAG,"TOUCH");
        //Log.d(TAG,motionEvent.toString());
       // gdt.onTouchEvent(motionEvent);
        if (mGestureDetector != null && mGestureDetector.onTouchEvent(motionEvent)) return true;
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


    /**
     * Менят видимость панелей с кнопками
     * @param mode
     */
    private void changeViewPanel(int mode) {
        upPanel = (LinearLayout) findViewById(R.id.up_button_panel);
        downPanel = (LinearLayout) findViewById(R.id.down_button_panel);
        twoPanel = (LinearLayout) findViewById(R.id.two_panel);

        switch (mode){
            case EXT_PANEL_VIEW:
                upPanel.setVisibility(View.GONE);
                downPanel.setVisibility(View.GONE);
                twoPanel.setVisibility(View.VISIBLE);
                break;
            case MAIN_PANEL_VIEW:
                upPanel.setVisibility(View.VISIBLE);
                downPanel.setVisibility(View.VISIBLE);
                twoPanel.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Смена камеры для работы
     */
    private void changeCamera() {
        // гасим текущую
        if (camera != null) camera.release();
        camera = null;

        CAMERA_ID = CAMERA_ID ^ 1;
        stop=true;
        initalizeCamera();
        //TODO изменение индикатора камеры
        setZoom(1);
        if (CAMERA_ID == 1 ){
            changeBtn.setImageResource(R.drawable.ic_camera_rear_black_24dp);
        } else {
            changeBtn.setImageResource(R.drawable.ic_camera_front_black_24dp);
        }
    }


    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
           // Log.d(TAG,"sufraceCreated");
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

    private float mOldScale;
    private class MyScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
           // Log.d(TAG,"ONSCALE");
          //  Log.d(TAG,Float.toString(scaleGestureDetector.getScaleFactor()));
            float scaleFactor=scaleGestureDetector.getScaleFactor();//получаем значение зума относительно предыдущего состояния
            //получаем координаты фокальной точки - точки между пальцами
            float focusX=scaleGestureDetector.getFocusX();
            float focusY=scaleGestureDetector.getFocusY();
            if (mOldScale>scaleFactor) {
                setLensSize(MODE_MINUS,1);
            }else {
                setLensSize(MODE_PLUS,1);
            }
          //  Log.d(TAG,Float.toString(scaleGestureDetector.getCurrentSpan()));
            //mOldScale=scaleFactor;
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            mOldScale=scaleGestureDetector.getScaleFactor();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        }
    }
}
