package com.github.sparkler.framework.context;

import com.github.sparkler.framework.annotation.Autowired;
import com.github.sparkler.framework.annotation.Controller;
import com.github.sparkler.framework.annotation.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {
    private Map<String,Object> instanceMapping = new ConcurrentHashMap<>();
    /**
     * 类似于内部的配置信息,我们在外面是看不到的
     * 我们能看到的只有ioc容器 getBean的方法来间接调用的
     */
    private List<String> classCache = new ArrayList<>();
    public ApplicationContext(String location){
        InputStream in = null;
        try {
            in = this.getClass().getClassLoader().getResourceAsStream(location);
            Properties props = new Properties();
            props.load(in);
            //注册,将所有的class找出来存储
            doRegister(props.getProperty("scanPackage"));
            //初始化,循环class
            doCreateBean();
            //载入
            populate();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void populate() {
        if(instanceMapping.isEmpty()){
            return ;
        }

        for(Map.Entry<String,Object> entry :instanceMapping.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field :fields){
                if(!field.isAnnotationPresent(Autowired.class)){
                    continue;
                }
                Autowired autowired = field.getAnnotation(Autowired.class);
                String id = autowired.value().trim();
                //如果id为空,也就是说没有自己配置 默认根据类型注入
                if("".equals(id)){
                    id = field.getType().getName();
                }
                field.setAccessible(true); //将私有变量开放访问权限
                try {
                    field.set(entry.getValue(),instanceMapping.get(id));//?
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doCreateBean() {
        if(classCache.isEmpty()){
            return ;
        }
        try {
            for (String className : classCache){
                Class<?> clzz = Class.forName(className);

                if(clzz.isAnnotationPresent(Controller.class)){
                    String id = lowFirstCase(clzz.getSimpleName());
                    instanceMapping.put(id,clzz.newInstance());
                }else if(clzz.isAnnotationPresent(Service.class)){
                    Service service = clzz.getAnnotation(Service.class);
                    //如果设置了自定义名称,就优先用自己定义的名字
                    String id = service.value();
                    if( !"".equals(id.trim())){
                        instanceMapping.put(id,clzz.newInstance());
                        continue;
                    }
                    //如果是空的,则使用默认规则
                    //1.类名首字母小写
                    //如果这个类是接口
                    //2.可以根据类型类匹配

                    Class<?>[] interfaces = clzz.getInterfaces();
                    //如果这个类实现了接口,就用接口的类型作为id
                    for(Class<?> i : interfaces){
                        instanceMapping.put(i.getName(),clzz.newInstance());
                    }

                }else{
                    continue;
                }

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }  catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    private String lowFirstCase(String name){
        char[] c = name.toCharArray();
        c[0] += 32;
        return String.valueOf(c);

    }
    //把符合条件的所有class都加载出来
    private void doRegister(String packageName){
        URL url = this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for (File file :dir.listFiles()){
            if(file.isDirectory()){
                doRegister(packageName+"."+file.getName());
            }else{
                classCache.add(packageName+"."+file.getName().replace(".class","").trim());
            }
        }
    }
    public Object getBean(String name){
        return instanceMapping.get(name);
    }

    public Map<String,Object> getAll(){
        return instanceMapping;
    }
}
