package com.mict.mictpicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mict.mictpicture.model.dto.user.UserQueryRequest;
import com.mict.mictpicture.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mict.mictpicture.model.vo.LoginUserVo;
import com.mict.mictpicture.model.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author iconSon
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-01-21 11:57:03
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount 账号
     * @param userPassword 密码
     * @param checkPassword 确认密码
     * @param userName 用户名
     * @return 新用户id
     */
    long userRegister(String userAccount,String userPassword,String checkPassword,String userName);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return 脱敏后的用户数据
     */
    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取脱敏后的登录用户信息
     * @param user
     * @return
     */
    LoginUserVo getLoginUserVo(User user);

    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
    UserVo getUserVo(User user);

    /**
     * 获取脱敏后的用户信息列表
     * @param userList
     * @return
     */
    List<UserVo> getUserVoList(List<User> userList);

    /**
     * 获取加密后的密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);


    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户登出
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
