package cn.shadow.mvcframework.v3.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.omg.CORBA.OBJ_ADAPTER;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;

import cn.shadow.mvcframework.v1.annotation.MyAutowired;
import cn.shadow.mvcframework.v1.annotation.MyController;
import cn.shadow.mvcframework.v1.annotation.MyRequestMapping;
import cn.shadow.mvcframework.v1.annotation.MyRequestParam;
import cn.shadow.mvcframework.v1.annotation.MyService;

public class MyDispatchServlet extends HttpServlet{
	
	private Properties contestConfig=new Properties(); 

	private List<String>classNames=new ArrayList<String>();
	
	private Map<String,Object>IOC=new HashMap<String,Object>();
	
	//private Map<String,Method>handlerMapping=new HashMap<String,Method>();
	
	//为什么不能使用map.shiyong map的话key值只能是url
	//handler本身功能就是对应url和method
	//存在荣誉，单一职责，最少知道原则
	//从性能上讲交给map便利不如直接就是list
	private List<HandlerMapping>handlerMappings=new ArrayList<HandlerMapping>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//所有的请求都转到dopost方法去执行
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			//全部交给doDispatch来执行
			doDispatch(req, resp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp)throws Exception{
		// TODO Auto-generated method stub
		//request中提取用户的访问路径
		String url = req.getRequestURI();
		//第一步拿到用户的访问路径
		String contextPath=req.getContextPath();
		//去掉访问头例如http://ip:端口/
		url=url.replaceAll(contextPath, "").replaceAll("/+", "/");
		HandlerMapping handlerMapping= getHandler(req);
		if(handlerMapping==null) {
		/*if(!handlerMappings.containsKey(url)) {*/
			//没有找到的情况下肢解写404没找到
			resp.getWriter().write("404");
			return;
		}
		
		//参数类型列表
		Class<?>[]paramTypes=handlerMapping.method.getParameterTypes();
		//对象列表
		Object[] paramValues=new Object[paramTypes.length];
		//http协议是基于字符串的url？para=aaa,键值对其中key值会有多个值所以这里是数组
		Map<String,String[]>params=req.getParameterMap();
		for (Map.Entry<String, String[]> parm : params.entrySet()) {
			//将数组化解
			String value =Arrays.toString(parm.getValue()).replaceAll("\\[|\\]", "").replaceAll("\\s", ",");
			if(!handlerMapping.paramIndexMapping.containsKey(parm.getKey())) {
				//如果没有对应的key值这跳出
				continue;
			}
			//依照方法的参数类型列表将值春芳在列表
			int index=handlerMapping.paramIndexMapping.get(parm.getKey());
			paramValues[index]=convert(paramTypes[index], value);
		}
		//handlerMapping.method.invoke(handlerMapping.controller, paramValues);
		if(handlerMapping.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
			//存入request
			int reqIndex=handlerMapping.paramIndexMapping.get(HttpServletRequest.class.getName());
			paramValues[reqIndex]=req; 
		}
		if(handlerMapping.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
			//存入response
			int respIndex=handlerMapping.paramIndexMapping.get(HttpServletResponse.class.getName());
			paramValues[respIndex]=resp; 
		}
		//调用找到的方法去执行
		Object returnValue=handlerMapping.method.invoke(handlerMapping.controller, paramValues);
		//当返回值是空或者方法是void的时候
		if(returnValue==null||returnValue instanceof Void) {
			return;
			//正常返回
		}
		//当不是的时候让response去写出结果
		resp.getWriter().write(returnValue.toString());
		/*//如果有的情况下从handlerMapping中拿到所需要的方法
		Method method=getHandler(req).getMethod();
		//从req中拿到key->value的对应关系
		Map<String,String[]> params=req.getParameterMap();
		//将方法的参数类型和参数进行一次整合
		Class<?>[]paramsTypes=method.getParameterTypes();
		Object[]paramValues=new Object[paramsTypes.length];
		
		
		for(int i=0;i<paramsTypes.length;i++) {
			Class paramsType=paramsTypes[i];
			//如果是HttpServletRequest或HttpServletResponse者类（该类有web容器自动生成）直接赋值
			if(paramsType==HttpServletRequest.class) {
				paramValues[i]=req;
				continue;
			}else if(paramsType==HttpServletResponse.class) {
				paramValues[i]=resp;
				continue;
			}//如果类型是String类型的话这就如同http://IP:端口/aaa/bbb?name=aaa这种访问路径
			else if(paramsType==String.class) {
				//在本情况先首先获取这个方法的参数的注解
				Annotation[][] pa=method.getParameterAnnotations();
				for(int j=0;j<pa.length;j++) {
					for(Annotation a:pa[i]) {
						if(a instanceof MyRequestParam) {
							//从req中获取对应的key值
							//该key值存在一对多的关系
							String paramName=((MyRequestParam)a).value();
							if(params.containsKey(paramName)) {
								//for(Map.Entry<String, String[]>param:params.entrySet()) {
									//此处以数组的形式整理
									
									String value=Arrays.toString(params.get(paramName)).replaceAll("\\[|\\]","").replaceAll("\\s", "");
									 
									paramValues[i]=convert(paramsType, Arrays.toString(params.get(paramName)));
									
								//}
							}
						}
					}
				}
			}
		}
		String beanName=method.getDeclaringClass().getSimpleName();
		method.invoke(IOC.get(beanName), paramValues);
		*/
		
	}
	private HandlerMapping getHandler(HttpServletRequest req) {
		// TODO Auto-generated method stub
		//当handlerMapping为空是返回
		if(handlerMappings.isEmpty()) {
			return null;
		}
		String url = req.getRequestURI();
		//第一步拿到用户的访问路径
		String contextPath=req.getContextPath();
		//去掉访问头例如http://ip:端口/
		url=url.replaceAll(contextPath, "").replaceAll("/+", "/");
		for(HandlerMapping mapping:this.handlerMappings) {
			//遍历list
			Matcher match=mapping.getUrl().matcher(url);
			if(!match.matches()) {continue;}
			return mapping;
			
		}
		return null;
	}

	private Object convert(Class<?>type,String value) {
		//类型转化
		if(Integer.class==type) {
			return Integer.valueOf(value);
		}else if(String.class==type) {
			value.replaceAll("\\[\\]", "").replaceAll("\\s", "");
			return value;
		}
		//这里应该使用策略模式
		return value;
		
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		//本类的的初始化函数
		//加载配置文件括号里的是个key值与web.xml有关
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		//扫描配置文件配置的包
		doScanner(contestConfig.getProperty("scanPackage"));
		//ioc控制反转将扫描到的bean类加载到ioc容器之中
		doInstance();
		//进行依赖注入
		doAutowired();
		//加载处理器使功能能够正常使用
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
			//保存卸载方法上的url
			for (Method method : clazz.getMethods()) {
				if(!method.isAnnotationPresent(MyRequestMapping.class)) {
					continue;
				}
				MyRequestMapping requestMapping=method.getAnnotation(MyRequestMapping.class);
				String url=("/"+baseUrl+"/"+requestMapping.value()).replaceAll("/+", "/");
				Pattern pattern=Pattern.compile(url);
				
				this.handlerMappings.add(new HandlerMapping(pattern, method,entry.getValue()));
				//handlerMappings.put(url, method);
				System.out.println(url+","+method.getName());
			}
			//在声明一个容器
			
			
		}
	}

	private void doAutowired() {
		// TODO Auto-generated method stub
		//将service自动注入
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
				//如果该类是被MyAutowired所注释
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
		//
		if(classNames.isEmpty()) {
			return;
		}
		try {
			for (String className : classNames) {
				Class<?>clazz=Class.forName(className);
				//判断是否是以MyController这个类为注释的
				if(clazz.isAnnotationPresent(MyController.class)) {
					Object instance=clazz.newInstance();
					//key值可以使用classname首字母小写
					String beanName=clazz.getSimpleName();
					//如果是则用将其放入IOC容器之中
					IOC.put(beanName, instance);
				}
				//检查类名是否够使用MyService作为注释
				else if(clazz.isAnnotationPresent(MyService.class)){
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
		
		//获得配置文件中的键值对
		URL url=this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.", "/"));
		File classPath=new File(url.getFile());
		for (File file : classPath.listFiles()) {
			if(file.isDirectory()) {
				doScanner(scanPackage+"."+file.getName());
			}else {
				if(!file.getName().endsWith(".class")) {continue;}
				String className=scanPackage+"."+file.getName().replace(".class", "");	
				//整理类名
				classNames.add(className);
			}
		}
	}

	private void doLoadConfig(String contextConfigLocation) {
		// TODO Auto-generated method stub
		//加载配置文件
		InputStream is=null;
		is=this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			//其中意思有变量的形式保存加载文件
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
	
	public class HandlerMapping{
		//必须将俩放到mapping中
		private Pattern url;
		private Method method;
		private Object controller;
		private Class<?>[] paramTypes;
		//还可以保存形参列表
		public Map<String,Integer>paramIndexMapping;
		public HandlerMapping(Pattern url, Method method, Object controller) {
			super();
			this.url = url;
			this.method = method;
			this.controller = controller;
			paramTypes=method.getParameterTypes();
			paramIndexMapping=new HashMap<String,Integer>();
			putParamIndexMapping(method);
		}
		private void putParamIndexMapping(Method method) {
			// TODO Auto-generated method stub
			Annotation[][]pa=method.getParameterAnnotations();
			for(int i=0;i<pa.length;i++) {
				for(Annotation a:pa[i]) {
					if(a instanceof MyRequestParam) {
						String paramName=((MyRequestParam)a).value();
						if(!"".equals(paramName.trim())) {
							paramIndexMapping.put(paramName, i);
						}
						
					}
				}
			}
			Class<?>[] paramTypes=method.getParameterTypes();
			for(int i=0;i<paramTypes.length;i++) {
				Class<?> type=paramTypes[i];
				if(type==HttpServletRequest.class||type==HttpServletResponse.class) {
				}
			}
		}
		
		public Pattern getUrl() {
			return url;
		}
		public void setUrl(Pattern url) {
			this.url = url;
		}
		public Method getMethod() {
			return method;
		}
		public void setMethod(Method method) {
			this.method = method;
		}
		public Object getController() {
			return controller;
		}
		public void setController(Object controller) {
			this.controller = controller;
		}
		public Map<String, Integer> getParamIndexMapping() {
			return paramIndexMapping;
		}
		public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
			this.paramIndexMapping = paramIndexMapping;
		}
		public Class<?>[] getParamTypes() {
			return paramTypes;
		}
		public void setParamTypes(Class<?>[] paramTypes) {
			this.paramTypes = paramTypes;
		}
	}
		
}
