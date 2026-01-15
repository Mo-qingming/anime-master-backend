package cn.luopan.animemasterbackend.service;

import cn.luopan.animemasterbackend.entity.UserAnimeStatus;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.Map;

public interface IUserAnimeStatusService extends IService<UserAnimeStatus> {
    /**
     * 获取用户的特定状态的动漫列表
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 动漫状态列表
     */
    List<UserAnimeStatus> getUserAnimeListByStatus(Long userId, String status);

    /**
     * 更新用户动漫状态
     *
     * @param userId 用户ID
     * @param animeId 动漫ID
     * @param status 状态
     * @return 更新结果
     */
    boolean updateUserAnimeStatus(Long userId, Long animeId, String status);

    /**
     * 添加观看进度
     *
     * @param userId 用户ID
     * @param animeId 动漫ID
     * @param progress 进度
     * @return 更新结果
     */
    boolean updateAnimeProgress(Long userId, Long animeId, Integer progress);

    /**
     * 获取用户的所有动漫状态列表
     *
     * @param userId 用户ID
     * @return 用户的所有动漫状态
     */
    List<UserAnimeStatus> getAllUserAnimeStatus(Long userId);

    /**
     * 获取用户的所有收藏，按状态分类
     *
     * @param userId 用户ID
     * @return 按状态分类的收藏列表
     */
    Map<String, List<UserAnimeStatus>> getCollectionsByStatus(Long userId);

    /**
     * 添加动漫到收藏
     *
     * @param userAnimeStatus 收藏信息
     * @return 添加结果
     */
    boolean addToCollection(UserAnimeStatus userAnimeStatus);

    /**
     * 更新收藏状态
     *
     * @param userId 用户ID
     * @param animeId 动漫ID
     * @param status 新状态
     * @param progress 观看进度
     * @return 更新结果
     */
    boolean updateCollectionStatus(Long userId, Long animeId, String status, Integer progress);

    /**
     * 从收藏中移除动漫
     *
     * @param userId 用户ID
     * @param animeId 动漫ID
     * @return 移除结果
     */
    boolean removeFromCollection(Long userId, Long animeId);

    /**
     * 检查动漫是否已经在收藏中
     *
     * @param userId 用户ID
     * @param animeId 动漫ID
     * @return 是否已经收藏
     */
    boolean isAnimeInCollection(Long userId, Long animeId);
}