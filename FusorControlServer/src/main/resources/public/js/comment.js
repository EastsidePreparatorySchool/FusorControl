//
// fusor main observer code
//


function submitComment(event) {
    var fd =  new FormData(event);
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
    chat.innerText += observer +
            //+ "(" + ip + ")," 
            " (" + Math.round((time - logStart) / 100) / 10 + "): " +
            text;
    chat.innerHTML += "<br>";
    chat.scrollTop = chat.scrollHeight;
}

function print(msg) {
    var chat = document.getElementById("chat");
    chat.innerText += msg;
}


