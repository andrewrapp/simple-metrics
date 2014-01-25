package com.rapplogic.simplemetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.Logger;

public class MetricsScheduler implements Runnable {

	private final static Logger log = Logger.getLogger(MetricsScheduler.class);
	
	private List<SimpleMetric> simpleMetrics = new ArrayList<SimpleMetric>();
	
	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
	
	private long tick;
		
	// computes count per time period
	public MetricsScheduler(List<SimpleMetric> simpleMetrics) {
		
		// sort by highest frequency (lowest millisecond) first
		Collections.sort(simpleMetrics, new Comparator<SimpleMetric>() {
			@Override
			public int compare(SimpleMetric o1, SimpleMetric o2) {
				if (o1.getMillisecondFrequency() < o2.getMillisecondFrequency()) {
					return -1;
				} else if (o1.getMillisecondFrequency() > o2.getMillisecondFrequency()) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		
		// verify they are all divisible by the smallest
		if (simpleMetrics.size() > 1) {						
			for (int i = 1; i < simpleMetrics.size(); i++) {
				if (simpleMetrics.get(i).getMillisecondFrequency() % simpleMetrics.get(0).getMillisecondFrequency() != 0) {
					throw new IllegalArgumentException("Update frequency [" + simpleMetrics.get(i).getMillisecondFrequency() + "] is not divisable by smallest [" + simpleMetrics.get(0).getMillisecondFrequency() + "]");
				}
			}			
		}
		
		this.simpleMetrics = simpleMetrics;
		
		// initialize
		this.initialize();
		
		// create scheduler for highest frequency
		// this should never drift since it's fixed rate scheduler
		scheduledExecutorService.scheduleAtFixedRate(this, simpleMetrics.get(0).getUpdateFrequency(), simpleMetrics.get(0).getUpdateFrequency(), simpleMetrics.get(0).getUpdateFrequencyUnit());	
	}
	
	void initialize() {
		for (SimpleMetric simpleMetric : simpleMetrics) {
			simpleMetric.tick();
		}
	}
	
	@Override
	public void run() {
		try {
			tick++;
			
			simpleMetrics.get(0).tick();
			
			// each tick represents the lowest updateFrequency in milliseconds
			for (int i = 1; i < simpleMetrics.size(); i++) {
				if (tick *  simpleMetrics.get(0).getMillisecondFrequency() % simpleMetrics.get(i).getMillisecondFrequency() == 0) {					
					simpleMetrics.get(i).tick();
					
					// if last reset
					if (i == simpleMetrics.size() - 1) {
						tick = 0;
					}
				}
			}			
		} catch (Throwable t) {
			log.error("Unexpected error processing tick", t);
		}
	}
}
