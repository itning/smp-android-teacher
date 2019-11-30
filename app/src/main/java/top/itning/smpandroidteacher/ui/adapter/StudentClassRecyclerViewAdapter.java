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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import top.itning.smpandroidteacher.R;
import top.itning.smpandroidteacher.entity.StudentClassDTO;
import top.itning.smpandroidteacher.ui.view.RoundBackChange;
import top.itning.smpandroidteacher.util.DateUtils;

/**
 * @author itning
 */
public class StudentClassRecyclerViewAdapter extends RecyclerView.Adapter<StudentClassRecyclerViewAdapter.ViewHolder> implements View.OnClickListener {
    @NonNull
    private final List<StudentClassDTO> studentClassDtoList;
    @NonNull
    private final Context context;
    @Nullable
    private final OnItemClickListener<StudentClassDTO> onItemClickListener;
    private final List<Integer> colorList = new ArrayList<>(7);
    private int nexIndex;

    public StudentClassRecyclerViewAdapter(@NonNull List<StudentClassDTO> studentClassDtoList, @NonNull Context context, @Nullable OnItemClickListener<StudentClassDTO> onItemClickListener) {
        this.studentClassDtoList = studentClassDtoList;
        this.context = context;
        this.onItemClickListener = onItemClickListener;
        initColorArray();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_join_class, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentClassDTO studentClassDTO = studentClassDtoList.get(position);
        holder.itemView.setTag(studentClassDTO);
        holder.createTime.setText(DateUtils.format(studentClassDTO.getGmtCreate(), DateUtils.YYYYMMDDHHMM_DATE_TIME_FORMATTER_2));
        holder.className.setText(studentClassDTO.getName());
        holder.peopleCount.setText(MessageFormat.format("{0}人", studentClassDTO.getStudentClassUserList().size()));
        holder.roundBackChange.setBackColor(getNextColor());
    }

    @Override
    public int getItemCount() {
        return studentClassDtoList.size();
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, (StudentClassDTO) v.getTag());
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
        private TextView className;
        private TextView createTime;
        private TextView peopleCount;
        private RoundBackChange roundBackChange;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            set(itemView);
        }

        private void set(View itemView) {
            this.className = itemView.findViewById(R.id.tv_class);
            this.createTime = itemView.findViewById(R.id.tv_create_time);
            this.peopleCount = itemView.findViewById(R.id.tv_people);
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
