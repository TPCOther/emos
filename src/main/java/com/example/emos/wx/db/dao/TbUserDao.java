package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.Set;

@Mapper
public interface TbUserDao {
    public boolean haveRootUser();  //是否已绑定超级管理员
    public int insert(HashMap param); //插入用户数据
    public Integer searchIdByOpenId(String openId); //查询用户ID
    public Set<String> searchUserPermissions(int userId); //查询用户权限，set用于多角色权限去重
}