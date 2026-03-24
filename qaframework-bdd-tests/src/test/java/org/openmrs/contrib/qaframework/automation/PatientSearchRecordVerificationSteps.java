/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.contrib.qaframework.automation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.openmrs.contrib.qaframework.RunTest;
import org.openmrs.contrib.qaframework.page.ClinicianFacingPatientDashboardPage;

public class PatientSearchRecordVerificationSteps extends Steps {

    @Before(RunTest.HOOK.SELENIUM_PATIENT_SEARCH_VERIFICATION)
    public void setUp() {
        initiateWithLogin();
    }

    @After(RunTest.HOOK.SELENIUM_PATIENT_SEARCH_VERIFICATION)
    public void tearDown() {
        quit();
    }

    // Step 1 – Given a user logs into the OpenMRS system
    @Given("a user logs into the OpenMRS system")
    public void aUserLogsIntoTheOpenMRSSystem() {
        // Login is performed in the @Before hook via initiateWithLogin(),
        // which authenticates using credentials from TestProperties and
        // navigates to the home page. This step asserts the home page is loaded.
        assertNotNull(homePage);
    }

    // Step 2 – And the user navigates to the patient search page
    @And("the user navigates to the patient search page")
    public void theUserNavigatesToThePatientSearchPage() {
        findPatientPage = homePage.goToFindPatientRecord();
    }

    // Step 3 – When they search for a patient named "John Smith"
    @When("they search for a patient named {string}")
    public void theySearchForAPatientNamed(String patientName) {
        findPatientPage.enterPatient(patientName);
        findPatientPage.waitForPageToLoad();
    }

    // Step 4 – And they open the patient record
    @And("they open the patient record")
    public void theyOpenThePatientRecord() {
        // NOTE: This step assumes at least one result is returned for the
        // search term entered in the previous step. clickOnFirstPatient()
        // clicks the first result row in the patient search results table
        // and returns the ClinicianFacingPatientDashboardPage.
        dashboardPage = (ClinicianFacingPatientDashboardPage) findPatientPage.clickOnFirstPatient().waitForPage();
    }

    // Step 5 – Then the patient dashboard should be displayed
    @Then("the patient dashboard should be displayed")
    public void thePatientDashboardShouldBeDisplayed() {
        assertTrue(driver.getCurrentUrl().contains(ClinicianFacingPatientDashboardPage.URL_PATH));
        assertNotNull(dashboardPage);
    }

    // Step 6 – And the patient's name should be "John Smith"
    @And("the patient's name should be {string}")
    public void thePatientSNameShouldBe(String expectedFullName) {
        // expectedFullName is expected in "GivenName FamilyName" format (e.g. "John Smith").
        // The dashboard exposes separate getPatientGivenName() and getPatientFamilyName()
        // methods; these are concatenated to form the full name for assertion.
        // NOTE: getPatientGivenName() uses the CSS selector
        //   #content div span.PersonName-givenName
        // and getPatientFamilyName() uses
        //   .patient-header .demographics .name .PersonName-familyName
        // Both selectors must resolve correctly against the live application.
        // If the application renders a middle name between given and family name,
        // the assertion below will fail. Adjust the split index or use
        // containsString() from Hamcrest if a partial match is acceptable.
        String[] nameParts = expectedFullName.split(" ", 2);
        String expectedGivenName  = nameParts[0];
        String expectedFamilyName = nameParts.length > 1 ? nameParts[1] : "";

        String actualGivenName  = dashboardPage.getPatientGivenName();
        String actualFamilyName = dashboardPage.getPatientFamilyName();

        assertTrue(
            "Expected given name '" + expectedGivenName + "' but found '" + actualGivenName + "'",
            actualGivenName.equalsIgnoreCase(expectedGivenName)
        );
        assertTrue(
            "Expected family name '" + expectedFamilyName + "' but found '" + actualFamilyName + "'",
            actualFamilyName.equalsIgnoreCase(expectedFamilyName)
        );
    }
}

