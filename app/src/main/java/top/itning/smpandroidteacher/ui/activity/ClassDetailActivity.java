package top.itning.smpandroidteacher.ui.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import top.itning.smpandroidteacher.BuildConfig;
import top.itning.smpandroidteacher.R;
import top.itning.smpandroidteacher.R2;
import top.itning.smpandroidteacher.client.ClassClient;
import top.itning.smpandroidteacher.client.http.HttpHelper;
import top.itning.smpandroidteacher.client.http.Page;
import top.itning.smpandroidteacher.client.http.RestModel;
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
    /**
     * 文件选择请求码
     */
    private static final int FILE_SELECT_REQUEST_CODE = 107;
    /**
     * XLSX MIME
     */
    private static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /**
     * XLS MIME
     */
    private static final String XLS_MIME = "application/vnd.ms-excel";

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
    /**
     * 资源
     */
    private Disposable fileDisposable;
    /**
     * 资源
     */
    private Disposable upFileDisposable;
    /**
     * 资源
     */
    private Disposable modifyStudentClassNameDisposable;


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
        if (fileDisposable != null && !fileDisposable.isDisposed()) {
            fileDisposable.dispose();
        }
        if (upFileDisposable != null && !upFileDisposable.isDisposed()) {
            upFileDisposable.dispose();
        }
        if (modifyStudentClassNameDisposable != null && !modifyStudentClassNameDisposable.isDisposed()) {
            modifyStudentClassNameDisposable.dispose();
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

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_del_class:
                return doDelClass();
            case R.id.item_export_check:
                return doExportClassCheck();
            case R.id.item_import_student:
                return doImportStudentInfo();
            case R.id.item_modify_class_name:
                return doModifyClassName();
            default:
                return false;
        }
    }

    /**
     * 修改班级名称
     *
     * @return <code>true</code>正常处理信息
     */
    private boolean doModifyClassName() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams") View newClassView = getLayoutInflater().inflate(R.layout.alert_modify_class_name, null);
        TextInputLayout textInputLayout = newClassView.findViewById(R.id.ti_layout);
        EditText editText = textInputLayout.getEditText();
        if (editText != null) {
            editText.setSingleLine();
            if (studentClassDto != null) {
                editText.setText(studentClassDto.getName());
                editText.setSelectAllOnFocus(true);
            }
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (editText.getText().length() == 0 || "".contentEquals(editText.getText())) {
                        textInputLayout.setError("请输入新班级名称");
                        return false;
                    }
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null) {
                        inputMethodManager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    }
                    doModifyClassName(editText.getText().toString(), bottomSheetDialog);
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
        return true;
    }

    /**
     * 修改班级名称
     *
     * @param newClassName      新班级名称
     * @param bottomSheetDialog BottomSheetDialog
     */
    private void doModifyClassName(String newClassName, BottomSheetDialog bottomSheetDialog) {
        if (studentClassDto == null) {
            Snackbar.make(coordinatorLayout, "班级信息获取错误", Snackbar.LENGTH_LONG).show();
            return;
        }
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在修改");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();
        modifyStudentClassNameDisposable = HttpHelper.get(ClassClient.class)
                .modifyStudentClassName(newClassName, studentClassDto.getId())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(objectResponse -> {
                    if (objectResponse.errorBody() != null) {
                        RestModel<String> restModel = HttpHelper.getRestModelFromErrorBody(objectResponse.errorBody());
                        if (restModel != null) {
                            Log.w(TAG, "网络请求错误:" + restModel.toString());
                            Snackbar.make(coordinatorLayout, "网络请求错误:" + restModel.getMsg(), Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        studentClassDto.setName(newClassName);
                        classNameTextView.setText(MessageFormat.format("班名：{0}", studentClassDto.getName()));
                    }
                    App.needRefreshData = true;
                    bottomSheetDialog.dismiss();
                    progressDialog.dismiss();
                }, HttpHelper.ErrorInvoke.get(this)
                        .before(t -> progressDialog.dismiss())
                        .orElseException(t -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        }));
    }

    /**
     * 导入学生信息
     *
     * @return <code>true</code>正常处理信息
     */
    private boolean doImportStudentInfo() {
        if (studentClassDto == null) {
            Snackbar.make(coordinatorLayout, "班级信息异常", Snackbar.LENGTH_LONG).show();
            return true;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{XLSX_MIME, XLS_MIME});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择学生数据表格"), FILE_SELECT_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "没有找到文件管理APP", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * 导出班级签到信息
     *
     * @return <code>true</code>正常处理信息
     */
    private boolean doExportClassCheck() {
        if (studentClassDto == null) {
            Snackbar.make(coordinatorLayout, "班级信息异常", Snackbar.LENGTH_LONG).show();
            return true;
        }
        fileDisposable = HttpHelper.get(ClassClient.class)
                .exportCheck(studentClassDto.getId())
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(responseBody -> {
                    File externalFilesDir = getExternalFilesDir(null);
                    File file = new File(externalFilesDir + File.separator + studentClassDto.getName() + System.currentTimeMillis() + ".xlsx");
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        fileOutputStream.write(responseBody.source().readByteArray());
                        runOnUiThread(() -> Snackbar
                                .make(coordinatorLayout, "下载完成", Snackbar.LENGTH_LONG)
                                .setAction("打开", v -> {
                                    Intent intent = new Intent();
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    Uri contentUri = FileProvider.getUriForFile(ClassDetailActivity.this, BuildConfig.APPLICATION_ID + ".fileProvider", file);
                                    intent.setAction(Intent.ACTION_VIEW);
                                    intent.setDataAndType(contentUri, XLSX_MIME);
                                    ClassDetailActivity.this.startActivity(intent);
                                })
                                .show());
                    } catch (Exception e) {
                        Log.e(TAG, "write file exception", e);
                        runOnUiThread(() -> Snackbar.make(coordinatorLayout, "下载失败", Snackbar.LENGTH_LONG).show());
                    }
                }, HttpHelper.ErrorInvoke.get(this)
                        .orElseException(t -> runOnUiThread(() -> {
                            Log.w(TAG, "网络请求错误", t);
                            Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                        })));

        return true;
    }

    /**
     * 教师删除班级
     *
     * @return <code>true</code>正常处理信息
     */
    private boolean doDelClass() {
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
                            .subscribe(objectResponse -> {
                                progressDialog.dismiss();
                                App.needRefreshData = true;
                                if (objectResponse.errorBody() != null) {
                                    RestModel<String> restModel = HttpHelper.getRestModelFromErrorBody(objectResponse.errorBody());
                                    if (restModel != null) {
                                        Log.w(TAG, "错误：" + restModel.toString());
                                        Toast.makeText(this, "错误：" + restModel.getMsg(), Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    this.onBackPressed();
                                }
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

    @SuppressWarnings("deprecation")
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
        } else if (requestCode == FILE_SELECT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) {
                Snackbar.make(coordinatorLayout, "解析失败，URI为空", Snackbar.LENGTH_LONG).show();
                return;
            }
            ContentResolver contentResolver = getContentResolver();
            try (InputStream is = contentResolver.openInputStream(uri)) {
                if (is == null) {
                    Snackbar.make(coordinatorLayout, "解析失败，数据流为空", Snackbar.LENGTH_LONG).show();
                    return;
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] byteData = new byte[16384];
                while ((nRead = is.read(byteData, 0, byteData.length)) != -1) {
                    buffer.write(byteData, 0, nRead);
                }
                byte[] bytes = buffer.toByteArray();
                String mime = contentResolver.getType(uri);
                String fileName = System.currentTimeMillis() + "";
                if (XLSX_MIME.equals(mime)) {
                    fileName += ".xlsx";
                } else if (XLS_MIME.equals(mime)) {
                    fileName += ".xls";
                }
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("正在上传数据");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.show();
                RequestBody body = RequestBody.create(MediaType.parse("application/otcet-stream"), bytes);
                MultipartBody.Part part = MultipartBody.Part.createFormData("file", fileName, body);
                upFileDisposable = HttpHelper.get(ClassClient.class)
                        .importStudentByFile(studentClassDto.getId(), part)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(listRestModel -> {
                            progressDialog.dismiss();
                            List<StudentClassUser> studentClassUserList = listRestModel.getData();
                            if (studentClassUserList == null || studentClassUserList.isEmpty()) {
                                Snackbar.make(coordinatorLayout, "添加0人", Snackbar.LENGTH_LONG).show();
                            } else {
                                Snackbar.make(coordinatorLayout, "添加" + studentClassUserList.size() + "人", Snackbar.LENGTH_LONG).show();
                                List<StudentClassUser> list = studentClassDto.getStudentClassUserList();
                                list.addAll(0, studentClassUserList);
                                if (rv.getAdapter() != null) {
                                    rv.getAdapter().notifyDataSetChanged();
                                }
                                initClassInfo();
                                App.needRefreshData = true;
                            }
                        }, HttpHelper.ErrorInvoke.get(this)
                                .before(t -> progressDialog.dismiss())
                                .orElseCode(t -> {
                                    String msg = t.getT2() != null ? t.getT2().getMsg() : t.getT1().code() + "";
                                    Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_LONG).show();
                                })
                                .orElseException(t -> {
                                    Log.w(TAG, "网络请求错误", t);
                                    Snackbar.make(coordinatorLayout, "网络请求错误", Snackbar.LENGTH_LONG).show();
                                }));
            } catch (Exception e) {
                Log.e(TAG, "up file get exception", e);
                Snackbar.make(coordinatorLayout, "上传失败", Snackbar.LENGTH_LONG).show();
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
