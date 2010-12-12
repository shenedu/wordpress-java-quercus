package com.butterfly.filter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

/**
 * 
 * @author feng
 * @version 1.0, 2010/7/19
 * 
 */
public class ExpireHeaderFilter implements Filter {
	private List<ExpirePatternAndTime> configs;
	private DateFormat formater;

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response,
	        FilterChain chain) throws IOException, ServletException {
		HttpServletRequest requ = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		String uri = requ.getRequestURI();

		boolean flag = false;
		for (ExpirePatternAndTime config : configs) {
			if (config.pattern.matcher(uri).find()) {
				flag = true;
				Calendar c = GregorianCalendar.getInstance();
				c.add(Calendar.DAY_OF_YEAR, config.futureTimeInDays);
				resp.setHeader("Expires", formater.format(c.getTime()));
				resp.setHeader("Cache-Control", "public, max-age="
				        + TimeUnit.DAYS.toMillis(config.futureTimeInDays));
				break;
			}
		}

		if (flag == false) {
			resp.addHeader("Cache-Control", "private, max-age=0");
		}

		chain.doFilter(request, response);
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		formater = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
		        Locale.US);
		formater.setTimeZone(TimeZone.getTimeZone("GMT"));

		String configpath = filterConfig.getInitParameter("configpath");
		configpath = configpath == null ? "expireheader.properties"
		        : configpath;

		try {
			List<String> lines = IOUtils.readLines(ExpireHeaderFilter.class
			        .getClassLoader().getResourceAsStream(configpath));
			List<ExpirePatternAndTime> list = new ArrayList<ExpirePatternAndTime>();
			for (String config : lines) {
				// ignore empty line and comment line
				if (config.length() > 0 && !config.startsWith("#")) {
					String[] tmp = config.split("\t");
					ExpirePatternAndTime patternAndTime = new ExpirePatternAndTime(
					        Integer.parseInt(tmp[1]), Pattern.compile(tmp[0]));
					list.add(patternAndTime);
				} else {
				}
			}
			this.configs = list;

		} catch (Exception e) {

		}
	}
}

class ExpirePatternAndTime {
	final int futureTimeInDays;
	final Pattern pattern;

	public ExpirePatternAndTime(int futureTimeInDays, Pattern pattern) {
		this.futureTimeInDays = futureTimeInDays;
		this.pattern = pattern;
	}

}
