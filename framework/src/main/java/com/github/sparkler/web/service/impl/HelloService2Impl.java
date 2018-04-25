package com.github.sparkler.web.service.impl;

import com.github.sparkler.framework.annotation.Service;
import com.github.sparkler.web.service.IHelloService;

@Service("helloService2")
public class HelloService2Impl implements IHelloService {
    @Override
    public void sayHello() {
        System.out.println("hello world 2");
    }
}
