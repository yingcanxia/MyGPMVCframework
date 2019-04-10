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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	
	//Ϊʲô����ʹ��map.shiyong map�Ļ�keyֵֻ����url
	//handler�����ܾ��Ƕ�Ӧurl��method
	//������������һְ������֪��ԭ��
	//�������Ͻ�����map��������ֱ�Ӿ���list
	private List<HandlerMapping>handlerMappings=new ArrayList<HandlerMapping>();

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
		//��һ���õ��û��ķ���·��
		String contextPath=req.getContextPath();
		//ȥ������ͷ����http://ip:�˿�/
		url=url.replaceAll(contextPath, "").replaceAll("/+", "/");
		HandlerMapping handlerMapping= getHandler(req);
		if(handlerMapping==null) {
		/*if(!handlerMappings.containsKey(url)) {*/
			//û���ҵ��������֫��д404û�ҵ�
			resp.getWriter().write("404");
			return;
		}
		handlerMapping.method.getParameterTypes();
		
		
		
		
		/*//����е�����´�handlerMapping���õ�����Ҫ�ķ���
		Method method=getHandler(req).getMethod();
		//��req���õ�key->value�Ķ�Ӧ��ϵ
		Map<String,String[]> params=req.getParameterMap();
		//�������Ĳ������ͺͲ�������һ������
		Class<?>[]paramsTypes=method.getParameterTypes();
		Object[]paramValues=new Object[paramsTypes.length];
		
		
		for(int i=0;i<paramsTypes.length;i++) {
			Class paramsType=paramsTypes[i];
			//�����HttpServletRequest��HttpServletResponse���ࣨ������web�����Զ����ɣ�ֱ�Ӹ�ֵ
			if(paramsType==HttpServletRequest.class) {
				paramValues[i]=req;
				continue;
			}else if(paramsType==HttpServletResponse.class) {
				paramValues[i]=resp;
				continue;
			}//���������String���͵Ļ������ͬhttp://IP:�˿�/aaa/bbb?name=aaa���ַ���·��
			else if(paramsType==String.class) {
				//�ڱ���������Ȼ�ȡ��������Ĳ�����ע��
				Annotation[][] pa=method.getParameterAnnotations();
				for(int j=0;j<pa.length;j++) {
					for(Annotation a:pa[i]) {
						if(a instanceof MyRequestParam) {
							//��req�л�ȡ��Ӧ��keyֵ
							//��keyֵ����һ�Զ�Ĺ�ϵ
							String paramName=((MyRequestParam)a).value();
							if(params.containsKey(paramName)) {
								//for(Map.Entry<String, String[]>param:params.entrySet()) {
									//�˴����������ʽ����
									
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
		if(handlerMappings.isEmpty()) {
			return null;
		}
		String url = req.getRequestURI();
		//��һ���õ��û��ķ���·��
		String contextPath=req.getContextPath();
		//ȥ������ͷ����http://ip:�˿�/
		url=url.replaceAll(contextPath, "").replaceAll("/+", "/");
		for(HandlerMapping mapping:this.handlerMappings) {
			if(mapping.getUrl().equals(url)) {
				return mapping;
			}
		}
		return null;
	}

	private Object convert(Class<?>type,String value) {
		
		if(Integer.class==type) {
			return Integer.valueOf(value);
		}else if(String.class==type) {
			value.replaceAll("\\[\\]", "").replaceAll("\\s", "");
			return value;
		}
		//����Ӧ��ʹ�ò���ģʽ
		return value;
		
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		
		doScanner(contestConfig.getProperty("scanPackage"));
		
		doInstance();
		
		doAutowired();
		
		initHandlerMapping();
		
		System.out.println("�ҵ�mvc����Ѿ�����");
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
			//����д�����ϵ�url
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
				this.handlerMappings.add(new HandlerMapping(url, method,entry.getValue()));
				//handlerMappings.put(url, method);
				System.out.println(url+","+method.getName());
			}
			//������һ������
			
			
		}
	}

	private void doAutowired() {
		// TODO Auto-generated method stub
		if(IOC.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> entry : IOC.entrySet()) {
			//�����ֶΰ���private��protected��defaults��public
			Field[] fields=entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				if(!field.isAnnotationPresent(MyAutowired.class)) {
					return;
				}
				MyAutowired autowired=field.getAnnotation(MyAutowired.class);
				String beanName=autowired.value().trim();
				//�û�û���Զ������ֵ������
				
				if("".equals(beanName)) {
					beanName=field.getType().getName();
				}
				//public��������η�����ע���ʱ��ҲҪǿ�Ƹ��ơ��ڷ����н�����������
				field.setAccessible(true);
				try {
					//�÷������
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
					//keyֵ����ʹ��classname����ĸСд
					String beanName=clazz.getSimpleName();
					IOC.put(beanName, instance);
				}else if(clazz.isAnnotationPresent(MyService.class)){
					//Ĭ������
					String beanName=clazz.getSimpleName();//Ĭ������ĸСд
					
					//�Զ�������
					MyService service=clazz.getAnnotation(MyService.class);
					Object instance=clazz.newInstance();
					if(!"".equals(service.value())) {
						beanName=service.value();
						IOC.put(beanName, instance);
					}
					//���������Զ���ֵ
					for (Class<?> i : clazz.getInterfaces()) {
						if(IOC.containsKey(i.getName())) {
							throw new Exception("�������Ѿ���ռ��");
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
	
	public class HandlerMapping{
		//���뽫���ŵ�mapping��
		private String url;
		private Method method;
		private Object controller;
		private Class<?>[] paramTypes;
		//�����Ա����β��б�
		private Map<String,Integer>paramIndexMapping;
		public HandlerMapping(String url, Method method, Object controller) {
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
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
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
