package com.mict.mictpicture.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mict.mictpicture.exception.BusinessException;
import com.mict.mictpicture.exception.ErrorCode;
import com.mict.mictpicture.exception.ThrowUtils;
import com.mict.mictpicture.mapper.SpaceMapper;
import com.mict.mictpicture.model.dto.space.analyze.*;
import com.mict.mictpicture.model.entity.Picture;
import com.mict.mictpicture.model.entity.Space;
import com.mict.mictpicture.model.entity.User;
import com.mict.mictpicture.model.vo.space.analyze.*;
import com.mict.mictpicture.service.PictureService;
import com.mict.mictpicture.service.SpaceAnalyzeService;
import com.mict.mictpicture.service.SpaceService;
import com.mict.mictpicture.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        //全空间或者公共图库 需要从Picture表中查询
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()){
            //权限校验 ，仅管理员可以访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest,loginUser);
            //统计图库的使用空间
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            //补充查询范围
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest,queryWrapper);
            //调用list()返回的是List<Picture>，存的是一个对象，当数据量大的时候会占用大量空间，需要转换为List<Long>
            //getBaseMapper().selectObjs 返回的其实是Long
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize =  pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();
            long usedCount = pictureObjList.size();
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //公共图库无数量和容量限制
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        }else {
            //特定空间直接从Space表中查询
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            //获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            //权限校验 仅管理员可以访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest,loginUser);
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            //计算比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize() , 2).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        //检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest,loginUser);
        //查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest,queryWrapper);
        //分组查询
        queryWrapper.select("category","count(*) as count","sum(picSize) as size")
                .groupBy("category");
        //查询并转换结果
        List<SpaceCategoryAnalyzeResponse> list = pictureService.getBaseMapper().selectMaps(queryWrapper).stream()
                .map(result -> {
                    String category = (String) result.get("category");
                    Long count = ((Number) result.get("count")).longValue();
                    Long size = ((Number) result.get("size")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, size);
                })
                .collect(Collectors.toList());
        return list;
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        //检查权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest,loginUser);
        //查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest,queryWrapper);
        //查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        //解析标签并统计
        //扁平化 把多个数组的值合并成一个数组
        Map<String, Long> tagCountMap = tagJsonList.stream()
                //["a","b"],["a","c"] => ["a","b","a","c"]
                .flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        //转换为响应对象，按照使用次数进行排序
        return tagCountMap.entrySet().stream()
               .sorted((e1,e2) -> Long.compare(e2.getValue(),e1.getValue()))//降序
               .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(),entry.getValue()))
               .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        //检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest,loginUser);
        //查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest,queryWrapper);
        //查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        List<Long> pictureSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
               .filter(ObjUtil::isNotNull)
               .map(size -> (Long) size)
               .collect(Collectors.toList());
        //定义分段范围，使用有序的Map存储
        Map<String,Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB",pictureSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB",pictureSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB",pictureSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeRanges.put(">1MB",pictureSizeList.stream().filter(size -> size >= 1024 * 1024).count());
        //转换为响应对象
        return sizeRanges.entrySet().stream()
              .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(),entry.getValue()))
              .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        //检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest,loginUser);
        //查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest,queryWrapper);
        //补充用户id查询
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId),"userId",userId);
        //补充时间维度: 每日 每周 每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension){
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') as period","count(*) as count");
                break;
            case "week":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%u') as period","count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') as period","count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"非法时间维度");
        }
        //分组排序
        queryWrapper.groupBy("period").orderByAsc("period");
        //查询并封装结果
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period,count);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null,ErrorCode.PARAMS_ERROR);
        //检查权限
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        //构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","spaceName","userId","totalSize")
                .orderByDesc("totalSize")//降序
               .last("limit " + spaceRankAnalyzeRequest.getTopN());
        return spaceService.list(queryWrapper);
    }

    /**
     * 校验空间权限
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser){
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        //仅管理员可以全空间分析或者公共图库分析
        if (queryAll || queryPublic){
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        }else{
            //分析特定空间，仅本人或者管理员可以
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            spaceService.checkSpaceAuth(loginUser,space);
        }
    }

    /**
     * 根据请求对象封装查询条件
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper){
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll)return;
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic){
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null){
            queryWrapper.eq("spaceId",spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR,"未指定查询范围");
    }
}
