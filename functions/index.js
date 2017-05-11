'use strict';

// [START import]
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const spawn = require('child-process-promise').spawn;
admin.initializeApp(functions.config().firebase);
// [END import]


exports.prova = functions.https.onRequest((req, res) => {

  // Get a database reference to our posts
  var db = admin.database();
  var ref = db.ref("places");

  // Attach an asynchronous callback to read the data
  ref.once("value", function(snapshot) {
    console.log(snapshot.val());
  }, function (errorObject) {
    console.log("The read failed: " + errorObject.code);
  });

  res.status(200).end();
});
