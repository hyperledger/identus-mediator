Feature: Ping protocol

Scenario: Trusted ping
  When Recipient sends trusted ping message to mediator
  Then He gets trusted ping message back
