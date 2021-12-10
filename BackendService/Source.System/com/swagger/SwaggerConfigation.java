package com.swagger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.configuration.ConfigurationMenu;
import com.initialization.Global;
import com.system.information.SystemProtocol;

import io.swagger.jaxrs.config.BeanConfig;

public class SwaggerConfigation extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		BeanConfig beanConfig = new BeanConfig();
		beanConfig.setBasePath("/" + Global.getConfig.getConfigValue(ConfigurationMenu.projectName) + "/rest");
		beanConfig.setHost("localhost:" + SystemProtocol.getIPv4Port());
		beanConfig.setResourcePackage("com.api");
		beanConfig.setPrettyPrint(true);
		beanConfig.setScan(true);
//		beanConfig.setSchemes(new String[] { "http" });
		beanConfig.setVersion("1.0");
	}
}