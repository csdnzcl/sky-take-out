package com.sky.annotation;


import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标识需要填充的字段
 */
@Target(ElementType.METHOD)// 表示该注解用于方法上
@Retention(RetentionPolicy.RUNTIME)// 表示该注解在运行时生效
public @interface AutoFill {
    // 枚举值，用于指定填充数据的操作类型 Update,  Insert
    OperationType value();
}
