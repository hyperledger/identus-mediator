db.createUser({
    user: "admin",
    pwd: "admin",
    roles: [
        { role: "readWrite", db: "mediator" }
    ]
});

const database = 'mediator';
const collectionDidAccount = 'did_account';
const collectionMessages = 'messages';

// The current database to use.
use(database);

// Create  collections.
db.createCollection(collectionDidAccount);
db.createCollection(collectionMessages);
//create index
db.collection(collectionDidAccount).createIndex({ 'did': 1 }, { unique: true });
db.collection(collectionDidAccount).createIndex({ 'alias': 1 }, { unique: true });
