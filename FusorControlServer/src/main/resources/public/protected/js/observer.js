//
// fusor main client code
//

var endTest = false;
var statusTimer = null;
var logStart = undefined;
var maxTime = 0;
var liveServer = window.location.href.startsWith("http");


function mapBoolean(b, trueval, falseval) {
    if (b === true) {
        return trueval;
    } else if (b === false) {
        return falseval;
    }
    return "<n/c>";
}

function infoFromData(data) {
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
        info += "power draw: " + (Math.round(amps * 100) / 100) + " A\n";
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


    info += "Variac: ";
    if (isDevicePresent(data, "VARIAC")) {
        var volts = getVariable(data, "VARIAC", "input_volts");
        info += "" + volts + " V target ";
        volts = getVariable(data, "VARIAC", "pontentiometer");
        info += "" + volts + " V actual\n";
    } else {
        info += "n/c\n";
    }

    info += "HV Low side: ";
    if (isDevicePresent(data, "HV-LOWSIDE")) {
        info += "variac rms: " + Math.round(getVariable(data, "HV-LOWSIDE", "variac_rms") * 1000) / 1000 + " V, ";
        info += "nst rms: " + Math.round(getVariable(data, "HV-LOWSIDE", "nst_rms") * 1000) / 1000 + " KV, ";
        info += "cw avg: " + Math.round(getVariable(data, "HV-LOWSIDE", "cw_avg") * 1000) / 1000 + " KV, ";
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

function getVariableTimeStamp(data, device, variable) {
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
    value = variable["vartime"];
    if (value === undefined) {
        return "<n/a>";
    }
    return value;
}


function getDeviceInfo(data, device, variable) {
    var value;
    device = data.find((item) => item["device"] === device);
    if (device === undefined) {
        return ("<n/c>");
    }

    // treat servertime and devicetime differently (fix this some day)
    if (variable === "servertime") {
        data = device[variable]; // servertime is in the envelope added by the server
        if (data === undefined) {
            return "<n/a>";
        }
        return data;
    } else if (variable === "devicetime") {
        data = device["data"]; // devicetime is defined in the "data" section
        if (data === undefined) {
            return "<n/a>";
        }
        data = data[variable];
        if (data === undefined) {
            return "<n/a>";
        }
        return data;
    }
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

var vizChannels = {
    'TMP.tmp': {name: 'TMP status', variable: 'tmp', min: 0, max: 1},
    'TMP.pump_freq': {name: 'TMP frequency (Hz)', variable: 'pump_freq', min: 0, max: 1250},
    'TMP.pump_curr_amps': {name: 'TMP current (A)', variable: 'pump_curr_amps', min: 0, max: 2.5},
    'DIAPHRAGM.diaphragm_adc': {name: 'Rough pressure (adc)', variable: 'diaphragm_adc', min: 0, max: 110},
    'PIRANI.pirani_adc': {name: 'Fine pressure (adc)', variable: 'pirani_adc', min: 0, max: 1024},
    'VARIAC.input_volts': {name: 'Variac target (V)', variable: 'input_volts', min: 0, max: 130},
    'VARIAC.potentiometer': {name: 'Variac actual (V)', variable: 'potentiometer', min: 0, max: 130},
    'HV-LOWSIDE.variac_rms': {name: 'Variac RMS (V)', variable: 'variac_rms', min: 0, max: 130},
    'HV-LOWSIDE.nst_rms': {name: 'NST RMS (KV)', variable: 'nst_rms', min: 0, max: 15},
    'HV-LOWSIDE.cw_avg': {name: 'CW AVG (KV)', variable: 'cw_avg', min: 0, max: 50},
    'HV-HIGHSIDE.hs_current_adc': {name: 'CW current ()', variable: 'hs_current_adc', min: 0, max: 50},
    'GC-SERIAL.cps': {name: 'GCW (cps)', variable: 'cps', min: 0, max: 100},
    'PN-JUNCTION.total': {name: 'PNJ (adc)', variable: 'total', min: 0, max: 100}
};

var vizData = [];
var chart = null;
var vizFrozen = false;

function createViz() {
    var options = {
        zoomEnabled: true,
        animationEnabled: true,
        title: {
            text: ""
        },
        axisY: {
            title: "Percent",
            includeZero: true,
            suffix: " %",
            lineThickness: 1,
            maximum: 100
        },
        axisX: {
            title: "time",
            includeZero: false,
            suffix: " s",
            lineThickness: 1,
            viewportMinimum: 0,
            viewportMaximum: 60
        },
        legend: {
            cursor: "pointer",
            itemmouseover: function (e) {
                e.dataSeries.lineThickness = e.chart.data[e.dataSeriesIndex].lineThickness * 2;
                e.dataSeries.markerSize = e.chart.data[e.dataSeriesIndex].markerSize + 2;
                e.chart.render();
            },
            itemmouseout: function (e) {
                e.dataSeries.lineThickness = e.chart.data[e.dataSeriesIndex].lineThickness / 2;
                e.dataSeries.markerSize = e.chart.data[e.dataSeriesIndex].markerSize - 2;
                e.chart.render();
            },
            itemclick: function (e) {
                if (typeof (e.dataSeries.visible) === "undefined" || e.dataSeries.visible) {
                    e.dataSeries.visible = false;
                } else {
                    e.dataSeries.visible = true;
                }
                e.chart.render();
            }
        },
        toolTip: {
            shared: false,
            content: "{name}: t: {x}, y: {value}"
        },
        data: vizData,
        rangeChanging: function (e) {
            vizFrozen = (e.trigger !== "reset");
        }
    };
    for (var channel in vizChannels) {
        var dataSeries = {
            type: "line",
            name: vizChannels[channel].name,
            showInLegend: true,
            dataPoints: []
        };

        vizData.push(dataSeries);
        vizChannels[channel].dataSeries = dataSeries;
    }

    chart = new CanvasJS.Chart("chartContainer", options);
    chart.render();
}


function updateViz(dataArray, startTime) {
    for (var i = 0; i < dataArray.length; i++) {
        var data = dataArray[i];
        var devicename = data["device"];
        var devicedata = data["data"];

        for (var variable in devicedata) {
            var vc = vizChannels[devicename + "." + variable];
            if (vc === undefined) {
                continue;
            }
            var dataSeries = vc.dataSeries;

            // get value for this channel
            var value = Number(devicedata[variable]["value"]);
            var percent = (Math.abs(value) - vc.min) * 100 / (vc.max - vc.min);

            // get the three relevant timestamps, and do the math
            if (vc.offset === undefined) {
                var serverTime = Number(data["servertime"]);
                var deviceTime = Number(devicedata["devicetime"]);
                if (startTime === undefined) {
                    startTime = serverTime;
                    logStart = serverTime;
                }
                var offset = deviceTime - (serverTime - startTime);
                if (!isNaN(offset)) {
                    vc.offset = offset;
                }
            }
            var varTime = Number(devicedata[variable]["vartime"]);
            varTime -= vc.offset;
            varTime = Math.max(varTime, 0);
            var secs = Math.round(varTime * 10) / 10000;

            maxTime = Math.max(maxTime, secs);
            if (liveServer) {
                if (!vizFrozen) {
                    chart.axisX[0].set("viewportMinimum", Math.max(maxTime - 60, 0));
                    chart.axisX[0].set("viewportMaximum", Math.max(maxTime, 60));
                }
            }

            //console.log("x: "+varTime+" y: "+percent)

            dataSeries.dataPoints.push({x: secs, y: percent, value: value});
            while (dataSeries.dataPoints.length > 100000) {
                dataSeries.dataPoints.shift();
            }
        }
    }
    if (!liveServer) {
        chart.axisX[0].set("viewportMinimum", 0);
        chart.axisX[0].set("viewportMaximum", maxTime);
    }

    chart.render();
}

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

function updateStatus(data, raw, startTime) {
    if (data !== null) {

        document.getElementById("data").value = infoFromData(data, startTime);
        if (raw !== null) {
            document.getElementById("data").value += "\n\n" + raw;
        }
        updateViz(data, startTime);
    }
}


function getStatus() {
    // for the real thing: web request to server
    request({url: "/protected/getstatus", method: "GET"})
            .then(raw => {
                data = JSON.parse(raw);
                updateStatus(data, raw, logStart);
                //console.log(data);
            })
            .catch(error => {
                console.log("getstatus error: " + error);
                //console.log("stopping status requests to server");
                stopStatus();
                selectButton("stopLog", "startLog");
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

function openTab(evt, tabName) {
    // Declare all variables
    var i, tabcontent, tablinks;
    // Get all elements with class="tabcontent" and hide them
    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }

    // Get all elements with class="tablinks" and remove the class "active"
    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
    }

    // Show the current tab, and add an "active" class to the button that opened the tab
    document.getElementById(tabName).style.display = "block";
    if (evt !== null) {
        evt.currentTarget.className += " active";
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




        