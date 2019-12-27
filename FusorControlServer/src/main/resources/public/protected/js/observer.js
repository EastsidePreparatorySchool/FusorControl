//
// fusor main observer code
//


//
// init code
//

openTab(null, "chart_info");
createViz();
//
// for local testing: read next line from data file
//



if (liveServer) {
    initStatus();
} else {
    testData = fullData;
    if (testData.length > 0) {
        startTime = testData[0]["servertime"];
    }
    console.log("length of test data: " + testData.length);

    updateStatus(testData, null, startTime);
}




        