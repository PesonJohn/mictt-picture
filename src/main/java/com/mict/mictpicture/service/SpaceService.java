package com.mict.mictpicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mict.mictpicture.model.dto.space.SpaceAddRequest;
import com.mict.mictpicture.model.dto.space.SpaceQueryRequest;
import com.mict.mictpicture.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mict.mictpicture.model.entity.User;
import com.mict.mictpicture.model.vo.SpaceVo;

import javax.servlet.http.HttpServletRequest;

/**
* @author iconSon
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-02-05 16:03:28
*/
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest,User loginUser);

    /**
     * 获取查询对象
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间包装类（单个）
     * @param space
     * @param request
     * @return
     */
    SpaceVo getSpaceVo(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVo> getSpaceVoPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 空间数据校验 用于更新和修改空间时判断
     * @param space
     * @param add 是否为创建时校验
     */
    void validSpace(Space space,boolean add);

    /**
     * 根据空间级别填充空间对象
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);
}
