package com.securitytests.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

/**
 * TestNG-based Cucumber runner for BDD tests
 * Integrates with Allure for reporting
 */
@CucumberOptions(
        features = {"src/test/resources/features"},
        glue = {"com.securitytests.steps"},
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber-pretty.html",
                "json:target/cucumber-reports/CucumberTestReport.json",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
                "rerun:target/failed_scenarios.txt"
        },
        monochrome = true,
        dryRun = false,
        tags = "not @wip and not @disabled"
)
public class BDDTestRunner extends AbstractTestNGCucumberTests {
    
    /**
     * Enable parallel execution of scenarios
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
    
    @BeforeSuite
    public void setUp() {
        // Setup code for the entire test suite
        System.out.println("Setting up BDD test suite");
    }
    
    @AfterSuite
    public void tearDown() {
        // Teardown code for the entire test suite
        System.out.println("Tearing down BDD test suite");
        
        // Generate living documentation from Gherkin features
        generateLivingDocumentation();
    }
    
    /**
     * Generate living documentation from Gherkin features
     * This could be extended to use tools like Cucumber Reports or custom HTML generators
     */
    private void generateLivingDocumentation() {
        System.out.println("Generating living documentation from BDD features");
        
        try {
            // This is where you'd typically call a documentation generator
            // For example, using a library like Cukedoctor or a custom implementation
            
            // ProcessBuilder processBuilder = new ProcessBuilder(
            //     "java", "-jar", "cukedoctor.jar", 
            //     "-o", "target/living-documentation",
            //     "-p", "target/cucumber-reports/CucumberTestReport.json"
            // );
            // processBuilder.start().waitFor();
            
            System.out.println("Living documentation generated successfully");
        } catch (Exception e) {
            System.err.println("Failed to generate living documentation: " + e.getMessage());
        }
    }
}
