Feature: General Mediator Functionality

Scenario: Wrong content type
  When Recipient sends a didcomm message with the wrong content type
  Then Mediator returns a correct error message to Recipient
