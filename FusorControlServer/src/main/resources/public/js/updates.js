//
// fusor get updates from server
//

var statusTimer = null;
var logStart = undefined;
var startTime = undefined;
var maxTime = 0;
var updateInterval = 100;
var logFileName = "Unnamed";
var liveServer = true;


function updateStatus(data, raw, startTime) {
    if (data !== null) {
        //console.log("updating "+raw.length+" bytes");
        data.sort(function (a, b) {
            return a["servertime"] - b["servertime"];
        });
        updateViz(data, false);
    }
}


var globalData;
var dotCount = 0;
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
                            //selectButton("startLog", "stopLog");
                        } else {
                            renderText(false, 0);
                            //selectButton("stopLog", "startLog");
                        }
                        setTimeout(getStatus, updateInterval);
                    }
                    //console.log(data);
                })
                .catch(error => {
                    if (error === "Unauthorized") {
                        setTimeout(reload, 5000);
                    } else if (error === "expired") {
                        setTimeout(reload, 5000);
                    } else {
                        console.log("getstatus error: " + error);
                        console.log(globalData);
                        //console.log("stopping status requests to server");
                        if (isAdmin) {
                            //stopStatus();
                            //selectButton("stopLog", "startLog");
                        }
                        print(".");
                        if (++dotCount % 50 === 0) {
                            printHTML("<br>");
                        }
                        setTimeout(getStatus, updateInterval);
                        //waitForSite();
                    }
                });
    }
}


function reload() {
    location.assign("/");

}



function initStatus() {
    resetViz();
    liveServer = true;
    getStatus();
    console.log("now receiving status");
    console.log("logFileName "+logFileName);
    document.getElementById("logInfo").innerText = logFileName;
}

function stopStatus() {
    liveServer = false;
    renderText(false);
    console.log("now no longer receiving status");
}










        