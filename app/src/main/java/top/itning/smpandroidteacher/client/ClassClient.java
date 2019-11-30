package top.itning.smpandroidteacher.client;

import java.time.LocalDate;
import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Query;
import top.itning.smpandroidteacher.client.http.Page;
import top.itning.smpandroidteacher.client.http.RestModel;
import top.itning.smpandroidteacher.entity.LeaveDTO;
import top.itning.smpandroidteacher.entity.StudentClassCheckMetaData;
import top.itning.smpandroidteacher.entity.StudentClassDTO;

/**
 * @author itning
 */
public interface ClassClient {
    /**
     * 获取教师所创建的班级
     *
     * @param page 页数
     * @param size 数量
     * @return 教师所创建的班级
     */
    @GET("/class/student_class")
    Observable<RestModel<Page<StudentClassDTO>>> getAllClass(@Query("page") Integer page, @Query("size") Integer size);

    /**
     * 获取所有签到元数据
     *
     * @param studentClassId 班级ID
     * @param page           页数
     * @param size           数量
     * @return 所有签到元数据
     */
    @GET("/class/student_class_check_meta_data/{studentClassId}")
    Observable<RestModel<Page<StudentClassCheckMetaData>>> getAllStudentClassCheckMetaData(@Field("studentClassId") String studentClassId, @Query("page") Integer page, @Query("size") Integer size);

    /**
     * 获取班级请假信息
     *
     * @param studentClassId 班级ID
     * @param whereDay       哪天
     * @return 班级请假信息
     */
    @GET("/class/student_class_leave")
    Observable<RestModel<List<LeaveDTO>>> getStudentClassLeave(@Query("studentClassId") String studentClassId, @Query("whereDay") LocalDate whereDay);
}
