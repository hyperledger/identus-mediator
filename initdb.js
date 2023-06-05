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
db.collectionDidAccount.createIndex({ 'alias': 1 }, { unique: true });
