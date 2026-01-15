package cn.luopan.animemasterbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 动漫实体类
 */
@Data
@TableName("anime")
public class Anime implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("bangumi_id")
    private Integer bangumiId;

    @TableField("name")
    private String name;

    @TableField("name_cn")
    private String nameCn;

    @TableField("images")
    private String images;

    @TableField("rating")
    private String rating;

    @TableField("tags")
    private String tags;

    @TableField("type")
    private Integer type;

    @TableField("collection")
    private String collection;

    @TableField("date")
    private String date;

    @TableField("eps")
    private Integer eps;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}