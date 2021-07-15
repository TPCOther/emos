package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.config.SysConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.db.pojo.TbHolidays;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.task.EmailTask;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.resource.HttpResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Scope("prototype")
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    @Autowired
    private SysConstants constants;

    @Autowired
    private TbHolidaysDao holidaysDao;

    @Autowired
    private TbWorkdayDao workdayDao;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private TbFaceModelDao faceModelDao;

    @Autowired
    private TbCityDao cityDao;

    @Autowired
    private EmailTask emailTask;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Value("${emos.email.hr}")
    private String hrEmail;

    @Override
    public String validCanCheckIn(int userId, String date) {
        boolean bool_1 = holidaysDao.searchTodayIsHolidays() != null ? true : false;   //判断是否是特殊工作日
        boolean bool_2 = workdayDao.searchTodayIsWorkday() != null ? true : false;  //判断是否是特殊节假日
        String type = "工作日";
        if (DateUtil.date().isWeekend()) { type = "节假日"; }  //判断是否为周末
        if (bool_1) { type = "节假日"; }
        else if (bool_2) { type = "工作日"; }

        if (type.equals("节假日")) { return "节假日不需要考勤"; }
        else {
            DateTime now = DateUtil.date();
            String start = DateUtil.today() + " " + constants.attendanceStartTime;  //获取上班时间
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) { return "未到上班考勤开始时间"; }
            else if (now.isAfter(attendanceEnd)) { return "上班考勤时间已过";}
            else{                                                           //判断是否已经打过卡
                HashMap map = new HashMap();
                map.put("userId",userId);
                map.put("date",date);
                map.put("start",start);
                map.put("end",end);
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "今日已考勤，不用重复考勤" : "可以考勤";
            }
        }
    }

    @Override
    public void checkin(HashMap param) {
        int status = 1;
        Date d1 = DateUtil.date();
        Date d2 = DateUtil.parse(DateUtil.today() + " "  + constants.attendanceTime);
        Date d3 = DateUtil.parse(DateUtil.today() + " "  + constants.attendanceEndTime);
        if (d1.compareTo(d2)<=0){ status = 1; }
        else if(d1.compareTo(d2)>0&&d1.compareTo(d3)<0){ status = 2; }
        int userId = (Integer) param.get("userId");
        String faceModel = faceModelDao.searchFaceModel(userId);
        if(faceModel==null){ throw new EmosException("不存在人脸模型"); }
        else{
            String path = (String)param.get("path");
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            request.form("photo", FileUtil.file(path),"targetModel",faceModel); //上传照片与储存的人脸模型
            HttpResponse response = request.execute();
            if(response.getStatus()!=200){
                log.error("人脸识别服务异常");
                throw new EmosException("人脸识别服务异常");
            }
            String body = response.body();
            if("无法识别出人脸".equals(body)||"照片中存在多张人脸".equals(body)){
                throw new EmosException(body);
            }
            else if("False".equals(body)){ throw new EmosException("签到无效，非本人签到"); }
            else if("True".equals(body)){
                int risk = 1;
                String city = (String)param.get("city");    //获取签到地点
                String district = (String)param.get("district");
                String address = (String)param.get("address");
                String country = (String)param.get("country");
                String province = (String)param.get("province");

                if(!StrUtil.isBlank(city)&&!StrUtil.isBlank(district)){
                    String code = cityDao.searchCode(city);
                    try{
                        String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                        Document document = Jsoup.connect(url).get();
                        Elements elements = document.getElementsByClass("list-content");
                        if(elements.size()>0){
                            Element element = elements.get(0);
                            String result = element.select("p:last-child").text();
                            if("高风险".equals(result)){
                                risk = 3;
                                HashMap<String, String> map = userDao.searchNameAndDept(userId);
                                String name = map.get("name");
                                String deptName = map.get("dept_name");
                                deptName = deptName != null ? deptName : "";
                                SimpleMailMessage message = new SimpleMailMessage();
                                message.setTo(hrEmail);
                                message.setSubject("员工" + name + "身处高风险疫情地区警告");
                                message.setText(deptName + "员工" + name + "," + DateUtil.format(new Date(), "yyyy年MM月dd日") + "处于" + address +",属于新冠疫情高风险区，请及时与该员工联系，核实情况！");
                            }
                            else if("中风险".equals(result)){ risk = 2; }
                        }
                    }catch (Exception e){
                        log.error("执行异常");
                        throw new EmosException("获取风险等级失败");
                    }
                }
                //保存签到记录
                TbCheckin entity = new TbCheckin();
                entity.setRisk(risk);
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setCity(city);
                entity.setProvince(province);
                entity.setDistrict(district);
                entity.setStatus((byte)status);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            }
        }
    }

    @Override
    public void createFaceModel(int userId, String path) {
        HttpRequest request = HttpUtil.createPost(createFaceModelUrl);  //请求flask接口
        request.form("photo",FileUtil.file(path));  //上传图片文件
        HttpResponse response = request.execute();
        String body = response.body();
        if("无法识别人脸".equals(body)||"照片中存在多张人脸".equals(body)){    //处理错误消息
            throw new EmosException(body);
        }
        else{
            TbFaceModel entity = new TbFaceModel(); //储存base64编码的人脸模型
            entity.setUserId(userId);
            entity.setFaceModel(body);
            faceModelDao.insert(entity);
        }
    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map = checkinDao.searchTodayCheckin(userId);
        return map;
    }

    @Override
    public long searchCheckinDays(int userId) {
        long days = checkinDao.searchCheckinDays(userId);
        return days;
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        ArrayList<HashMap> checkinList = checkinDao.searchWeekCheckin(param);   //查询周签到记录
        ArrayList holidaysList = holidaysDao.searchHolidaysInRange(param);  //判断周内的特殊假日与工作日
        ArrayList workdayList = workdayDao.searchWorkdayInRange(param);
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate = DateUtil.parseDate(param.get("endDate").toString());
        DateRange range = DateUtil.range(startDate,endDate, DateField.DAY_OF_MONTH);    //生成周对象
        ArrayList<HashMap> list = new ArrayList<>();
        range.forEach(one ->{   //遍历一周处理考勤数据
            String date = one.toString("yyyy-MM-dd");
            String type = "工作日";    //判断当天是否是特殊节假日/工作日
            if(one.isWeekend()){ type = "节假日"; }
            if(holidaysList != null && holidaysList.contains(date)){ type = "节假日"; }
            else if(workdayList != null && workdayList.contains(date)){ type = "工作日"; }
            String status = "";
            if(type.equals("工作日") && DateUtil.compare(one,DateUtil.date()) <= 0){   //判断当天是否打卡及打卡状态
                status = "缺勤";
                boolean flag = false;
                for(HashMap<String,String> map:checkinList){
                    if(map.containsValue(date)){
                        status = map.get("status");
                        flag = true;
                        break;
                    }
                }
                DateTime endTime = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
                String today = DateUtil.today();
                if(date.equals(today) && DateUtil.date().isBefore(endTime) && !flag){   //对于日期为当天的特殊处理
                    status = "";
                }
            }
            HashMap map = new HashMap();    //将处理完的信息放入map返回
            map.put("date",date);
            map.put("status",status);
            map.put("type",type);
            map.put("day",one.dayOfWeekEnum().toChinese("周"));
            list.add(map);
        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return checkinDao.searchWeekCheckin(param);
    }
}