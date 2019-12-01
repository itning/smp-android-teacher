package top.itning.smpandroidteacher.client;

import java.time.LocalDate;
import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import top.itning.smpandroidteacher.client.http.Page;
import top.itning.smpandroidteacher.client.http.RestModel;
import top.itning.smpandroidteacher.entity.LeaveDTO;
import top.itning.smpandroidteacher.entity.StudentClass;
import top.itning.smpandroidteacher.entity.StudentClassCheckDTO;
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
    Observable<RestModel<Page<StudentClassCheckMetaData>>> getAllStudentClassCheckMetaData(@Path("studentClassId") String studentClassId, @Query("page") Integer page, @Query("size") Integer size);

    /**
     * 获取班级请假信息
     *
     * @param studentClassId 班级ID
     * @param whereDay       哪天
     * @return 班级请假信息
     */
    @GET("/class/student_class_leave")
    Observable<RestModel<List<LeaveDTO>>> getStudentClassLeave(@Query("studentClassId") String studentClassId, @Query("whereDay") LocalDate whereDay);

    /**
     * 根据签到元数据获取该班级的签到信息
     *
     * @param studentClassCheckMetaDataId 元数据ID
     * @return 该班级的签到信息
     */
    @GET("/class/check/{studentClassCheckMetaDataId}")
    Observable<RestModel<List<StudentClassCheckDTO>>> check(@Path("studentClassCheckMetaDataId") String studentClassCheckMetaDataId);

    /**
     * 创建班级
     *
     * @param className 班级名
     * @return 创建的班级
     */
    @FormUrlEncoded
    @POST("/class/new_class")
    Observable<RestModel<StudentClass>> newClass(@Field("className") String className);
}
