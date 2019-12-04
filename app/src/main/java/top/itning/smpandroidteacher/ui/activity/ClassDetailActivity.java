package top.itning.smpandroidteacher.ui.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import top.itning.smpandroidteacher.R;
import top.itning.smpandroidteacher.R2;
import top.itning.smpandroidteacher.client.ClassClient;
import top.itning.smpandroidteacher.client.http.HttpHelper;
import top.itning.smpandroidteacher.client.http.Page;
import top.itning.smpandroidteacher.entity.StudentClassCheckMetaData;
import top.itning.smpandroidteacher.entity.StudentClassDTO;
import top.itning.smpandroidteacher.entity.StudentClassUser;
import top.itning.smpandroidteacher.ui.adapter.StudentClassCheckMetaDataRecyclerViewAdapter;
import top.itning.smpandroidteacher.ui.adapter.StudentClassUserRecyclerViewAdapter;
import top.itning.smpandroidteacher.ui.listener.AbstractLoadMoreListener;
import top.itning.smpandroidteacher.util.PageUtils;

import static top.itning.smpandroidteacher.util.DateUtils.ZONE_ID;

/**
 * 班级详情
 *
 * @author itning
 */
public class ClassDetailActivity extends AppCompatActivity implements StudentClassUserRecyclerViewAdapter.OnItemClickListener<StudentClassUser>, MenuItem.OnMenuItemClickListener {
    private static final String TAG = "ClassDetailActivity";
    /**
     * 班级打卡用户请求码
     */
    public static final int CLASS_CHECK_USER_REQUEST_CODE = 106;

    @BindView(R2.id.tb)
    MaterialToolbar toolbar;
    @BindView(R2.id.tv_class_name)
    AppCompatTextView classNameTextView;
    @BindView(R2.id.tv_class_num)
    AppCompatTextView classNumTextView;
    @BindView(R2.id.tv_class_leave)
    AppCompatTextView classLeaveTextView;
    @BindView(R2.id.tv_count)
    TextView countTextView;
    @BindView(R2.id.cl_content)
    CoordinatorLayout coordinatorLayout;
    @BindView(R2.id.recycler_view)
    RecyclerView rv;
    /**
     * 学生班级DTO 从启动Activity获取的
     */
    private StudentClassDTO studentClassDto;
    /**
     * 资源
     */
    private Disposable leaveDisposable;
    /**
     * 当前页码信息
     */
    private Page<StudentClassCheckMetaData> studentClassCheckMetaDataPage;
    /**
     * 学生打卡元数据集合
     */
    private List<StudentClassCheckMetaData> studentClassCheckMetaDataList;
    /**
     * 资源
     */
    private Disposable allStudentCheckMetaDataDisposable;
    /**
     * 资源
     */
    private Disposable delClassDisposable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_detail);
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
            Snackbar.make(coordinatorLayout, "数据异常", Snackbar.LENGTH_LONG).show();
            return;
        }
        initClassInfo();
        initRecyclerView();
    }

    /**
     * 初始化班级信息
     */
    private void initClassInfo() {
        classNameTextView.setText(MessageFormat.format("班名：{0}", studentClassDto.getName()));
        classNumTextView.setText(MessageFormat.format("班号：{0}", studentClassDto.getClassNum()));
        classLeaveTextView.setText("请假：数据加载中...");
        countTextView.setText(MessageFormat.format("{0}人", studentClassDto.getStudentClassUserList().size()));
        leaveDisposable = HttpHelper.get(ClassClient.class)
                .getStudentClassLeave(studentClassDto.getId(), LocalDate.now(ZONE_ID))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(leaveDtoRestModel -> classLeaveTextView.setText(MessageFormat.format("今天请假{0}人", leaveDtoRestModel.getData().size())), HttpHelper.ErrorInvoke.get(this)
                        .orElseCode(t -> Snackbar.make(coordinatorLayout, t.getT2() == null ? t.getT1().code() + "" : t.getT2().getMsg(), Snackbar.LENGTH_LONG).show())
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        }));
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(new StudentClassUserRecyclerViewAdapter(studentClassDto.getStudentClassUserList(), this, this));
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
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
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
        if (rv != null) {
            rv.clearOnScrollListeners();
        }
        if (leaveDisposable != null && !leaveDisposable.isDisposed()) {
            leaveDisposable.dispose();
        }
        if (allStudentCheckMetaDataDisposable != null && !allStudentCheckMetaDataDisposable.isDisposed()) {
            allStudentCheckMetaDataDisposable.dispose();
        }
        if (delClassDisposable != null && !delClassDisposable.isDisposed()) {
            delClassDisposable.dispose();
        }
        super.onBackPressed();
    }

    /**
     * 签到历史按钮点击事件处理
     *
     * @param v View
     */
    public void onCheckHistoryClick(View v) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams") View classCheckMetaDataView = getLayoutInflater().inflate(R.layout.alert_leave_reason, null);
        RecyclerView classCheckMetaRecyclerView = classCheckMetaDataView.findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        classCheckMetaRecyclerView.setLayoutManager(layoutManager);
        studentClassCheckMetaDataList = new ArrayList<>();
        classCheckMetaRecyclerView.setAdapter(new StudentClassCheckMetaDataRecyclerViewAdapter(studentClassCheckMetaDataList, this, this::onMetaDataClick));
        classCheckMetaRecyclerView.clearOnScrollListeners();
        classCheckMetaRecyclerView.addOnScrollListener(new AbstractLoadMoreListener() {
            @Override
            protected void onLoading(int countItem, int lastItem) {
                PageUtils.getNextPageAndSize(studentClassCheckMetaDataPage, t -> initStudentClassCheckMetaData(false, t.getT1(), t.getT2(), classCheckMetaRecyclerView));
            }
        });
        initStudentClassCheckMetaData(true, PageUtils.DEFAULT_PAGE, PageUtils.DEFAULT_SIZE, classCheckMetaRecyclerView);

        bottomSheetDialog.setContentView(classCheckMetaDataView);
        bottomSheetDialog.show();
    }

    /**
     * 发起签到按钮点击事件处理
     *
     * @param v View
     */
    public void onNewClassCheckClick(View v) {
        if (studentClassDto == null) {
            return;
        }
        Intent intent = new Intent(this, NewClassCheckActivity.class);
        intent.putExtra("data", studentClassDto);
        startActivity(intent);
    }

    /**
     * 初始化学生课堂签到元数据
     *
     * @param clear                      是否清空集合
     * @param page                       页数
     * @param size                       每页数量
     * @param classCheckMetaRecyclerView RecyclerView
     */
    @SuppressWarnings("deprecation")
    private void initStudentClassCheckMetaData(boolean clear,
                                               @Nullable Integer page,
                                               @Nullable Integer size,
                                               RecyclerView classCheckMetaRecyclerView) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("加载中");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();
        allStudentCheckMetaDataDisposable = HttpHelper.get(ClassClient.class)
                .getAllStudentClassCheckMetaData(studentClassDto.getId(), page, size)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pageRestModel -> {
                    if (pageRestModel.getData().getContent() == null) {
                        progressDialog.dismiss();
                        return;
                    }
                    if (clear) {
                        studentClassCheckMetaDataList.clear();
                    }
                    studentClassCheckMetaDataPage = pageRestModel.getData();
                    studentClassCheckMetaDataList.addAll(pageRestModel.getData().getContent());
                    if (classCheckMetaRecyclerView.getAdapter() != null) {
                        classCheckMetaRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                    progressDialog.dismiss();
                }, HttpHelper.ErrorInvoke.get(this)
                        .before(t -> progressDialog.dismiss())
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        }));

    }

    @Override
    public void onItemClick(View view, StudentClassUser object) {
        Intent intent = new Intent(this, ClassCheckUserActivity.class);
        intent.putExtra("data", object);
        startActivityForResult(intent, CLASS_CHECK_USER_REQUEST_CODE);
    }

    /**
     * 当点击卡片中的某一项时（签到元数据）
     *
     * @param view                      View
     * @param studentClassCheckMetaData StudentClassCheckMetaData
     */
    private void onMetaDataClick(View view, StudentClassCheckMetaData studentClassCheckMetaData) {
        Intent intent = new Intent(this, ClassCheckDetailActivity.class);
        intent.putExtra("data", studentClassCheckMetaData);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_class, menu);
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.item_del_class) {
            if (studentClassDto == null) {
                return false;
            }
            new AlertDialog.Builder(this)
                    .setTitle("确定解散？")
                    .setCancelable(false)
                    .setNegativeButton("确定", (dialog, which) -> {
                        ProgressDialog progressDialog = new ProgressDialog(this);
                        progressDialog.setMessage("请稍后");
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                        delClassDisposable = HttpHelper.get(ClassClient.class)
                                .delClass(studentClassDto.getId())
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(pageRestModel -> {
                                    progressDialog.dismiss();
                                    App.needRefreshData = true;
                                    this.onBackPressed();
                                }, HttpHelper.ErrorInvoke.get(this)
                                        .before(t -> progressDialog.dismiss())
                                        .orElseException(t -> {
                                            Log.w(TAG, "网络请求错误", t);
                                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                                        }));
                    })
                    .setPositiveButton("取消", null)
                    .show();
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CLASS_CHECK_USER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            StudentClassUser studentClassUser = (StudentClassUser) data.getSerializableExtra("delStudentClassUser");
            if (studentClassUser != null) {
                List<StudentClassUser> studentClassUserList = studentClassDto.getStudentClassUserList();
                studentClassUserList.remove(studentClassUser);
                studentClassDto.setStudentClassUserList(studentClassUserList);
                if (rv.getAdapter() != null) {
                    rv.getAdapter().notifyDataSetChanged();
                }
                initClassInfo();
                App.needRefreshData = true;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 当标题信息面板被点击时的事件处理
     *
     * @param view View
     */
    public void onShadowViewClick(View view) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null && studentClassDto != null) {
            ClipData clipData = ClipData.newPlainText("class_num", studentClassDto.getClassNum());
            clipboardManager.setPrimaryClip(clipData);
            Snackbar.make(coordinatorLayout, "已复制班号", Snackbar.LENGTH_SHORT).show();
        }
    }
}
