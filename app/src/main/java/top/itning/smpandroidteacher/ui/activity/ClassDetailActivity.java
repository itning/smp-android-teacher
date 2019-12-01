package top.itning.smpandroidteacher.ui.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
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
import top.itning.smpandroidteacher.entity.LeaveDTO;
import top.itning.smpandroidteacher.entity.StudentClassCheckMetaData;
import top.itning.smpandroidteacher.entity.StudentClassDTO;
import top.itning.smpandroidteacher.entity.StudentClassUser;
import top.itning.smpandroidteacher.ui.adapter.StudentClassCheckMetaDataRecyclerViewAdapter;
import top.itning.smpandroidteacher.ui.adapter.StudentClassUserRecyclerViewAdapter;
import top.itning.smpandroidteacher.ui.listener.AbstractLoadMoreListener;
import top.itning.smpandroidteacher.util.PageUtils;

import static top.itning.smpandroidteacher.util.DateUtils.ZONE_ID;

/**
 * @author itning
 */
public class ClassDetailActivity extends AppCompatActivity implements StudentClassUserRecyclerViewAdapter.OnItemClickListener<StudentClassUser> {
    private static final String TAG = "ClassDetailActivity";

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
    @Nullable
    private StudentClassDTO studentClassDto;
    private List<LeaveDTO> leaveDtoList;
    private Disposable leaveDisposable;
    private Page<StudentClassCheckMetaData> studentClassCheckMetaDataPage;
    private List<StudentClassCheckMetaData> studentClassCheckMetaDataList;
    private Disposable allStudentCheckMetaDataDisposable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_detail);
        ButterKnife.bind(this);
        studentClassDto = (StudentClassDTO) getIntent().getSerializableExtra("data");
        initView();
    }

    private void initView() {
        initToolBar();
        if (studentClassDto == null) {
            Snackbar.make(coordinatorLayout, "数据异常", Snackbar.LENGTH_LONG).show();
            return;
        }
        initClassInfo();
        initRecyclerView();
    }

    private void initClassInfo() {
        assert studentClassDto != null;
        classNameTextView.setText(MessageFormat.format("班名：{0}", studentClassDto.getName()));
        classNumTextView.setText(MessageFormat.format("班号：{0}", studentClassDto.getClassNum()));
        classLeaveTextView.setText("请假：数据加载中...");
        countTextView.setText(MessageFormat.format("{0}人", studentClassDto.getStudentClassUserList().size()));
        leaveDisposable = HttpHelper.get(ClassClient.class)
                .getStudentClassLeave(studentClassDto.getId(), LocalDate.now(ZONE_ID))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(leaveDtoRestModel -> {
                    leaveDtoList = leaveDtoRestModel.getData();
                    classLeaveTextView.setText(MessageFormat.format("今天请假{0}人", leaveDtoRestModel.getData().size()));
                }, HttpHelper.ErrorInvoke.get(this)
                        .orElseCode(t -> Snackbar.make(coordinatorLayout, t.getT2() == null ? t.getT1().code() + "" : t.getT2().getMsg(), Snackbar.LENGTH_LONG).show())
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        }));
    }

    private void initRecyclerView() {
        assert studentClassDto != null;
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(new StudentClassUserRecyclerViewAdapter(studentClassDto.getStudentClassUserList(), this, this));
    }

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
        if (rv != null) {
            rv.clearOnScrollListeners();
        }
        if (leaveDisposable != null && !leaveDisposable.isDisposed()) {
            leaveDisposable.dispose();
        }
        if (allStudentCheckMetaDataDisposable != null && !allStudentCheckMetaDataDisposable.isDisposed()) {
            allStudentCheckMetaDataDisposable.dispose();
        }
        super.onBackPressed();
    }

    public void onCheckClick(View v) {
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

    @SuppressWarnings("deprecation")
    private void initStudentClassCheckMetaData(boolean clear,
                                               @Nullable Integer page,
                                               @Nullable Integer size,
                                               RecyclerView classCheckMetaRecyclerView) {
        assert studentClassDto != null;
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
        Log.d(TAG, object.toString());
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
}
