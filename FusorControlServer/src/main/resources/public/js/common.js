//
// fusor common client code
//



//
// HTML utilities
//

function selectButton(select, unselect) {
    document.getElementById(unselect).style.border = "none";
    document.getElementById(select).style.border = "2px solid black";
}

function enableButton(button) {
    document.getElementById(button).disabled = false;
}

function disableButton(button) {
    document.getElementById(button).disabled = true;
}

//
// promise-style request and underlying XmlRequest
//

function request(obj) {
    return new Promise((resolve, reject) => {
        let xhr = new XMLHttpRequest();

        // insert clientID
        if (obj.method !== "POST") {
            if (obj.url.includes("?")) {
                obj.url += "&";
            } else {
                obj.url += "?";
            }
            obj.url += "clientID=" + getClientID();
        }

        if (obj.async === undefined) {
            obj.async = true;
        }

        xhr.open(obj.method || "GET", obj.url, obj.async);
        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                resolve(xhr.response);
            } else {
                reject(xhr.statusText);
            }
        };
        xhr.onerror = () => {
            reject(xhr.statusText, xhr.status);
        };
        xhr.send(obj.body);
    });
}

function request_plain(obj) {
    return new Promise((resolve, reject) => {
        let xhr = new XMLHttpRequest();

        xhr.open(obj.method || "GET", obj.url);
        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                resolve(xhr.response);
            } else {
                reject(xhr.statusText);
            }
        };
        xhr.onerror = () => reject(xhr.statusText);
        xhr.send(obj.body);
    });
}
function xmlRequest(verb, url) {
    var xhr = new XMLHttpRequest();
    xhr.open(verb || "GET", url, true);
    xhr.onload = () => {
        console.log(xhr.response);
    };
    xhr.onerror = () => {
        console.log("error: " + xhr.statusText);
    };
    xhr.send();
}


// wait for website

async function waitForSite() {
    request({url: "/index.html", method: "get"})
            .then(data => {
                location.assign("/");
            })
            .catch(error => {
                print(".");
                waitForSite();
            });
    for (; ; ) {
        await sleep(5000);
    }
}

//
// timeout/logout
//

function checkTimeout() {
    //output ("checking liveness");
    request({url: "/protected/checktimeout", method: "get"})
            .then(data => {
                //output(data);
            })
            .catch(error => {
                location.assign("/expired.html");
            });

}

function logout() {
    request({url: "/logout", method: "post"})
            .then(data => {
                //output(data);
                location.assign("/login.html");

            })
            .catch(error => {
                location.assign("/login.html");
            });

}

function enableTimeout() {
    if (!(location.href.toLowerCase().endsWith("login.html") || location.href.toLowerCase().endsWith("expired.html"))) {
        setInterval(checkTimeout, 1000);
    }
}

//
// Not used - could be used to make browser tabs have independent logins. See Ephemera project
// Browser sessionStorage is individual in a window between tabs - but the server session is shared, hence this becomes necessary.
//
function makeClientID() {
    window.sessionStorage.setItem("clientID", "" + ((new Date()).getTime()) % 100000);
}

function getClientID() {
    return window.sessionStorage.getItem("clientID");
}





        