//
// fusor main observer code
//

//
// init code
//

createViz();




//
// for local testing: read next line from data file
//



if (liveServer) {
    initStatus();
    localStorage.setItem("fusor_client", "observer");
} else {
    testData = fullData;
    if (testData.length > 0) {
        console.log("length of test data: " + testData.length);
        updateStatus(testData, null, testData[0]["servertime"]);
    }
}




        