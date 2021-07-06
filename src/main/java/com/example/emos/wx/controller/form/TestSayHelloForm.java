package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@ApiModel //为swagger测试标记
@Data //提供set/get方法
public class TestSayHelloForm {
//    @NotBlank //不能为空
//    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,15}$") //正则匹配名字为2~15个汉字
    @ApiModelProperty("姓名") //为属性注释
    private String name;
}
