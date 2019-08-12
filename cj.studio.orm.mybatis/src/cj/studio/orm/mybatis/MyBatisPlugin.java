package cj.studio.orm.mybatis;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.EcmException;
import cj.studio.ecm.IAssemblyContext;
import cj.studio.ecm.IChipPlugin;
import cj.studio.ecm.context.Element;
import cj.studio.ecm.context.IElement;
import cj.studio.ecm.context.INode;
import cj.studio.ecm.context.IProperty;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.gson2.com.google.gson.reflect.TypeToken;
import cj.ultimate.net.sf.cglib.proxy.Callback;
import cj.ultimate.net.sf.cglib.proxy.Enhancer;

//DataSource dataSource = BlogDataSourceFactory.getBlogDataSource();
//TransactionFactory transactionFactory = new JdbcTransactionFactory();//ManagedTransactionFactory
//Environment environment = new Environment("development", transactionFactory, dataSource);
//Configuration configuration = new Configuration(environment);
//configuration.addMapper(BlogMapper.class);
//SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
public class MyBatisPlugin implements IChipPlugin {
	private static ISafeSqlSessionFactory factory;
	ClassLoader classLoader;
	public static ISafeSqlSessionFactory getFactory() {
		return factory;
	}
	@Override
	public Object getService(String name) {
		Class<?> clazz = null;
		try {
			if(classLoader!=null) {
				clazz = Class.forName(name,true,classLoader);
			}else{
				clazz = Class.forName(name);
			}
		} catch (ClassNotFoundException e) {
			throw new EcmException(e);
		}
		if(!clazz.isInterface()) {
			throw new EcmException("不是mybatis的mapping:"+clazz);
		}
		//生成mybatis mapping接口的代理
		Callback cb=new MappingCallback(factory,clazz);
		Object obj=Enhancer.create(Object.class,new Class[] {clazz} ,cb);
		return obj;
	}

	@Override
	public void load(IAssemblyContext ctx, IElement args) {
		ClassLoader the = ctx.getResource().getClassLoader();
		this.classLoader=the;
		InputStream in = the.getResourceAsStream("cj/db/mybatis.xml");
		if (in == null) {
			throw new EcmException("mybatis.xml不存在:cj/db/mybatis.xml");
		}
		Resources.setDefaultClassLoader(the);
		SqlSessionFactory sqlfactory = null;
		try {
			sqlfactory = new SqlSessionFactoryBuilder().build(in);
			factory = new SafeSqlSessionFactory(sqlfactory);
		} catch (Exception e) {
			String title = getAssemblyTitle(ctx);
			throw new EcmException(String.format("%s, 在程序集：%s", e.getMessage(), title));
		}
		Configuration conf = sqlfactory.getConfiguration();
		addClasses(conf,args);
		addPackages(conf,args);
	}

	private void addPackages(Configuration conf, IElement args) {
		IProperty prop = (IProperty) args.getNode("packages");
		if (prop != null) {
			String json = prop.getValue() == null ? "[]" : prop.getValue().getName();
			List<String> list = new Gson().fromJson(json, new TypeToken<ArrayList<String>>() {
			}.getType());
			for (String packageStr : list) {
				conf.addMappers(packageStr);
			}
		}
	}
	private void addClasses(Configuration conf, IElement args) {
		IProperty prop = (IProperty) args.getNode("classes");
		if (prop != null) {
			String json = prop.getValue() == null ? "[]" : prop.getValue().getName();
			List<String> list = new Gson().fromJson(json, new TypeToken<ArrayList<String>>() {
			}.getType());
			for (String classStr : list) {
				Class<?> clazz = null;
				try {
					clazz = Class.forName(classStr);
				} catch (ClassNotFoundException e) {
					CJSystem.logging().warn(getClass(), e.getMessage());
					continue;
				}
				conf.addMapper(clazz);
			}
		}
	}
	private String getAssemblyTitle(IAssemblyContext ctx) {
		IElement root = ctx.getElement();
		INode infoNode = root.getNode("assemblyInfo");
		Element ct = (Element) infoNode;
		IProperty atProp = (IProperty) ct.getNode("assemblyTitle");
		String name = atProp.getValue().getName();
		return name;
	}

	@Override
	public void unload() {
		factory = null;
	}

}
