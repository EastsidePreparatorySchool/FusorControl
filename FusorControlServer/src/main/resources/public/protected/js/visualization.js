//
// fusor device status -> graph
//

var vizData = [];
var chart = null;
var vizFrozen = false;


var vizChannels = {
    'heartbeat.beat': {name: 'Heartbeat', variable: 'beat', min: 0, max: 5},
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


function resetViz() {
    for (var channel in vizChannels) {
        var vc = vizChannels[channel];
        vc.dataSeries.dataPoints = [];
        maxTime = 0;
        startTime = undefined;
        logstart = undefined;
        vc.offset = undefined;
    }
    chart.render();
}

function updateViz(dataArray, batchStartTime) {
    for (var i = 0; i < dataArray.length; i++) {
        var data = dataArray[i];
        var devicename = data["device"];
        var devicedata = data["data"];

        if (devicename === "<reset>") {
            // restart visualization with fresh log data
            resetViz();
            continue;
        }

        if (devicename === "comment") {
            // display chat comment - see comment.js
            displayComment(devicedata["observer"], data["servertime"], devicedata["text"]);
            continue;
        }

        //
        // now add important variables to display
        // see declaration of vizChannels above to see what is included
        //
        
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








        