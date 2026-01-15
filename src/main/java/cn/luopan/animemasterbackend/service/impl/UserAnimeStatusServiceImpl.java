package cn.luopan.animemasterbackend.service.impl;

import cn.luopan.animemasterbackend.entity.UserAnimeStatus;
import cn.luopan.animemasterbackend.mapper.UserAnimeStatusMapper;
import cn.luopan.animemasterbackend.service.IUserAnimeStatusService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserAnimeStatusServiceImpl extends ServiceImpl<UserAnimeStatusMapper, UserAnimeStatus> implements IUserAnimeStatusService {

    @Autowired
    private UserAnimeStatusMapper userAnimeStatusMapper;

    @Override
    public List<UserAnimeStatus> getUserAnimeListByStatus(Long userId, String status) {
        QueryWrapper<UserAnimeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("status", status);
        return userAnimeStatusMapper.selectList(queryWrapper);
    }

    @Override
    public boolean updateUserAnimeStatus(Long userId, Long animeId, String status) {
        QueryWrapper<UserAnimeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("anime_id", animeId);

        UserAnimeStatus userAnimeStatus = userAnimeStatusMapper.selectOne(queryWrapper);
        
        if (userAnimeStatus == null) {
            // 如果不存在，则创建新的记录
            userAnimeStatus = new UserAnimeStatus();
            userAnimeStatus.setUserId(userId);
            userAnimeStatus.setAnimeId(animeId);
            userAnimeStatus.setStatus(status);
            userAnimeStatus.setProgress(0);
            userAnimeStatus.setCreatedAt(LocalDateTime.now());
            userAnimeStatus.setUpdatedAt(LocalDateTime.now());
            return userAnimeStatusMapper.insert(userAnimeStatus) > 0;
        } else {
            // 如果存在，则更新状态
            userAnimeStatus.setStatus(status);
            userAnimeStatus.setUpdatedAt(LocalDateTime.now());
            return userAnimeStatusMapper.updateById(userAnimeStatus) > 0;
        }
    }

    @Override
    public boolean updateAnimeProgress(Long userId, Long animeId, Integer progress) {
        QueryWrapper<UserAnimeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("anime_id", animeId);

        UserAnimeStatus userAnimeStatus = userAnimeStatusMapper.selectOne(queryWrapper);
        if (userAnimeStatus != null) {
            userAnimeStatus.setProgress(progress);
            userAnimeStatus.setUpdatedAt(LocalDateTime.now());
            return userAnimeStatusMapper.updateById(userAnimeStatus) > 0;
        }
        return false;
    }

    @Override
    public List<UserAnimeStatus> getAllUserAnimeStatus(Long userId) {
        QueryWrapper<UserAnimeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return userAnimeStatusMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, List<UserAnimeStatus>> getCollectionsByStatus(Long userId) {
        // 获取用户所有收藏
        List<UserAnimeStatus> allCollections = getAllUserAnimeStatus(userId);
        
        // 按状态分类
        Map<String, List<UserAnimeStatus>> collectionsByStatus = new HashMap<>();
        collectionsByStatus.put("wantToWatch", new ArrayList<>());
        collectionsByStatus.put("watching", new ArrayList<>());
        collectionsByStatus.put("watched", new ArrayList<>());
        collectionsByStatus.put("dropped", new ArrayList<>());

        // 将收藏项分配到对应状态列表
        for (UserAnimeStatus collection : allCollections) {
            String status = collection.getStatus();
            if (collectionsByStatus.containsKey(status)) {
                collectionsByStatus.get(status).add(collection);
            }
        }

        return collectionsByStatus;
    }

    @Override
    public boolean addToCollection(UserAnimeStatus userAnimeStatus) {
        // 检查是否已存在
        if (isAnimeInCollection(userAnimeStatus.getUserId(), userAnimeStatus.getAnimeId())) {
            return false;
        }

        // 根据status设置合理的progress值，确保符合检查约束
        Integer progress = userAnimeStatus.getProgress();
        String finalStatus = userAnimeStatus.getStatus();

        if (progress == null) {
            // 根据状态设置默认进度
            if ("wantToWatch".equals(finalStatus) || "dropped".equals(finalStatus)) {
                userAnimeStatus.setProgress(0);
            } else if ("watching".equals(finalStatus)) {
                userAnimeStatus.setProgress(1); // 开始观看至少看了1集
            } else if ("watched".equals(finalStatus)) {
                // 如果没有提供episodes信息，默认设置为1表示已看完
                userAnimeStatus.setProgress(userAnimeStatus.getEpisodes() != null ? userAnimeStatus.getEpisodes() : 1);
            }
        } else {
            // 确保进度与状态一致
            if ("wantToWatch".equals(finalStatus) || "dropped".equals(finalStatus)) {
                // 想看或弃番时，进度应该为0
                userAnimeStatus.setProgress(0);
            } else if ("watched".equals(finalStatus)) {
                // 已看时，进度应该等于总集数或至少为1
                Integer episodes = userAnimeStatus.getEpisodes();
                if (episodes != null) {
                    userAnimeStatus.setProgress(Math.min(progress, episodes));
                } else {
                    userAnimeStatus.setProgress(Math.max(1, progress));
                }
            } else if ("watching".equals(finalStatus)) {
                // 观看中时，进度应该至少为1
                userAnimeStatus.setProgress(Math.max(1, progress));
            }
        }
        
        System.out.println("Adding to collection with status: " + userAnimeStatus.getStatus() + ", progress: " + userAnimeStatus.getProgress());
        
        // 设置时间戳
        userAnimeStatus.setCreatedAt(LocalDateTime.now());
        userAnimeStatus.setUpdatedAt(LocalDateTime.now());
        
        // 插入记录
        return userAnimeStatusMapper.insert(userAnimeStatus) > 0;
    }

    @Override
    public boolean updateCollectionStatus(Long userId, Long animeId, String status, Integer progress) {
        QueryWrapper<UserAnimeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("anime_id", animeId);
        
        UserAnimeStatus userAnimeStatus = userAnimeStatusMapper.selectOne(queryWrapper);
        if (userAnimeStatus == null) {
            return false;
        }
        
        // 更新状态
        userAnimeStatus.setStatus(status);
        
        // 如果提供了进度，则更新进度
        if (progress != null) {
            userAnimeStatus.setProgress(progress);
        }
        
        // 设置更新时间
        userAnimeStatus.setUpdatedAt(LocalDateTime.now());
        
        // 更新记录
        return userAnimeStatusMapper.updateById(userAnimeStatus) > 0;
    }

    @Override
    public boolean removeFromCollection(Long userId, Long animeId) {
        QueryWrapper<UserAnimeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("anime_id", animeId);
        
        // 检查是否存在
        if (userAnimeStatusMapper.selectCount(queryWrapper) == 0) {
            return false;
        }
        
        // 删除记录
        return userAnimeStatusMapper.delete(queryWrapper) > 0;
    }

    @Override
    public boolean isAnimeInCollection(Long userId, Long animeId) {
        QueryWrapper<UserAnimeStatus> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("anime_id", animeId);
        return userAnimeStatusMapper.selectCount(queryWrapper) > 0;
    }
}