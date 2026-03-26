Feature: Add Location with a Custom Attribute Type

    @selenium
    @location
    Scenario: An administrator logs in and adds a new location with a custom attribute type applied

        # --- LOGIN ---
        # EXISTING STEP: LoginSteps.visitLoginPage()
        Given User visits login page

        # EXISTING STEP: LoginSteps.anyUsername() — reads login.username from test.properties
        And User enters "setupUser" username

        # EXISTING STEP: LoginSteps.anyPassword() — reads login.password from test.properties
        And User enters "setupPass" password

        # EXISTING STEP: LoginSteps.selectLoginLocation() — reads login.location from test.properties
        And User Selects "setupLocation" Login Location

        # EXISTING STEP: LoginSteps.userLogsIn()
        And User Logs in

        # --- NAVIGATE TO LOCATION MANAGEMENT ---
        # EXISTING STEP: LocationManagementSteps.userClicksOnSystemAdminLink()
        Given a user clicks on the System Administration Link from home page

        # EXISTING STEP: LocationManagementSteps.userClicksOnAdvancedAdminLink()
        When User clicks on the Advanced Administration link from the System Administration Page

        # EXISTING STEP: LocationManagementSteps.userClicksOnManageLocation()
        And a user clicks on the manage location link

        # EXISTING STEP: LocationManagementSteps.systemLoadsManageLocationPage()
        Then the system loads the manage location page

        # --- ADD A NEW LOCATION ---
        # EXISTING STEP: LocationManagementSteps.userClicksOnAddNewLocationButton()
        And the user clicks on the add new location button

        # EXISTING STEP: LocationManagementSteps.userFillsAddNewLocationForm()
        And the user fills the form

        # NEW STEP: new step definition and new page object method on AdministrationAddEditLocationPage
        # addLocationAttributeValue(String attributeType, String value)
        And the user adds a location attribute of type "Care Setting" with value "Outpatient"

        # EXISTING STEP: LocationManagementSteps.userSavesAddNewLocationForm()
        Then the user saves the location form

        # NEW STEP: new step definition and new page object method on ManageLocationsOnAdminPage
        # verifyLocationExists(String locationName)
        Then the system confirms the new location appears in the location list
