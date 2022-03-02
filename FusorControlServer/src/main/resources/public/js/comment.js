//
// fusor main observer code
//


function submitComment(event) {
    var fd = new FormData(event);
    fd.append("clientID", getClientID());
    request({url: "/protected/comment", method: "POST", body: fd})
            .then(data => {
            })
            .catch(error => {
                console.log("Error: " + error);
            });
    document.getElementById("comment").value = "";
    return false;
}

function displayComment(observer, time, text) {
    var chat = document.getElementById("chat");
    chat.innerHTML += "<span " + (observer === "DeviceManager" && !text.includes("Adding") ? "style='color:red'" : "") + ">"
            + observer + "</span>"
            //+ "(" + ip + ")," 
            + " (" + Math.round((time - logStart) / 100) / 10 + "): ";

    // first we Base 64 decode the comment - atop(...) does that
    // then we use the Underscore.js library to escape any HTML to prevent XSS
    chat.innerHTML += _.escape(atob(text));

    chat.innerHTML += "<br>";
    chat.scrollTop = chat.scrollHeight;
}

function print(msg) {
    var chat = document.getElementById("chat");
    chat.innerText += msg;
}

function printHTML(msg) {
    var chat = document.getElementById("chat");
    chat.innerHTML += msg;
}
