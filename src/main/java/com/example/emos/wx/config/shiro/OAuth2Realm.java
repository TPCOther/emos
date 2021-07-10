package com.example.emos.wx.config.shiro;

import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OAuth2Realm extends AuthorizingRealm {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    public boolean supports(AuthenticationToken token){
        return token instanceof OAuth2Token;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection collection) {
        TbUser user = (TbUser) collection.getPrimaryPrincipal();  //取出之前令牌中封装的user
        int userId = user.getId();
        Set<String> permsSet = userService.searchUserPermissions(userId);   //获取权限列表
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setStringPermissions(permsSet);    //设置令牌权限
        return info;    //返回令牌
    }


    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = (String) token.getPrincipal(); //获得字符串形式的令牌
        int userId = jwtUtil.getUserId(accessToken);
        TbUser user = userService.searchById(userId);   //取得用户信息
        if(user == null){ throw new LockedAccountException("账号已被锁定，请联系管理员"); }  //员工离职异常
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, accessToken, getName()); //封装为令牌对象
        return info;
    }
}
