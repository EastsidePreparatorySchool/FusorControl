//
// fusor get updates from server
//

var statusTimer = null;
var logStart = undefined;
var liveServer = window.location.href.startsWith("http");
var maxTime = 0;
var startTime;





function updateStatus(data, raw, startTime) {
    if (data !== null) {

        document.getElementById("data").value = infoFromData(data, startTime);
        if (raw !== null) {
            document.getElementById("data").value += "\n\n" + raw;
        }
        updateViz(data, startTime);
    }
}


var globalData;
function getStatus() {
    // for the real thing: web request to server
    request({url: "/protected/getstatus", method: "GET"})
            .then(raw => {
                globalData = data;
                data = JSON.parse(raw);
                updateStatus(data, raw, logStart);
                //console.log(data);
            })
            .catch(error => {
                console.log("getstatus error: " + error);
                console.log(data)
                //console.log("stopping status requests to server");
                stopStatus();
                selectButton("stopLog", "startLog");
            });
}



function initStatus() {
    logStart = undefined;
    maxTime = 0;
    if (statusTimer === null) {
        console.log("now receiving status");
        statusTimer = setInterval(getStatus, 1000); //per every 1 second
    }
}

function stopStatus() {
    if (statusTimer !== null) {
        console.log("now no longer receiving status");
        clearInterval(statusTimer);
        statusTimer = null;
    }
}



//
// init code
//

openTab(null, "chart_info");
createViz();
//
// for local testing: read next line from data file
//

testDta = fullData;
var startTime;


if (!liveServer) {

    testData = fullData;
    if (testData.length > 0) {
        startTime = testData[0]["servertime"];
    }
    console.log("length of test data: " + testData.length);

    updateStatus(testData, null, startTime);
}




        