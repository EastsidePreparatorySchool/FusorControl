//
// fusor main observer code
//


function comment(data) {
    request({url: "/protected/comment", method: "POST", body: new FormData(data)})
            .then(data => {
            })
            .catch(error => {
                console.log("Error: " + error);
            });
    return false;
}


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
    localStorage.setItem("fusor_client", "observer");
} else {
    testData = fullData;
    if (testData.length > 0) {
        console.log("length of test data: " + testData.length);
        updateStatus(testData, null, testData[0]["servertime"]);
    }
}




        