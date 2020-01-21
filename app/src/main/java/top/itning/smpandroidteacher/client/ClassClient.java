package top.itning.smpandroidteacher.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import top.itning.smpandroidteacher.client.http.Page;
import top.itning.smpandroidteacher.client.http.RestModel;
import top.itning.smpandroidteacher.entity.LeaveDTO;
import top.itning.smpandroidteacher.entity.StudentClass;
import top.itning.smpandroidteacher.entity.StudentClassCheckDTO;
import top.itning.smpandroidteacher.entity.StudentClassCheckMetaData;
import top.itning.smpandroidteacher.entity.StudentClassDTO;
import top.itning.smpandroidteacher.entity.StudentClassUser;

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

    /**
     * 删除班级
     *
     * @param studentClassId 班级ID
     * @return no content
     */
    @FormUrlEncoded
    @POST("/class/del_class")
    Observable<Response<Object>> delClass(@Field("studentClassId") String studentClassId);

    /**
     * 教师发起签到
     *
     * @param longitude      经度
     * @param latitude       纬度
     * @param studentClassId 课堂ID
     * @param m              最远签到距离（米）
     * @param startTime      签到开始时间
     * @param endTime        签到结束时间
     * @return 签到元数据
     */
    @FormUrlEncoded
    @POST("/class/new_check")
    Observable<RestModel<StudentClassCheckMetaData>> newCheck(@Field("longitude") double longitude,
                                                              @Field("latitude") double latitude,
                                                              @Field("studentClassId") String studentClassId,
                                                              @Field("m") float m,
                                                              @Field("startTime") LocalDateTime startTime,
                                                              @Field("endTime") LocalDateTime endTime);


    /**
     * 获取签到信息
     *
     * @param studentUserName 学生用户名
     * @param studentClassId  班级ID
     * @return 签到信息
     */
    @GET("/class/user_check_detail")
    Observable<RestModel<List<StudentClassCheckDTO>>> getUserCheckDetail(@Query("studentUserName") String studentUserName, @Query("studentClassId") String studentClassId);

    /**
     * 教师删除自己班级的学生
     *
     * @param studentUserName 用户名
     * @param studentClassId  班级ID
     * @return no content
     */
    @FormUrlEncoded
    @POST("/class/del_student")
    Observable<Response<Object>> delStudent(@Field("studentUserName") String studentUserName,
                                            @Field("studentClassId") String studentClassId);

    /**
     * 导出某班打卡信息
     *
     * @param studentClassId 班级ID
     * @return ResponseBody
     */
    @Streaming
    @GET("/class/export/check")
    Observable<ResponseBody> exportCheck(@Query("studentClassId") String studentClassId);

    /**
     * 通过EXCEL文件导入学生到某个班级
     *
     * @param studentClassId 班级ID
     * @param file           文件
     * @return 导入的学生
     */
    @Multipart
    @POST("/class/student_class/file/{studentClassId}")
    Observable<RestModel<List<StudentClassUser>>> importStudentByFile(@Path("studentClassId") String studentClassId, @Part MultipartBody.Part file);

    /**
     * 教师修改自己班级名称
     *
     * @param newStudentClassName 新班级名称
     * @param studentClassId      班级ID
     * @return no content
     */
    @FormUrlEncoded
    @POST("/class/student_class_name")
    Observable<Response<Object>> modifyStudentClassName(@Field("newStudentClassName") String newStudentClassName, @Field("studentClassId") String studentClassId);
}
