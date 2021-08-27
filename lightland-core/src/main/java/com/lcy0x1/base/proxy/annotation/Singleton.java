package com.lcy0x1.base.proxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Singleton {
    String[] errMsgTemplate = {
            "%O", // checked object
            "%C", // checked Class
    };

    String errMsg() default "";

    Class<? extends RuntimeException> errClass() default RuntimeException.class;
}
