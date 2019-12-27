//
// fusor main admin code
//


//this kills the server (needs testing)
function kill() {
    request({url: "/protected/admin/kill", method: "GET"})
            .then(data => {
                console.log(data);
            })
            .catch(error => {
                console.log("error: " + error);
            });
}

//start a new log
function startLog() {
    console.log("startlog");
    request({url: "/protected/admin/startlog", method: "GET"})
            .then(data => {
                console.log(data);
                initStatus();
                selectButton("startLog", "stopLog");
            })
            .catch(error => {
                console.log("error: " + error);
            });
}

//stop logging
function stopLog() {
    console.log("stoplog");
    stopStatus();
    selectButton("stopLog", "startLog");
    request({url: "/protected/admin/stoplog", method: "GET"})
            .then(data => {
                console.log(data);
            })
            .catch(error => {
                console.log("error: " + error);
            });
}

//control variac
function variac(num) {
    var variacValue = num;
    console.log(num);
    try {
        request({url: "/protected/admin/variac?value=" + variacValue, method: "GET"})
                .then(data => {
                    console.log(data);
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error);
    }
}

//control needle valve
function needleValve(num) {
    console.log("needleValve set:" + num);
    request({url: "/protected/admin/needleValve?value=" + num, method: "GET"})
            .then(data => {
                console.log(data);
            })
            .catch(error => {
                console.log("needle valve error: " + error);
            });
}

//control tmp
function tmpOn() {
    try {
        request({url: "/protected/admin/tmpOn", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("tmpon", "tmpoff");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error);
    }
}

function tmpOff() {
    try {
        request({url: "/protected/admin/tmpOff", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("tmpoff", "tmpon");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error);
    }
}

//control the solenoid
function SolenoidOn() {
    try {
        request({url: "/protected/admin/solenoidOn", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("solon", "soloff");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error);
    }
}

function SolenoidOff() {
    try {
        request({url: "/protected/admin/solenoidOff", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("soloff", "solon");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error);
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

if (liveServer) {
    //initStatus();
} else {
    testData = fullData;
    if (testData.length > 0) {
        startTime = testData[0]["servertime"];
    }
    console.log("length of test data: " + testData.length);

    updateStatus(testData, null, startTime);
}




        