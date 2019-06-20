import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class TestLink {
    private static final Logger logger = LogManager.getLogger(TestLink.class);
    private static String username = "user";
    private static String password = "bitnami";
    private static final String HOST = "http://localhost";
    private static String projectName;
    private static String testPlanName;
    private static String buildName;
    private static String testSuiteName;
    private static String testCaseName;

    private static WebDriver driver;
    private static final long TIMEOUT = 2;

    @BeforeClass
    public static void generalSetup(){
        WebDriverManager.chromedriver().setup();

        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        driver.navigate().to(HOST);
        login(username, password);
    }

    @After
    public void returnToStartingPoint(){
        openHomepage();
    }

    @AfterClass
    public static void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void specifiedAttributesDuringCreationAreAppliedToCreatedProject(){
        openNavigationSection("Test Project Management");

        switchContextToMainFrame();
        driver.findElement(By.cssSelector("#create")).click();

        String projectName = "Project_" + getTimeStamp();
        driver.findElement(By.cssSelector("input[name='tprojectName']")).sendKeys(projectName);

        String casePrefix = "P_" + getTimeStamp();
        driver.findElement(By.cssSelector("input[name='tcasePrefix']")).sendKeys(casePrefix);

        WebElement descriptionFrame = driver.findElement(By.cssSelector("iframe[class='cke_wysiwyg_frame cke_reset']"));
        driver.switchTo().frame(descriptionFrame);

        WebElement body = driver.findElement(By.cssSelector("body"));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String projectDescription = "Basic TestLink Project";
        String scriptToExecute = "arguments[0].innerHTML = '<p>" + projectDescription + "</p>'";
        js.executeScript(scriptToExecute, body);
        driver.switchTo().parentFrame();

        driver.findElement(By.cssSelector("input[name='optReq']")).click();

        driver.findElement(By.cssSelector("input[value='Create']")).click();

        driver.findElement(By.cssSelector("input[type='Search']")).sendKeys(projectName);

        WebElement projectsTable = driver.findElement(By.cssSelector("table#item_view tbody"));
        WebElement projectNameColumn = projectsTable.findElement(By.xpath("./tr/td[1]/a"));
        String projectNameColumnText = projectNameColumn.getAttribute("innerText");
        Assert.assertEquals(projectName, projectNameColumnText);

        WebElement projectDescriptionColumn = projectsTable.findElement(By.xpath("./tr/td[2]/p"));
        String projectDescriptionColumnText = projectDescriptionColumn.getAttribute("innerText");
        Assert.assertEquals(projectDescription, projectDescriptionColumnText);

        WebElement prefixColumn = projectsTable.findElement(By.xpath("./tr/td[3]"));
        String prefixColumnText = prefixColumn.getAttribute("innerText");
        Assert.assertEquals(casePrefix, prefixColumnText);

        WebElement requirementColumn = projectsTable.findElement(By.xpath("./tr/td[6]/input"));
        Assert.assertTrue(requirementColumn.getAttribute("title").contains("Enabled"));

        WebElement activeColumn = projectsTable.findElement(By.xpath("./tr/td[7]/input"));
        Assert.assertTrue(activeColumn.getAttribute("title").contains("Active"));

        WebElement publicColumn = projectsTable.findElement(By.xpath("./tr/td[8]/img"));
        Assert.assertTrue(publicColumn.getAttribute("title").contains("Public"));
        driver.switchTo().defaultContent();
    }

    @Test
    public void testExecutionResultIsRepresentedWithBackgroundColor(){
        createBasicProject();
        selectTestProjectInDropdown(projectName);
        createBasicTestPlan();
        createBasicBuild();
        createTestSuite();
        createTestCaseWithinSuite(testSuiteName);
        addTestCaseToTestPlan();

        executeTest(testSuiteName, testCaseName, TestCaseOutcome.NOT_RUN);
        switchContextToWorkFrame();
        String rgbaColorBackground = driver.findElement(By.cssSelector("div.not_run")).getCssValue("background-color");
        String hexColorBackground = convertRGBtoHEX(rgbaColorBackground);
        Assert.assertEquals("#000000", hexColorBackground);
        driver.switchTo().defaultContent();

        executeTest(testSuiteName, testCaseName, TestCaseOutcome.PASSED);
        switchContextToWorkFrame();
        rgbaColorBackground = driver.findElement(By.cssSelector("div.passed")).getCssValue("background-color");
        hexColorBackground = convertRGBtoHEX(rgbaColorBackground);
        Assert.assertEquals("#006400", hexColorBackground);
        driver.switchTo().defaultContent();

        switchContextToTreeFrame();
        String testCaseInTreeXPath = String.format("//span[contains(text(), '%s')]", testCaseName);
        rgbaColorBackground = driver.findElement(By.xpath(testCaseInTreeXPath)).getCssValue("background-color");
        hexColorBackground = convertRGBtoHEX(rgbaColorBackground);
        Assert.assertEquals("#D5EED5", hexColorBackground);
        driver.switchTo().defaultContent();

        executeTest(testSuiteName, testCaseName, TestCaseOutcome.FAILED);
        switchContextToWorkFrame();
        rgbaColorBackground = driver.findElement(By.cssSelector("div.failed")).getCssValue("background-color");
        hexColorBackground = convertRGBtoHEX(rgbaColorBackground);
        Assert.assertEquals("#B22222", hexColorBackground);
        driver.switchTo().defaultContent();

        switchContextToTreeFrame();
        testCaseInTreeXPath = String.format("//span[contains(text(), '%s')]", testCaseName);
        rgbaColorBackground = driver.findElement(By.xpath(testCaseInTreeXPath)).getCssValue("background-color");
        hexColorBackground = convertRGBtoHEX(rgbaColorBackground);
        Assert.assertEquals("#EED5D5", hexColorBackground);
        driver.switchTo().defaultContent();
    }

    private static void login(String username, String password) {
        driver.findElement(By.cssSelector("#tl_login")).sendKeys(username);
        driver.findElement(By.cssSelector("#tl_password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type='Submit']")).click();
    }

    private static void openNavigationSection(String sectionName){
        switchContextToMainFrame();

        String sectionXPath = String.format("//a[contains(text(), '%s')]", sectionName);
        driver.findElement(By.xpath(sectionXPath)).click();
        driver.switchTo().defaultContent();
    }

    private static void openTestSuiteCreationForm(){
        showAvailableActions();
        driver.findElement(By.cssSelector("#new_testsuite")).click();
        driver.switchTo().defaultContent();
    }

    private static void showAvailableActions() {
        switchContextToWorkFrame();
        driver.findElement(By.cssSelector("img[title='Actions']")).click();
    }

    private static void createTestSuite(){
        openHomepage();
        openNavigationSection("Test Specification");
        openTestSuiteCreationForm();

        switchContextToWorkFrame();

        testSuiteName = "TS_" + getTimeStamp();
        driver.findElement(By.cssSelector("#name")).click();
        driver.findElement(By.cssSelector("#name")).sendKeys(testSuiteName);
        driver.findElement(By.cssSelector("input[name='add_testsuite_button']")).click();
        driver.switchTo().defaultContent();
    }

    private static void selectTestSuite(String tsName){
        switchContextToTreeFrame();

        String isTestSuiteExpandedMarker = "//span[contains(text(), '" + testSuiteName + "')]/ancestor::div[contains(@class, 'collapsed')]";
        if(driver.findElements(By.xpath(isTestSuiteExpandedMarker)).size() > 0){
            String tsXPath = String.format("//span[contains(text(), '%s')]", tsName);
            WebElement testSuite = driver.findElement(By.xpath(tsXPath));
            Actions actions = new Actions(driver);
            actions.doubleClick(testSuite).perform();
        }
        driver.switchTo().defaultContent();
    }

    private static void selectTestCaseWithinTestSuite(String testSuite, String testCase){
        selectTestSuite(testSuite);

        switchContextToTreeFrame();
        String testCaseXPath = String.format("//span[contains(text(), '%s')]", testCase);
        WebElement tc = driver.findElement(By.xpath(testCaseXPath));
        Actions actions = new Actions(driver);
        actions.doubleClick(tc).perform();
        driver.switchTo().defaultContent();
    }

    private static void openTestCaseCreationForm(){
        showAvailableActions();

        driver.findElement(By.cssSelector("#create_tc")).click();
        driver.switchTo().defaultContent();
    }

    private static void createTestCaseWithinSuite(String suiteName){
        openHomepage();
        openNavigationSection("Test Specification");
        selectTestSuite(suiteName);
        openTestCaseCreationForm();

        switchContextToWorkFrame();

        WebElement inputField = driver.findElement(By.cssSelector("#testcase_name"));
        inputField.click();
        testCaseName = "TestCase_" + getTimeStamp();
        inputField.sendKeys(testCaseName);
        driver.findElement(By.xpath("//div[@class='groupBtn']//input[@id='do_create_button']")).click();
        driver.findElement(By.cssSelector("input[name='create_step']")).click();
        driver.switchTo().defaultContent();
    }

    private static void createBasicProject(){
        openHomepage();
        openNavigationSection("Test Project Management");

        switchContextToMainFrame();

        driver.findElement(By.cssSelector("#create")).click();
        projectName = "Project_" + getTimeStamp();
        driver.findElement(By.cssSelector("input[name='tprojectName']")).sendKeys(projectName);
        String casePrefix = "P_" + getTimeStamp();
        driver.findElement(By.cssSelector("input[name='tcasePrefix']")).sendKeys(casePrefix);
        driver.findElement(By.cssSelector("input[value='Create']")).click();
        driver.switchTo().defaultContent();
    }

    private static void createBasicTestPlan(){
        openHomepage();
        openNavigationSection("Test Plan Management");

        switchContextToMainFrame();

        driver.findElement(By.cssSelector("input[name='create_testplan']")).click();
        testPlanName = "TestPlan_" + getTimeStamp();
        driver.findElement(By.cssSelector("input[name='testplan_name']")).sendKeys(testPlanName);
        driver.findElement(By.cssSelector("input[name='active']")).click();
        driver.findElement(By.cssSelector("input[name='is_public']")).click();
        driver.findElement(By.cssSelector("input[name='do_create']")).click();
        driver.switchTo().defaultContent();
    }

    private static void createBasicBuild(){
        openHomepage();
        openNavigationSection("Builds / Releases");

        switchContextToMainFrame();

        driver.findElement(By.cssSelector("input[name='create_build_bottom']")).click();
        buildName = "Build" + getTimeStamp();
        driver.findElement(By.cssSelector("#build_name")).sendKeys(buildName);
        driver.findElement(By.cssSelector("input[name='do_create']")).click();
        driver.switchTo().defaultContent();
    }

    private static void openHomepage(){
        driver.get(HOST);
    }

    private static void selectTestProjectInDropdown(String testProjectName){
        openHomepage();

        switchContextToTitleBar();

        WebElement dropdown = driver.findElement(By.cssSelector("select[name='testproject']"));
        dropdown.click();
        String dropdownItemLocator = "option[title*='" + testProjectName + "']";
        WebElement dropdownItem = dropdown.findElement(By.cssSelector(dropdownItemLocator));
        WebDriverWait wait = new WebDriverWait(driver, 3);
        wait.until(ExpectedConditions.elementToBeClickable(dropdownItem));
        dropdownItem.click();
        driver.switchTo().defaultContent();
    }

    private static void addTestCaseToTestPlan(){
        openHomepage();
        openNavigationSection("Add / Remove Test Cases");
        selectTestSuite(testSuiteName);

        switchContextToWorkFrame();

        String testCaseCheckboxXPath = String.format("//tr/td[2][span[contains(text(), '%s')]]/preceding-sibling::td", testCaseName);
        driver.findElement(By.xpath(testCaseCheckboxXPath)).click();
        driver.findElement(By.cssSelector("input[name='doAddRemove']")).click();
        driver.switchTo().defaultContent();
    }

    private static void executeTest(String testSuite, String testCase, TestCaseOutcome outcome){
        openHomepage();
        openNavigationSection("Execute Tests");
        selectTestCaseWithinTestSuite(testSuite, testCase);

        switch (outcome){
            case PASSED:
                switchContextToWorkFrame();
                driver.findElement(By.cssSelector("img[src*='test_status_passed.png']")).click();
                driver.findElement(By.cssSelector("img[src*='test_status_passed.png']")).click();
                driver.switchTo().defaultContent();
                break;
            case FAILED:
                switchContextToWorkFrame();
                driver.findElement(By.cssSelector("img[src*='test_status_failed.png']")).click();
                driver.findElement(By.cssSelector("img[src*='test_status_failed.png']")).click();
                driver.switchTo().defaultContent();
                break;
            default:
                break;
        }
    }

    private static String getTimeStamp(){
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmmss");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return sdf.format(timestamp);
    }

    private static void switchContextToMainFrame() {
        WebElement mainFrame = driver.findElement(By.cssSelector("frame[name='mainframe']"));
        driver.switchTo().frame(mainFrame);
    }

    private static void switchContextToTitleBar() {
        WebElement titleBar = driver.findElement(By.cssSelector("frame[name='titlebar']"));
        driver.switchTo().frame(titleBar);
    }

    private static void switchContextToWorkFrame() {
        switchContextToMainFrame();

        WebElement workFrame =  driver.findElement(By.cssSelector("frame[name='workframe']"));
        driver.switchTo().frame(workFrame);
    }

    private static void switchContextToTreeFrame() {
        switchContextToMainFrame();

        WebElement treeFrame =  driver.findElement(By.cssSelector("frame[name='treeframe']"));
        driver.switchTo().frame(treeFrame);
    }

    private static String convertRGBtoHEX(String rawRGB){
        String[] rgbComponentsArray = rawRGB.replace("rgba(", "").replace(")", "").split(",");
        int redComponent = Integer.parseInt(rgbComponentsArray[0].trim());
        int greenComponent = Integer.parseInt(rgbComponentsArray[1].trim());
        int blueComponent = Integer.parseInt(rgbComponentsArray[2].trim());

        return String.format("#%02x%02x%02x", redComponent, greenComponent, blueComponent).toUpperCase();
    }
}
