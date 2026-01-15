package cn.luopan.animemasterbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户动漫状态实体类
 * 用于记录用户对动漫的状态（想看、在看、已看、弃置）
 */
@Data
@TableName("user_anime_status")
public class UserAnimeStatus implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("anime_id")
    private Long animeId;

    /**
     * 动漫标题
     */
    @TableField("title")
    private String title;

    /**
     * 动漫中文标题
     */
    @TableField("title_cn")
    private String titleCn;

    /**
     * 动漫封面图URL
     */
    @TableField("image")
    private String image;

    /**
     * 总集数
     */
    @TableField("episodes")
    private Integer episodes;

    /**
     * 动漫状态：wantToWatch(想看), watching(在看), watched(已看), dropped(弃置)
     */
    @TableField("status")
    private String status;

    @TableField("progress")
    private Integer progress; // 观看进度，百分比

    @TableField("last_watched_episode")
    private Integer lastWatchedEpisode; // 最后观看的集数

    @TableField("rating")
    private Double rating; // 评分

    @TableField("notes")
    private String notes; // 笔记

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        WANT_TO_WATCH("wantToWatch", "想看"),
        WATCHING("watching", "在看"),
        WATCHED("watched", "已看"),
        DROPPED("dropped", "弃置");

        private final String value;
        private final String description;

        Status(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static Status fromValue(String value) {
            for (Status status : Status.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid status value: " + value);
        }
    }
}