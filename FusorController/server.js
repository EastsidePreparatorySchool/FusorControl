const express = require('express'),
      app = express(),
      server = require('http').Server(app),
      io = require('socket.io')(server),

      //my own files
      serial = require('./utils/serial.js'),
      ip = require('./utils/ip.js');


let arduino,
    messageBuffer = '',
    delineater = 'STOP',
    baud = 9600,
    PC_ID = 'PUMPCONTROLLER';

var gdata = true;
serial.refresh().then(() => {
  //iterates through all the serial ports available
  for (let i = 0; i < serial.length; i++) {

    //creates and opens port object
    //some distinguishment needs to be done to only select the arduino, not other serial devices
    let port = new serial.module(serial[i].comName, {
      baudRate: baud
    });

    setTimeout(() => { port.write('c0'); }, 1750);

    //code to interpret incoming data
    //sometimes it comes in weird chunks, this chops it up nicely
    port.on('data', (data) => {
      messageBuffer += data;

      gdata = true;
      if (messageBuffer.includes(delineater)) {
        let index = messageBuffer.indexOf(delineater);
        process(messageBuffer.substr(0, index), port);
        messageBuffer = messageBuffer.substr(index + delineater.length, messageBuffer.length);
      }
    });
  }
});


//processes incoming information from arduino, currently just passes it along
function process(s, port) {
  console.log(s);
  if (s == PC_ID) {
    arduino = port;
    setInterval(getData, 250);
  }
  else {
    //break the string into an object with a property
    let o = {},
        index = s.indexOf(':');
    o[s.substr(0,index)] = s.substr(index + 1, s.length);
    io.emit('update', o);
  }
}

//code to deal with sockets
io.on('connection', (socket) => {
  socket.on('turbopump_stop', (data) => { arduino.write('p0'); gdata = false;});
  socket.on('turbopump_slow', (data) => { arduino.write('p1');gdata = false; });
  socket.on('turbopump_fast', (data) => { arduino.write('p2');gdata = false; });
});


//just give whatever other files the client asks for
app.use(express.static(__dirname + '/public'));

var port = 8000;
server.listen(port, '0.0.0.0', () => {
  console.log('----Server Created----');
  console.log('\nGo to localhost:' + port + ' in your browser\n');
});

function getData() {
  if(gdata) {
    arduino.write('p3');
    console.log("asked");
  }
}
