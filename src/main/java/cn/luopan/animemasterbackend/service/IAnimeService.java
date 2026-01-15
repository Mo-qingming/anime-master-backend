package cn.luopan.animemasterbackend.service;

import cn.luopan.animemasterbackend.entity.Anime;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface IAnimeService extends IService<Anime> {
    /**
     * 根据动漫名称查找动漫
     * @param name 动漫名称
     * @return 动漫对象
     */
    Anime getAnimeByName(String name);

    /**
     * 获取每日放送动漫列表
     * @return 每日放送动漫数据列表
     */
    List<Map<String, Object>> getDailyAnime();

    /**
     * 获取动漫排行榜
     * @return 动漫排行榜数据列表
     */
    List<Map<String, Object>> getAnimeRanking();

    /**
     * 搜索动漫
     * @param keyword 搜索关键词
     * @param limit 返回数据数量
     * @param offset 偏移量
     * @return 搜索结果列表
     */
    List<Map<String, Object>> searchAnime(String keyword, Integer limit, Integer offset);
}