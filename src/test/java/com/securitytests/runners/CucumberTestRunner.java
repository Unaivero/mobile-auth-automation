package com.securitytests.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG Cucumber runner for BDD features
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.securitytests.steps",
        plugin = {
                "pretty",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
                "html:target/cucumber-reports/cucumber-pretty.html",
                "json:target/cucumber-reports/CucumberTestReport.json"
        },
        monochrome = true,
        tags = "not @ignore"
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
    
    /**
     * Override the scenarios method to enable parallel execution
     * This makes Cucumber scenarios run in parallel
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
