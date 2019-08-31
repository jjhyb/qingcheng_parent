package com.qingcheng.controller;

import com.qingcheng.entity.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 统一异常处理类
 */
@ControllerAdvice
public class BaseExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(BaseExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(Exception e) {
        logger.info(e.getMessage());
        logger.info("调用了公共异常处理类",e);
        return new Result(1,e.getMessage());
    }


}
