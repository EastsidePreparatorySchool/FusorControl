//
// fusor device status -> text
//

function mapBoolean(b, trueval, falseval) {
    if (b === true) {
        return trueval;
    } else if (b === false) {
        return falseval;
    }
    return "<n/c>";
}

function infoFromData(data) {
    info = "";
    info += "Pump: ";
    if (isDevicePresent(data, "TMP")) {
        var tmp = getVariable(data, "TMP", "tmp");
        info += "Status: " + mapBoolean(tmp, " on", "off") + ", ";
        var speed = getVariable(data, "TMP", "lowspeed");
        info += "speed: " + mapBoolean(speed, "low", "high") + ", ";
        var freq = getVariable(data, "TMP", "pump_freq");
        info += "frequency: " + Math.round(freq) + " Hz, ";
        var amps = getVariable(data, "TMP", "pump_curr_amps");
        info += "power draw: " + (Math.round(amps * 100) / 100) + " A\n";
    } else {
        info += "n/c\n";
    }

    info += "Pressure (diaphragm gauge): ";
    if (isDevicePresent(data, "DIAPHRAGM")) {
        var dia = getVariable(data, "DIAPHRAGM", "diaphragm_adc");
        info += "" + dia + " (adc)\n";
    } else {
        info += "n/c\n";
    }

    info += "Pressure (Pirani gauge): ";
    if (isDevicePresent(data, "PIRANI")) {
        var pirani = getVariable(data, "PIRANI", "pirani_adc");
        info += "" + pirani + " (adc)\n";
    } else {
        info += "n/c\n";
    }


    info += "Variac: ";
    if (isDevicePresent(data, "VARIAC")) {
        var volts = getVariable(data, "VARIAC", "input_volts");
        info += "" + volts + " V target ";
        volts = getVariable(data, "VARIAC", "pontentiometer");
        info += "" + volts + " V actual\n";
    } else {
        info += "n/c\n";
    }

    info += "HV Low side: ";
    if (isDevicePresent(data, "HV-LOWSIDE")) {
        info += "variac rms: " + Math.round(getVariable(data, "HV-LOWSIDE", "variac_rms") * 1000) / 1000 + " V, ";
        info += "nst rms: " + Math.round(getVariable(data, "HV-LOWSIDE", "nst_rms") * 1000) / 1000 + " KV, ";
        info += "cw avg: " + Math.round(getVariable(data, "HV-LOWSIDE", "cw_avg") * 1000) / 1000 + " KV, ";
        info += "n: " + getVariable(data, "HV-LOWSIDE", "n") + "\n";
    } else {
        info += "n/c\n";
    }

    info += "HV High side: ";
    if (isDevicePresent(data, "HV-HIGHSIDE")) {
        info += "Highside current: " + getVariable(data, "HV-HIGHSIDE", "hs_current_adc") + " (adc)\n";
    } else {
        info += "n/c\n";
    }

    info += "Gas injection: ";
    if (isDevicePresent(data, "GAS")) {
        var solenoid = getVariable(data, "GAS", "solenoid");
        info += "Solenoid " + mapBoolean(solenoid, " open", "closed") + "\n";
    } else {
        info += "n/c\n";
    }

    info += "GC-SERIAL: ";
    if (isDevicePresent(data, "GC-SERIAL")) {
        info += getVariable(data, "GC-SERIAL", "cps") + " cps\n";
    } else {
        info += "n/c\n";
    }

    info += "P-N-JUNCTION: ";
    if (isDevicePresent(data, "PN-JUNCTION")) {
        info += "total: " + getVariable(data, "PN-JUNCTION", "total") + " (adc)\n";
    } else {
        info += "n/c\n";
    }
    return info;
}

