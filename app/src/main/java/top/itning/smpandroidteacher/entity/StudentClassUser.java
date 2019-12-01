package top.itning.smpandroidteacher.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 学生班级学生
 *
 * @author itning
 */
@Data
public class StudentClassUser implements Serializable {
    /**
     * 学生 ID
     */
    private User user;
    /**
     * 班级 ID
     */
    private StudentClass studentClass;
    /**
     * 创建时间
     */
    private Date gmtCreate;
    /**
     * 更新时间
     */
    private Date gmtModified;
}
