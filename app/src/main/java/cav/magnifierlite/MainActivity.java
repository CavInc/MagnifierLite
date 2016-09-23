package cav.magnifierlite;

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

import java.io.IOException;

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
    private int maxZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        zoomText = (TextView) findViewById(R.id.zoom_text);
        flashImgBtn = (ImageView) findViewById(R.id.flash_img);
        zoomPlusBtn = (ImageView) findViewById(R.id.zoomPlus);
        zoomMinusBtn = (ImageView) findViewById(R.id.zoomMunus);

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

        }
        checkPreferns();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) camera.release();
        camera = null;
    }

    @Override
    public void onClick(View view) {

    }

    private void checkPreferns(){
        Parameters params = camera.getParameters();
        isZoom = params.isZoomSupported();
        if (params.isZoomSupported()) {
           maxZoom = params.getMaxZoom();
        }
        Log.d(TAG," "+isZoom);

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
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }
}
