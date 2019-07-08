package com.hiscene.flytech.ui;

import android.arch.lifecycle.Lifecycle;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.weiss.core.UserManager;
import com.github.weiss.core.bus.RxBus;
import com.github.weiss.core.utils.AppUtils;
import com.github.weiss.core.utils.LogUtils;
import com.github.weiss.core.utils.SPUtils;
import com.github.weiss.core.utils.ScreenUtils;
import com.github.weiss.core.utils.StringUtils;
import com.github.weiss.core.utils.ToastUtils;
import com.github.weiss.core.utils.helper.RxSchedulers;
import com.google.zxing.Result;
import com.hiscene.camera.listener.OnQrRecognizeListener;
import com.hiscene.camera.view.CameraView;
import com.hiscene.camera.vision.QRVision;
import com.hiscene.flytech.BaseActivity;
import com.hiscene.flytech.C;
import com.hiscene.flytech.C;
import com.hiscene.flytech.R;
import com.hiscene.flytech.entity.UserModel;
import com.hiscene.flytech.event.EventCenter;
import com.hiscene.flytech.event.SimpleEventHandler;
import com.hiscene.flytech.excel.ProcessExcel;
import com.hiscene.flytech.lifecycle.IComponentContainer;
import com.hiscene.flytech.lifecycle.LifeCycleComponent;
import com.hiscene.flytech.lifecycle.LifeCycleComponentManager;
import com.hiscene.flytech.recorder.CameraRecorder;
import com.hiscene.flytech.recorder.ScreenRecorderManager;
import com.hiscene.flytech.ui.fragment.DeviceFragment;
import com.hiscene.flytech.recorder.CameraRecorder;
import com.hiscene.flytech.ui.fragment.ExcelFragmentManager;
import com.hiscene.flytech.ui.fragment.LoginFragment;
import com.hiscene.flytech.ui.fragment.ScanDeviceFragment;
import com.hiscene.flytech.ui.fragment.ScanLoginFragment;
import com.hiscene.flytech.ui.fragment.StartEditExcelFragment;
import com.hiscene.flytech.util.PositionUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;

import static com.hiscene.flytech.App.userManager;
import static com.hiscene.flytech.ui.fragment.ExcelFragmentManager.RECOVERY;


public class MainActivity extends BaseActivity implements IComponentContainer {
    private static final int REQUEST_SCREEN_LIVE = 1;

    private int FLAG = -1;
    public static final int LOGIN = 0;//登录页
    public static final int SCAN_LOGIN = 1;//扫描登录页
    public static final int DEVICE = 2;//主界面页
    public static final int SCAN_DEVICE = 3;//扫描设备页
    public static final int START_EDIT = 4;//开始填写表单页

    public static final String BACK_TO_LOGIN ="BACK_TO_LOGIN" ;
    public static final String START_EDIT_EXCEL ="START_EDIT" ;
    public static final String START_SCAN_LOGIN ="START_SCAN_LOGIN" ;
    public static final String HILEIA="HILEIA";
    private LifeCycleComponentManager mComponentContainer = new LifeCycleComponentManager();

    @BindView(R.id.cameraLayout)
    LinearLayout cameraLayout;

    CameraView cameraView;
    CameraRecorder cameraRecorder;
    ScreenRecorderManager screenRecorderManager;
    boolean isLaunchHiLeia = false;

    LoginFragment loginFragment;

    ScanLoginFragment scanLoginFragment;

    DeviceFragment deviceFragment;

    ScanDeviceFragment scanDeviceFragment;

    StartEditExcelFragment startEditExcelFragment;

    ExcelFragmentManager excelFragmentManager;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        EventCenter.bindContainerAndHandler(this, mEventHandler);
        screenRecorderManager = new ScreenRecorderManager(this);
        if(userManager.isLogin()){
            startEditExcelFragment=StartEditExcelFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment,startEditExcelFragment).commitNowAllowingStateLoss();
        }else {
            loginFragment = LoginFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment, loginFragment).commitNowAllowingStateLoss();
        }
        new Thread(() -> excelFragmentManager = new ExcelFragmentManager(getSupportFragmentManager())).start();
        FLAG = LOGIN;
        cameraView = new CameraView(this);
        cameraLayout.addView(cameraView);
        cameraView.setVisibility(View.GONE);
        cameraRecorder = new CameraRecorder();
        cameraRecorder.setOnQrRecognizeListener(new OnQrRecognizeListener() {
            @Override
            public boolean OnRecognize(Result result) {
                addRxDestroy(Observable.just("didi")
                        .compose(RxSchedulers.io_main())
                        .subscribe(str -> {
                            LogUtils.d("OnQrRecognizeListener:" + result.getText());
                            String resultStr=result.getText();
                            resultStr="User:508f567cc3015cba395858d4493dd706";
                            String[] user= resultStr.split(":");
                            if(user.length>=1){
                                if("User:508f567cc3015cba395858d4493dd706".equals(resultStr)){
                                    cameraView.setVisibility(View.GONE);
                                    userManager.login(new UserModel(user[1]));
                                    startEditExcelFragment=StartEditExcelFragment.newInstance();
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment,startEditExcelFragment).commitNowAllowingStateLoss();
                                }
                            }
                        }, e -> e.printStackTrace()));
//                excelFragmentManager = new ExcelFragmentManager(getSupportFragmentManager());
                if(SCAN_LOGIN==FLAG||SCAN_DEVICE==FLAG) return false;
                return true;
            }
        });
        cameraRecorder.start();

    }

    public void hileia() {
        if (AppUtils.isInstallApp(this, "com.hiscene.hileia")) {
            screenRecorderManager.startCaptureIntent();
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.d("onResume");
        mComponentContainer.onBecomesVisibleFromPartiallyInvisible();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.d("onStart");
        mComponentContainer.onBecomesVisibleFromTotallyInvisible();
        if (isLaunchHiLeia) {
            LogUtils.d("isLaunchHiLeia");
            isLaunchHiLeia = false;
            screenRecorderManager.cancelRecorder();
            cameraRecorder.init();
//            cameraView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mComponentContainer.onBecomesPartiallyInvisible();
//        cameraView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mComponentContainer.onBecomesTotallyInvisible();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mComponentContainer.onDestroy();
        SPUtils.put(RECOVERY, true);
        SPUtils.put(PositionUtil.POSITION, excelFragmentManager.pos);

        if (isLaunchHiLeia) {
            LogUtils.d("isLaunchHiLeia onDestroy");
            isLaunchHiLeia = false;
            screenRecorderManager.cancelRecorder();
        }else {
            cameraRecorder.destroy();
            cameraRecorder.shutdown();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScreenRecorderManager.REQUEST_MEDIA_PROJECTION) {
            cameraRecorder.destroy();
            LogUtils.d("onActivityResult");
            screenRecorderManager.onActivityResult(requestCode, resultCode, data);
            AppUtils.launchAppForURLScheme(this, "com.hiscene.hileia",
                    "hileia://host:8080/launch?username=15920400762&password=qq137987751");
            cameraLayout.postDelayed(() -> isLaunchHiLeia = true, 800);
        }
    }

    public void scanLogin() {
        scanLoginFragment = ScanLoginFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, scanLoginFragment).commitNowAllowingStateLoss();
        FLAG = SCAN_LOGIN;
        cameraRecorder.startQRRecognize();
        cameraView.setVisibility(View.VISIBLE);
    }

    public void Login(){
        if(loginFragment==null){
            loginFragment=LoginFragment.newInstance();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, loginFragment).commit();
    }


//    @Override
//    public void scanDevice() {
//        scanDeviceFragment = ScanDeviceFragment.newInstance();
//        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, scanDeviceFragment).commitNowAllowingStateLoss();
//        FLAG = SCAN_DEVICE;
//        qrVision.startQRRecognize();
//    }

    public void startEdit() {
       if(excelFragmentManager!=null){
         excelFragmentManager.init();
        cameraView.setVisibility(View.VISIBLE);
        cameraRecorder.init();
       }else {
           ToastUtils.show("正在加載表格，請稍後");
       }
    }

    private SimpleEventHandler mEventHandler = new SimpleEventHandler() {
        @Subscribe
        public void onEvent(String code) {
            LogUtils.d("code:"+code);
            switch (code){
                case BACK_TO_LOGIN:
                    Login();
                    break;
                case START_EDIT_EXCEL:
                    startEdit();
                    break;
                case START_SCAN_LOGIN:
                    scanLogin();
                    break;
                case HILEIA:
                    hileia();
                    break;
            }
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void  onEvent( com.hiscene.flytech.entity.Result result ){
            switch (result.code){
                case C.EXCEL_WRITE_ERROR:
                    LogUtils.d("文件写入数据出错："+result.msg);
                    break;
                case C.EXCEL_WRITE_SUCCESS:
                    showToast("操作已经是最后一步了,数据写入文件成功,已保存到设备中");
                    // TODO: 退出应用,清除缓存数据,recovery,position,表单填写时间需要重置
                    break;
            }
        }

    };


    @Override
    public void addComponent( LifeCycleComponent component ) {
        mComponentContainer.addComponent(component);
    }
}