package com.example.emos.wx.exception;
import  lombok.Data;
@Data //自动生成set/get方法
public class EmosException extends RuntimeException{
    private String msg; //错误信息
    private int code = 500; //错误码

    public EmosException(String msg){
        super(msg);
        this.msg = msg;
    }

    public EmosException(String msg, Throwable e){
        super(msg,e);
        this.msg = msg;
    }

    public EmosException(String msg, int code){
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public EmosException(String msg, int code, Throwable e){
        super(msg,e);
        this.msg = msg;
        this.code = code;
    }
}
