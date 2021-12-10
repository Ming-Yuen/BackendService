package com.system.information;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;

public class SystemProtocol {

	public static boolean IPv4Process = false;
	public static boolean IPv6Process = false;

	public static String IPv4Scheme;
	public static String IPv6Scheme;

	public static String IPv4Host;
	public static String IPv6Host;

	public static String IPv4Port;
	public static String IPv6Port;

	public static void init() throws Exception {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			QueryExp subQuery1 = Query.match(Query.attr("protocol"), Query.value("HTTP/1.1"));
			QueryExp subQuery2 = Query.anySubString(Query.attr("protocol"), Query.value("Http11"));
			QueryExp query = Query.or(subQuery1, subQuery2);
			Set<ObjectName> objs = mbs.queryNames(new ObjectName("*:type=Connector,*"), query);
			String hostname = InetAddress.getLocalHost().getHostName();
			InetAddress[] addresses = InetAddress.getAllByName(hostname);
			for (Iterator<ObjectName> i = objs.iterator(); i.hasNext();) {
				ObjectName obj = i.next();
				String scheme = mbs.getAttribute(obj, "scheme").toString();
				String port = obj.getKeyProperty("port");
				for (InetAddress addr : addresses) {
					if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isMulticastAddress()) {
						continue;
					}
					String host = addr.getHostAddress();
					if (!IPv4Process) {
						IPv4Scheme = scheme;
						IPv4Host = host;
						IPv4Port = port;
					} else if (!IPv6Process) {
						IPv6Scheme = scheme;
						IPv6Host = host;
						IPv6Port = port;
					}
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static boolean isIPv4Process() {
		return IPv4Process;
	}

	public static boolean isIPv6Process() {
		return IPv6Process;
	}

	public static String getIPv4Scheme() {
		return IPv4Scheme;
	}

	public static String getIPv6Scheme() {
		return IPv6Scheme;
	}

	public static String getIPv4Host() {
		return IPv4Host;
	}

	public static String getIPv6Host() {
		return IPv6Host;
	}

	public static String getIPv4Port() {
		return IPv4Port;
	}

	public static String getIPv6Port() {
		return IPv6Port;
	}
}
