package tests;

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

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;

public class PageLoadTime_WebApp_Tests {

    /**
     *
     * ==================================================
     *                      READ ME                     =
     * ==================================================
     *
     * This particular test is an example of how Performance Transactions can be done on
     * Mobile Web Applications. While example is for iOS with Safari, this is also applicable
     * for Android with Chrome.
     *
     * The particular purpose of this test is to capture the Speed Index, understanding how
     * long it took to go from one page to another with the purpose of highlighting the
     * Reports with this data.
     *
     */

    protected IOSDriver driver = null;
    protected DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
    protected WebDriverWait wait = null;

    protected Helpers helper;

    protected String speedIndex = null;

    @BeforeMethod
    public void setUp(ITestContext context, Method method) throws MalformedURLException {
        String deviceQuery = context.getCurrentXmlTest().getParameter("deviceQuery");

        desiredCapabilities.setCapability("testName", method.getName());
        desiredCapabilities.setCapability("accessKey", new PropertiesReader().getProperty("accessKey"));
        desiredCapabilities.setCapability("deviceQuery", deviceQuery);
        desiredCapabilities.setCapability("automationName", "XCUITest");
        desiredCapabilities.setBrowserName("Safari");
        desiredCapabilities.setCapability("autoAcceptAlerts", true);

        driver = new IOSDriver(new URL(new PropertiesReader().getProperty("cloudUrl")), desiredCapabilities);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        helper = new Helpers(driver);
    }

    @Test
    @Parameters({"nvProfile", "captureLevel"})
    public void login_page_load_time(String nvProfile, String captureLevel, @Optional Method method) {
        // Navigate to demo environment
        driver.navigate().to("https://demo-bank.ct.digital.ai/");

        try {
            // Start a group that will contain the individual test steps until 'endGroupingOfSteps' is called
            helper.startGroupingOfSteps(method.getName() + "_functional_steps");

            // Functional Steps to get to the point before I want to start capturing the Performance Transaction
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@class='mobile-login-logo']")));

            driver.findElement(By.xpath("(//*[@class='dx-texteditor-input'])[1]")).sendKeys("company");
            driver.findElement(By.xpath("(//*[@class='dx-texteditor-input'])[2]")).sendKeys("company");

            // Start Performance Transaction Capturing
            helper.startCapturePerformanceMetrics(nvProfile, captureLevel, "com.apple.mobilesafari");

            // Functional step(s) to perform for which Performance Metrics will be captured on
            driver.findElement(By.xpath("//*[@class='dx-button-content' and contains(text(), 'Login')]")).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@class='total-balance']")));

            // End the Performance Transaction Capturing
            String response = helper.endCapturePerformanceMetrics(method.getName());

            // Ends the group that was started by 'startGroupingOfSteps'. In the Report we can now expand a group to see a set of steps within the group
            helper.endGroupingOfSteps();

            // Extract relevant properties from the Performance Transaction Response
            String link = helper.getPropertyFromPerformanceTransactionReport(response, "link");
            String transactionId = helper.getPropertyFromPerformanceTransactionReport(response, "transactionId");

            // Waiting few seconds to allow next API call to have some time for the data to accumulate after Transaction ends
            Thread.sleep(5000);

            // Extract relevant properties from the Performance Transaction API Response
            speedIndex = helper.getPropertyFromPerformanceTransactionAPI(transactionId, "speedIndex");
            System.out.println(speedIndex);
            System.out.println(link);

            // Add a custom step to the Automated Test Results with a link reference to the Performance Transaction Report
            helper.addReportStep(link);
            helper.addReportStep("Total Time to Navigate 'Login Page > Dashboard' Page in ms: " + speedIndex);

            // Get Network related Metrics
            ArrayList<String> metrics = helper.extractHARFileMetrics(transactionId, method.getName());

            // Add Network related Metrics to Functional Test Report
            for (String metric : metrics) {
                System.out.println(metric);
                helper.addReportStep(metric);
            }

        } catch (Exception e) {
            System.out.println("Something went wrong in the script for Test: '" + method.getName() + "'");
            e.printStackTrace();
        }

    }

    @AfterMethod(alwaysRun = true)
    @Parameters({"nvProfile", "captureLevel"})
    public void tearDown(String nvProfile, String captureLevel, @Optional ITestContext context) {
        try {
            // Add custom properties that allow for easier filtering for the Automated Test Results
            helper.addPropertyForReporting("nvProfile", nvProfile);
            helper.addPropertyForReporting("captureLevel", captureLevel);
            helper.addPropertyForReporting("category", "performance");
        } catch (Exception e) {
            e.printStackTrace();
        }

        driver.quit();
    }

}
