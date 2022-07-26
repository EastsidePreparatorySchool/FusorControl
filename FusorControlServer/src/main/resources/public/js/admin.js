//
// fusor main admin code
//

function checkAdminControls() {
    request({url: "/client", method: "GET"})
            .then(data => {
                isAdmin = (data.endsWith(" (admin)"));
                loginInfo = data;
                console.log("server session client: " + data);

                if (isAdmin) {
                    enableAdminControls(true);
                }

                document.getElementById("loginInfo").innerText = loginInfo;
            })
            .catch(error => {
                console.log("error: " + error);
            });
}

function enableAdminControls(enable) {
    // references global var "isAdmin"

    var adminControls = [
        "startLog", "saveLog", //"getStatus", "kill",
        "hvon", "hvoff", "tmpon", "tmpoff", "variacValue", "variacButton",
        "solon", "soloff", "needleValue", "needleButton",
        "variacStop", "variacZero", "tmplow", "tmphigh", "needleZero",
        "pressureTarget", "setPressureTarget", "releasePressureControl"
    ];

    for (var i = 0; i < adminControls.length; i++) {
        document.getElementById(adminControls[i]).disabled = (!isAdmin) || (!enable);
    }

    request({url: "/numcameras", method: "GET"})
            .then(data => {
                // yes: make the display visible and set the url
                var numCameras = Number(data);       // got number from server
                numCameras = Math.min(numCameras, 4); // 4 cameras max
                for (var i = 1; i <= numCameras; i++) {
                    var cam = document.getElementById("cam" + i);
                    cam.style.display = "inline";
                    cam.src = window.location.origin + ":45" + (i + 66) + "/mjpg";
                    //cam.src = "http://fusor3:45" + (i + 66) + "/mjpg";
                }
            })
            .catch(error => {
                console.log("camera error: " + error);
            });
}




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

// get status once
function getOneStatus() {
    // for the real thing: web request to server
    request({url: "/protected/getonestatus", method: "GET"})
            .then(raw => {
                if (liveServer) {
                    globalData = raw;
                    var data = JSON.parse(raw);
                    updateStatus(data, raw, logStart);
                }
                //console.log(data);
            })
            .catch(error => {
                console.log("getonestatus error: " + error);
                console.log(globalData);
            });
}

function newLogName(name) {
    let n = 1;
    if (name.endsWith(")")) {
        n = name.substring(name.lastIndexOf("(") + 1);
        n = n.substring(0, n.length - 1);
        n = Number(n) + 1;
        name = name.substring(0, name.lastIndexOf(" ("));
    }

    name += " (" + n + ")";



    return name;
}



//start a new log
function startLog() {
    var filename = prompt("Name for new log file:", "");
    if (filename === null) {
        return;
    }
    console.log("startlog");
    document.getElementById("chat").innerText = "";
    request({url: "/protected/admin/startlog?filename=" + filename, method: "GET"})
            .then(data => {
                console.log(data);
                logFileName = filename;
                initStatus();
                //selectButton("startLog", "stopLog");
            })
            .catch(error => {
                console.log("error: " + error);
            });
}

//start a new log
function saveLog() {

    console.log("savelog");
    document.getElementById("chat").innerText = "";
//    logFileName = newLogName(logFileName);
//    let name = logFileName;
//    if (name.startsWith("Unnamed ")) {
//        name = name.substring(8);
//    }
    request({url: "/protected/admin/startlog", method: "GET"})
            .then(data => {
                console.log(data);
                initStatus();
                //selectButton("startLog", "stopLog");
            })
            .catch(error => {
                console.log("error: " + error);
            });
}

//stop logging
function stopLog() {
    console.log("stoplog");
    //stopStatus();
    //selectButton("stopLog", "startLog");
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

function variac_stop() {
    console.log("variac stop");
    try {
        request({url: "/protected/admin/variac_stop", method: "GET"})
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

function setPressureTarget(num) {
    console.log("pressureTarget set:" + num);
    request({url: "/protected/admin/pressureTarget?value=" + num, method: "GET"})
            .then(data => {
                console.log(data);
            })
            .catch(error => {
                console.log("pressureTarget error: " + error);
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

function tmpLow() {
    try {
        request({url: "/protected/admin/tmpLow", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("tmplow", "tmphigh");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error);
    }
}

function tmpHigh() {
    try {
        request({url: "/protected/admin/tmpHigh", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("tmphigh", "tmplow");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error);
    }
}



//control hv
function hvOn() {
    try {
        request({url: "/protected/admin/hvOn", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("hvon", "hvoff");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error);
    }
}

function hvOff() {
    try {
        request({url: "/protected/admin/hvOff", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("hvoff", "hvon");
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



        