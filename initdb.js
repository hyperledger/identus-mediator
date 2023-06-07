db.createUser({
    user: "admin",
    pwd: "admin",
    roles: [
        { role: "readWrite", db: "mediator" }
    ]
});

const database = 'mediator';
const collectionDidAccount = 'user.account';
const collectionMessages = 'messages';

// The current database to use.
use(database);
// Create  collections.
db.createCollection(collectionDidAccount);
db.createCollection(collectionMessages);
//create index
db.getCollection(collectionDidAccount).createIndex({ 'did': 1 }, { unique: true });
db.getCollection(collectionDidAccount).createIndex({ 'alias': 1 }, { unique: true });
db.getCollection(collectionDidAccount).createIndex({ "messagesRef.hash": 1, "messagesRef.recipient": 1 });
