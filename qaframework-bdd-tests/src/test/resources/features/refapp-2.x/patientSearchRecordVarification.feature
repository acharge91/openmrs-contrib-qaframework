Feature: Patient search and record verification

  @selenium @patientSearchVerification
  Scenario: A user logs in and verifies an existing patient record
    Given a user logs into the OpenMRS system
    And the user navigates to the patient search page
    When they search for a patient named "John Smith"
    And they open the patient record
    Then the patient dashboard should be displayed
    And the patient's name should be "John Smith"