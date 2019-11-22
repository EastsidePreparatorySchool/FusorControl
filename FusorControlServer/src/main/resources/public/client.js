//
// fusor main client code
//

var endTest = false;
var statusTimer = null;

function mapBoolean(b, trueval, falseval) {
    if (b === true) {
        return trueval;
    } else if (b === false) {
        return falseval;
    }
    return "<n/c>";
}

function infoFromData(data) {
    data = JSON.parse(data);
    info = "";

    info += "Pump: ";
    if (isDevicePresent(data, "TMP")) {
        var tmp = getVariable(data, "TMP", "tmp");
        info += "Status: " + mapBoolean(tmp, " on", "off") + ", ";
        var speed = getVariable(data, "TMP", "lowspeed");
        info += "speed: " + mapBoolean(speed, "low", "high") + ", ";
        var freq = getVariable(data, "TMP", "pump_freq");
        info += "frequency: " + Math.round(freq) + " Hz, ";
        var amps = getVariable(data, "TMP", "pump_curr_amps");
        info += "power draw: " + (Math.round(amps*100)/100) + " A\n";
    } else {
        info += "n/c\n";
    }

    info += "Pressure (diaphragm gauge): ";
    if (isDevicePresent(data, "DIAPHRAGM")) {
        var dia = getVariable(data, "DIAPHRAGM", "diaphragm_adc");
        info += "" + dia + " (adc)\n";
    } else {
        info += "n/c\n";
    }

    info += "Pressure (Pirani gauge): ";
    if (isDevicePresent(data, "PIRANI")) {
        var pirani = getVariable(data, "PIRANI", "pirani_adc");
        info += "" + pirani + " (adc)\n";
    } else {
        info += "n/c\n";
    }


    info += "Variac input setting: ";
    if (isDevicePresent(data, "VARIAC")) {
        var volts = getVariable(data, "VARIAC", "input_volts");
        info += "" + volts + " V\n";
    } else {
        info += "n/c\n";
    }

    info += "HV Low side: ";
    if (isDevicePresent(data, "HV-LOWSIDE")) {
        info += "variac rms: " + getVariable(data, "HV-LOWSIDE", "variac_rms") + " V, ";
        info += "nst rms: " + getVariable(data, "HV-LOWSIDE", "nst_rms") + " V, ";
        info += "cw avg: " + getVariable(data, "HV-LOWSIDE", "cw_avg") + " V, ";
        info += "n: " + getVariable(data, "HV-LOWSIDE", "n") + "\n";
    } else {
        info += "n/c\n";
    }

    info += "HV High side: ";
    if (isDevicePresent(data, "HV-HIGHSIDE")) {
        info += "Highside current: " + getVariable(data, "HV-HIGHSIDE", "hs_current_adc") + " (adc)\n";
    } else {
        info += "n/c\n";
    }

    info += "Gas injection: ";
    if (isDevicePresent(data, "GAS")) {
        var solenoid = getVariable(data, "GAS", "solenoid");
        info += "Solenoid " + mapBoolean(solenoid, " open", "closed") + "\n";
    } else {
        info += "n/c\n";
    }

    info += "GC-SERIAL: ";
    if (isDevicePresent(data, "GC-SERIAL")) {
        info += getVariable(data, "GC-SERIAL", "cps") + " cps\n";
    } else {
        info += "n/c\n";
    }

    info += "P-N-JUNCTION: ";
    if (isDevicePresent(data, "PN-JUNCTION")) {
        info += "total: " + getVariable(data, "PN-JUNCTION", "total") + " (adc)\n";
    } else {
        info += "n/c\n";
    }
    return info;
}

function isDevicePresent(data, device) {
    var value;
    device = data.find((item) => item["device"] === device);
    return (device !== undefined);
}


function getVariable(data, device, variable) {
    var value;
    device = data.find((item) => item["device"] === device);
    if (device === undefined) {
        return ("<n/c>");
    }
    data = device["data"];
    if (data === undefined) {
        return "<n/a>";
    }
    variable = data[variable];
    if (variable === undefined) {
        return "<n/a>";
    }
    value = variable["value"];
    if (value === undefined) {
        return "<n/a>";
    }
    return value;
}


//document.getElementById("variacValue").addEventListener("keyup", function (event) {
//    if (event.keyCode === 13) { //enter key: 13
//        event.preventDefault();
//        document.getElementById("variacButton").click();
//    }
//});
//
//document.getElementById("needleValue").addEventListener("keyup", function (event) {
//    if (event.keyCode === 13) { //enter key: 13
//        event.preventDefault();
//        document.getElementById("needleButton").click();
//    }
//});

function selectButton(select, unselect) {
    document.getElementById(unselect).style.border = "none";
    document.getElementById(select).style.border = "2px solid black";
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
        console.log("error: " + xhr.statusText);
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
                //console.log("stopping status requests to server");
                //stopStatus();
                //selectButton("stopLog", "startLog");
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
        console.log("Error: " + error);
    }
}

//control needle valve
function needleValve(num) {
    console.log("needleValve set:" + num);
    request({url: "/needleValve?value=" + num, method: "GET"})
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
        request({url: "/tmpOn", method: "GET"})
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
        request({url: "/tmpOff", method: "GET"})
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
        request({url: "/solenoidOn", method: "GET"})
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
        request({url: "/solenoidOff", method: "GET"})
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

        