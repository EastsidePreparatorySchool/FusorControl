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
    'RP.rp_stat': {name: 'RP status', shortname: 'RP status', unit: '', min: 0, max: 2, type: "discrete", datatype: "boolean"},
    'TMP.tmp_stat': {name: 'TMP status', shortname: 'TMP status', unit: '', min: 0, max: 2, type: "discrete", datatype: "boolean"},
    'TMP.pump_freq': {name: 'TMP frequency (Hz)', shortname: 'TMP drv freq', unit: 'Hz', min: 0, max: 1250, type: "continuous", datatype: "numeric"},
    'TMP.pump_curr_amps': {name: 'TMP current (A)', shortname: 'TMP amps', unit: 'A', min: 0, max: 2.5, type: "continuous", datatype: "numeric"},
    'PIRANI.p3': {name: 'Piezo pressure', shortname: 'P-PIEZO', unit: 'mTorr', factor: 1000, min: 0, max: 800000, type: "continuous", datatype: "numeric"},
    'PIRANI.p1': {name: 'Pirani pressure', shortname: 'P-PIRANI', unit: 'mTorr', factor: 1000, min: 0, max: 500, type: "continuous", datatype: "numeric"},
    'PIRANI.p4': {name: 'Pirani pressure (fine)', shortname: 'P-COMB', unit: 'mTorr', factor: 1000, min: 0, max: 50, type: "continuous", datatype: "numeric"},
    'GAS.sol_stat': {name: 'Solenoid status', shortname: 'SOL status', unit: '', min: 0, max: 3, type: "discrete", datatype: "boolean"},
    'GAS.nv_stat': {name: 'Needle valve', shortname: 'NV status', unit: '%', min: 0, max: 100, type: "discrete", datatype: "numeric"},
    'VARIAC.input_volts': {name: 'Variac target (V)', shortname: 'VAR target', unit: 'V', min: 0, max: 130, type: "continuous", datatype: "numeric"},
    'VARIAC.dial_volts': {name: 'Variac dial (V)', shortname: 'VAR dial', unit: 'V', min: 0, max: 130, type: "continuous", datatype: "numeric"},
    'HV-LOWSIDE.variac_rms': {name: 'Variac RMS (V)', shortname: 'VAR rms', unit: 'V', min: 0, max: 130, type: "continuous", datatype: "numeric"},
    'HV-LOWSIDE.nst_rms': {name: 'NST RMS (kV)', shortname: 'NST rms', unit: 'KV', min: 0, max: 15, type: "continuous", datatype: "numeric"},
    'HV-LOWSIDE.cw_avg': {name: 'CW ABS AVG (kV)', shortname: 'CW volt', unit: 'KV', min: 0, max: 50, type: "continuous", datatype: "numeric"},
    'HV-HIGHSIDE.hs_current_adc': {name: 'CW current (adc)', shortname: 'CW current', unit: 'adc', min: 0, max: 50, type: "continuous", datatype: "numeric"},
    'GC-SERIAL.cps': {name: 'GCW (cps)', shortname: 'GCW clicks', unit: 'cps', min: 0, max: 100, type: "discrete trailing", datatype: "numeric"},
    'PN-JUNCTION.total': {name: 'PNJ (adc)', shortname: 'PN-J raw', unit: 'adc', min: 0, max: 100, type: "continuous", datatype: "numeric"},
    'Heartbeat.beat': {name: 'Heartbeat', shortname: 'HEARTBEAT', unit: '', min: 0, max: 50, type: "momentary", datatype: "numeric"},
    //'Heartbeat.logsize': {name: 'Log size (kEntries)', shortname: 'LOGSIZE', unit: 'kEntries', min: 0, max: 10000, type: "discrete", datatype: "numeric"},
    'Comment.text': {name: 'Comment', shortname: '', min: 0, max: 30, type: "momentary", datatype: "text"},
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
        }
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
            devices[deviceName] = {time: -1};
            var name = vizChannels[channel].shortname;
            name = name + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;".substring(0, (14 - name.length) * 6);
            textDisplay += name + ":&nbsp;<span id='" + channel + "'>n/c&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>&nbsp"
                    + vizChannels[channel].min + "&nbsp;-&nbsp;" + vizChannels[channel].max + "&nbsp;"
                    + vizChannels[channel].unit + "<br>"; // + "&nbsp;(<span id='" + channel + ".time'>n/c</span>)<br>";
            textChannels[channel] = {
                value: 0,
                last: -1,
                current: -1,
                type: vizChannels[channel].datatype,
                device: devices[deviceName]
            };
        }
    }
    document.getElementById("data").innerHTML = textDisplay;
}

//
// this is called from within updateViz() to populate the textChannels data structure with new data
// CanvasJS/ChartJS agnostic
//
function updateText(channel, value, type, time, deviceTime) {
    var tc = textChannels[channel];
    if (tc !== undefined) {
        tc.value = value;
        tc.current = time;
        tc.type = type;
        tc.device.time = deviceTime;
    }
}

//
// this pushes the text data out on to the screen
// CanvasJS/ChartJS agnostic
//
function renderText(update, secs) {
    for (var channel in textChannels) {
        var tc = textChannels[channel];
        //var timespan = document.getElementById(channel + ".time");
        var valspan = document.getElementById(channel);
        if (((tc.current !== tc.last) && update) || offline) {
            // value for variable is new, according to its timestamp
            valspan.style.color = "gold";
            valspan.style.fontWeight = "bold";
            if (tc.type === "boolean") {
                valspan.innerHTML = tc.value !== 0 ?
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;on" :
                        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;off";
            } else if (tc.type === "numeric") {
                // make it a nice 6.2 format
                var text = Number.parseFloat(tc.value).toFixed(2);
                valspan.innerHTML = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;".substring(text.length * 6) + text;
            }
        } else if ((secs > tc.device.time + 10) || !update) {
            // device has not reported in n seconds
            valspan.style.color = "gray";
            valspan.style.fontWeight = "normal";
        } else if ((secs > tc.last + 10) || !update) {
            // device is there, but variable is stale
            valspan.style.color = "gold";
            valspan.style.fontWeight = "normal";
        }
        tc.last = tc.current;
    }
    renderButtons();
}

//
// this updates the buttons on the left to reflect certain status like Solenoid on/off
// CanvasJS/ChartJS agnostic
// incomplete/buggy
//
function renderButtons() {
    var tc = textChannels["RP.rp_stat"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("rpon", "rpoff");
    } else {
        selectButton("rpoff", "rpon");
    }

    var tc = textChannels["TMP.tmp_stat"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("tmpon", "tmpoff");
    } else {
        selectButton("tmpoff", "tmpon");
    }

    tc = textChannels["GAS.sol_stat"];
    if (tc !== undefined && tc.value !== 0) {
        selectButton("solon", "soloff");
    } else {
        selectButton("soloff", "solon");
    }

    tc = textChannels["VARIAC.input_volts"];
    if (tc !== undefined && !isAdmin) {
        document.getElementById("variacValue").value = tc.value;
    }

    tc = textChannels["GAS.nv_stat"];
    if (tc !== undefined && !isAdmin) {
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
                        value *= vc.factor;
                    }
                    percent = (Math.abs(value) - vc.min) * 100 / (vc.max - vc.min);
                }

                // get the three relevant timestamps, and do the math
                var serverTime = Number(data["servertime"]);
                var deviceTime = Number(devicedata["devicetime"]);
                if (vc.offset === undefined) {
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
                deviceTime = Number(devicedata["devicetime"]);
                deviceTime -= vc.offset;
                deviceTime = Math.max(deviceTime, 0);
                var deviceSecs = Math.round(deviceTime * 10) / 10000;
                if (isNaN(maxTime)) {
                    maxTime = 0;
                }
                maxTime = Math.max(maxTime, secs);
                //console.log("x: "+varTime+" y: "+percent)
                if (!textOnly) {
                    addDataPoint(dataSeries, vc.type, secs, percent, value, vc.unit, serverTime, vc.name);
                }
                updateText(devicename + "." + variable, value, vc.datatype, secs, deviceSecs);
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
            dataPoints.push({x: secs, y: 0, value: value, unit: unit, time: time, device: device});
            break;
        case "continuous":
        // just put the point in
        default:
            dataPoints.push({x: secs, y: percent, value: value, unit: unit, time: time, device: device});
            break;
    }
    // in live view, constrain ourselves to xxx data points per series
    if (liveServer) {
        if (data.length > 2000) {
            while (data.length > 1000) {
                data.shift();
            }
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
                        mode: 'x',
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







