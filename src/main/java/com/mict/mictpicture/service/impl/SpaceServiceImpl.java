package com.mict.mictpicture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mict.mictpicture.exception.BusinessException;
import com.mict.mictpicture.exception.ErrorCode;
import com.mict.mictpicture.exception.ThrowUtils;
import com.mict.mictpicture.model.dto.space.SpaceAddRequest;
import com.mict.mictpicture.model.dto.space.SpaceQueryRequest;
import com.mict.mictpicture.model.entity.Picture;
import com.mict.mictpicture.model.entity.Space;
import com.mict.mictpicture.model.entity.User;
import com.mict.mictpicture.model.enums.SpaceLevelEnum;
import com.mict.mictpicture.model.vo.PictureVo;
import com.mict.mictpicture.model.vo.SpaceVo;
import com.mict.mictpicture.model.vo.UserVo;
import com.mict.mictpicture.service.SpaceService;
import com.mict.mictpicture.mapper.SpaceMapper;
import com.mict.mictpicture.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author iconSon
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-02-05 16:03:28
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    //编程式事务管理器
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 空间数据校验 用于更新和修改空间时判断
     *
     * @param space
     */
    @Override
    public void validSpace(Space space,boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从空间中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        //创建时校验
        if (add){
            if (StrUtil.isBlank(spaceName)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空");
            }
            if (spaceLevel == null){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不能为空");
            }
        }
        // 修改数据时，空间名称校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        //空间级别进行校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null){
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null){
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null){
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        //仅本人或者管理员可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 获取空间包装类（单个）
     *
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVo getSpaceVo(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVo spaceVO = SpaceVo.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVo userVo = userService.getUserVo(user);
            spaceVO.setUser(userVo);
        }
        return spaceVO;
    }

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVo> getSpaceVoPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVo> spaceVoPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVoPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVo> spaceVoList = spaceList.stream().map(SpaceVo::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVoList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVo(user));
        });
        spaceVoPage.setRecords(spaceVoList);
        return spaceVoPage;
    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //1.校验参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest,space);
        if (StrUtil.isBlank(space.getSpaceName())){
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        //填充容量和大小
        fillSpaceBySpaceLevel(space);
        //2. 校验参数
        validSpace(space,true);
        //3. 校验权限 非管理员只能创建普通空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限创建指定级别空间");
        }
        //4. 控制同一用户只能创建一个私有空间
        //intern保证拿到是该userId下的同一个对象
        String lock = String.valueOf(userId).intern();
        //通过加锁保证同一时间只能创建一个空间
        synchronized (lock){
            Long newSpaceId = transactionTemplate.execute(status -> {
                //判断是否有空间
                boolean exists = lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .exists();
                //已有空间不能再创建
                ThrowUtils.throwIf(exists,ErrorCode.OPERATION_ERROR,"每个用户仅能拥有一个私有空间");
                //创建空间
                boolean res = save(space);
                ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR,"保存空间到数据库失败");
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 从多字段中搜索
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }
}




