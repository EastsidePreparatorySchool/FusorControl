//
// fusor main client code
//

var endTest = false;
var statusTimer = null;

function infoFromData(data) {
    data = JSON.parse(data);
    info = "";

    var volts = getVariable(data, "VARIAC", "volts");

    info += "Variac setting: " + volts + " V\n";
    info += "Lowside: variac rms: " + getVariable(data, "HV-LOWSIDE", "variac_rms") + " V, ";
    info += "nst rms: " + getVariable(data, "HV-LOWSIDE", "nst_rms") + " V, ";
    info += "cw avg: " + getVariable(data, "HV-LOWSIDE", "cw_avg") + " V, ";
    info += "n: " + getVariable(data, "HV-LOWSIDE", "n") + "\n";
    info += "GC: " + getVariable(data, "GC-SERIAL", "cps") + " cps\n";
    info += "PN: " + getVariable(data, "PN-JUNCTION", "total") + "\n";
    return info;
}


function getVariable(data, device, variable) {
    var value;
    device = data.find((item) => item["device"] === device);
    if (device == undefined) {
        return ("<n/c>");
    }
    data = device["data"];
    if (variable == undefined) {
        return "corrupt";
    }
    variable = data[variable];
    if (variable == undefined) {
        return "<n.s.v.>";
    }
    value = variable["value"];
    if (value == undefined) {
        return "<corrupt>";
    }
    return value;
}


document.getElementById("variacValue").addEventListener("keyup", function (event) {
    if (event.keyCode === 13) { //enter key: 13
        event.preventDefault();
        document.getElementById("variacButton").click();
    }
});


function selectButton(select, unselect) {
    document.getElementById(unselect).style.border = "none";
    document.getElementById(select).style.border = "2px solid black"
}

//new request & xmlRequest function (spark stuff (this has nothing to do with spark. this is just http. GM.))
function request(obj) {
    return new Promise((resolve, reject) => {
        let xhr = new XMLHttpRequest();
        xhr.open(obj.method || "GET", obj.url);

        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                resolve(xhr.response);
            } else {
                reject(xhr.statusText);
            }
        };
        xhr.onerror = () => reject(xhr.statusText);

        xhr.send(obj.body);
    });
}


function xmlRequest(verb, url) {
    var xhr = new XMLHttpRequest();
    xhr.open(verb || "GET", url, true);
    xhr.onload = () => {
        console.log(xhr.response);
    };
    xhr.onerror = () => {
        console.log("error: " + statusText);
    };
    xhr.send();
}


//gets status of all non-core devices
function getStatus() {
    request({url: "/getstatus", method: "GET"})
            .then(data => {
                document.getElementById("data").value = infoFromData(data);
                document.getElementById("data").value += "\n\n" + data;

                //console.log(data);
            })
            .catch(error => {
                console.log("getstatus error: " + error);
                console.log("stopping status requests to server");
                stopStatus();
                selectButton("stopLog", "startLog");
            });
}

//this kills the server (needs testing)
function kill() {
    request({url: "/kill", method: "GET"})
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
    request({url: "/startlog", method: "GET"})
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
    request({url: "/stoplog", method: "GET"})
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
        request({url: "/variac?value=" + variacValue, method: "GET"})
                .then(data => {
                    console.log(data);
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error)
    }
}

//control tmp
function tmpOn() {
    try {
        request({url: "/tmpOn", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("tmpon", "tmpoff");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error)
    }
}

function tmpOff() {
    try {
        request({url: "/tmpOff", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("tmpoff", "tmpon");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error)
    }
    tmpValue.Value = "";
}

//control the solenoid
function SolenoidOn() {
    try {
        request({url: "/solenoidOn", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("solon", "soloff");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error)
    }
}

function SolenoidOff() {
    try {
        request({url: "/solenoidOff", method: "GET"})
                .then(data => {
                    console.log(data);
                    selectButton("soloff", "solon");
                })
                .catch(error => {
                    console.log("error: " + error);
                });
    } catch (error) {
        console.log("Error: " + error)
    }
}

function initStatus() {
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

        