Feature: Pickup message protocol

#Scenario: Recipient sends a status-request message
#  Given Recipient sends a mediate request message to the mediator
#  And Mediator responds to Recipient with mediate grant message
#  And Sender sent a forward message to Recipient
#  When Recipient sends a status-request message
#  Then Mediator responds with a status message detailing the queued messages of Recipient

#  Scenario: Shailesh
#    When Recipient shailesh step

Scenario: Recipient sends a delivery-request message
  Given Recipient sends a mediate request message to the mediator
  And Mediator responds to Recipient with mediate grant message
  And Sender sent a forward message to Recipient
  When Recipient sends a delivery-request message
  Then Mediator delivers message of Sender to Recipient

#
#Scenario:
#  Given the mediator has no messages for the recipient
#  When the recipient sends a status-request message
#  Then the mediator responds with a status message with message_count as 0
#
#Scenario:
#  Given the mediator has messages for multiple DIDs
#  When the recipient sends a status-request message for a specific DID
#  Then the mediator responds with a status message detailing only the queued messages for that specific DID
#
#Scenario:
#  Given the mediator has messages for the recipient
#  When the recipient sends a delivery-request message
#  Then the mediator responds with a message-delivery message containing up to the requested limit of messages
#
#Scenario:
#  Given the mediator has no messages for the recipient
#  When the recipient sends a delivery-request message
#  Then the mediator responds with a status message indicating no messages are available
#
#Scenario:
#  Given the mediator has messages for multiple DIDs
#  When the recipient sends a delivery-request message for a specific DID
#  Then the mediator responds with a message-delivery message containing messages for that specific DID only
#
#Scenario:
#  Given the mediator has sent a message-delivery to the recipient
#  When the recipient sends a message-received message
#  Then the mediator removes the acknowledged messages from the queue and sends an updated status message
#
#Scenario:
#  Given the mediator receives a message addressed to multiple recipients
#  When one recipient retrieves the message and indicates it has been received
#  Then the mediator still holds the message for the other recipients and only removes it from the queue when all recipients have retrieved it
#
#Scenario:
#  Given the mediator has messages for multiple DIDs but none for a specific DID
#  When the recipient sends a status-request message for the specific DID
#  Then the mediator responds with a status message with a message_count of 0 for that specific DID
#
#Scenario:
#  Given the mediator has less messages for the recipient than the limit set in the delivery request
#  When the recipient sends a delivery-request message
#  Then the mediator responds with a message-delivery message containing all the queued messages
#
#Scenario:
#  Given the mediator has no messages for a specific DID but has messages for other DIDs
#  When the recipient sends a delivery-request message for the specific DID
#  Then the mediator responds with a status message with a message_count of 0 for that specific DID
#
#Scenario:
#  Given the mediator has not sent any messages to the recipient
#  When the recipient sends a message-received message
#  Then the mediator responds with a problem report or ignores the message, since there are no messages to acknowledge
#
#Scenario:
#  Given the mediator receives a message addressed to multiple recipients and one of them is offline
#  When the offline recipient comes online and fetches messages
#  Then the mediator delivers the message to the recipient, despite other recipients having already fetched it
#
#Scenario:
#  Given the mediator receives a message addressed to multiple recipients
#  When all recipients retrieve the message and indicate it has been received
#  Then the mediator removes the message from the queue completely
#
#Scenario:
#  Given the mediator has messages for the recipient
#  When the recipient sends a delivery-request message with a limit of 0
#  Then the mediator responds with a problem report indicating that the limit must be greater than 0
#
#Scenario:
#  Given the mediator has messages for the recipient that haven't been delivered yet
#  When the recipient sends a message-received message for these undelivered messages
#  Then the mediator responds with a problem report indicating that these messages haven't been delivered yet
#
#Scenario:
#  Given the mediator has messages queued for various DIDs
#  When the recipient sends a delivery-request message for a DID that doesn't exist in the mediator's queue
#  Then the mediator responds with a status message with a message_count of 0 for that specific DID
#
#Scenario:
#  Given the mediator has a maximum batch size it can handle
#  When the recipient sends a delivery-request message with a limit exceeding this maximum
#  Then the mediator responds with a problem report indicating that the request exceeds the maximum batch size
