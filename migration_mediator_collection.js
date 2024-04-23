// migration script
// Please utilize the following script to update your existing collection for Mediator release v0.14.5 and beyond.
const collectionName = 'messages';
const collectionNameUserAccount = 'user.account';
let userHashes = [];

db.getCollection(collectionNameUserAccount).find({}).forEach(function(user) {
    user.messagesRef.forEach(function(messageRef) {
        userHashes.push(messageRef.hash);
    });
});

db.getCollection('messages').find({}).forEach(function(message) {
    let newTimestamp = new Date(message.ts);
    if(userHashes.includes(message._id)) {
        db.getCollection('messages').updateOne({ _id: message._id }, { $set: { message_type: 'User', ts: newTimestamp } });
    } else {
        db.getCollection('messages').updateOne({ _id: message._id }, { $set: { message_type: 'Mediator', ts: newTimestamp } });
    }
});

// There are 2 message types  `Mediator`  and `User` Please follow the Readme for more details in the section Mediator storage
const expireAfterSeconds = 7 * 24 * 60 * 60; // 7 day * 24 hours * 60 minutes * 60 seconds
db.getCollection(collectionMessages).createIndex(
    { ts: 1 },
    {
        name: "message-ttl-index",
        partialFilterExpression: { "message_type" : "Mediator" },
        expireAfterSeconds: expireAfterSeconds
    }
)