package com.github.sparkler.framework.servelt;

import com.github.sparkler.framework.annotation.Controller;
import com.github.sparkler.framework.annotation.RequestMapping;
import com.github.sparkler.framework.annotation.RequestParam;
import com.github.sparkler.framework.context.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServelt extends HttpServlet {
    private static String location ="application.properties";
//    private Map<Pattern,Handler> handlerMaping = new HashMap<>();
    private List<Handler> handlerList = new ArrayList<>();
//    private List<Handler> handlerMapings = new ArrayList<Handler>();

    private Map<Handler,HandlerAdapter>  adapterMap = new HashMap<>();
    //初始化IOC容器
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("invoke init method");
        ApplicationContext context = new ApplicationContext(location);
        Map<String,Object> ioc = context.getAll();

        System.out.println(ioc);
        System.out.println(ioc.get("helloService"));


        this.initMultipartResolver(context);// 请求解析
        this.initLocaleResolver(context);//多语言,国际化
        this.initThemeResolver(context);//主题
        this.initHandlerMappings(context);//解析url与method的关系 **
        this.initHandlerAdapters(context);//适配器(匹配过程) **
        this.initHandlerExceptionResolvers(context);//异常解析
        this.initRequestToViewNameTranslator(context);//视图转发(根据视图名称匹配到一个具体模板)
        this.initViewResolvers(context);//解析模板中的内容(根据服务器传过来的内容,生成html代码) **
        this.initFlashMapManager(context);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    //调用Controller方法
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatcher(req, resp);
    }

    public void doDispatcher(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        //从handlerMapping中取出handler
        Handler handler = getHandler(req);
        if(handler == null){
            resp.getWriter().write("404");
            return;
        }
        //再取出一个adapter
        HandlerAdapter adapter = getHandlerAdapter(handler);

        //由adpter具体调用方法
        try {
            adapter.handle(req,resp,handler);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    private Handler getHandler(HttpServletRequest req){
        if(handlerList.isEmpty()){
            return null;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();

        url = url.replace(contextPath,"").replaceAll("/+","/");
        for(Handler handler :handlerList){
            Matcher matcher = handler.pattern.matcher(url);
            if(!matcher.matches()){
                continue;
            }
            return handler;
        }
        return null;
    }
    private HandlerAdapter getHandlerAdapter(Handler handler){
        if(adapterMap.isEmpty()){
            return null;
        }
        return adapterMap.get(handler);
    }
    private void initMultipartResolver(ApplicationContext context) {

    }
    
    private void initLocaleResolver(ApplicationContext context) {

    }
    private void initThemeResolver(ApplicationContext context) {

    }
    private void initHandlerMappings(ApplicationContext context) {
        //只要是由controller修饰的类,里面的方法全部找出来
        //而且这个方法应该要加上了@requestMapping注解,如果没加注解,这个方法是不能为外界所访问的
        Map<String,Object> ioc = context.getAll();
        if(ioc.isEmpty()){
            return ;
        }
        for (Map.Entry<String,Object> entry:ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(Controller.class)){
                continue;
            }
            String url = "";
            if(clazz.isAnnotationPresent(RequestMapping.class)){
               RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
               url = requestMapping.value();
            }
            //扫描controller下的所有方法
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if(!method.isAnnotationPresent(RequestMapping.class)){
                    continue;
                }
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String mappingUrl = (url + requestMapping.value()).replaceAll("/+","/");
                String regex = mappingUrl.replaceAll("\\*",".*");
                Pattern pattern = Pattern.compile(regex);

                System.out.println("mappingUrl:"+mappingUrl+"||regex:"+regex);
                handlerList.add(new Handler(entry.getValue(),method,pattern));
            }

        }
        //requestMapping 会配置一个url,那么一个url就对应一个方法,并将这个关系保存到map中
    }

    private void initHandlerAdapters(ApplicationContext context) {
        if (handlerList.isEmpty()){
            return ;
        }
        Map<String,Integer> paramMapping = new HashMap<>();
        //只需要取出来具体的某个方法
        for(Handler handler : handlerList){
            //把这个方法上面所有的参数全部获取到
            Class<?>[] parameterTypes = handler.method.getParameterTypes();
            //有顺序,但是通过反射,没法拿到我们参数名字
            for (int i = 0; i <parameterTypes.length ; i++) {
                //
                Class<?> type = parameterTypes[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class){
                    paramMapping.put(type.getName(),i);
                }

            }
            Annotation[][] pa = handler.method.getParameterAnnotations();
            for (int i=0;i<pa.length;i++){
                for(Annotation a : pa[i]){
                    if(a instanceof RequestParam){
                        String paramName = ((RequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramMapping.put(paramName,i);
                        }
                    }
                }
            }

            adapterMap.put(handler,new HandlerAdapter(paramMapping));
        }
    }

    private void initHandlerExceptionResolvers(ApplicationContext context) {

    }
    private void initRequestToViewNameTranslator(ApplicationContext context) {

    }

    private void initViewResolvers(ApplicationContext context) {

    }

    private void initFlashMapManager(ApplicationContext context) {

    }


    class Handler{
        protected Object controller;
        protected Method method;
        protected Pattern pattern;
        public Handler(){

        }

        public Handler(Object controller, Method method,Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
        }
    }
    class HandlerAdapter{
        Map<String,Integer> paramMapping;
        public HandlerAdapter(Map<String,Integer> paramMapping ) {
            this.paramMapping = paramMapping;
        }

        //主要目的是用反射调用url对应的method
        public void handle(HttpServletRequest req, HttpServletResponse resp, Handler handler) throws InvocationTargetException, IllegalAccessException {
            //为什么要传 req,resp,handler
            //handler 根据handler 找到method

            Class<?>[] parameterTypes = handler.method.getParameterTypes();

            Object[] paramValues = new Object[parameterTypes.length];
            //要想给参数赋值,只能通过索引号来找到具体的某个参数

            Map<String,String[]> params = req.getParameterMap();
            for(Map.Entry<String,String[]> param : params.entrySet()){
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","").replaceAll(",\\s", ",");
                if( !this.paramMapping.containsKey(param.getKey())){
                    continue;
                }
                int index = this.paramMapping.get(param.getKey());

                //单个赋值是不行的
                paramValues[index] = castStringValue(value,parameterTypes[index]);
            }
            //req ,resp 赋值
            if(paramMapping.containsKey(HttpServletRequest.class.getName())){
                int reqIndex = this.paramMapping.get(HttpServletRequest.class.getName());
                paramValues[reqIndex] = req;
            }
            if(paramMapping.containsKey(HttpServletResponse.class.getName())) {
                int respIndex = this.paramMapping.get(HttpServletResponse.class.getName());
                paramValues[respIndex] = resp;
            }
            handler.method.invoke(handler.controller,paramValues);
        }
    }
    private Object castStringValue(String value,Class<?> clazz){
        if(clazz == String.class){
            return value;
        }else if(clazz == Integer.class){
            return Integer.valueOf(value);
        }else if(clazz == int.class){
            return Integer.valueOf(value).intValue();
        }else{
            return null;
        }
    }
}
