//
// fusor device status -> graph
//

var vizData = [];
var chart = null;
var vizFrozen = false;
var vizChannels = {
    'TMP.tmp_stat': {name: 'TMP status', shortname: 'TMP status', unit: '', min: 0, max: 2, type: "discrete", datatype: "boolean"},
    'TMP.pump_freq': {name: 'TMP frequency (Hz)', shortname: 'TMP drv freq', unit: 'Hz', min: 0, max: 1250, type: "continuous", datatype: "numeric"},
    'TMP.pump_curr_amps': {name: 'TMP current (A)', shortname: 'TMP amps', unit: 'A', min: 0, max: 2.5, type: "continuous", datatype: "numeric"},
    'DIAPHRAGM.diaphragm_adc': {name: 'Rough pressure (adc)', shortname: 'DPHRGM raw', unit: 'adc', min: 0, max: 110, type: "continuous", datatype: "numeric"},
    'PIRANI.pirani_adc': {name: 'Fine pressure (adc)', shortname: 'PIRANI raw', unit: 'adc', min: 0, max: 1024, type: "continuous", datatype: "numeric"},
    'GAS.sol_stat': {name: 'Solenoid status', shortname: 'SOL status', unit: '', min: 0, max: 3, type: "discrete", datatype: "boolean"},
    'GAS.nv_stat': {name: 'Needle valve', shortname: 'NV status', unit: '%', min: 0, max: 100, type: "discrete", datatype: "numeric"},
    'VARIAC.input_volts': {name: 'Variac target (V)', shortname: 'VAR targ', unit: 'V', min: 0, max: 130, type: "continuous", datatype: "numeric"},
    'VARIAC.potentiometer': {name: 'Variac actual (V)', shortname: 'VAR dial', unit: 'V', min: 0, max: 130, type: "continuous", datatype: "numeric"},
    'HV-LOWSIDE.variac_rms': {name: 'Variac RMS (V)', shortname: 'VAR rms', unit: 'V', min: 0, max: 130, type: "continuous", datatype: "numeric"},
    'HV-LOWSIDE.nst_rms': {name: 'NST RMS (kV)', shortname: 'NST rms', unit: 'KV', min: 0, max: 15, type: "continuous", datatype: "numeric"},
    'HV-LOWSIDE.cw_avg': {name: 'CW ABS AVG (kV)', shortname: 'CW volts', unit: 'KV (abs)', min: 0, max: 50, type: "continuous", datatype: "numeric"},
    'HV-HIGHSIDE.hs_current_adc': {name: 'CW current (adc)', shortname: 'CW current', unit: 'adc', min: 0, max: 50, type: "continuous", datatype: "numeric"},
    'GC-SERIAL.cps': {name: 'GCW (cps)', shortname: 'GCW clicks', unit: 'cps', min: 0, max: 100, type: "discrete trailing", datatype: "numeric"},
    'PN-JUNCTION.total': {name: 'PNJ (adc)', shortname: 'PN-J raw', unit: 'adc', min: 0, max: 100, type: "continuous", datatype: "numeric"},
    'Heartbeat.beat': {name: 'Heartbeat', shortname: 'HEARTBEAT', unit: '', min: 0, max: 50, type: "momentary", datatype: "numeric"},
    'Heartbeat.logsize': {name: 'Log size (kEntries)', shortname: 'LOGSIZE', unit: 'kEntries', min: 0, max: 10000, type: "discrete", datatype: "numeric"},

    'Comment.text': {name: 'Comment', shortname: '', min: 0, max: 4, type: "momentary", datatype: "text"},
    'Login.text': {name: 'Login', shortname: '', min: 0, max: 3, type: "momentary", datatype: "text"},
    'Command.text': {name: 'Command', shortname: '', min: 0, max: 2, type: "momentary", datatype: "text"}

//    'OCR.text': {name: 'OCR text', shortname:'', min: 0, max: 1.5, type: "momentary", datatype: "text"},
//    'OCR.confidence': {name: 'OCR confidence', shortname:'', min: 0, max: 200, type: "momentary", datatype: "numeric"},
//    'OCR.double': {name: 'OCR value', shortname:'', min: 0, max: 1000, type: "momentary", datatype: "numeric"},
};


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

var textChannels = {};

function createText() {
    var textDisplay = "";
    for (var channel in vizChannels) {
        if (vizChannels[channel].shortname !== '') {
            textDisplay += vizChannels[channel].shortname + ":&nbsp;<span id='" + channel + "'>n/c</span>&nbsp"
                    + vizChannels[channel].unit + "<br>";// + "&nbsp;(<span id='" + channel + ".time'>n/c</span>)<br>";
            textChannels[channel] = {value: 0, last: 0, current: 0, type: vizChannels[channel].datatype};
        }
    }
    document.getElementById("data").innerHTML = textDisplay;
}


function updateText(channel, value, type, time) {
    var tc = textChannels[channel];

    if (tc !== undefined) {
        tc.value = value;
        tc.current = time;
        tc.type = type;
    }
}

function renderText(update, secs) {
    for (var channel in textChannels) {
        var tc = textChannels[channel];
        //var timespan = document.getElementById(channel + ".time");
        var valspan = document.getElementById(channel);

        if ((tc.current !== tc.last) && update) {
            valspan.style.color = "gold";
            if (tc.type === "boolean") {
                valspan.innerText = tc.value !== 0 ? "on" : "off";
            } else if (tc.type === "numeric") {
                valspan.innerText = Math.round(tc.value * 100) / 100;
            }
        } else if ((secs > tc.last + 3) || !update) {
            valspan.style.color = "gray";
        }
        tc.last = tc.current;
    }
}

function renderButtons() {
    var tc = textChannels["TMP.tmp"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("tmpon", "tmpoff");
    } else {
        selectButton("tmpoff", "tmpon");
    }
    
    tc = textChannels["GAS.solenoid"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("solon", "soloff");
    } else {
        selectButton("soloff", "solon");
    }
}
function resetViz() {
    for (var channel in vizChannels) {
        var vc = vizChannels[channel];
        vc.dataSeries.dataPoints = [];
        maxTime = 0;
        startTime = undefined;
        logstart = undefined;
        vc.offset = undefined;
    }
    renderChart();
}

function updateViz(dataArray) {
    for (var i = 0; i < dataArray.length; i++) {
        var data = dataArray[i];
        var devicename = data["device"];
        var devicedata = data["data"];
        if (devicename === "<reset>") {
            // restart visualization with fresh log data
            resetViz();
            continue;
        }
        if (devicename === "<promote>") {
            checkAdminControls();
        }

        //
        // now add important variables to display
        // see declaration of vizChannels above to see what is included
        //

        for (var variable in devicedata) {
            try {
                var vc = vizChannels[devicename + "." + variable];
                if (vc === undefined) {
                    continue;
                }
                var dataSeries = vc.dataSeries;
                // get value for this channel
                var value;
                var percent;
                if (vc.datatype === "text") {
                    percent = (1 - vc.min) * 100 / (vc.max - vc.min);
                    if (devicedata["observer"] !== undefined) {
                        value = devicedata["observer"]["value"] + ":" + devicedata[variable]["value"];
                        displayComment(devicedata["observer"]["value"], data["servertime"], devicedata["text"]["value"]);
                    } else {
                        value = "\"" + devicedata[variable]["value"] + "\"";
                    }
                } else {
                    value = Number(devicedata[variable]["value"]);
                    percent = (Math.abs(value) - vc.min) * 100 / (vc.max - vc.min);
                }

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


                //console.log("x: "+varTime+" y: "+percent)
                addDataPoint(dataSeries, vc.type, secs, percent, value);
                updateText(devicename + "." + variable, value, vc.datatype, secs);
            } catch (error) {
                console.log(error);
            }
        } // for variable
    } // for data item

    //
    // adjust the view port
    //

    if (liveServer) {
        if (!vizFrozen) {
            setViewPort(Math.max(maxTime - 60, 0), Math.max(maxTime, 60));
        }
        document.getElementById("logtime").innerText = String(Math.round(maxTime * 100) / 100);
        renderText(true, maxTime);
    } else {
        setViewPort(0, maxTime);
    }

    renderChart();
}

function addDataPoint(dataSeries, type, secs, percent, value) {
    switch (type) {
        case "momentary":
            dataSeries.dataPoints.push({x: secs - 0.0001, y: 0, value: 0});
            dataSeries.dataPoints.push({x: secs, y: percent, value: value});
            dataSeries.dataPoints.push({x: secs + 0.0001, y: 0, value: 0});
            break;
        case "discrete":
            if (dataSeries.dataPoints.length > 0) {
                var lastPoint = dataSeries.dataPoints[dataSeries.dataPoints.length - 1];
                dataSeries.dataPoints.push({x: secs - 0.0001, y: lastPoint.y, value: lastPoint.value});
            }
            dataSeries.dataPoints.push({x: secs, y: percent, value: value});
            break;
        case "discrete trailing":
            if (dataSeries.dataPoints.length > 0) {
                var lastPoint = dataSeries.dataPoints[dataSeries.dataPoints.length - 1];
                dataSeries.dataPoints.push({x: lastPoint.x + 0.0001, y: percent, value: value});
            }
            dataSeries.dataPoints.push({x: secs, y: 0, value: value});
            break;
        case "continuous":
        default:
            dataSeries.dataPoints.push({x: secs, y: percent, value: value});
            break;
    }
    // in live view, constrain ourselves to xxx data points per series
    if (liveServer) {
        while (dataSeries.dataPoints.length > 10000) {
            dataSeries.dataPoints.shift();
        }
    }
}

function setViewPort(min, max) {
    chart.axisX[0].set("viewportMinimum", min);
    chart.axisX[0].set("viewportMaximum", max);
}


function renderChart() {
    chart.render();
}







        