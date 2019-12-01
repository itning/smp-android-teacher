package top.itning.smpandroidteacher.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;

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
import top.itning.smpandroidteacher.entity.StudentClassCheckDTO;
import top.itning.smpandroidteacher.entity.StudentClassUser;
import top.itning.smpandroidteacher.ui.adapter.StudentCheckDetailRecyclerViewDataAdapter;

/**
 * 班级打卡用户
 *
 * @author itning
 */
public class ClassCheckUserActivity extends AppCompatActivity implements MenuItem.OnMenuItemClickListener {
    private static final String TAG = "ClassCheckUserActivity";

    @BindView(R2.id.tb)
    MaterialToolbar toolbar;
    @BindView(R2.id.recycler_view)
    RecyclerView rv;
    @BindView(R2.id.srl)
    SwipeRefreshLayout swipeRefreshLayout;

    /**
     * 学生班级用户 从启动Activity获取的
     */
    private StudentClassUser studentClassUser;
    /**
     * 资源
     */
    private Disposable userCheckDetailDisposable;
    /**
     * 学生打卡DTO集合
     */
    private List<StudentClassCheckDTO> studentClassCheckDtoList;
    private Disposable delStudentDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_check_user);
        ButterKnife.bind(this);
        studentClassUser = (StudentClassUser) getIntent().getSerializableExtra("data");
        initView();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        initToolBar();
        if (studentClassUser == null) {
            Toast.makeText(this, "数据异常", Toast.LENGTH_LONG).show();
            return;
        }
        initSwipeRefreshLayout();
        initRecyclerView();
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
        rv.setLayoutManager(layoutManager);
        studentClassCheckDtoList = new ArrayList<>();
        rv.setAdapter(new StudentCheckDetailRecyclerViewDataAdapter(this, studentClassCheckDtoList));
        initRecyclerViewData();
    }

    /**
     * 初始化RecyclerView数据
     */
    private void initRecyclerViewData() {
        swipeRefreshLayout.setRefreshing(true);
        userCheckDetailDisposable = HttpHelper.get(ClassClient.class)
                .getUserCheckDetail(studentClassUser.getUser().getUsername(), studentClassUser.getStudentClass().getId())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(listRestModel -> {
                    swipeRefreshLayout.setRefreshing(false);
                    studentClassCheckDtoList.clear();
                    studentClassCheckDtoList.addAll(listRestModel.getData());
                    if (rv.getAdapter() != null) {
                        rv.getAdapter().notifyDataSetChanged();
                    }
                }, HttpHelper.ErrorInvoke.get(this)
                        .before(t -> swipeRefreshLayout.setRefreshing(false))
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Toast.makeText(this, "网络请求错误", Toast.LENGTH_LONG).show();
                        }));
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
        if (userCheckDetailDisposable != null && !userCheckDetailDisposable.isDisposed()) {
            userCheckDetailDisposable.dispose();
        }
        if (delStudentDisposable != null && !delStudentDisposable.isDisposed()) {
            delStudentDisposable.dispose();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_class_user_detail, menu);
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.item_del_student) {
            new AlertDialog.Builder(this)
                    .setTitle("确定请离？")
                    .setCancelable(false)
                    .setNegativeButton("确定", (dialog, which) -> {
                        ProgressDialog progressDialog = new ProgressDialog(this);
                        progressDialog.setMessage("请稍后");
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.setCancelable(false);
                        progressDialog.show();
                        delStudentDisposable = HttpHelper.get(ClassClient.class)
                                .delStudent(studentClassUser.getUser().getUsername(), studentClassUser.getStudentClass().getId())
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(objectResponse -> {
                                    progressDialog.dismiss();
                                    Intent intent = new Intent();
                                    intent.putExtra("delStudentClassUser", studentClassUser);
                                    this.setResult(RESULT_OK, intent);
                                    this.onBackPressed();
                                }, HttpHelper.ErrorInvoke.get(this)
                                        .before(t -> progressDialog.dismiss())
                                        .orElseException(t -> {
                                            Log.w(TAG, "网络请求错误", t);
                                            Toast.makeText(this, "网络请求错误", Toast.LENGTH_LONG).show();
                                        }));
                    })
                    .setPositiveButton("取消", null)
                    .show();
            return true;
        }
        return false;
    }
}
