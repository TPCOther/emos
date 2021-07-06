package com.example.emos.wx.config.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request){
        super(request);
    }

    @Override
    public String getParameter(String name){
        String value = super.getParameter(name);
        if(!StrUtil.hasEmpty(value)){
            value = HtmlUtil.filter(value); //过滤html标签防止注入攻击
        }
        return value;
    }

    @Override
    public  String[] getParameterValues(String name){
        String[] values = super.getParameterValues(name);
        if(values!=null){
            for(int i=0;i<values.length;i++){
                String value = values[i];
                if(!StrUtil.hasEmpty(value)){
                    value = HtmlUtil.filter(value);
                }
                values[i] = value;
            }
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap(){
        Map<String, String[]> parameters = super.getParameterMap();
        LinkedHashMap<String, String[]> map = new LinkedHashMap();
        if(parameters!=null){
            for(String key:parameters.keySet()){ //取出map集合中的元素进行过滤再装回
                String[] values = parameters.get(key);
                for(int i = 0;i<values.length;i++){
                    String value = values[i];
                    if(!StrUtil.hasEmpty(value)){
                        value = HtmlUtil.filter(value);
                    }
                    values[i] = value;
                }
                map.put(key,values);
            }
        }
        return map;
    }

    @Override
    public String getHeader(String name){
        String value = super.getHeader(name);
        if(!StrUtil.hasEmpty(value)){
            value = HtmlUtil.filter(value); //过滤html标签防止注入攻击
        }
        return value;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream in = super.getInputStream(); //获取原始IO流
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8); //创建字符流读取数据
        BufferedReader buffer = new BufferedReader(reader); //利用缓冲流高效读取
        StringBuffer body = new StringBuffer(); //暂存字符流的数据
        String line = buffer.readLine();
        while (line!=null){
            body.append(line);
            line = buffer.readLine();
        }
        buffer.close(); //关闭流
        reader.close();
        in.close();
        Map<String,Object> map = JSONUtil.parseObj(body.toString()); //将json数据转换成map对象
        Map<String,Object> result = new LinkedHashMap<>();
        for(String key:map.keySet()){
            Object val = map.get(key);
            if(val instanceof String){
                String value = val.toString();
                if(!StrUtil.hasEmpty(value)){
                    value = HtmlUtil.filter(value);
                }
                result.put(key,value);
            }
            else{
                result.put(key,val);
            }
        }
        String json = JSONUtil.toJsonStr(result);
        ByteArrayInputStream bain = new ByteArrayInputStream(json.getBytes());
        return new ServletInputStream() {
            @Override
            public int read() throws IOException { return bain.read(); }

            @Override
            public boolean isFinished() { return false; }

            @Override
            public boolean isReady() { return false; }

            @Override
            public void setReadListener(ReadListener readListener) { }
        }; //匿名类返回过滤了的读入流
    }
}
