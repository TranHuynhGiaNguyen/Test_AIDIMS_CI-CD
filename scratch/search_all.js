const fs = require('fs');

const path = 'c:\\Users\\PC\\Downloads\\AIDIMS\\AIDIMS\\tests\\aidims_collection.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

function printItems(items, depth = 0) {
    for (const item of items) {
        if (item.item) {
            console.log('  '.repeat(depth) + `Folder: ${item.name}`);
            printItems(item.item, depth + 1);
        } else {
            const req = item.request || {};
            const url = req.url ? (req.url.raw || req.url) : 'N/A';
            console.log('  '.repeat(depth) + `Request: ${item.name} | Method: ${req.method} | URL: ${url}`);
        }
    }
}

printItems(data.item);
