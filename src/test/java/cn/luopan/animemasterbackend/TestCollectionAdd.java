package cn.luopan.animemasterbackend;

import cn.luopan.animemasterbackend.entity.UserAnimeStatus;
import cn.luopan.animemasterbackend.mapper.UserAnimeStatusMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class TestCollectionAdd {

    @Autowired
    private UserAnimeStatusMapper userAnimeStatusMapper;

    @Test
    public void testAddCollectionDirectly() {
        // 创建一个测试对象
        UserAnimeStatus userAnimeStatus = new UserAnimeStatus();
        userAnimeStatus.setUserId(1L);
        userAnimeStatus.setAnimeId(123456L); // 使用一个不太可能存在的animeId
        userAnimeStatus.setTitle("测试动漫");
        userAnimeStatus.setStatus("WANT_TO_WATCH"); // 使用数据库中定义的有效枚举值
        userAnimeStatus.setProgress(0);
        userAnimeStatus.setCreatedAt(LocalDateTime.now());
        userAnimeStatus.setUpdatedAt(LocalDateTime.now());

        System.out.println("准备插入记录，状态值为: " + userAnimeStatus.getStatus());
        
        try {
            // 直接插入记录
            int result = userAnimeStatusMapper.insert(userAnimeStatus);
            System.out.println("插入成功，影响行数: " + result);
        } catch (Exception e) {
            System.out.println("插入失败，错误信息: " + e.getMessage());
            e.printStackTrace();
        }
    }
}