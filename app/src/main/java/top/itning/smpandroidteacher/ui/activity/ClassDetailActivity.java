package top.itning.smpandroidteacher.ui.activity;

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
import com.google.android.material.snackbar.Snackbar;

import java.text.MessageFormat;
import java.time.LocalDate;
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
import top.itning.smpandroidteacher.entity.StudentClassDTO;
import top.itning.smpandroidteacher.entity.StudentClassUser;
import top.itning.smpandroidteacher.ui.adapter.StudentClassUserRecyclerViewAdapter;

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
        if (leaveDisposable != null && !leaveDisposable.isDisposed()) {
            leaveDisposable.dispose();
        }
        super.onBackPressed();
    }

    public void onShadowClick(View v) {
        Log.d(TAG, "aa");
    }

    @Override
    public void onItemClick(View view, StudentClassUser object) {
        Log.d(TAG, object.toString());
    }
}
