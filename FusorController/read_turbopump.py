import urllib2
import json

def valtostate(val):
	if val:
		return "ON"
	else:
		return "OFF"

def valtoans(val):
	if val:
		return "YES"
	else:
		return "NO"

def readall():
	f = urllib2.urlopen("http://192.168.1.1")
	s = f.read()[:-5] + "]"
	#return s
	arr = json.loads(s)

	print "---- Status ----"
	print "Supply current amperage: " + str(arr[12]) + " (" + str(round(arr[12]/1023.0)*2.5) + " A)"
	print "Motor driving frequency: " + str(arr[14]) + " (" + str(round((arr[14]/1023.0)*10.6)*125) + " Hz)"
	print "Low speed mode:   " + valtostate(arr[0])
	print "Freq. Sub 800 Hz: " + valtoans(arr[4])
	print "FAILURE: " + valtoans(arr[2])
	print "----------------"

	#return [arr[12], arr[14]]
	# 0 indexed
	# 500 = 5V
	# Important value for A0: 12
	# Important value for A1: 14
	# Important value for A2/ORANGE/5: 0
	# Important value for A3/BLUE/3: 2
	# Important value for A4/GREEN STRIPED/6: 4


# PIN 4 is the blue wire - Error reset
# PIN 3 is the orange striped wire - ON/OFF
# PIN 2 is the orange wire - Low speed mode activation
def write(pin, on):
	f = urllib2.urlopen("http://192.168.1.1/" + str(int(on)) + str(pin) + "WW")

def repeat_reading(times):
	for i in range(0, times):
		readall()