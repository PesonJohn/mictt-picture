package com.mict.mictpicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mict.mictpicture.model.dto.space.SpaceAddRequest;
import com.mict.mictpicture.model.dto.space.SpaceQueryRequest;
import com.mict.mictpicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.mict.mictpicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.mict.mictpicture.model.entity.Space;
import com.mict.mictpicture.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mict.mictpicture.model.entity.User;
import com.mict.mictpicture.model.vo.SpaceUserVo;
import com.mict.mictpicture.model.vo.SpaceVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author iconSon
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-02-13 14:56:04
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 创建空间成员
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 获取空间成员包装类（单个）
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVo getSpaceUserVo(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员包装类（多条）
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUserList);

    /**
     * 空间成员校验
     * @param spaceUser
     * @param add 是否为创建时校验
     */
    void validSpaceUser(SpaceUser spaceUser,boolean add);

    /**
     * 获取查询对象
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
