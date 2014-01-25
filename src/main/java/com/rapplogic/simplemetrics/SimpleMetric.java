package com.rapplogic.simplemetrics;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class SimpleMetric {

	private final static Logger log = Logger.getLogger(SimpleMetric.class);
	
	private volatile Long[] wheel;
	private volatile int tick;
	private GetCount count;
	private int window;
	private TimeUnit windowUnit;
	private int updateFrequency;
	private TimeUnit updateFrequencyUnit;
	private long millisecondFrequency;
	private Long min;
	private Long max;
	
	// NOTE: For large time windows (e.g. day, weeks, month etc.) it wouldn't be appropriate to keep stats in memory, in most cases. Intead we'd need to persist to disk to survive restarts 
	
	public SimpleMetric(int window, TimeUnit windowUnit, int updateFrequency, TimeUnit updateFrequencyUnit, GetCount getCount) {		
		
		if (updateFrequencyUnit == TimeUnit.MICROSECONDS || updateFrequencyUnit == TimeUnit.NANOSECONDS) {
			throw new IllegalArgumentException("Microsecond and nanosecond units are not supported -- use milliseconds");
		}
		
		long converted = updateFrequencyUnit.convert(window, windowUnit);
		// how many ticks in wheel
		
		if (converted % updateFrequency != 0) {
			throw new IllegalArgumentException(
					"Window time period must be divisable by the update frequency time period. Specifically " + converted + " is not divisable by " + updateFrequency + ", remainder is " + converted % updateFrequency);
		}
		
		if (updateFrequency == converted) {
			throw new IllegalArgumentException("The update frequency cannot be the same as the window");			
		}
		
		int ticks = (int) (converted / updateFrequency);
		wheel = new Long[ticks];
		
		log.debug("Creating a wheel with resolution of " + ticks + " ticks, window of " + window + " " + windowUnit + ", that is updated every " + updateFrequency + " " + updateFrequencyUnit + "");
	
		this.count = getCount;
		this.window = window;
		this.windowUnit = windowUnit;
		this.updateFrequency = updateFrequency;
		this.updateFrequencyUnit = updateFrequencyUnit;
		
		millisecondFrequency = TimeUnit.MILLISECONDS.convert(updateFrequency, updateFrequencyUnit);
	}
	
	// this method must be called on the update frequency
	void tick() {
		if (tick == wheel.length - 1) {
			// flip
			tick = 0;
		} else if (wheel[tick] != null) {
			tick++;
		}
		
		wheel[tick] = count.getCount();
		
		if (this.isFull()) {
			Long windowCount = this.getWindowCount();
			if (min == null || windowCount < min) {
				min = windowCount;
			}
			
			if (max == null || windowCount > max) {
				max = windowCount;
			}
		}
		
		log.debug("tick is " + this);
	}
	
	public boolean isFull() {
		return wheel[wheel.length - 1] != null;
	}
	
	public long getWheelIndex() {
		return tick;
	}
	
	public Long getCurrentCount() {
		if (wheel != null) {
			return wheel[tick];	
		}
		
		return null;
	}
	
	public long getWheelSize() {
		return wheel.length;
	}
	
	public Long getMin() {
		return min;
	}

	public Long getMax() {
		return max;
	}

	public Long getWindowCount() {
		if (!this.isFull()) {
			// wheel is not full yet. we could do some math but for now we return 0 until wheel is full
			return null;
		}
		
		// tick can move while we compute delta so save tick in local var
		int now = tick;
		
		if (now == wheel.length - 1) {
			// at end of wheel
			return wheel[now] - wheel[0];
		}
		
		// return the delta between the current and oldest count
		return wheel[now] - wheel[now + 1];
	}

	public int getWindow() {
		return window;
	}

	public TimeUnit getWindowUnit() {
		return windowUnit;
	}

	public int getUpdateFrequency() {
		return updateFrequency;
	}

	public TimeUnit getUpdateFrequencyUnit() {
		return updateFrequencyUnit;
	}

	GetCount getGetCount() {
		return count;
	}

	public long getMillisecondFrequency() {
		return millisecondFrequency;
	}

	public interface GetCount {
		long getCount();
	}
	
	@Override
	public String toString() {
		return "SimpleMetric [size=" + wheel.length + ", position=" + tick + ", windowcount=" + this.getWindowCount() + ", min=" + min + ", max= " + max + ", isFull=" + this.isFull() + ", window=" + window + ", windowUnit=" + windowUnit + ", updateFrequency=" + updateFrequency + ", updateFrequencyUnit="
				+ updateFrequencyUnit + "]"; // , wheel=" + Arrays.asList(wheel) + "
	}
}
