package top.itning.smpandroidteacher.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.text.MessageFormat;
import java.time.LocalDateTime;
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
import top.itning.smpandroidteacher.entity.LeaveDTO;
import top.itning.smpandroidteacher.entity.StudentClassCheckDTO;
import top.itning.smpandroidteacher.entity.StudentClassCheckMetaData;
import top.itning.smpandroidteacher.ui.adapter.StudentClassCheckRecyclerViewAdapter;
import top.itning.smpandroidteacher.util.DateUtils;

/**
 * 班级打卡详情
 *
 * @author itning
 */
public class ClassCheckDetailActivity extends AppCompatActivity {
    private static final String TAG = "ClassCheckDetailActivity";

    @BindView(R2.id.tb)
    MaterialToolbar toolbar;
    @BindView(R2.id.cl_content)
    CoordinatorLayout coordinatorLayout;
    @BindView(R2.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R2.id.srl)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R2.id.tv_class_c1)
    AppCompatTextView class1;
    @BindView(R2.id.tv_class_c2)
    AppCompatTextView class2;
    @BindView(R2.id.tv_class_leave)
    AppCompatTextView classLeave;
    @BindView(R2.id.tv_count)
    TextView count;
    @BindView(R2.id.tv_class_start)
    TextView classStart;
    @BindView(R2.id.tv_class_end)
    TextView classEnd;

    /**
     * 从启动者获取的学生班级打卡元数据
     */
    @Nullable
    private StudentClassCheckMetaData studentClassCheckMetaData;
    /**
     * 学生打卡DTO集合
     */
    private List<StudentClassCheckDTO> studentClassCheckDtoList;
    /**
     * 资源
     */
    private Disposable checkDisposable;
    /**
     * 资源
     */
    private Disposable classLeaveDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_check_detail);
        ButterKnife.bind(this);
        studentClassCheckMetaData = (StudentClassCheckMetaData) getIntent().getSerializableExtra("data");
        initView();
    }

    /**
     * 初始化视图
     * 当studentClassCheckMetaData为空时无需初始化
     */
    private void initView() {
        initToolBar();
        if (studentClassCheckMetaData == null) {
            Snackbar.make(coordinatorLayout, "数据异常", Snackbar.LENGTH_LONG).show();
            return;
        }
        initInfo();
        initSwipeRefreshLayout();
        initRecyclerView();
    }

    /**
     * 初始化信息
     */
    private void initInfo() {
        assert studentClassCheckMetaData != null;
        class1.setText("应签：数据加载中...");
        class2.setText("实签：数据加载中...");
        classLeave.setText("请假：数据加载中...");
        count.setText("N/A");
        classStart.setText(MessageFormat.format("开始时间：{0}", DateUtils.format(studentClassCheckMetaData.getStartTime(), DateUtils.YYYYMMDDHHMMSS_DATE_TIME_FORMATTER_1)));
        classEnd.setText(MessageFormat.format("结束时间：{0}", DateUtils.format(studentClassCheckMetaData.getEndTime(), DateUtils.YYYYMMDDHHMMSS_DATE_TIME_FORMATTER_1)));
    }

    /**
     * 初始化下拉刷新
     */
    private void initSwipeRefreshLayout() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary, R.color.colorAccent, R.color.class_color_1,
                R.color.class_color_2, R.color.class_color_3, R.color.class_color_4,
                R.color.class_color_5, R.color.class_color_6, R.color.class_color_7
        );
        swipeRefreshLayout.setOnRefreshListener(this::initRecyclerViewData);
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        studentClassCheckDtoList = new ArrayList<>();
        recyclerView.setAdapter(new StudentClassCheckRecyclerViewAdapter(studentClassCheckDtoList, this));
        initRecyclerViewData();
    }

    /**
     * 初始化RecyclerView数据
     */
    private void initRecyclerViewData() {
        assert studentClassCheckMetaData != null;
        swipeRefreshLayout.setRefreshing(true);
        checkDisposable = HttpHelper.get(ClassClient.class)
                .check(studentClassCheckMetaData.getId())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pageRestModel -> {
                    if (pageRestModel.getData() == null) {
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }

                    setInfo(studentClassCheckMetaData, pageRestModel.getData());

                    studentClassCheckDtoList.clear();
                    studentClassCheckDtoList.addAll(pageRestModel.getData());
                    if (recyclerView.getAdapter() != null) {
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }
                    swipeRefreshLayout.setRefreshing(false);
                }, HttpHelper.ErrorInvoke.get(this)
                        .before(t -> swipeRefreshLayout.setRefreshing(false))
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        }));

    }

    /**
     * 请求网络进行刷新标题数据
     *
     * @param studentClassCheckMetaData 签到元数据
     * @param data                      学生班级签到DTO集合
     */
    private void setInfo(StudentClassCheckMetaData studentClassCheckMetaData, List<StudentClassCheckDTO> data) {
        classLeaveDisposable = HttpHelper.get(ClassClient.class)
                .getStudentClassLeave(studentClassCheckMetaData.getStudentClass().getId(), LocalDateTime.ofInstant(studentClassCheckMetaData.getGmtCreate().toInstant(), DateUtils.ZONE_ID).toLocalDate())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pageRestModel -> {
                    List<LeaveDTO> leaveDtoList = pageRestModel.getData();
                    class1.setText(MessageFormat.format("应签：{0}人", data.size() - leaveDtoList.size()));
                    classLeave.setText(MessageFormat.format("请假：{0}人", leaveDtoList.size()));
                }, HttpHelper.ErrorInvoke.get(this)
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        }));
        long count = data.stream().filter(dd -> {
            if (dd.getCheck() == null) {
                return false;
            }
            return dd.getCheck();
        }).count();

        class2.setText(MessageFormat.format("实签：{0}人", count));

        this.count.setText(MessageFormat.format("{0}人", data.size()));
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
        if (checkDisposable != null && !checkDisposable.isDisposed()) {
            checkDisposable.dispose();
        }
        if (classLeaveDisposable != null && !classLeaveDisposable.isDisposed()) {
            classLeaveDisposable.dispose();
        }
        super.onBackPressed();
    }
}
