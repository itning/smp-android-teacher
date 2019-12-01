package top.itning.smpandroidteacher.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.DecimalFormat;
import java.time.LocalDateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import top.itning.smpandroidteacher.R;
import top.itning.smpandroidteacher.R2;
import top.itning.smpandroidteacher.client.ClassClient;
import top.itning.smpandroidteacher.client.http.HttpHelper;
import top.itning.smpandroidteacher.entity.StudentClassDTO;

import static top.itning.smpandroidteacher.util.DateUtils.ZONE_ID;

/**
 * 新签到
 *
 * @author itning
 */
public class NewClassCheckActivity extends AppCompatActivity {
    private static final String TAG = "NewClassCheckActivity";
    /**
     * 数字格式化
     */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");
    /**
     * 高德地图客户端实例
     */
    private AMapLocationClient locationClient = null;
    /**
     * 经度
     */
    private double longitude = 0;
    /**
     * 纬度
     */
    private double latitude = 0;
    /**
     * 学生班级DTO
     */
    private StudentClassDTO studentClassDto;
    /**
     * 资源
     */
    private Disposable disposable;

    @BindView(R2.id.tb)
    MaterialToolbar toolbar;
    @BindView(R2.id.tv_address)
    TextView addressTextView;
    @BindView(R2.id.tv_class_name)
    TextView classNameTextView;
    @BindView(R2.id.btn_new_check)
    AppCompatButton newCheckBtn;
    @BindView(R2.id.spinner_time)
    AppCompatSpinner timeSpinner;
    @BindView(R2.id.spinner_m)
    AppCompatSpinner mSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_class_check);
        ButterKnife.bind(this);
        studentClassDto = (StudentClassDTO) getIntent().getSerializableExtra("data");
        initView();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        initToolBar();
        if (studentClassDto == null) {
            Toast.makeText(this, "数据异常", Toast.LENGTH_LONG).show();
            return;
        }
        initLocation();
        initInfo();
    }

    /**
     * 初始化信息
     */
    private void initInfo() {
        final float[] m = {5f};
        final int[] time = {3};
        newCheckBtn.setVisibility(View.INVISIBLE);
        classNameTextView.setText(studentClassDto.getName());
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        time[0] = 5;
                        break;
                    case 2:
                        time[0] = 10;
                        break;
                    case 3:
                        time[0] = 15;
                        break;
                    default:
                        time[0] = 3;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        m[0] = 50f;
                        break;
                    case 2:
                        m[0] = 100f;
                        break;
                    case 3:
                        m[0] = 200f;
                        break;
                    case 4:
                        m[0] = 500f;
                        break;
                    case 5:
                        m[0] = 1000f;
                        break;
                    default:
                        m[0] = 15f;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        newCheckBtn.setOnClickListener(v -> {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在发起");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
            LocalDateTime start = LocalDateTime.now(ZONE_ID);
            LocalDateTime end = LocalDateTime.now(ZONE_ID).plusMinutes(time[0]);
            disposable = HttpHelper.get(ClassClient.class)
                    .newCheck(longitude, latitude, studentClassDto.getId(), m[0], start, end)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(restModel -> {
                        progressDialog.dismiss();
                        Intent intent = new Intent(this, ClassCheckDetailActivity.class);
                        intent.putExtra("data", restModel.getData());
                        startActivity(intent);
                        onBackPressed();
                    }, HttpHelper.ErrorInvoke.get(this)
                            .before(t -> progressDialog.dismiss())
                            .orElseCode(t -> Toast.makeText(this, t.getT2().getMsg(), Toast.LENGTH_LONG).show())
                            .orElseException(t -> {
                                Log.w(TAG, "网络请求错误", t);
                                Toast.makeText(this, "网络请求错误", Toast.LENGTH_LONG).show();
                            }));
        });
    }

    /**
     * 初始化地理信息
     */
    private void initLocation() {
        AMapLocationClient.setApiKey("9da9e9d79cc99c0b7e11e1e69f93e495");
        locationClient = new AMapLocationClient(getApplicationContext());
        // 设置定位回调监听
        locationClient.setLocationListener(aMapLocation -> {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    //可在其中解析amapLocation获取相应内容。
                    longitude = aMapLocation.getLongitude();
                    latitude = aMapLocation.getLatitude();
                    if (longitude != 0 && latitude != 0) {
                        newCheckBtn.setVisibility(View.VISIBLE);
                    }
                    if (addressTextView != null) {
                        if (!"".equals(aMapLocation.getDescription())) {
                            StringBuilder sb = new StringBuilder()
                                    .append("精度（米）：")
                                    .append(aMapLocation.getAccuracy())
                                    .append(" ")
                                    .append(aMapLocation.getDescription());
                            addressTextView.setText(sb);
                        } else {
                            StringBuilder sb = new StringBuilder()
                                    .append("精度（米）：")
                                    .append(aMapLocation.getAccuracy())
                                    .append(" 经度：")
                                    .append(DECIMAL_FORMAT.format(aMapLocation.getLongitude()))
                                    .append(" 纬度：")
                                    .append(DECIMAL_FORMAT.format(aMapLocation.getLatitude()));
                            addressTextView.setText(sb);
                        }
                    }
                } else {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    Log.e(TAG, "location Error, ErrCode:"
                            + aMapLocation.getErrorCode() + ", errInfo:"
                            + aMapLocation.getErrorInfo());
                }
            }
        });
        //初始化AMapLocationClientOption对象
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        AMapLocationClientOption option = new AMapLocationClientOption();

        // 设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景）
        option.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
        if (null != locationClient) {
            locationClient.setLocationOption(option);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            locationClient.stopLocation();
            locationClient.startLocation();
        }
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        locationOption.setNeedAddress(true);
        //设置是否允许模拟位置,默认为true，允许模拟位置
        locationOption.setMockEnable(true);
        //给定位客户端对象设置定位参数
        locationClient.setLocationOption(locationOption);
        //启动定位
        locationClient.startLocation();
    }

    /**
     * 初始化工具栏
     */
    private void initToolBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        locationClient.stopLocation();
        locationClient.onDestroy();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        super.onBackPressed();
    }
}
