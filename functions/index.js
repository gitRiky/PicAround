'use strict';

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

const MIN_DIST = 0.001;
const MAX_DIST = 1000;
var points;
var newPoints = 0;
var mergedPlaces = 0;

class Point {
  constructor(lat, lon){
    this.id = null;
    this.lat = lat;
    this.lon = lon;
    this.pictures = null;
	this.popularity = 0.;
  }
}

class Picture {
  constructor(description, id, likes, views, popularity, name, path, timestamp, userIcon, userId, username){
    this.id = id;
    this.description = description;
    this.likes = likes;
    this.views = views;
    this.popularity = popularity;
    this.name = name;
    this.path = path;
    this.timestamp = timestamp;
    this.userIcon = userIcon;
    this.userId = userId;
    this.username = username;
  }
}

var databaseRef = admin.database().ref();
exports.aggregatePlaces = functions.https.onRequest((req, res) => {

  // read from the database
  var ref = databaseRef.child("places");
  ref.once("value", function(snapshot) {
    // console.log("snapshot:" + snapshot);
    var places = snapshot.val();

    var ref = databaseRef.child("points");
    ref.once("value", function(snapshot) {
      points = snapshot.val();
      aggregatePlaces(places, points);
    });
  });
  console.log('Function successfully executed');
  res.status(200).end();
});

function updatePopularity(points){
	var pointsRef = databaseRef.child("points");
	for (var key in points){
		var counter = 0;
		var popularity = 0;
		var pictures = points[key].pictures;
		for (var picKey in pictures){
			popularity += pictures[picKey].popularity;
			counter++;
		}
		points[key].popularity = parseFloat(popularity) / parseFloat(counter);
    console.log("popularity: " + popularity + ", counter: " + counter);
		pointsRef.child(key).child("popularity").set(points[key].popularity);
	}
}

function aggregatePlaces(places, points){
  for (var keyPlace in places){
    var place = places[keyPlace];
    var placeLat = parseFloat(place.lat);
    var placeLon  = parseFloat(place.lon);
    var minDist = MAX_DIST;
    var minPointKey = null;

    for (var keyPoint in points){
      var point = points[keyPoint];
      var pointLat = parseFloat(point.lat);
      var pointLon = parseFloat(point.lon);
      if ((placeLat <= pointLat+MIN_DIST) && (placeLat >= pointLat-MIN_DIST) &&
          (placeLon <= pointLon+MIN_DIST) && (placeLon >= pointLon-MIN_DIST)){
            var dist = computeDist(place, point);
            if (dist < minDist){
              minDist = dist;
              minPointKey = keyPoint;
            }
      }
    }
    if (minPointKey === null){
      createPoint(place);
	  newPoints++;
	}
    else{
      merge(place, points[minPointKey]);
	  mergedPlaces++;
	}
	console.log("Created " + newPoints + " points and aggregated " + mergedPlaces + " places");
  }
  var updatedPoints;
  var pointsRef = databaseRef.child("points");
  pointsRef.once("value", function(snapshot){
	  updatedPoints = snapshot.val();
	  updatePopularity(updatedPoints);
  });
}

function merge(place, point){
  var pushRef = databaseRef.child("points").child(point.id).child("pictures");
  var picturesRef = databaseRef.child("pictures");
  if (place.pictures != null){
    for (var key in place.pictures){
      // assign pointID to picture
      place.pictures[key].pointId = point.id;
      place.pictures[key].inPlace = false;
      pushRef.child(key).set(place.pictures[key]);
	  //update the picture also in pictures
	  picturesRef.child(place.pictures[key].id).set(place.pictures[key]);
    }
  }
  deletePlace(place.id);
  mergedPlaces++;
}

function createPoint(place){
  var toPut = new Point(place.lat, place.lon);
  var pointsRef = databaseRef.child("points");
  var pushRef = pointsRef.push();
  toPut.id = pushRef.key;
  points[hashCode(String(place.id))] = toPut;
  var picturesRef = databaseRef.child("pictures");
  for (var key in place.pictures){
  // assign pointID to picture
    place.pictures[key].pointId = toPut.id;
    place.pictures[key].inPlace = false;
    toPut.pictures = place.pictures;
    pushRef.set(toPut);
	//update the picture also in pictures
	picturesRef.child(place.pictures[key].id).set(toPut.pictures[key]);

    deletePlace(place.id);
	newPoints++;
  }
}

function computeDist(place, point){
  var placeLat = place.lat;
  var placeLon  = place.lon;
  var pointLat = point.lat;
  var pointLon = point.lon;
  var diffLat = placeLat-pointLat;
  var diffLon = placeLon-pointLon;
  return diffLat*diffLat + diffLon*diffLon;
}

function hashCode(str){
	var hash = 0;
	if (str.length == 0) return hash;
	for (var i = 0; i < str.length; i++) {
		var char = str.charCodeAt(i);
    hash = ((hash<<5)-hash)+char;
    hash = hash & hash; // Convert to 32bit integer
	}
	return hash;
}

function deletePlace(placeId){
  var placesRef = databaseRef.child("places");
  placesRef.child(placeId).set(null);
}
