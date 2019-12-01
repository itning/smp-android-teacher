package top.itning.smpandroidteacher.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import top.itning.smpandroidteacher.R;
import top.itning.smpandroidteacher.R2;
import top.itning.smpandroidteacher.client.ClassClient;
import top.itning.smpandroidteacher.client.http.HttpHelper;
import top.itning.smpandroidteacher.client.http.Page;
import top.itning.smpandroidteacher.entity.StudentClassDTO;
import top.itning.smpandroidteacher.ui.adapter.StudentClassRecyclerViewAdapter;
import top.itning.smpandroidteacher.ui.listener.AbstractLoadMoreListener;
import top.itning.smpandroidteacher.util.DateUtils;
import top.itning.smpandroidteacher.util.PageUtils;
import top.itning.smpandroidteacher.util.Tuple2;

import static top.itning.smpandroidteacher.util.DateUtils.MMDDHHMME_DATE_TIME_FORMATTER_4;
import static top.itning.smpandroidteacher.util.DateUtils.ZONE_ID;

/**
 * @author itning
 */
public class MainActivity extends AppCompatActivity implements StudentClassRecyclerViewAdapter.OnItemClickListener<StudentClassDTO> {
    private static final String TAG = "MainActivity";
    private static final int SETTING_REQUEST_CODE = 104;
    private static final int MUST_PERMISSIONS_REQUEST_CODE = 100;
    @BindView(R2.id.tv_hello)
    TextView helloTextView;
    @BindView(R2.id.tv_time)
    TextView tv;
    @BindView(R2.id.srl)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R2.id.cl_content)
    CoordinatorLayout coordinatorLayout;
    @BindView(R2.id.recycler_view)
    RecyclerView rv;
    @Nullable
    private Disposable titleDisposable;
    @Nullable
    private Disposable recyclerViewDataDisposable;
    private List<StudentClassDTO> studentClassDtoList;
    private Page<StudentClassDTO> studentClassDtoPage;
    private Disposable createClassDisposable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        checkPermissions();
        initView();
    }

    private void initView() {
        initTitleView();
        initSwipeRefreshLayout();
        initRecyclerView();
    }

    private void initTitleView() {
        final SharedPreferences preferences = getSharedPreferences(App.SHARED_PREFERENCES_OWN, Context.MODE_PRIVATE);
        titleDisposable = Observable
                .fromCallable(() -> new Tuple2<>(LocalDateTime.now(ZONE_ID).format(MMDDHHMME_DATE_TIME_FORMATTER_4),
                        DateUtils.helloTime(preferences.getString(HttpHelper.LOGIN_USER_NAME_KEY, null))))
                .repeatWhen(objectObservable -> objectObservable.delay(5, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    tv.setText(s.getT1());
                    helloTextView.setText(s.getT2());
                }, throwable -> Log.e(TAG, "title view error", throwable));
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary, R.color.colorAccent, R.color.class_color_1,
                R.color.class_color_2, R.color.class_color_3, R.color.class_color_4,
                R.color.class_color_5, R.color.class_color_6, R.color.class_color_7
        );
        swipeRefreshLayout.setOnRefreshListener(() -> initRecyclerViewData(true, PageUtils.DEFAULT_PAGE, PageUtils.DEFAULT_SIZE));
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        studentClassDtoList = new ArrayList<>();
        rv.setAdapter(new StudentClassRecyclerViewAdapter(studentClassDtoList, this, this));
        rv.clearOnScrollListeners();
        rv.addOnScrollListener(new AbstractLoadMoreListener() {
            @Override
            protected void onLoading(int countItem, int lastItem) {
                PageUtils.getNextPageAndSize(studentClassDtoPage, t -> initRecyclerViewData(false, t.getT1(), t.getT2()));
            }
        });
        initRecyclerViewData(true, PageUtils.DEFAULT_PAGE, PageUtils.DEFAULT_SIZE);
    }

    private void initRecyclerViewData(boolean clear, @Nullable Integer page, @Nullable Integer size) {
        swipeRefreshLayout.setRefreshing(true);
        recyclerViewDataDisposable = HttpHelper.get(ClassClient.class)
                .getAllClass(page, size)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pageRestModel -> {
                    if (pageRestModel.getData().getContent() == null) {
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    if (clear) {
                        studentClassDtoList.clear();
                    }
                    studentClassDtoPage = pageRestModel.getData();
                    studentClassDtoList.addAll(pageRestModel.getData().getContent());
                    if (rv.getAdapter() != null) {
                        rv.getAdapter().notifyDataSetChanged();
                    }
                    swipeRefreshLayout.setRefreshing(false);
                }, HttpHelper.ErrorInvoke.get(this)
                        .before(t -> swipeRefreshLayout.setRefreshing(false))
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        }));

    }

    public void onShadowClick(View view) {
        if (view.getId() == R.id.btn_personal) {
            startActivity(new Intent(this, PersonalActivity.class));
        }
    }

    public void onFabClick(View view) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams") View newClassView = getLayoutInflater().inflate(R.layout.alert_new_class, null);
        TextInputLayout textInputLayout = newClassView.findViewById(R.id.ti_layout);
        EditText editText = textInputLayout.getEditText();
        if (editText != null) {
            editText.setSingleLine();
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (editText.getText().length() == 0 || "".contentEquals(editText.getText())) {
                        textInputLayout.setError("请输入要创建的班级名");
                        return false;
                    }
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null) {
                        inputMethodManager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    }
                    doCreateClass(editText.getText().toString(), bottomSheetDialog);
                    return true;
                }
                return false;
            });
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    textInputLayout.setError(null);
                }
            });
        }
        bottomSheetDialog.setContentView(newClassView);
        bottomSheetDialog.show();
    }

    @SuppressWarnings("deprecation")
    private void doCreateClass(String className, BottomSheetDialog bottomSheetDialog) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在创建");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();
        createClassDisposable = HttpHelper.get(ClassClient.class)
                .newClass(className)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pageRestModel -> {
                    bottomSheetDialog.dismiss();
                    progressDialog.dismiss();
                    initRecyclerViewData(true, PageUtils.DEFAULT_PAGE, PageUtils.DEFAULT_SIZE);
                }, HttpHelper.ErrorInvoke.get(this)
                        .before(t -> progressDialog.dismiss())
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        }));
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        String[] ps = Stream.of
                (
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .filter(permission -> ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                .toArray(String[]::new);
        if (ps.length != 0) {
            ActivityCompat.requestPermissions(this, ps, MUST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MUST_PERMISSIONS_REQUEST_CODE) {
            boolean granted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                }
            }
            if (granted) {
                checkPermissions();
            } else {
                new AlertDialog
                        .Builder(this)
                        .setTitle("需要权限")
                        .setMessage("请授予权限")
                        .setCancelable(false)
                        .setPositiveButton("确定", (dialog, which) -> startActivityForResult(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null)), SETTING_REQUEST_CODE))
                        .show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTING_REQUEST_CODE) {
            checkPermissions();
        }
    }

    @Override
    public void onBackPressed() {
        if (titleDisposable != null && !titleDisposable.isDisposed()) {
            titleDisposable.dispose();
        }
        if (recyclerViewDataDisposable != null && !recyclerViewDataDisposable.isDisposed()) {
            recyclerViewDataDisposable.dispose();
        }
        if (createClassDisposable != null && !createClassDisposable.isDisposed()) {
            createClassDisposable.dispose();
        }
        super.onBackPressed();
    }

    @Override
    public void onItemClick(View view, StudentClassDTO object) {
        Intent intent = new Intent(this, ClassDetailActivity.class);
        intent.putExtra("data", object);
        startActivity(intent);
    }
}
