simple-metrics
==============

Computes how many n (e.g. hits) in the last time window y, updated every z time unit

Usage:

Create a counter and provide access to it via GetCount interface. Increment the counter to track whatever your
interested in (e.g. requests, db queries etc). The counter must only be incremented over time and never decremented.

	final AtomicLong counter = new AtomicLong();

	final SimpleMetric.GetCount getCount = new GetCount() {
		@Override
		public long getCount() {
			return counter.get();
		}
	};
		
		
Create a few metrics

	final List<SimpleMetric> simpleMetrics = new ArrayList<SimpleMetric>();

	simpleMetrics.add(new SimpleMetric(10, TimeUnit.SECONDS, 500, TimeUnit.MILLISECONDS, getCount));
	simpleMetrics.add(new SimpleMetric(1, TimeUnit.MINUTES, 1, TimeUnit.SECONDS, getCount));
	simpleMetrics.add(new SimpleMetric(5, TimeUnit.MINUTES, 5, TimeUnit.SECONDS, getCount));
	simpleMetrics.add(new SimpleMetric(30, TimeUnit.MINUTES, 30, TimeUnit.SECONDS, getCount));
	simpleMetrics.add(new SimpleMetric(1, TimeUnit.HOURS, 90, TimeUnit.SECONDS, getCount));
	simpleMetrics.add(new SimpleMetric(7, TimeUnit.DAYS, 1, TimeUnit.HOURS, getCount));
	
Start the scheduler

	new MetricsScheduler(simpleMetrics);
		
		
Now print metrics

	for (SimpleMetric simpleMetric : simpleMetrics) {    				
		System.out.println("There have been " + simpleMetric.getWindowCount() + " hits in the last " + simpleMetric.getWindow() + " " + simpleMetric.getWindowUnit() + ". This metric is updated every " + simpleMetric.getUpdateFrequency() + " " + simpleMetric.getUpdateFrequencyUnit());
	}
	
	
See MetricsServer.java for a detailed example
