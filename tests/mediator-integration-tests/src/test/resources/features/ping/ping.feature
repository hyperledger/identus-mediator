Feature: Ping protocol

Scenario: Trusted ping
  When Recipient sends trusted ping message to mediator
  Then Recipient gets trusted ping message back
