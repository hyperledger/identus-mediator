Feature: Mediatior Coordination protocol

Scenario: Successful Mediation Request
  When Recipient sends a mediate request message to the mediator
  Then Mediator responds to him with mediate grant message

#Scenario: Recipient adds new key to keylist
#  Given Recipient successfully set up a connection with the mediator
#  When Recipient creates a new key to be registered with the mediator
#  And Recipient sends a keylist update message to the mediator
#  Then Mediator updates the key list and responds with a correct keylist update message
#
#Scenario: Recipient removes key from keylist
#  Given Recipient successfully set up a connection with the mediator
#  When Recipient sends a keylist update message to the mediator to remove a key
#  Then Mediator updates the key list and responds with a correct keylist update message
#
#Scenario: Recipient query keylist
#  Given Recipient successfully set up a connection with the mediator
#  When Recipient sends a keylist query message to the mediator
#  Then Mediator responds with a keylist message containing the current list of keys
