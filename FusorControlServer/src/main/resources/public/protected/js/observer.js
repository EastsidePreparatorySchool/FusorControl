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
request({url: "/client", method: "GET"})
        .then(data => {
            isAdmin = (data.endsWith(" (admin)"));
            loginInfo = data;
            console.log("server session client: " + data);
            
            if (isAdmin) {
                enableAdminControls(true);
                startLog();
            }
            
            document.getElementById("loginInfo").innerText = loginInfo;
        })
        .catch(error => {
            console.log("error: " + error);
        });





function loadLog() {
    stopStatus();
    updateStatus(fullData, null, fullData[0]["servertime"]);
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
enableAdminControls(true);
initStatus();


        