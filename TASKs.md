# ATL-5236 - Error Handling (PRISM Agent and Mediator)

https://input-output.atlassian.net/browse/ATL-5236

https://identity.foundation/didcomm-messaging/spec/#problem-reports


On each step of all our protocols processing, when something wrong is happening, we need to:
Goals
- Update the record to a documented error state
- Log the error in the service logs
- Send the problem report message when appropriate

Goal other: error recover/resilient
- [optional] Send event that record state changed to error
- Decide on the policy of re-trying sending errors (one of the proposals is just to send it once, and if a recipient did not get this, then it’s on its own requesting record ID state)



Tasks:
- (2W * 2Dev) Mediator (Note: most errors in Mediator will be synchronous)
 - Store messages when sending (1w)
 - Catch Errors and send Problem Reports (1w):
   - (sync) e.p.crypto - is message is tampering (any crypto error).
   - (sync) e.p.crypto.unsupported - is message is tampering (any crypto error).
   - (sync & async) e.p.crypto.replay - if the message is replay (possible he replay attack).
   - (sync) e.p.req - pickup message before enroling.
    - [QA] StatusRequest - https://didcomm.org/messagepickup/3.0/status-request
    - [QA] DeliveryRequest - https://didcomm.org/messagepickup/3.0/delivery-request
   - (sync) e.p.me.res.storage - connection MongoBD is not working.
    - [QA] catch all StorageCollection Error
   - (sync) e.p.me.res.storage - business logic MongoBD is not working.
    - [QA] catch all StorageThrowable
   - (sync) e.p.did - for any DID method that is not `did.peer`.
   - (sync) e.p.did.malformed - for any DID method malformed.
   - (sync) e.p.msg - for parsing error from the message.
   - (sync) e.p.msg.unsupported - for the message type LiveModeChange and all message that is not role of the mediator
     - [QA] MediateGrant
     - [QA] MediateDeny
     - [QA] KeylistResponse
     - [QA] Status - https://didcomm.org/messagepickup/3.0/status
     - [QA] LiveModeChange - https://didcomm.org/messagepickup/3.0/live-delivery-change
     - [TODO] ...
   - (sync) e.p.msg.unsupported - for parsing error due to unsupported version or protocol.
     - [QA] MissingProtocolExecuter (unsupported protocol it also works fine for unsupported versions)
   - (sync & async) e.p.req.not_enroll - Get a Forward message to a DID that is not enrolled.
   - (sync & async) e.p.me - catch all error at the end.
 - Receive a problem report (1w):
  - in case of Warnings Reply `w.p` -> log warnings and escalate to an error `e.p` on the reply
  - in case of Error `e.p` -> log error

- (4/6W * 2Dev) PRISM Agent (Note: most error in PRISM Agent will be asynchronous)
 - Store DID Comm messages in a searchable way (4W)
  - Store receiving messages (in a searchable way)
  - Store sending messages (in a searchable way)
    - both plaintext and encrypted
  - [optional] Maybe later MongoBD for PRISM Agent
 - Implement Problem Reports in Mercury (DONE??)
 - Catch all the Errors and send Problem Reports: (4W)
   - e.p.xfer - if it found nobody listening on the specified port. (service endpoint unavailable)
   - (async) e.p.crypto - is message is tampering (any crypto error).
   - (async) e.p.crypto.unsupported - is message is tampering (any crypto error).
   - (sync & async) e.p.crypto.replay - if the message is replay (possible he replay attack).
   - (async) e.p.me.res.storage - connection MongoBD is not working.
   - (async) e.p.me.res.storage - business logic MongoBD is not working.
   - (async) e.p.did - for any DID method that is not `did.peer`.
   - (async) e.p.did.malformed - for any DID method malformed.
   - (async) e.p.msg - for parsing errors from the message.
   - (async) e.p.msg.unsupported - for the message with the wrong role of the agent.
   - (async) e.p.msg.unsupported - for parsing error due to unsupported version or protocol.
   - (sync & async) e.p.me - catch all error at the end.
   - (async) TODO `w.m`
 - Receive e Problem Reports (1W):
   - support for scope `m` -> state update (of the protocol execution)
   - support for scope `p` -> state update (and fail protocol execution)

- Other task (not for this ticket)
 - Traceability of the MsgID of the Problem Report to the original error (2d) -> ATL-4147
  - [optional] Log - https://input-output.atlassian.net/browse/ATL-4147
 - escalate_to must be configurable (1d)
 - [optional] update the protocol with new tokens (2d)
   - `e.p.me.res.storage`
