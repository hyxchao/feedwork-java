package fd.ng.netserver.http;

import fd.ng.core.exception.internal.RawlayerRuntimeException;
import fd.ng.core.utils.ClassUtil;
import fd.ng.netserver.conf.HttpServerConf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.Servlet;

public class WebServer extends AbstractDatabaseServer {
	protected static final Logger logger = LogManager.getLogger(WebServer.class.getName());
	public static void main( String[] args ) {
		new WebServer().running();
	}

	public WebServer() { super(); }
	public WebServer(String serverName) {
		super(serverName);
	}
	@Override
	protected void doInit() throws Exception {
		super.doInit();
		ClassUtil.loadClass(HttpServerConf.class.getName());
		logger.info("Initialize webinfo done.");
//		ClassUtil.loadClass(ActionInstanceHelper.class.getName());
//		logger.info("Initialize actions done.");
	}

	@Override
	protected void configueConnector(Server server) {
		ServerConnector connector = new ServerConnector(server);

		if(HttpServerConf.Host!=null) {
			connector.setHost(HttpServerConf.Host);
		}

		connector.setPort(HttpServerConf.HttpPort);

		connector.setIdleTimeout(HttpServerConf.IdleTimeout);

		/*
		 * 据说：解决Windows下重复启动Jetty不报告端口冲突的问题。
		 * 在Windows下有个Windows + Sun的connector实现的问题：reuseAddress=true时重复启动同一个端口的Jetty不会报错。
		 * 所以必须设为false。代价是若上次退出不干净(比如有TIME_WAIT),会导致新的Jetty不能启动。
		 */
//		connector.setReuseAddress(false);
		server.addConnector(connector);
	}

	@Override
	protected void doConfigureHandler(Server server) {
		ServletContextHandler contextRoot = new ServletContextHandler(ServletContextHandler.SESSIONS);
		contextRoot.setContextPath("/");
//		context.setResourceBase(System.getProperty("java.io.tmpdir"));

		ServletContextHandler contextApp = new ServletContextHandler(ServletContextHandler.SESSIONS);
		contextApp.setContextPath(HttpServerConf.WebContext);
		logger.info("WebContext : {}", HttpServerConf.WebContext);
//		context.setResourceBase(System.getProperty("java.io.tmpdir"));

		SessionHandler sessions = contextApp.getSessionHandler();
		sessions.setMaxInactiveInterval(HttpServerConf.Session_MaxAge);
		sessions.setHttpOnly(HttpServerConf.Session_HttpOnly);
		logger.info("Session    : MaxInactiveInterval={}s, HttpOnly={}",
				HttpServerConf.Session_MaxAge, HttpServerConf.Session_HttpOnly);

//		sessions.setRefreshCookieAge(3600);
//		sessions.setSessionCookie("");
//		sessions.setUsingCookies(true);

		SessionCache cache = new DefaultSessionCache(sessions);
		cache.setSessionDataStore(new NullSessionDataStore());
		sessions.setSessionCache(cache);

		// 业务处理
		ServletHolder servletHolder = null;
		try {
			servletHolder = new ServletHolder((Class<? extends Servlet>) ClassUtil.loadClass("fd.ng.web.handler.WebServlet"));
		} catch (ClassNotFoundException e) {
			throw new RawlayerRuntimeException(e);
		}
		contextApp.addServlet(servletHolder, HttpServerConf.ActionPattern);
		logger.info("ActionPattern : {}", HttpServerConf.ActionPattern);

		// 用户自定义 servlet, filter, listener。子类重载这3个函数即可
		addServlet(contextApp);
		addFilter(contextApp);
		addListener(contextApp);

		// 组合多个 context
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(new Handler[] { contextRoot, contextApp });

		server.setHandler(contexts);
	}

	// ---------  子类重载后，可自定义各种 Servlet, Filter, Listener  ----------------------
	// 使用的例子，见 test 中的 customs 包
	protected void addServlet(ServletContextHandler contextApp) {

	}
	protected void addFilter(ServletContextHandler contextApp) {
	}

	protected void addListener(ServletContextHandler contextApp) {
	}
}
