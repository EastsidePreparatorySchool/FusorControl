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
        "startLog", "stopLog", "getStatus", "kill",
        "tmpon", "tmpoff", "variacValue", "variacButton",
        "solon", "soloff", "needleValue", "needleButton",
        "variacStop", "variacZero"
    ];

    for (var i = 0; i < adminControls.length; i++) {
        document.getElementById(adminControls[i]).disabled = (!isAdmin) || (!enable);
    }
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


//start a new log
function startLog() {
    var filename = prompt("Custom name for log file:", "");
    if (filename === null) {
        filename = "";
    }
    console.log("startlog");
    document.getElementById("chat").innerText = "";
    request({url: "/protected/admin/startlog?filename="+filename, method: "GET"})
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

function variac_stop(num) {
    var variacValue = num;
    console.log("variac stop:", num);
    try {
        request({url: "/protected/admin/variac_stop?value=" + variacValue, method: "GET"})
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



        