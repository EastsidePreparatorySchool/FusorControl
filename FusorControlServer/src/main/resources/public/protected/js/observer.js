//
// fusor main observer code
//

//
// init code
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
                enableAdminControls();
            }
        })
        .catch(error => {
            console.log("error: " + error);
        });


window.addEventListener('beforeunload', function (event) {
    localStorage.setItem("fusor_client", undefined);
});




createViz();




//
// for local testing: read next line from data file
//



if (liveServer) {
    initStatus();
    localStorage.setItem("fusor_client", "observer");
} else {
    testData = fullData;
    if (testData.length > 0) {
        console.log("length of test data: " + testData.length);
        updateStatus(testData, null, testData[0]["servertime"]);
    }
}




        