const ifaces = require('os').networkInterfaces();

var address = '127.0.0.1';
function refresh() {
  if (ifaces['Ethernet'] != null) address = ifaces['Ethernet'][1].address;
  else if (ifaces['Wi-Fi 2'] != null) address = ifaces['Wi-Fi 2'][1].address;
}
refresh();

module.exports.address = address;
module.exports.refresh = refresh;
