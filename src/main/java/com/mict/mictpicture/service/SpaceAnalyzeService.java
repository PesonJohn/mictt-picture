package com.mict.mictpicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mict.mictpicture.model.dto.space.SpaceAddRequest;
import com.mict.mictpicture.model.dto.space.SpaceQueryRequest;
import com.mict.mictpicture.model.dto.space.analyze.*;
import com.mict.mictpicture.model.entity.Space;
import com.mict.mictpicture.model.entity.User;
import com.mict.mictpicture.model.vo.SpaceVo;
import com.mict.mictpicture.model.vo.space.analyze.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author iconSon
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-02-05 16:03:28
*/
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间资源使用分析
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,User loginUser);

    /**
     * 获取空间图片分类分析
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片标签分析
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片大小分析
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     *  获取空间用户上传行为分析
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间使用排行分析
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
