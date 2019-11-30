package top.itning.smpandroidteacher.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import top.itning.smpandroidteacher.R;
import top.itning.smpandroidteacher.entity.StudentClassUser;
import top.itning.smpandroidteacher.ui.view.RoundBackChange;
import top.itning.smpandroidteacher.util.DateUtils;

/**
 * @author itning
 */
public class StudentClassUserRecyclerViewAdapter extends RecyclerView.Adapter<StudentClassUserRecyclerViewAdapter.ViewHolder> implements View.OnClickListener {
    @NonNull
    private final List<StudentClassUser> studentClassUserList;
    @NonNull
    private final Context context;
    @Nullable
    private final OnItemClickListener<StudentClassUser> onItemClickListener;
    private final List<Integer> colorList = new ArrayList<>(7);
    private int nexIndex;

    public StudentClassUserRecyclerViewAdapter(@NonNull List<StudentClassUser> studentClassUserList, @NonNull Context context, @Nullable OnItemClickListener<StudentClassUser> onItemClickListener) {
        this.studentClassUserList = studentClassUserList;
        this.context = context;
        this.onItemClickListener = onItemClickListener;
        initColorArray();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_class_user, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentClassUser studentClassUser = studentClassUserList.get(position);
        holder.itemView.setTag(studentClassUser);
        holder.name.setText(studentClassUser.getUser().getName());
        holder.studentNum.setText(studentClassUser.getUser().getStudentUser().getStudentId());
        holder.joinTime.setText(DateUtils.format(studentClassUser.getGmtCreate(), DateUtils.YYYYMMDDHHMM_DATE_TIME_FORMATTER_2));
        holder.roundBackChange.setBackColor(getNextColor());
    }

    @Override
    public int getItemCount() {
        return studentClassUserList.size();
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, (StudentClassUser) v.getTag());
        }
    }

    public interface OnItemClickListener<T> {
        /**
         * 当每一项点击时
         *
         * @param view   View
         * @param object 对象
         */
        void onItemClick(View view, T object);
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView studentNum;
        private TextView joinTime;
        private RoundBackChange roundBackChange;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            set(itemView);
        }

        private void set(View itemView) {
            this.name = itemView.findViewById(R.id.tv_name);
            this.studentNum = itemView.findViewById(R.id.tv_student_num);
            this.joinTime = itemView.findViewById(R.id.tv_join_time);
            this.roundBackChange = itemView.findViewById(R.id.round);
        }
    }

    /**
     * 初始化颜色数组
     */
    private void initColorArray() {
        colorList.add(ContextCompat.getColor(context, R.color.class_color_1));
        colorList.add(ContextCompat.getColor(context, R.color.class_color_2));
        colorList.add(ContextCompat.getColor(context, R.color.class_color_3));
        colorList.add(ContextCompat.getColor(context, R.color.class_color_4));
        colorList.add(ContextCompat.getColor(context, R.color.class_color_5));
        colorList.add(ContextCompat.getColor(context, R.color.class_color_6));
        colorList.add(ContextCompat.getColor(context, R.color.class_color_7));
    }

    /**
     * 获取随机颜色
     *
     * @return 颜色
     */
    @ColorInt
    private int getNextColor() {
        if (nexIndex == colorList.size()) {
            nexIndex = 0;
        }
        return colorList.get(nexIndex++);
    }
}
