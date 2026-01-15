-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS anime_master CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE anime_master;

-- 用户表
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    disabled TINYINT(1) DEFAULT 0 COMMENT '是否禁用，0-未禁用，1-已禁用',
    login_fail_count INT DEFAULT 0 COMMENT '登录失败次数',
    last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
    locked_until TIMESTAMP NULL COMMENT '账号锁定时间'
);

-- 动漫表
CREATE TABLE anime (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bangumi_id INT UNIQUE COMMENT 'Bangumi API的动漫ID',
    name VARCHAR(255) NOT NULL COMMENT '动漫原名',
    name_cn VARCHAR(255) COMMENT '动漫中文名',
    images JSON COMMENT '动漫图片信息，包含large、medium、small',
    rating JSON COMMENT '动漫评分信息，包含score、total',
    tags JSON COMMENT '动漫标签列表',
    type INT DEFAULT 0 COMMENT '类型（2表示TV动画）',
    collection JSON COMMENT '收藏信息，包含collect、doing',
    date VARCHAR(20) COMMENT '开播日期',
    eps INT DEFAULT 0 COMMENT '集数',
    description TEXT COMMENT '动漫描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户动漫状态表
CREATE TABLE user_anime_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    anime_id BIGINT NOT NULL,
    status ENUM('wantToWatch', 'watching', 'watched', 'dropped') NOT NULL COMMENT '动漫状态：想看、在看、已看、弃置',
    progress INT DEFAULT 0 COMMENT '观看进度，百分比',
    last_watched_episode INT DEFAULT 0 COMMENT '最后观看的集数',
    rating DECIMAL(3, 1) DEFAULT NULL COMMENT '评分',
    notes TEXT COMMENT '笔记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (anime_id) REFERENCES anime(id),
    UNIQUE KEY uk_user_anime (user_id, anime_id)
);

-- 插入一些示例数据
INSERT INTO user (username, email, password) VALUES
('testuser', 'test@example.com', 'encoded_password');

INSERT INTO anime (name, title, description, episodes_count, release_date) VALUES
('Dragon Ball', '龙珠', '经典动漫', 153, '1986-02-26 00:00:00'),
('Naruto', '火影忍者', '忍者动漫', 220, '2002-10-03 00:00:00'),
('One Piece', '海贼王', '海盗冒险动漫', 1000, '1999-10-20 00:00:00');