package com.github.sparkler.web.service.impl;

import com.github.sparkler.framework.annotation.Service;
import com.github.sparkler.web.service.IHelloService;

@Service("helloService")
public class HelloServiceImpl implements IHelloService {
    @Override
    public void sayHello() {
        System.out.println("hello world");
    }
}
