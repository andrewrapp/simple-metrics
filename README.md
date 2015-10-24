simple-metrics
==============

Computes how many times a counter was incremented in the last time window y, updated every z time unit

Usage:

Create a counter and provide access to it via GetCount interface. Increment the counter to track whatever your
interested in (e.g. requests, db queries etc). The counter must only be incremented over time and never decremented.

```java
	final AtomicLong counter = new AtomicLong();

	final SimpleMetric.GetCount getCount = new GetCount() {
		@Override
		public long getCount() {
			return counter.get();
		}
	};
```		
		
Create a few metrics

```java
	final List<SimpleMetric> simpleMetrics = new ArrayList<SimpleMetric>();

	// tracks count in last 10 seconds, updated every 500 milliseconds
	simpleMetrics.add(new SimpleMetric(10, TimeUnit.SECONDS, 500, TimeUnit.MILLISECONDS, getCount));
	// track count in last minute, updated every second
	simpleMetrics.add(new SimpleMetric(1, TimeUnit.MINUTES, 1, TimeUnit.SECONDS, getCount));
	// tracks count in last hour, updated every 90 seconds
	simpleMetrics.add(new SimpleMetric(1, TimeUnit.HOURS, 90, TimeUnit.SECONDS, getCount));
```

Start the scheduler

```java
	new MetricsScheduler(simpleMetrics);
```		
		
Print some metrics

```java
	for (SimpleMetric simpleMetric : simpleMetrics) {    				
		System.out.println("There have been " + simpleMetric.getWindowCount() + " hits in the last " + simpleMetric.getWindow() + " " + simpleMetric.getWindowUnit() + ". This metric is updated every " + simpleMetric.getUpdateFrequency() + " " + simpleMetric.getUpdateFrequencyUnit());
	}
```	
	
	
I've included a demo (MetricsServer.java) that starts an embedded Jetty server. Increment the counter with http://localhost:8090/hit and view metrics with http://localhost:8090/metrics

Sample output:

There have been 3 hits in the last 10 SECONDS. This metric is updated every 500 MILLISECONDS

There have been 13 hits in the last 1 MINUTES. This metric is updated every 1 SECONDS

Metric window is not full yet. Metric window is 5 MINUTES, updated every 5 SECONDS, It is 25.0% full, current count is 16

Each metric is represented as a array  with size equal to window / update frequency (e.g. if 5 minute window with 1s frequency, size is 300). Of course as the window increases and update frequency decreases, more memory will be consumed, so choose accordingly.

This was purely a programming exercise, there are no tests, use accordingly
