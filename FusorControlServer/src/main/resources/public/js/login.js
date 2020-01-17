//
// login code
//



function submitLogin(event) {
    var fd = new FormData(event);
    fd.append("clientID", getClientID());
    request({url: "/login", method: "POST", body: fd})
            .then(data => {
                location.assign("/console.html");
            })
            .catch(error => {
                console.log("Error: " + error);
            });
    return false;
}





makeClientID();

