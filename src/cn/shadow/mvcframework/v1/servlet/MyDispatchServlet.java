package cn.shadow.mvcframework.v1.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.shadow.mvcframework.v1.annotation.MyAutowired;
import cn.shadow.mvcframework.v1.annotation.MyController;
import cn.shadow.mvcframework.v1.annotation.MyRequestMapping;
import cn.shadow.mvcframework.v1.annotation.MyService;

public class MyDispatchServlet extends HttpServlet{
	
	private Properties contestConfig=new Properties(); 

	private List<String>classNames=new ArrayList<String>();
	
	private Map<String,Object>IOC=new HashMap<String,Object>();
	
	private Map<String,Method>handlerMapping=new HashMap<String,Method>();
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			doDispatch(req, resp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp)throws Exception{
		// TODO Auto-generated method stub
		String url = req.getRequestURI();
		
		String contextPath=req.getContextPath();
		url=url.replaceAll(contextPath, "").replaceAll("/+", "/");
		if(!handlerMapping.containsKey(url)) {
			resp.getWriter().write("404");
			return;
		}
		Method method=handlerMapping.get(url);
		String beanName=method.getDeclaringClass().getSimpleName();
		Map<String,String[]> params=req.getParameterMap();
		Class<?>[]paramsTypes=method.getParameterTypes();
		for(int i=0;i<paramsTypes.length;i++) {
			
		}
		
		method.invoke(IOC.get(beanName), new Object[] {req,resp,params.get("name")[0]});
		
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		
		doScanner(contestConfig.getProperty("scanPackage"));
		
		doInstance();
		
		doAutowired();
		
		initHandlerMapping();
		
		System.out.println("我的mvc框架已经启动");
	}

	private void initHandlerMapping() {
		// TODO Auto-generated method stub
		if(IOC.isEmpty()) {
			return;
		}
		for(Map.Entry<String,Object>entry:IOC.entrySet()) {
			Class<?>clazz=entry.getValue().getClass();
			if(clazz.isAnnotationPresent(MyController.class)) {
				continue;
			}
			//保存写在类上的url
			String baseUrl="";
			if(clazz.isAnnotationPresent(MyRequestMapping.class)) {
				MyRequestMapping requestMapping=clazz.getAnnotation(MyRequestMapping.class);
				baseUrl=requestMapping.value()[0];
			}
			for (Method method : clazz.getMethods()) {
				if(!method.isAnnotationPresent(MyRequestMapping.class)) {
					continue;
				}
				MyRequestMapping requestMapping=method.getAnnotation(MyRequestMapping.class);
				String url=("/"+baseUrl+"/"+requestMapping.value()).replaceAll("/+", "/");
				handlerMapping.put(url, method);
				System.out.println(url+","+method.getName());
			}
			//在声明一个容器
			
			
		}
	}

	private void doAutowired() {
		// TODO Auto-generated method stub
		if(IOC.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> entry : IOC.entrySet()) {
			//所有字段包括private，protected，defaults，public
			Field[] fields=entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				if(!field.isAnnotationPresent(MyAutowired.class)) {
					return;
				}
				MyAutowired autowired=field.getAnnotation(MyAutowired.class);
				String beanName=autowired.value().trim();
				//用户没有自定义名字的情况下
				
				if("".equals(beanName)) {
					beanName=field.getType().getName();
				}
				//public以外的修饰符加了注解的时候也要强制复制。在反射中叫做暴力访问
				field.setAccessible(true);
				try {
					//用反射机制
					field.set(entry.getValue(), IOC.get(beanName));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		
		
	}

	private void doInstance() {
		// TODO Auto-generated method stub
		if(classNames.isEmpty()) {
			return;
		}
		try {
			for (String className : classNames) {
				Class<?>clazz=Class.forName(className);
				if(clazz.isAnnotationPresent(MyController.class)) {
					Object instance=clazz.newInstance();
					//key值可以使用classname首字母小写
					String beanName=clazz.getSimpleName();
					IOC.put(beanName, instance);
				}else if(clazz.isAnnotationPresent(MyService.class)){
					//默认类名
					String beanName=clazz.getSimpleName();//默认首字母小写
					
					//自定义类名
					MyService service=clazz.getAnnotation(MyService.class);
					Object instance=clazz.newInstance();
					if(!"".equals(service.value())) {
						beanName=service.value();
						IOC.put(beanName, instance);
					}
					//根据类型自动赋值
					for (Class<?> i : clazz.getInterfaces()) {
						if(IOC.containsKey(i.getName())) {
							throw new Exception("该名字已经被占用");
						}
						IOC.put(i.getName(), instance);
					}
					
					
				}
				else {
					continue;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
			
	}

	private void doScanner(String scanPackage) {
		// TODO Auto-generated method stub
		URL url=this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.", "/"));
		File classPath=new File(url.getFile());
		for (File file : classPath.listFiles()) {
			if(file.isDirectory()) {
				doScanner(scanPackage+"."+file.getName());
			}else {
				if(!file.getName().endsWith(".class")) {continue;}
				String className=scanPackage+"."+file.getName().replace(".class", "");	
				classNames.add(className);
			}
			
			
			
			
			
		}
		
		
		
	}

	private void doLoadConfig(String contextConfigLocation) {
		// TODO Auto-generated method stub
		InputStream is=null;
		is=this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			contestConfig.load(is);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(null!=is) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
}
