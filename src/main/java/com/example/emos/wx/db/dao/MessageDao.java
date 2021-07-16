package com.example.emos.wx.db.dao;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Repository
public class MessageDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    public String insert(MessageEntity entity){
        Date sendTime = entity.getSendTime();
        sendTime = DateUtil.offset(sendTime, DateField.HOUR,8);
        entity.setSendTime(sendTime);
        entity = mongoTemplate.save(entity);
        return  entity.get_id();
    }

    public List<HashMap> searchMessageByPage(int userId,long start,int length){
        JSONObject json = new JSONObject();
        json.set("$toString","$_id");
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.addFields().addField("id").withValue(json).build(),
                Aggregation.lookup("message_ref","id","messageId","ref"),
                Aggregation.match(Criteria.where("ref.receiverId").is(userId)),
                Aggregation.sort(Sort.by(Sort.Direction.DESC,"sendTime")),
                Aggregation.skip(start),
                Aggregation.limit(length)
        );  //集合联合查询构造
        AggregationResults<HashMap> results = mongoTemplate.aggregate(aggregation,"message",HashMap.class); //执行查询
        List<HashMap> list = results.getMappedResults();   //获取结果
        list.forEach(one -> {
            List<MessageRefEntity> refList = (List<MessageRefEntity>) one.get("ref");   //取出ref(收件人)对象
            MessageRefEntity entity = refList.get(0);   //取出ref中的必要数据
            boolean readFlag = entity.getReadFlag();
            String refId = entity.get_id();
            one.put("readFlag",readFlag);
            one.put("refId",refId);
            one.remove("ref");  //删除ref与消息主体id(无用部分)
            one.remove("_id");
            Date sendTime = (Date) one.get("sendTime");
            sendTime = DateUtil.offset(sendTime, DateField.HOUR, -8);   //格林乔治时间->北京时间
            String today = DateUtil.today();
            if(today.equals(DateUtil.date(sendTime).toDateStr())){
                one.put("sendTime",DateUtil.format(sendTime,"HH:mm"));
            }
            else{
                one.put("sendTime",DateUtil.format(sendTime,"yyyy/MM/dd"));
            }
        });
        return list;
    }

    public HashMap searchMessageById(String id){
        HashMap map = mongoTemplate.findById(id,HashMap.class,"message");
        Date sendTime = (Date) map.get("sendTime");
        sendTime = DateUtil.offset(sendTime, DateField.HOUR,-8);
        map.replace("sendTime", DateUtil.format(sendTime,"yyyy-MM-dd HH:mm"));  //日期对象转字符串
        return map;
    }
}
