package com.github.sparkler.web.action;

import com.github.sparkler.framework.annotation.*;
import com.github.sparkler.web.service.IHelloService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/hello")
public class HelloAction {
    @Autowired("helloService")
    private IHelloService helloService;

    @RequestMapping("/sayhello/*")
    @ResponseBody
    public void sayHello(HttpServletRequest req, HttpServletResponse resp,@RequestParam("name") String name) throws IOException {
        System.out.println(name);
        helloService.sayHello();
        resp.getWriter().write(name);
    }
}
