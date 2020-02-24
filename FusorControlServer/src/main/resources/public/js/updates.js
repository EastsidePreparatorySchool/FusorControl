//
// fusor get updates from server
//

var statusTimer = null;
var logStart = undefined;
var startTime = undefined;
var maxTimeTotal = 0;
var updateInterval = 100;

var liveServer = true;


function updateStatus(data, raw, startTime) {
    if (data !== null) {
        updateViz(data, false);
    }
}


var globalData;
function getStatus() {
    if (!offline) {
        // for the real thing: web request to server
        request({url: "/protected/getstatus", method: "GET"})
                .then(raw => {
                    if (liveServer) {
                        globalData = raw;
                        if (raw !== "not logging") {
                            var data = JSON.parse(raw);
                            updateStatus(data, raw, logStart);
                            selectButton("startLog", "stopLog");
                        } else {
                            renderText(false, 0);
                            selectButton("stopLog", "startLog");
                        }
                        setTimeout(getStatus, updateInterval);
                    }
                    //console.log(data);
                })
                .catch(error => {
                    console.log("getstatus error: " + error);
                    console.log(globalData);
                    //console.log("stopping status requests to server");
                    if (isAdmin) {
                        //stopStatus();
                        selectButton("stopLog", "startLog");
                    }
                    setTimeout(getStatus, updateInterval);
                });
    }
}



function initStatus() {
    resetViz();
    liveServer = true;
    getStatus();
    console.log("now receiving status");
}

function stopStatus() {
    liveServer = false;
    renderText(false);
    console.log("now no longer receiving status");
}










        