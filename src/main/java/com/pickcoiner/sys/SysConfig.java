package com.pickcoiner.sys;

import java.sql.Connection;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallFilter;
import com.jfinal.config.Constants;
import com.jfinal.config.Handlers;
import com.jfinal.config.Interceptors;
import com.jfinal.config.JFinalConfig;
import com.jfinal.config.Plugins;
import com.jfinal.config.Routes;
import com.jfinal.core.JFinal;
import com.jfinal.ext.handler.ContextPathHandler;
import com.jfinal.json.MixedJsonFactory;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.render.FreeMarkerRender;
import com.jfinal.render.JsonRender;
import com.jfinal.render.ViewType;
import com.jfinal.template.Engine;
import com.pickcoiner.common.FrontRouts;
/**
 * 系统站点核心配置
 * com.pickcoiner.sys.SysConfig.java
 * @author Dicky Yin
 * @date 2017-10-23
 */
public class SysConfig extends JFinalConfig{
	private static Prop p = loadConfig();
	private WallFilter wallFilter;

	/**
	 * loading系统核心配置
	 * @return
	 */
	private static Prop loadConfig() {
		try {
			// 优先加载生产环境配置文件
			return PropKit.use("SysConfig.properties");
		} catch (Exception e) {
			// 找不到生产环境配置文件，再去找开发环境配置文件
			return PropKit.use("SysConfig_Dev.properties");
		}
	}
	
	public void configConstant(Constants me) {
		me.setDevMode(p.getBoolean("devMode", false));
		
		//设置json工厂
		me.setJsonFactory(MixedJsonFactory.me());
		
		//TODO 这里暂时设为20m，后面需要改为更大值，由于 这里Jfinal使用的是int类型，所以后续需要修改为long
		me.setMaxPostSize(1024*1024*20);//20M
		
		//设置404,500页面
		me.setError404View("error/404.html");
		me.setError500View("error/500.html");
		
		// 设置视图模板类型
		me.setViewType(ViewType.FREE_MARKER);
		me.setViewExtension(".html");
	}

	public void configEngine(Engine me) {
		
	}

	public void configHandler(Handlers me) {
		me.add(new ContextPathHandler("BASE_PATH"));
	}

	public void configInterceptor(Interceptors me) {
		
	}

	/**
	 * 配置插件
	 */
	public void configPlugin(Plugins me) {
		// 配置 druid 数据库连接池插件
		DruidPlugin druidPlugin = getDruidPlugin();
		wallFilter = new WallFilter();	// 加强数据库安全
		wallFilter.setDbType("mysql");	//设置数据库类型
	    druidPlugin.addFilter(wallFilter);
	    druidPlugin.addFilter(new StatFilter());    // 添加 StatFilter 才会有统计数据
		me.add(druidPlugin);
		
		// 配置ActiveRecord插件
		ActiveRecordPlugin arp = new ActiveRecordPlugin(druidPlugin);
		arp.setTransactionLevel(Connection.TRANSACTION_READ_COMMITTED);		//设置事物级别
		com.pickcoiner.common.kit._MappingKit.mapping(arp);		//完美解决表的映射问题
		me.add(arp);
		
		if(p.getBoolean("devMode",false)){
			arp.setShowSql(true);	//开发模式下ShowSql
		}
	}
	
	 /**
     * 抽取成独立的方法，例于 _Generator 中重用该方法，减少代码冗余
     */
	public static DruidPlugin getDruidPlugin() {
		return new DruidPlugin(p.get("jdbcUrl"), p.get("user"), p.get("password").trim());
	}
	
	/**
	 * 配置访问路由
	 */
	public void configRoute(Routes me) {
		//me.setBaseViewPath("/WEB-INF/template");
		me.add(new FrontRouts());
	}
	
	/**
     * 本方法会在 jfinal 启动过程完成之后被回调，详见 jfinal 手册
     */
	public void afterJFinalStart() {
		
		// 调用不带参的 renderJson() 时，排除对 loginAccount、remind 的 json 转换
		JsonRender.addExcludedAttrs();
		// 让 druid 允许在 sql 中使用 union
		wallFilter.getConfig().setSelectUnionCheck(false);
		
		FreeMarkerRender.setProperties(loadPropertyFile("freemarker.properties"));	//加载properties配置
	}
	
	
	/**
	 * 运行此 main 方法可以启动项目，此main方法可以放置在任意的Class类定义中，不一定要放于此
	 * 
	 * 使用本方法启动过第一次以后，会在开发工具的 debug、run config 中自动生成
	 * 一条启动配置，可对该自动生成的配置再添加额外的配置项，例如 VM argument 可配置为：
	 * -XX:PermSize=64M -XX:MaxPermSize=256M
	 */
	public static void main(String[] args) {
		/**
		 * 特别注意：Eclipse 之下建议的启动方式
		 */
		JFinal.start("src/main/webapp", 80, "/", 5);
		
		/**
		 * 特别注意：IDEA 之下建议的启动方式，仅比 eclipse 之下少了最后一个参数
		 */
		// JFinal.start("src/main/webapp", 80, "/");
	}


}
