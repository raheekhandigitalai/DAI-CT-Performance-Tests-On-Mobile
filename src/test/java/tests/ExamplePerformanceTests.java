package tests;

import com.mashape.unirest.http.exceptions.UnirestException;
import helpers.Helpers;
import helpers.PropertiesReader;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class ExamplePerformanceTests {

    protected IOSDriver driver = null;
    protected DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
    protected WebDriverWait wait;
    protected Helpers helper;

    @BeforeMethod
    public void setUp(Method method) throws MalformedURLException {
        desiredCapabilities.setCapability("testName", method.getName());
        desiredCapabilities.setCapability("accessKey", new PropertiesReader().getProperty("accessKey"));
        desiredCapabilities.setCapability("deviceQuery", "@os='ios' and @category='PHONE'");
        desiredCapabilities.setCapability("automationName", "XCUITest");
        desiredCapabilities.setCapability("app", "cloud:com.experitest.ExperiBank");
        desiredCapabilities.setCapability("bundleId", "com.experitest.ExperiBank");

        driver = new IOSDriver(new URL(new PropertiesReader().getProperty("cloudUrl")), desiredCapabilities);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        helper = new Helpers(driver);
    }

    @Test
    public void test_login_response_time(Method method) throws UnirestException {
        // Creates a group of steps within the Functional Report
        helper.startGroupingOfSteps(method.getName() + " - GROUP");

        // Wait until element becomes clickable
        wait.until(ExpectedConditions.elementToBeClickable(By.name("usernameTextField")));

        // Functional steps to enter credentials onto username and password fields
        driver.findElement(By.name("usernameTextField")).sendKeys("company");
        driver.findElement(By.name("passwordTextField")).sendKeys("company");

        // Start Performance Transaction Capturing
        helper.startCapturePerformanceMetrics("4G-average", "Device", "com.experitest.ExperiBank");

        // Functional step(s) to perform for which to capture Performance Metrics for
        driver.findElement(By.name("loginButton")).click();

        // Validate user landed on the Login page
        wait.until(ExpectedConditions.elementToBeClickable(By.name("Make Payment")));

        // End the Performance Transaction Capturing
        String response = helper.endCapturePerformanceMetrics(method.getName());

        // Stops the grouping of steps
        helper.endGroupingOfSteps();

        // Extract values from the response object after a Performance Transaction Ends
        String transactionId = helper.getPropertyFromPerformanceTransactionReport(response, "transactionId");
        String link = helper.getPropertyFromPerformanceTransactionReport(response, "link");

        // Extract values using API from the response after a Performance Transaction Ends
        String networkProfile = helper.getPropertyFromPerformanceTransactionAPI(transactionId, "networkProfile");
        String speedIndex = helper.getPropertyFromPerformanceTransactionAPI(transactionId, "speedIndex");

        // Assert against conditions that is relevant for the scenario. In this case, Speed Index
        SoftAssert softAssert = new SoftAssert();
        int speedIndexValue = Integer.parseInt(speedIndex);

        if (speedIndexValue < 2000) {
            // Speed index is acceptable, marking the step as passed in Digital.ai's Platform
            helper.addReportStep("Speed Index Captured is within accepted range: " + speedIndex, "true");
        } else {
            // Speed index is too high, marking the step and the entire test as failed in Digital.ai's Platform
            helper.addReportStep("Speed Index Captured is too high: " + speedIndex, "false");
            softAssert.fail("Speed Index is greater than 2 seconds: " + speedIndex);
        }

        softAssert.assertAll();

        // Adding custom steps to the report
        helper.addReportStep("Using Network Profile: " + networkProfile);
        helper.addReportStep(link);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.isSuccess()) {
            helper.setReportStatus("Passed", "Test Passed");
        } else {
            helper.setReportStatus("Failed", "Test Failed");
        }
        driver.quit();
    }

}