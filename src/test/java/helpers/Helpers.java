package helpers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.appium.java_client.AppiumDriver;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Helpers {

    protected AppiumDriver driver;

    public Helpers(AppiumDriver driver) {
        this.driver = driver;
    }

    public void startCapturePerformanceMetrics(String nvProfile, String captureLevel, String applicationName) {
        try {
            if (captureLevel.equalsIgnoreCase("Device")) {
                driver.executeScript("seetest:client.startPerformanceTransaction(\"" + nvProfile + "\")");
            } else if (captureLevel.equalsIgnoreCase("Application")) {
                driver.executeScript("seetest:client.startPerformanceTransactionForApplication(\"" + applicationName + "\", \"" + nvProfile + "\")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not start Capturing. Accepted Values: [Device, Application]");
        }
    }

    public String endCapturePerformanceMetrics(String transactionName) {
        Object transaction = driver.executeScript("seetest:client.endPerformanceTransaction(\"" + transactionName + "\")");
        System.out.println("Transaction Information: " + transaction.toString());
        return transaction.toString();
    }

    // Properties that can be fetched:
    // transactionName / transactionId / appName / appVersion / link (Link to Performance Transaction Report)
    public String getPropertyFromPerformanceTransactionReport(String response, String property) {
        // Remove the prefix
        String dataString = response.replace("Transaction Information: ", "").trim();
        dataString = dataString.substring(1, dataString.length() - 1); // Remove the outer braces

        Map<String, Object> transactionMap = new HashMap<>();

        // Split the main sections
        String[] pairs = dataString.split(",\\s*(?=\\w+=)"); // Match key-value pairs
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2); // Split on the first '='
            String key = keyValue[0].trim();
            String value = keyValue.length > 1 ? keyValue[1].trim() : null;

            // Handle nested objects or arrays
            if (value != null && value.startsWith("{")) {
                // For simplicity, store the nested data as a string
                transactionMap.put(key, value);
            } else {
                // Handle nulls and other values
                transactionMap.put(key, value != null && value.equals("null") ? null : value);
            }
        }

        return (String) transactionMap.get(property);
    }

    // Properties that can be fetched:
    // networkProfile / cpuAvg / cpuMax / cpuCoreCount / memAvg / memMax / memTotalInBytes / batteryAvg / batteryMax / duration / speedIndex
    public String getPropertyFromPerformanceTransactionAPI(String transactionId, String property) throws UnirestException {
        HttpResponse<String> response = Unirest.get(new PropertiesReader().getProperty("urlForAPIs") + "/reporter/api/transactions/" + transactionId)
//                .header("Authorization", "Bearer " + System.getenv("ACCESS_KEY"))
                .header("Authorization", "Bearer " + new PropertiesReader().getProperty("accessKey"))
                .asString();

        String responseBody = response.getBody();
        JSONObject jsonObject = new JSONObject(responseBody);

        if (jsonObject.get(property) instanceof String) {
            property = jsonObject.getString(property);
        } else {
            property = jsonObject.get(property).toString();
        }
        return property;
    }

    // This method helps to extract metrics from the HAR file, which can help us understand various metrics
    // such as "How many network calls were made during the Transaction".
    // This method can be expanded further with parameterization to retrieve other metrics
    public ArrayList<String> extractHARFileMetrics(String transactionId, String fileName) throws IOException, URISyntaxException {
        // Getting .json file from local directory
        File jsonFile = downloadHARFileFromPerformanceTransaction(transactionId, fileName + "_" + getCurrentDateAndTime());

        ArrayList<String> metrics = new ArrayList<>();
        int numberOfNetworkCalls = 0;
        double totalTimeTakenForNetworkCallsInSeconds = 0;

        try {
            String json = new String(Files.readAllBytes(Paths.get(String.valueOf(jsonFile))), StandardCharsets.UTF_8);

            // Gets "entries" array from .json file, since information like request time is contained within "entries"
            JSONObject jsonObj = new JSONObject(json);
            JSONArray entriesArr = jsonObj.getJSONObject("log").getJSONArray("entries");
            numberOfNetworkCalls = entriesArr.length();

            long totalTimeMs = 0;

            // Finds all the "time" objects from "entries"
            for (int i = 0; i < numberOfNetworkCalls; i++) {
                JSONObject entryObj = entriesArr.getJSONObject(i);
                long time = entryObj.getLong("time");
                totalTimeMs += time;
            }

            // Since "time" object is in Milliseconds, converting the total value accumulated to Seconds
            totalTimeTakenForNetworkCallsInSeconds = (double) totalTimeMs / 1000;

            metrics.add("Number of Network Calls made: " + numberOfNetworkCalls);
            metrics.add("Total time taken for all Network Calls in Seconds: " + totalTimeTakenForNetworkCallsInSeconds);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return metrics;
    }

    // This method is used to download HAR file from individual Performance Transactions
    public File downloadHARFileFromPerformanceTransaction(String transactionId, String fileName) throws IOException, URISyntaxException {

        // Calls API to get HAR file
        URI uri = new URIBuilder(new PropertiesReader().getProperty("urlForAPIs") + "/reporter/api/transactions/" + transactionId + "/har")
//                .setParameter("token", System.getenv("ACCESS_KEY"))
                .setParameter("token", new PropertiesReader().getProperty("accessKey"))
                .build();

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(uri);
        org.apache.http.HttpResponse response = httpClient.execute(httpGet);

        // HAR file is saved to local folder
        File harFile = new File(System.getProperty("user.dir") + "/har_files/" + fileName + ".har");
        FileOutputStream outputStream = new FileOutputStream(harFile);
        response.getEntity().writeTo(outputStream);
        outputStream.close();

        return harFile;
    }

    public void setReportStatus(String status, String message) {
        driver.executeScript("seetest:client.setReportStatus(\"" + status + "\", \"" + status + "\", \"" + message + "\")");
    }

    public void addReportStep(String input) {
        driver.executeScript("seetest:client.report", input, "true");
    }

    public void addReportStep(String input, String status) {
        driver.executeScript("seetest:client.report(\"" + input + "\", " + status + ")");
    }

    public void addPropertyForReporting(String property, String value) {
        driver.executeScript("seetest:client.addTestProperty(\"" + property + "\", \"" + value + "\")");
    }

    public void startGroupingOfSteps(String testName) {
        driver.executeScript("seetest:client.startStepsGroup(\"" + testName + "\")");
    }

    public void endGroupingOfSteps() {
        driver.executeScript("seetest:client.stopStepsGroup()");
    }

    public String getCurrentDateAndTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
        String currentDateAndTime = now.format(formatter);
        return currentDateAndTime;
    }

}
