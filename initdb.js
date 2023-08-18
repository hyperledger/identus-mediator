db.createUser({
    user: "admin",
    pwd: "admin",
    roles: [
        { role: "readWrite", db: "mediator" }
    ]
});

const database = 'mediator';
const collectionDidAccount = 'user.account';
const collectionMessages = 'messages'; // TODO rename to messages.inbox
const collectionMessagesSend = 'outbox'; // TODO rename to messages.outbox

// The current database to use.
use(database);
// Create  collections.
db.createCollection(collectionDidAccount);
db.createCollection(collectionMessages);
//create index
db.getCollection(collectionDidAccount).createIndex({ 'did': 1 }, { unique: true });
// Only enforce uniqueness on non-empty arrays
db.getCollection(collectionDidAccount).createIndex({ 'alias': 1 }, { unique: true, partialFilterExpression: { "alias.0": { $exists: true } } });
db.getCollection(collectionDidAccount).createIndex({ "messagesRef.hash": 1, "messagesRef.recipient": 1 });
