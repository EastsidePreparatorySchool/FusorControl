//
// fusor get updates from server
//

var statusTimer = null;
var logStart = undefined;
var startTime = undefined;
var maxTime = 0;

var liveServer = true;


function updateStatus(data, raw, startTime) {
    if (data !== null) {

        document.getElementById("data").innerText = infoFromData(data, startTime);
        if (raw !== null) {
            document.getElementById("data").innerHTML += "<br><br>";
            document.getElementById("data").innerText += raw;
        }
        updateViz(data, startTime);
    }
}


var globalData;
function getStatus() {
    // for the real thing: web request to server
    request({url: "/protected/getstatus", method: "GET"})
            .then(raw => {
                if (liveServer) {
                    globalData = data;
                    data = JSON.parse(raw);
                    updateStatus(data, raw, logStart);
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
    liveServer = false;;
}










        