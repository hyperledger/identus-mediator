Feature: Mediatior Coordination protocol

Scenario: Successful Mediation Request
  When Recipient sends a mediate request message to the mediator
  Then Mediator responds to Recipient with mediate grant message

# BUG: field should be called recipient_did instead of routing_did, Fabio to fix soon
Scenario: Recipient adds new key to keylist
  Given Recipient successfully set up a connection with the mediator
  When Recipient sends a keylist update message to the mediator with a new peer did
  Then Mediator responds to Recipient with a correct keylist update add message

Scenario: Recipient removes alias from keylist
  Given Recipient successfully set up a connection with the mediator
  And Recipient sends a keylist update message to the mediator with a new peer did
  When Recipient sends a keylist update message to the mediator to remove added alias
  Then Mediator responds to Recipient with a correct keylist update remove message

# BUG: server-error is returned, lets change to no_change
Scenario: Recipient removes not existing alias
  Given Recipient successfully set up a connection with the mediator
  When Recipient sends a keylist update message to the mediator to remove not existing alias
  Then Mediator responds to Recipient with a message with no_change status

# BUG: server-error is returned, no logs available on the mediator side
Scenario: Recipient removes the last alias from keylist
  Given Recipient successfully set up a connection with the mediator
  When Recipient sends a keylist update message to the mediator to remove the last alias
  Then Mediator responds to Recipient with a correct keylist update remove message

# NOT IMPLEMENTED YET
Scenario: Recipient query keylist
  Given Recipient successfully set up a connection with the mediator
  When Recipient sends a keylist query message to the mediator
  Then Mediator responds to Recipient with keylist message containing the current list of keys
