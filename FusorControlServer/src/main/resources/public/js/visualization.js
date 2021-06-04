//
// fusor device status -> graph
//

var urlParams = new URLSearchParams(window.location.search);
var usingChartJS = (urlParams.get("chartjs") === "1");
var vizData = [];           // holds all our data series
var chart = null;           // holds central chart object
var vizFrozen = false;      // CanvasJS allows to zoom and pan, we freeze the display for it
var continuousViz = false;  // set viewport every time we have new data
var currentViewMin = -1;    // keep track
var currentViewMax = -1;    // keep track
var viewWidth = 60;         // how much data to show in window
var viewIncrement = 1;      // how often to slide window
var viewLead = viewIncrement / 5;
var maxTime = 0;

//
// this is the most important data structure here
//
// the key is a concatenation of device name (as reported by its Arduino) and a variable name published by that device
//
// name: is the name of the line in the chart
// shortname: is the line in the text display on the left. No shortname, no text display
// unit: is for tooltips and text display
// factor: if present, will multiply incoming data by this (e.g. Pirani reports Torr, but I want to display microns (milliTorr))
// min and max: let us calculate a percentage from the actual value, and display that. changing this allows to change the height of "momentary" spikes
// type: for explanation, see addDataPoint() further down
// datatype: used for both line and text display
//
var vizChannels = {
    'RP.rp_in': {name: 'RP status', shortname: 'RP status', unit: '', min: 0, max: 2, type: "discrete", datatype: "boolean"},
    'TMP.tmp': {name: 'TMP status', shortname: 'TMP status', unit: '', min: 0, max: 2, type: "discrete", datatype: "boolean"},
    'TMP.error': {name: 'TMP error', shortname: 'TMP error', unit: '', min: 0, max: 2, type: "discrete", datatype: "error"},
    'TMP.lowspeed': {name: 'TMP lowspeed', shortname: 'TMP lowspeed', unit: '', min: 0, max: 2, type: "discrete", datatype: "boolean"},
    'TMP.pump_freq': {name: 'TMP frequency (Hz)', shortname: 'TMP drv freq', unit: 'Hz', min: 0, max: 1250, type: "continuous", datatype: "numeric", graph: "yes"},
    'TMP.pump_curr_amps': {name: 'TMP current (A)', shortname: 'TMP amps', unit: 'A', min: 0, max: 2.5, type: "continuous", datatype: "numeric", graph: "yes"},
    'PIRANI.p2': {name: 'Piezo relative pressure', shortname: 'P piezo rel', unit: 'Torr', factor: 1, min: -770, max: 0, type: "continuous", datatype: "numeric", graph: "yes"},
    'PIRANI.p3': {name: 'Pirani pressure (coarse)', shortname: 'P abs coarse', unit: 'mTorr', factor: 1000, min: 0, max: 500, type: "continuous", datatype: "numeric", graph: "yes"},
    'PIRANI.p4': {name: 'Pirani pressure (fine)', shortname: 'P abs fine', unit: 'mTorr', factor: 1000, min: 0, max: 100, type: "continuous", datatype: "numeric", graph: "yes"},
    'GAS.sol_in': {name: 'Solenoid status', shortname: 'SOL status', unit: '', min: 0, max: 3, type: "discrete", datatype: "boolean"},
    'GAS.nv_in': {name: 'Needle valve percent', shortname: 'NV %', unit: '%', min: 0, max: 100, type: "discrete", datatype: "numeric", graph: "yes"},
    'HV-RELAY.in': {name: 'Variac relay', shortname: 'VAR relay', unit: '', min: 0, max: 1.8, type: "discrete", datatype: "boolean"},
    'VARIAC.input_volts': {name: 'Variac target (V)', shortname: 'VAR target', unit: 'V', min: 0, max: 140, type: "discrete", datatype: "numeric", graph: "yes"},
    'VARIAC.dial_volts': {name: 'Variac dial (V)', shortname: 'VAR dial', unit: 'V', min: 0, max: 140, type: "discrete", datatype: "numeric", graph: "yes"},
    'HV-LOWSIDE.n': {name: 'Variac samples', shortname: 'VAR samples', unit: '', min: 0, max: 1000, type: "discrete", datatype: "numeric"},
    'HV-LOWSIDE.variac_rms': {name: 'Variac RMS (V)', shortname: 'VAR rms', unit: 'V', min: 0, max: 130, type: "continuous", datatype: "numeric", graph: "yes"},
    'HV-LOWSIDE.nst_rms': {name: 'NST RMS (kV)', shortname: 'NST rms', unit: 'kV', min: 0, max: 8, type: "continuous", datatype: "numeric", graph: "yes"},
    'HV-LOWSIDE.cw_avg': {name: 'CW ABS AVG (kV)', shortname: 'CW abs voltage', unit: 'kV', min: 0, max: 50, factor: -1, type: "continuous", datatype: "numeric", graph: "yes"},
    'HV-LOWSIDE.cwc_avg': {name: 'CW current (mA)', shortname: 'CW current', unit: 'mA', min: 0, max: 10, type: "continuous", datatype: "numeric", graph: "yes"},
    'PN-JUNCTION.total': {name: 'Photons PNJ (ADC)', shortname: 'PN-J', unit: 'ADC raw', min: 0, max: 1023, type: "continuous", datatype: "numeric", graph: "yes"},
    'SENSORARRAY.pin': {name: 'Photons PIN (uSv/h)', shortname: 'PIN', unit: 'uSv/h', min: 0, max: 100, type: "continuous", datatype: "numeric", graph: "yes"},
    'SENSORARRAY.gc1': {name: 'GC1 (Whitmer, inside) (cps)', shortname: 'GC1 (W) inside', unit: 'cps', min: 0, max: 100, type: "discrete trailing", datatype: "numeric", graph: "yes"},
    'SENSORARRAY.gc2': {name: 'GC2 (inside) (cps)', shortname: 'GC2 inside', unit: 'cps', min: 0, max: 250, type: "discrete trailing", datatype: "numeric", graph: "yes"},
    'SENSORARRAY.gc3': {name: 'GC2 (outside) (cps)', shortname: 'GC3 outside', unit: 'cps', min: 0, max: 250, type: "discrete trailing", datatype: "numeric", graph: "yes"},
    'Heartbeat.beat': {name: 'Heartbeat', shortname: '', unit: '', min: 0, max: 50, type: "momentary", datatype: "numeric", graph: "yes"},
    //'Heartbeat.logsize': {name: 'Log size (kEntries)', shortname: 'LOGSIZE', unit: 'kEntries', min: 0, max: 10000, type: "discrete", datatype: "numeric"},
    'Comment.text': {name: 'Comment', shortname: '', min: 0, max: 5, type: "momentary", datatype: "text", graph:"yes"},
    'Login.text': {name: 'Login', shortname: '', min: 0, max: 40, type: "momentary", datatype: "text"},
    'Command.text': {name: 'Command', shortname: '', min: 0, max: 20, type: "momentary", datatype: "text"}

};
//
// create the HTML and chart for whatever library we are using
//
function createViz() {
    var container = document.getElementById("fchart");
    var chart;
    if (usingChartJS) {
        container.innerHTML = "<canvas id='chartContainer' style='background-color: white; width:100%;height:100%'></canvas>";
        chart = createVizChartJS();
        container.ondblclk = function () {
            chart.resetZoom();
        };
    } else {
        container.innerHTML = "<div id='chartContainer'></div>";
        chart = createVizCanvasJS();
    }
}

//
// create the chart for CanvasJS. For a chartJS version, see end of the file
//
function createVizCanvasJS() {
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
            maximum: 100,
            minimum: 0
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
            content: "{name}: t: {x}, y: {value} {unit}",
            contentFormatter: function (e) {
                var dataPoint = e.entries[0].dataPoint;
                updateCorrespondingText(dataPoint);
                return makeTooltipText(dataPoint);
            }
        },
        data: vizData,
        rangeChanging: function (e) {
            vizFrozen = (e.trigger !== "reset");
        }

    };
    for (var channel in vizChannels) {
        if (vizChannels[channel]['graph'] !== undefined) {
            var dataSeries = {
                type: "line",
                name: vizChannels[channel].name,
                showInLegend: true,
                dataPoints: [],
                markerType: "none"
            };
            vizData.push(dataSeries);
            vizChannels[channel].dataSeries = dataSeries;
        }
    }

    chart = new CanvasJS.Chart("chartContainer", options);
    chart.render();
    return chart;
}


//
// Update text data for a corresponding graph point
// CanvasJS/ChartJS agnostic
//

function updateCorrespondingText(dataPoint) {
    if (!offline || liveServer) {
        return;
    }

    // find the data index belonging to that chart point
    var index = bSearchLog(dataPoint.time);
    // go back to just past the previous entry for this device,
    var prior = findPrior(index, dataPoint.device);
    // now run that slice of data through the updater.
    updateViz(offlineLog.slice(prior, index + 1), true);
}


//
// make tooltip text
// CanvasJS/ChartJS agnostic
//
function makeTooltipText(dataPoint) {
    if (isNaN(dataPoint.value)) {
        return `${dataPoint.device}: t: ${Number(dataPoint.x).toFixed(2)}, y: ${dataPoint.value} ${dataPoint.unit}`;
    }
    return `${dataPoint.device}: t: ${Number(dataPoint.x).toFixed(2)}, y: ${Number(dataPoint.value).toFixed(2)} ${dataPoint.unit}`;
}

//
// set up the text display on the right of the screen
// textChannels keeps track of what values we have seen
// devices keeps track of when we last heard from a device
// CanvasJS/ChartJS agnostic
//

var textChannels = {};
var devices = {};
function createText() {
    var textDisplay = "";
    for (var channel in vizChannels) {
        if (vizChannels[channel].shortname !== '') {
            // make a device map to keep track of device time
            var deviceName = channel.substring(0, channel.indexOf('.'));
            devices[deviceName] = {time: -1, timeS: -1};
            var name = vizChannels[channel].shortname;
            name = name + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;".substring(0, (14 - name.length) * 6);
            textDisplay += name + ":&nbsp;<span id='" + channel + "' style='color:gray;font-weight:normal;'>n/c&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>&nbsp"
                    + (vizChannels[channel].unit.length > 0 ? (vizChannels[channel].min + "&nbsp;-&nbsp;" + vizChannels[channel].max + "&nbsp;"
                            + vizChannels[channel].unit) : "")
                    + "<br>"; // + "&nbsp;(<span id='" + channel + ".time'>n/c</span>)<br>";
            textChannels[channel] = {
                value: 0,
                last: -1,
                lastS: -1,
                current: -1,
                currentS: -1,
                type: vizChannels[channel].datatype,
                device: devices[deviceName],
                updated: false
            };
        }
    }
    document.getElementById("data").innerHTML = textDisplay;
}

//
// this is called from within updateViz() to populate the textChannels data structure with new data
// CanvasJS/ChartJS agnostic
//
function updateText(channel, value, type, varTime, varTimeS, deviceTime, deviceTimeS) {
    var tc = textChannels[channel];
    if (tc !== undefined) {
        tc.value = value;
        tc.current = varTime;
        tc.currentS = varTimeS;
        tc.type = type;
        tc.device.time = deviceTime;
        tc.device.timeS = deviceTimeS;
        tc.updated = (varTime !== tc.last) && varTime !== -1;
    }
}

//
// this pushes the text data out on to the screen
// CanvasJS/ChartJS agnostic
//
function renderText(update, now) {
    for (var channel in textChannels) {
        var tc = textChannels[channel];
        //var timespan = document.getElementById(channel + ".time");
        var valspan = document.getElementById(channel);
        if ((tc.updated && update) || offline) {
            // value for variable is new, according to its "updated" marker
            tc.updated = false;
//            if (channel === "HV-RELAY.in")
//                console.log("BOLD "+channel + " D: " + tc.device.time + ", L: " + tc.last + ", V: " + tc.current);
            valspan.style.color = "gold";
            valspan.style.fontWeight = "bold";
            if (tc.type === "boolean") {
                valspan.innerHTML = tc.value !== 0 ?
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;on" :
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;off";
            }
            if (tc.type === "error") {
                valspan.innerHTML = tc.value === 0 ?
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ok" :
                        "&nbsp;&nbsp;&nbsp;&nbsp;<span style='color:red'>error</span>";
                //console.log(tc.value);
            } else if (tc.type === "numeric") {
                // make it a nice 6.2 format
                var text = Number.parseFloat(tc.value).toFixed(2);
                valspan.innerHTML = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;".substring(text.length * 6) + text;
            }
        } else if ((now > tc.device.timeS + 10) || !update) {
            // device has not reported in n seconds
            //console.log(channel+" D: "+tc.device.time+", L: "+secs);
            valspan.style.color = "gray";
            valspan.style.fontWeight = "normal";
        } else if ((now > tc.lastS + 10) || !update) {
            // device is there, but variable is stale
            valspan.style.color = "gold";
            valspan.style.fontWeight = "normal";
        } else {
            var test = 1;
            //console.log("WTF " + channel + " D: " + tc.device.time + ", L: " + tc.last + ", V: " + tc.current);
        }
        tc.last = tc.current;
        tc.lastS = tc.currentS;
    }
    renderButtons();
}

//
// this updates the buttons on the left to reflect certain status like Solenoid on/off
// CanvasJS/ChartJS agnostic
// incomplete/buggy
//
function renderButtons() {
    var tc = textChannels["RP.rp_in"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("rpon", "rpoff");
    } else {
        selectButton("rpoff", "rpon");
    }

    var tc = textChannels["TMP.tmp"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("tmpon", "tmpoff");
    } else {
        selectButton("tmpoff", "tmpon");
    }

    var tc = textChannels["TMP.lowspeed"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("tmplow", "tmphigh");
    } else {
        selectButton("tmphigh", "tmplow");
    }

    tc = textChannels["GAS.sol_in"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("solon", "soloff");
    } else {
        selectButton("soloff", "solon");
    }

    tc = textChannels["VARIAC.input_volts"];
    if (tc !== undefined && tc.current !== tc.last) {
        document.getElementById("variacValue").value = tc.value;
    }

    tc = textChannels["GAS.nv_in"];
    if (tc !== undefined && tc.current !== tc.last) {
        document.getElementById("needleValue").value = tc.value;
    }
}

//
// this resets the visualization for switches between live/offline
// stale, might have bugs
//
function resetViz() {
    for (var channel in vizChannels) {
        var vc = vizChannels[channel];
        if (vc.dataSeries === undefined) {
            continue;
        }
        if (usingChartJS) {
            vc.dataSeries.data = [];
        } else {
            vc.dataSeries.dataPoints = [];
        }
        maxTime = 0;
        startTime = undefined;
        logstart = undefined;
        vc.offset = undefined;
    }
    renderChart();
}

//
// main function to interpret JSON data, scale data for display, push all updates
// CanvasJS/CharJS agnostic
// textOnly: don't update the chart. Don't add datapoints to series. just update the text display
//
function updateViz(dataArray, textOnly) {
    // updates come in JSON array of device entries
    for (var i = 0; i < dataArray.length; i++) {
        var data = dataArray[i];
        var devicename = data["device"];
        var devicedata = data["data"];
        // special pseudo device for reset
        if (devicename === "<reset>") {
            // restart visualization with fresh log data
            resetViz();
            console.log("<reset> ts " + data["servertime"]);
            startTime = Number(data["servertime"]);
            logStart = startTime;
            continue;
        }

        // if someone was promoted to admin, update the controls
        // done with another pseudo-device
        if (devicename === "<promote>") {
            checkAdminControls();
            continue;
        }

        //
        // now add important variables to display
        // see declaration of vizChannels above to see what is included
        //
        for (var variable in devicedata) {
            try {
                var vc = vizChannels[devicename + "." + variable];
                if (vc === undefined) {
                    // vizChannels are a de-facto view of the data for us
                    // if this variable is not listed, we are on to the next one
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
                        if (liveServer && !textOnly) {
                            displayComment(devicedata["observer"]["value"], data["servertime"], devicedata["text"]["value"]);
                        }
                    } else {
                        value = "\"" + devicedata[variable]["value"] + "\"";
                    }
                } else {
                    value = Number(devicedata[variable]["value"]);
                    if (vc.factor !== undefined) {
                        if (vc.factor === 0) {
                            value = Math.abs(value);
                        } else {
                            value *= vc.factor;
                        }
                    }
                    percent = (value - vc.min) * 100 / (vc.max - vc.min);
                }

                // get the three relevant timestamps, and do the math
                var serverTime = Number(data["servertime"]); // time when msg was received by server
                var deviceTime = Number(devicedata["devicetime"]); // device time when it was sent
                var varTime = Number(devicedata[variable]["vartime"]); // device time when variable was last updated
                if (startTime === undefined) {
                    startTime = serverTime;
                    logStart = serverTime;
                }
                var varTimeS = varTime - deviceTime; // we care about when the variable was read relative to the device timestamp
                varTimeS += serverTime - startTime; // and we want the start of the log to be at 0 s
                varTimeS = Math.max(varTimeS, 0); // but no negative times please

                // we will call the rounded output "secs"
                var secs = Math.round(varTimeS * 10) / 10000;

                // also, convert and record the device timestamp so we can keep track of it and identify stale devices
                var deviceTimeS = deviceTime + serverTime - startTime;
                deviceTimeS = Math.max(deviceTimeS, 0);
                var deviceSecs = Math.round(deviceTime * 10) / 10000;

                // lastly, maxTime helps us make the chart wide enough
                if (isNaN(maxTime)) {
                    maxTime = 0;
                }
                maxTime = Math.max(maxTime, secs);

                //console.log("x: "+varTime+" y: "+percent)
                if (!textOnly && dataSeries !== undefined) {
                    addDataPoint(dataSeries, vc.type, secs, percent, value, vc.unit, serverTime, vc.name);
                }
                updateText(devicename + "." + variable, value, vc.datatype,
                        varTime / 1000, varTimeS / 1000,
                        deviceTime / 1000, deviceTimeS / 1000);
            } catch (error) {
                console.log(error);
            }
        } // for variable
    } // for data item

    //
    // adjust the view port
    //

    if (liveServer) { // view ports are different for offline - show everything - and live - show the last minute
        if (!textOnly) { // don't do this for just a text update
            if (!vizFrozen) { // leave it alone if live but panning and zooming
                if (continuousViz) {
                    setViewPort(Math.max(maxTime - viewWidth, 0), Math.max(maxTime, viewWidth));
                } else {
                    var nextIncrement = Math.ceil((maxTime + viewLead) / viewIncrement) * viewIncrement;
                    setViewPort(Math.max(nextIncrement - viewWidth, 0), Math.max(nextIncrement, viewWidth));
                }
                //console.log ("set view port for "+dataArray.length+" records");
            }
            // update the big time display on the right
            document.getElementById("logtime").innerText = Number.parseFloat(maxTime).toFixed(2);
        }
        renderText(true, maxTime);
    } else {
        if (!textOnly) { // don't do this for just a text update
            setViewPort(0, maxTime);
            renderChart();
        }
        renderText(true, secs);
    }
}

//
// this adds one x,y (secs, percent) datapoint, but also more info to show in a tooltip
//
function addDataPoint(dataSeries, type, secs, percent, value, unit, time, device) {
    // some parameter sanitation
    if (unit === undefined) {
        unit = "";
    }

    //
    // get me the right queue depending on the viz library we are using
    //
    var dataPoints;
    if (usingChartJS) {
        dataPoints = dataSeries.data;
        //secs = moment.unix(secs); // but we don't have the moments library
    } else {
        dataPoints = dataSeries.dataPoints;
    }

    //
    // this big switch makes different line behavior happen for different devices/variables, as defined in their vizChannel
    //
    switch (type) {
        case "momentary":
            // make a spike out of three points
            dataPoints.push({x: secs - 0.0001, y: 0, value: 0, unit: unit, time: time, device: device});
            dataPoints.push({x: secs, y: percent, value: value, unit: unit, time: time, device: device});
            dataPoints.push({x: secs + 0.0001, y: 0, value: 0, unit: unit, time: time, device: device});
            break;
        case "discrete":
            // flat lines that move at the time of the event
            // i.e. the datapoint reflects how things are from hereon
            if (dataPoints.length > 0) {
                var lastPoint = dataPoints[dataPoints.length - 1];
                dataPoints.push({x: secs - 0.0001, y: lastPoint.y, value: lastPoint.value, unit: unit, time: time, device: device});
            }
            dataPoints.push({x: secs, y: percent, value: value, unit: unit, time: time, device: device});
            break;
        case "discrete trailing":
            // flat lines that move at the last event before ours
            // i.e. the datapoint is interpreted to reflect how things have been since the last datapoint
            if (dataPoints.length > 0) {
                var lastPoint = dataPoints[dataPoints.length - 1];
                dataPoints.push({x: lastPoint.x + 0.0001, y: percent, value: value, unit: unit, time: time, device: device});
            }
            dataPoints.push({x: secs, y: percent, value: value, unit: unit, time: time, device: device});
            break;
        case "continuous":
        // just put the point in
        default:
            dataPoints.push({x: secs, y: percent, value: value, unit: unit, time: time, device: device});
            break;
    }
    // in live view, constrain ourselves to xxx data points per series
    if (liveServer) {
        while (dataPoints.length > 2000) {
            dataPoints.shift();
        }
    }
}

//
// set the view port
//
function setViewPort(min, max) {
    if (min === currentViewMin || max === currentViewMax) {
        // let's just render the chart and get out of here if the viewport
        // has not changed
        if (usingChartJS) {
        } else {
            renderChart();
        }
        return;
    }

    if (usingChartJS) {
    } else {
        chart.axisX[0].set("viewportMinimum", min);
        chart.axisX[0].set("viewportMaximum", max);
    }
    currentViewMin = min;
    currentViewMax = max;
}

//
// trigger the actual visual update
//
function renderChart() {
    if (usingChartJS) {
        chart.update();
    } else {
        chart.render();
    }
}


//
// chart.js specific: createViz
//
function createVizChartJS() {
    var color = Chart.helpers.color;
    var cfg = {
        data: {
            datasets: vizData
        },
        options: {
            legend: {
                position: "bottom"
            },
            animation: {
                duration: 0
            },
            scales: {
                xAxes: [{
                        type: 'time',
                        time: {
                            unit: 'second'
                        },
                        displayFormats: {
                            second: 'XXX.XX'
                        },
                        distribution: 'linear',
                        offset: true,
                        ticks: {
                            major: {
                                enabled: true,
                                fontStyle: 'bold'
                            },
                            source: 'data',
                            autoSkip: true,
                            autoSkipPadding: 75,
                            maxRotation: 0,
                            sampleSize: 100
                        }
                    }],
                yAxes: [{
                        gridLines: {
                            drawBorder: false
                        }
                    }]
            },
            tooltips: {
                intersect: false,
                mode: 'nearest',
                callbacks: {
                    title: function () {
                        return null;
                    },
                    label: function (tooltipItem, myData) {
                        var dataPoint = myData.datasets[tooltipItem.datasetIndex].data[tooltipItem.index];
                        updateCorrespondingText(dataPoint);
                        return makeTooltipText(dataPoint);
                    }
                }
            },

            plugins: {
                zoom: {
                    pan: {
                        enabled: true,
                        mode: 'x'
                    },

                    // Container for zoom options
                    zoom: {
                        enabled: true,
                        drag: true,
                        mode: 'x',

                        // Speed of zoom via mouse wheel
                        // (percentage of zoom on a wheel event)
                        speed: 0.1
                    }
                }
            }
        }
    };

    var ctx = document.getElementById('chartContainer').getContext('2d');
    chart = new Chart(ctx, cfg);
    window.chartColors = [
        'maroon',
        'brown',
        'olive',
        'teal',
        'navy',
        'black',
        'red',
        'orange',
        'yellow',
        'lime',
        'green',
        'cyan',
        'blue',
        'purple',
        'magenta',
        'grey',
        'pink',
        'apricot',
        'beige',
        'mint',
        'lavender'
    ];
    var i = 0;
    for (var channel in vizChannels) {
        var dataset = {
            label: vizChannels[channel].name,
            backgroundColor: color(window.chartColors[i]).alpha(0.5).rgbString(),
            borderColor: window.chartColors[i],
            data: [],
            type: 'line',
            pointRadius: 0,
            fill: false,
            lineTension: 0,
            borderWidth: 2
        };
        //        switch (vizChannels[channel].type) {
        //            case "momentary":
        //                dataset.steppedLine = 'middle';
        //                break;
        //            case "discrete":
        //                dataset.steppedLine = 'after';
        //                break;
        //            case "discrete trailing":
        //                dataset.steppedLine = 'before';
        //                break;
        //            case "continuous":
        //            default:
        //                dataset.steppedLine = false;
        //                break;
        //        }
        vizData.push(dataset);
        vizChannels[channel].dataSeries = dataset;
        i++;
    }

}







