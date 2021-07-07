package com.example.emos.wx.config.shiro;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

//完成JWT相关方法实现
@Component
@Slf4j
public class JwtUtil {
    @Value("${emos.jwt.secret}$") //注入配置信息
    private String secret;

    @Value("${emos.jwt.expire}$")
    private int expire;

    public String createToken(int userId){
        Date date = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR,5); //计算过期日期
        Algorithm algorithm = Algorithm.HMAC256(secret); //选择加密算法
        JWTCreator.Builder builder = JWT.create(); //建造者模式生成token
        return builder.withClaim("userId",userId).withExpiresAt(date).sign(algorithm); //组装token并返回
    }

    public int getUserId(String token){
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getClaim("userId").asInt();
    }

    public void verifierToken(String token){
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm).build(); //创建对应算法的验证器
        verifier.verify(token); //利用验证器验证token，不成功时抛出RuntimeException
    }

}
