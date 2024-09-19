# DAI-CT-Performance-Tests-on-Mobile-Sample

### Prerequisites

Provide your Cloud URL and Access Key in ```config.properties``` file.

[Obtain your Access Key](https://docs.digital.ai/bundle/TE/page/obtaining_access_key.html)

### Overview

This repository provides a practical example on how you can start to capture Performance Transactions from your Functional Appium Scripts.

The example we will use in this repository is from ```src/test/tests/ExamplePerformanceTests.java```

We start to capture the Performance Transaction in the following way:

```agsl
// Start Performance Transaction Capturing
helper.startCapturePerformanceMetrics("4G-average", "Device", "com.experitest.ExperiBank");
```

The first parameter "4G-average" represents the Network Profile we want to use to simulate a different network condition.

The second parameter "Device" is whether we want to capture the metrics (CPU, Memory, Battery, Network) from the Device OS level, or Application level. If only on Application level, changing the value to "Application".

The third parameter "com.experitest.ExperiBank" is which Application in context we want to capture the metrics for. In this case, it is against the Native Application we are testing against. 

We end the capturong of Performance Metrics with the following line:

```agsl
// End the Performance Transaction Capturing
String response = helper.endCapturePerformanceMetrics(method.getName());
```

With the response, we can now extract values from it with the following function:

```agsl
// Accepted values for 2nd parameter: transactionName / transactionId / appName / appVersion / link (Link to Performance Transaction Report)
helper.getPropertyFromPerformanceTransactionReport(response, "value");
```

If we want to go further, we can also use the following function to extract values using Rest API:

```agsl
// Accepted values for 2nd parameter: // networkProfile / cpuAvg / cpuMax / cpuCoreCount / memAvg / memMax / memTotalInBytes / batteryAvg / batteryMax / duration / speedIndex
helper.getPropertyFromPerformanceTransactionAPI(transactionId, "value");
```

With this level of extraction and modification to the script, we end up with a Functional Report like this:

![FunctionalTestReport.png](images%2FFunctionalTestReport.png)

And you can still go to the individual Performance Transaction with the Link attached within the Report:

![PerformanceTransactionReport.png](images%2FPerformanceTransactionReport.png)