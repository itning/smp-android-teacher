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
import top.itning.smpandroidteacher.entity.StudentClassCheckMetaData;
import top.itning.smpandroidteacher.ui.view.RoundBackChange;
import top.itning.smpandroidteacher.util.DateUtils;

/**
 * @author itning
 */
public class StudentClassCheckMetaDataRecyclerViewAdapter extends RecyclerView.Adapter<StudentClassCheckMetaDataRecyclerViewAdapter.ViewHolder> implements View.OnClickListener {
    @NonNull
    private List<StudentClassCheckMetaData> studentClassCheckMetaDataList;
    @NonNull
    private Context context;
    @Nullable
    private OnItemClickListener<StudentClassCheckMetaData> onItemClickListener;
    private final List<Integer> colorList = new ArrayList<>(7);
    private int nexIndex;

    public StudentClassCheckMetaDataRecyclerViewAdapter(@NonNull List<StudentClassCheckMetaData> studentClassCheckMetaDataList, @NonNull Context context, @Nullable OnItemClickListener<StudentClassCheckMetaData> onItemClickListener) {
        this.studentClassCheckMetaDataList = studentClassCheckMetaDataList;
        this.context = context;
        this.onItemClickListener = onItemClickListener;
        initColorArray();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_class_meta_data, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentClassCheckMetaData studentClassCheckMetaData = studentClassCheckMetaDataList.get(position);
        holder.itemView.setTag(studentClassCheckMetaData);
        holder.createTime.setText(DateUtils.format(studentClassCheckMetaData.getGmtCreate(), DateUtils.YYYYMMDDHHMMSS_DATE_TIME_FORMATTER_1));
        holder.roundBackChange.setBackColor(getNextColor());
    }

    @Override
    public int getItemCount() {
        return studentClassCheckMetaDataList.size();
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, (StudentClassCheckMetaData) v.getTag());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView createTime;
        private RoundBackChange roundBackChange;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            set(itemView);
        }

        private void set(View itemView) {
            this.createTime = itemView.findViewById(R.id.tv_create_time);
            this.roundBackChange = itemView.findViewById(R.id.round);
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
