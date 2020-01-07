//
// fusor main observer code
//


//
// reset weblog observer
//
request({url: "/resetobserver", method: "GET"})
        .then(data => {
        })
        .catch(error => {
            console.log("error: " + error);
        });



//
// enable admin controls if appropriate
//
var isAdmin = false;
var loginInfo = "<unknown>";


//
// get list of log files
//

function getLogs() {
    request({url: "/protected/getlogfilenames", method: "GET"})
            .then(raw => {
                var files = JSON.parse(raw);
                var list = document.getElementById("files");
                var listDiv = document.getElementById("filesdiv");
                listDiv.style.display = "block";
                var filesText = "<a class='hover' onclick='loadLog(this)'>[sample log]</a><br>";
                for (var i = 0; i < files.length; i++) {
                    filesText += "<a class='hover' onclick='loadLog(this)'>";
                    filesText += files[i];
                    filesText += "</a><br>";
                }
                list.innerHTML += filesText;
            })
            .catch(error => {
                console.log("error: " + error);
            });
}



function loadLog(fileName) {
    stopStatus();
    console.log("loading log: " + fileName.text);
    document.getElementById("filesdiv").style.display = "none";
    if (fileName.text === "[sample log]") {
        displayLog(fullData, fullData[0]["servertime"]);
        return;
    }

    request({url: "/protected/getlogfile?filename=" + fileName.text, method: "GET"})
            .then(raw => {
                if (raw.endsWith("},\n")) {
                    raw += "{}]}";
                }
                var data = JSON.parse(raw);
                displayLog(data["log"], data["base-timestamp"]);
            })
            .catch(error => {
                console.log("getlogfile error: " + error);
            });
}



function displayLog(data, timestamp) {
    updateStatus(data, null, timestamp);
    document.getElementById("loadLog").innerText = "Live View";
    document.getElementById("loadLog").onclick = displayLiveData;
    document.getElementById("comment").value = "<offline>";
    document.getElementById("comment").disabled = true;
    document.getElementById("commentbutton").disabled = true;
    document.getElementById("chat").innerText = "<offline>";
    enableAdminControls(false);
}

function displayLiveData() {
//    initStatus();
//    document.getElementById("loadLog").innerText = "Load Log";
//    document.getElementById("loadLog").onclick = loadLog;
//    document.getElementById("comment").disabled = false;
//    document.getElementById("commentbutton").disabled = false;
//    document.getElementById("comment").focus();
//    enableAdminControls(true);
    location.reload();
}

//
// init code
//


createViz();
checkAdminControls();
initStatus();


        