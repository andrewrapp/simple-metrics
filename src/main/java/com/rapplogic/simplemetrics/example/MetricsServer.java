package com.rapplogic.simplemetrics.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.rapplogic.simplemetrics.MetricsScheduler;
import com.rapplogic.simplemetrics.SimpleMetric;
import com.rapplogic.simplemetrics.SimpleMetric.GetCount;

public class MetricsServer {

	private Server server = new Server(8090);

    final AtomicLong counter = new AtomicLong();

	public static void main(String[] args) throws Exception {
		new MetricsServer().startJetty();
	}
	
	public void startJetty() throws Exception {

		PropertyConfigurator.configure("log4j.properties");
		
		final SimpleMetric.GetCount getCount = new GetCount() {
			@Override
			public long getCount() {
				return counter.get();
			}
		};
	    
		final List<SimpleMetric> simpleMetrics = new ArrayList<SimpleMetric>();
		
		simpleMetrics.add(new SimpleMetric(10, TimeUnit.SECONDS, 500, TimeUnit.MILLISECONDS, getCount));
		simpleMetrics.add(new SimpleMetric(1, TimeUnit.MINUTES, 1, TimeUnit.SECONDS, getCount));
		simpleMetrics.add(new SimpleMetric(5, TimeUnit.MINUTES, 5, TimeUnit.SECONDS, getCount));
		simpleMetrics.add(new SimpleMetric(30, TimeUnit.MINUTES, 30, TimeUnit.SECONDS, getCount));
		simpleMetrics.add(new SimpleMetric(1, TimeUnit.HOURS, 90, TimeUnit.SECONDS, getCount));
		simpleMetrics.add(new SimpleMetric(7, TimeUnit.DAYS, 1, TimeUnit.HOURS, getCount));
		
		new MetricsScheduler(simpleMetrics);
		
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new HttpServlet() {
    		@Override
    		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,IOException {
    			resp.getWriter().println("<html><body><div>" + counter.getAndIncrement() + "</div></body></html>");
    		}	        	
		}),"/hit");                

        context.addServlet(new ServletHolder(new HttpServlet() {
    		@Override
    		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,IOException {
    			
    			resp.getWriter().println("<html><body>");
    			
    			for (SimpleMetric simpleMetric : simpleMetrics) {    				
    				if (simpleMetric.isFull()) {
    					resp.getWriter().println("<div>There have been " + simpleMetric.getWindowCount() + " hits in the last " + simpleMetric.getWindow() + " " + simpleMetric.getWindowUnit() + ". This metric is updated every " + simpleMetric.getUpdateFrequency() + " " + simpleMetric.getUpdateFrequencyUnit() + "</div>");	
    				} else {
    					resp.getWriter().println("<div>Metric window is not full yet. Metric window is " +  simpleMetric.getWindow() + " " + simpleMetric.getWindowUnit() + ", updated every " + simpleMetric.getUpdateFrequency() + " " + simpleMetric.getUpdateFrequencyUnit() + ", It is " + 100*((float)(simpleMetric.getWheelIndex() + 1) / simpleMetric.getWheelSize()) + "% full, current count is " + simpleMetric.getCurrentCount() + "</div>");
    				}   				
    			}
    			
    			resp.getWriter().println("</body></html>");
    		}	        	
		}),"/metrics");     
        
        server.start();
        server.join();
	}
}