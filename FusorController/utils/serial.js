const serialports = require('serialport');

var serial = {
  //portstring: '/dev/ttyACM',
  portstring: 'COM',
  refresh: () => {
    if (serial.length) for (var i = 0; i < serial.length; i++) serial[i] = null;
    serial.length = 0;

    return serialports.list().then((ports) => {
        ports.forEach((port) => {if (port.comName.includes(serial.portstring)) serial[serial.length++] = port;});
    });
  }
}
module.exports = serial;
module.exports.module = serialports;
