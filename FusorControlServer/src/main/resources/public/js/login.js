//
// login code
//



function submitLogin(event) {
    //event.preventDefault();
    request({url: "/login?clientID="+getClientID(), method: "POST", body: new FormData(event)})
            .then(data => {
                location.assign("/protected/index.html");
            })
            .catch(error => {
                console.log("Error: " + error);
            });
    return false;
}

makeClientID();



