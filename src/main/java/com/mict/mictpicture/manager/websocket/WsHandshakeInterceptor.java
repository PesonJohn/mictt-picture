package com.mict.mictpicture.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.mict.mictpicture.manager.auth.SpaceUserAuthManager;
import com.mict.mictpicture.manager.auth.model.SpaceUserPermissionConstant;
import com.mict.mictpicture.model.entity.Picture;
import com.mict.mictpicture.model.entity.Space;
import com.mict.mictpicture.model.entity.User;
import com.mict.mictpicture.model.enums.SpaceTypeEnum;
import com.mict.mictpicture.service.PictureService;
import com.mict.mictpicture.service.SpaceService;
import com.mict.mictpicture.service.SpaceUserService;
import com.mict.mictpicture.service.UserService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket握手拦截器 建立连接前要先校验
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;



    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 建立连接前校验
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 给webSocketSession会话设置属性
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request,@NotNull ServerHttpResponse response,@NotNull WebSocketHandler wsHandler,@NotNull Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest){
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            //从请求中获取参数
            String pictureId = httpServletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)){
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            //获取登录用户
            User loginUser = userService.getLoginUser(httpServletRequest);
            if (loginUser == null){
                log.error("用户未登录，拒绝握手");
                return false;
            }
            //校验用户是否有编辑当前图片的权限
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture)){
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null){
                space = spaceService.getById(spaceId);
                if (ObjUtil.isEmpty(space)){
                    log.error("图片所在空间不存在，拒绝握手");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()){
                    log.error("图片所在空间不是团队空间，拒绝握手");
                    return false;
                }
            }
            //如果是团队空间且有编辑权限才能建立连接
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)){
                log.error("用户没有编辑权限，拒绝握手");
                return false;
            }
            //设置用户登录信息灯属性到websocket会话中
            attributes.put("user",loginUser);
            attributes.put("userId",loginUser.getId());
            attributes.put("pictureId",Long.valueOf(pictureId));//需要转为Long类型
        }
        return true;
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request,@NotNull ServerHttpResponse response,@NotNull WebSocketHandler wsHandler, Exception exception) {

    }
}
