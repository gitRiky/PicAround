'use strict';

// [START import]
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const gcs = require('@google-cloud/storage')();
const spawn = require('child-process-promise').spawn;
admin.initializeApp(functions.config().firebase);
// [END import]

// Keeps track of the length of the 'likes' child list
// exports.countLikes = functions.database.ref('/pictures/{pictureId}/likesList/{likeId}').onWrite(event => {
//   const collectionRef = event.data.ref.parent;
//   const countRef = collectionRef.parent.child('likes');
//
//   // event.data.ref.parent.parent.child('likes').once("value", function(snapshot) {
//   //   console.log("Likes1:",snapshot.val());
//   // }, function (errorObject) {
//   //   console.log("The read failed: " + errorObject.code);
//   // });
//   //
//   // event.data.ref.parent.parent.child('likes').once('value').then(snapshot => {
//   //   console.log("Likes2:",snapshot.val());
//   // });
//
//
//   // Return the promise from countRef.transaction() so our function
//   // waits for this async event to complete before it exits.
//   return countRef.transaction(current => {
//     if (event.data.exists() && !event.data.previous.exists()) {
//       return (current || 0) + 1;
//     }
//     else if (!event.data.exists() && event.data.previous.exists()) {
//       return (current || 0) - 1;
//     }
//   });
// });
//
// // Keeps track of the length of the 'views' child list
// exports.countViews = functions.database.ref('/pictures/{pictureId}/viewsList/{viewId}').onWrite(event => {
//   const collectionRef = event.data.ref.parent;
//   const countRef = collectionRef.parent.child('views');
//
//   // Return the promise from countRef.transaction() so our function
//   // waits for this async event to complete before it exits.
//   return countRef.transaction(current => {
//     if (event.data.exists() && !event.data.previous.exists()) {
//       return (current || 0) + 1;
//     }
//     else if (!event.data.exists() && event.data.previous.exists()) {
//       return (current || 0) - 1;
//     }
//   });
// });







// [START generateThumbnail]
/**
 * When an image is uploaded in the Storage bucket We generate a thumbnail automatically using
 * ImageMagick.
 */
// [START generateThumbnailTrigger]
exports.generateThumbnail = functions.storage.object().onChange(event => {
// [END generateThumbnailTrigger]
  // [START eventAttributes]
  const object = event.data; // The Storage object.

  const fileBucket = object.bucket; // The Storage bucket that contains the file.
  const filePath = object.name; // File path in the bucket.
  const contentType = object.contentType; // File content type.
  const resourceState = object.resourceState; // The resourceState is 'exists' or 'not_exists' (for file/folder deletions).
  // [END eventAttributes]

  // [START stopConditions]
  // Exit if this is triggered on a file that is not an image.
  if (!contentType.startsWith('image/')) {
    console.log('This is not an image.');
    return;
  }

  // Get the file name.
  const fileName = filePath.split('/').pop();
  // Exit if the image is already a thumbnail.
  if (fileName.startsWith('thumb_')) {
    console.log('Already a Thumbnail.');
    return;
  }

  // Exit if this is a move or deletion event.
  if (resourceState === 'not_exists') {
    console.log('This is a deletion event.');
    return;
  }
  // [END stopConditions]

  // [START thumbnailGeneration]
  // Download file from bucket.
  const bucket = gcs.bucket(fileBucket);
  const tempFilePath = `/tmp/${fileName}`;
  return bucket.file(filePath).download({
    destination: tempFilePath
  }).then(() => {
    console.log('Image downloaded locally to', tempFilePath);
    // Generate a thumbnail using ImageMagick.
    return spawn('convert', [tempFilePath, '-thumbnail', '200x200>', tempFilePath]).then(() => {
      console.log('Thumbnail created at', tempFilePath);
      // We add a 'thumb_' prefix to thumbnails file name. That's where we'll upload the thumbnail.
      const thumbFilePath = filePath.replace(/(\/)?([^\/]*)$/, `$1thumb_$2`);
      // Uploading the thumbnail.
      return bucket.upload(tempFilePath, {
        destination: thumbFilePath
      });
    });
  });
  // [END thumbnailGeneration]
});
// [END generateThumbnail]
