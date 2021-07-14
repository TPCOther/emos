package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbWorkday;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbWorkdayDao {
    public Integer searchTodayIsWorkday();
    public ArrayList<String> searchWorkdayInRange(HashMap param);
}