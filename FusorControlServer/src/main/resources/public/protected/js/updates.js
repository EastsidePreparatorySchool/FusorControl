//
// fusor get updates from server
//

var statusTimer = null;
var logStart = undefined;
var startTime = undefined;
var maxTime = 0;
var updateInterval = 100;

var liveServer = true;


function updateStatus(data, raw, startTime) {
    if (data !== null) {

        document.getElementById("data").innerText = infoFromData(data, startTime);
        if (raw !== null) {
            document.getElementById("data").innerHTML += "<br><br>";
            document.getElementById("data").innerText += raw;
        }
        updateViz(data);
    }
}


var globalData;
function getStatus() {
    // for the real thing: web request to server
    request({url: "/protected/getstatus", method: "GET"})
            .then(raw => {
                if (liveServer) {
                    globalData = raw;
                    var data = JSON.parse(raw);
                    updateStatus(data, raw, logStart);
                    setTimeout(getStatus, updateInterval);
                }
                //console.log(data);
            })
            .catch(error => {
                console.log("getstatus error: " + error);
                console.log(globalData);
                //console.log("stopping status requests to server");
                stopStatus();
                selectButton("stopLog", "startLog");
            });
}



function initStatus() {
    resetViz();
    liveServer = true;
    getStatus();
    console.log("now receiving status");
}

function stopStatus() {
    liveServer = false;
    console.log("now no longer receiving status");
}










        