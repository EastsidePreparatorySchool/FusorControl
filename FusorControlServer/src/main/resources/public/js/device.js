//
// fusor variable parsing
//


function isDevicePresent(data, device) {
    var value;
    device = data.find((item) => item["device"] === device);
    return (device !== undefined);
}


function getVariable(data, device, variable) {
    var value;
    device = data.find((item) => item["device"] === device);
    if (device === undefined) {
        return ("<n/c>");
    }
    data = device["data"];
    if (data === undefined) {
        return "<n/a>";
    }
    variable = data[variable];
    if (variable === undefined) {
        return "<n/a>";
    }
    value = variable["value"];
    if (value === undefined) {
        return "<n/a>";
    }
    return value;
}

function getVariableTimeStamp(data, device, variable) {
    var value;
    device = data.find((item) => item["device"] === device);
    if (device === undefined) {
        return ("<n/c>");
    }
    data = device["data"];
    if (data === undefined) {
        return "<n/a>";
    }
    variable = data[variable];
    if (variable === undefined) {
        return "<n/a>";
    }
    value = variable["vartime"];
    if (value === undefined) {
        return "<n/a>";
    }
    return value;
}


function getDeviceInfo(data, device, variable) {
    var value;
    device = data.find((item) => item["device"] === device);
    if (device === undefined) {
        return ("<n/c>");
    }

    // treat servertime and devicetime differently (fix this some day)
    if (variable === "servertime") {
        data = device[variable]; // servertime is in the envelope added by the server
        if (data === undefined) {
            return "<n/a>";
        }
        return data;
    } else if (variable === "devicetime") {
        data = device["data"]; // devicetime is defined in the "data" section
        if (data === undefined) {
            return "<n/a>";
        }
        data = data[variable];
        if (data === undefined) {
            return "<n/a>";
        }
        return data;
    }
}

        