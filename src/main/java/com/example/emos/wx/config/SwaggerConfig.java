package com.example.emos.wx.config;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket createRestApi(){
        Docket docket = new Docket(DocumentationType.SWAGGER_2);
        ApiInfoBuilder builder = new ApiInfoBuilder(); //传入信息的类
        builder.title("EMOS"); //swagger界面标题
        ApiInfo info = builder.build();
        docket.apiInfo(info);

        ApiSelectorBuilder selectorBuilder = docket.select(); //添加需要测试的接口的类
        selectorBuilder.paths(PathSelectors.any()); //将所有类加入swagger
        selectorBuilder.apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class)); //筛选特定注解类加入Swagger
        docket = selectorBuilder.build();

        //向swagger添加JWT兼容
        ApiKey apiKey = new ApiKey("token","token","header");
        List<ApiKey>apiKeyList = new ArrayList<>();
        apiKeyList.add(apiKey);
        docket.securitySchemes(apiKeyList);

        //设定JWT作用域,需要多层封装成swagger的指定参数类型
        AuthorizationScope scope = new AuthorizationScope("global","accessEverything");
        AuthorizationScope [] scopes = {scope};
        SecurityReference reference = new SecurityReference("token",scopes);
        List refList = new ArrayList();
        refList.add(reference);
        SecurityContext context = SecurityContext.builder().securityReferences(refList).build();
        List cxtList = new ArrayList();
        cxtList.add(context);
        docket.securityContexts(cxtList);

        return docket;
    }
}
