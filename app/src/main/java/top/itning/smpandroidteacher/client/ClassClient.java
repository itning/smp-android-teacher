package top.itning.smpandroidteacher.client;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;
import top.itning.smpandroidteacher.client.http.Page;
import top.itning.smpandroidteacher.client.http.RestModel;
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
}
