//
// fusor main observer code
//


//
// enable admin controls if appropriate
//
var isAdmin = false;
request({url: "/clienttype", method: "GET"})
        .then(data => {
            isAdmin = (data === "admin");
            console.log("server session client: " + data);
            if (isAdmin) {
                enableAdminControls(true);
                startLog();
            }
        })
        .catch(error => {
            console.log("error: " + error);
        });


//
// erase the admin/observer thing before closing the window
//

window.addEventListener('beforeunload', function (event) {
    localStorage.setItem("fusor_client", undefined);
});


function loadLog() {
    stopStatus();
    updateStatus(fullData, null, fullData[0]["servertime"]);
    document.getElementById("loadLog").innerText = "Live View";
    document.getElementById("loadLog").onclick = displayLiveData;
    document.getElementById("comment").disabled = true;
    document.getElementById("commentbutton").disabled = true;
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


        