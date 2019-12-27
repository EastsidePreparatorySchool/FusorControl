function requestToken() {
// Change clientId and replyUrl to reflect your app's values 
// found on the Configure tab in the Azure Management Portal. 
// Also change {your_subdomain} to your subdomain for both endpointUrl and resource. 
    var clientId = 'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX';
    var replyUrl = 'http://replyUrl/';
    var endpointUrl = 'https://eastsidepreparatory-my.sharepoint.com/_api/v1.0/me/files';
    var resource = "https://eastsidepreparatory.-my.sharepoint.com";
    var authServer = 'https://login.windows.net/common/oauth2/authorize?';
    var responseType = 'token';
    var url = authServer +
            "response_type=" + encodeURI(responseType) + "&" +
            "client_id=" + encodeURI(clientId) + "&" +
            "resource=" + encodeURI(resource) + "&" +
            "redirect_uri=" + encodeURI(replyUrl);
    window.location = url;
}

function o365login() {
    requestToken();
}

function test() {
    requestWithHeader({url: "https://outlook.office365.com/EWS/OData/Me/Events", method: "get"}, 
            "Authentication", 
            "Basic " + encodeBase64("dumbass:pw"))
            .then(data => {
                output(data);
            })
            .catch(error => {
                output("Error: " + error);
            });
}

