//
// Fusor project - SensorArray
// Arduino Mega 2560 R3
//
// Connects to 
// - a serial Geiger counter (Dr. Whitmer's Geiger counter on pin 14/15)
// - two edge-detected Geiger counters (ISRs on D2 and D3)
// - a PN-junction stolen from the SEM (A0 and A1 for the left and right half, only the total value is interesting)
// - a serial PIN-diode gamma sensor on Serial1 (pin 18/19)
//
//

#include "fusor.h"

volatile int d2 = 0; // d2 is inside enclosure
volatile int d3 = 0; // d3 is outside enclosure
static long timeLastPulseGc2 = 0;
static long timeLastPulseGc3 = 0;

void setup(){
  // must do this in init, the rest is optional
  fusorInit("SENSORARRAY");
  fusorAddVariable("gc1",FUSOR_VARTYPE_INT);
  fusorAddVariable("lastByte", FUSOR_VARTYPE_INT);
  fusorAddVariable("pin",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("gc2",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("gc3",FUSOR_VARTYPE_FLOAT);
  fusorSetIntVariable("gc1",0);
  fusorSetFloatVariable("pin",0.0);
  fusorSetFloatVariable("gc1",0.0);
  fusorSetFloatVariable("gc2",0.0);

  Serial1.begin(9600); // PIN gamma sensor (8N1 ?)
  Serial3.begin(9600);  // Dr. Whitmer's Geiger counter (8N1)

  pinMode(2, INPUT);
  pinMode(3, INPUT);

  //attachInterrupt(digitalPinToInterrupt(2), ISR2, RISING);
  //attachInterrupt(digitalPinToInterrupt(3), ISR3, RISING);
  
  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}
int info1, info2;
void loop() {
  fusorLoop();
  
  updateAll();
  delay(5);
}

void updateAll()
{
    static char text[12];
    static char *str = text;
    static float decayingAvgCps = 0;
    const float newFraction = 0.1;

    // read the latest message from the serial GC if there is one
    // format: low byte, high byte
    static bool haveByte1 = false;
    static int byte1, byte2;
    static long time1;
    if (Serial3.available())
    {
        // if we don't have the first byte [of a pair]
        if(!haveByte1) {
            byte1 = Serial3.read();
            haveByte1 = true;
            time1 = micros();
        } else { // if we do have the first byte
            byte2 = Serial3.read();
            long time2 = micros();

            // if the time between the two bytes was less than 0.1 seconds
            // then we assume the two bytes belong together
            if(time2 - time1 < 100000) {
                haveByte1 = false;
                info1 = byte1;
                info2 = byte2;
                /*
                   Take the 1's and 0's in the first byte, shift them to the
                   left eight units, and then add to byte2. Assuming byte1 =
                   10101010 and byte2 = 11100011, here's what that does. Note
                   that multiplying by 256 is the same as doing a left bit-shift
                   eight units

                   10101010 << 8 = 1010101000000000
                   
                   1010101000000000
                   +       11100011
                   ----------------
                   1010101011100011
                 */
                fusorSetIntVariable("gc1", (byte2 * 256) + byte1);
            } else {
                /*
                 * if the two bytes were so far apart that we don't think they belong together
                 * discard the lone byte, as we can't use it, and assume that the second byte
                 * that we read was really the start of the next pair
                 *
                 * So, we say to take the last byte of the 'broken' pair, and assume it is the
                 * start of the next pair
                 */
                byte1 = byte2;
                time1 = time2;
                haveByte1 = true;
            }
        }
    }

    // read the latest message from the PIN diode sensor if there is one
    if (Serial1.available())
    {
        while (Serial1.available())
        {
            // format: <x02>M:1.24<CR><LF>
            // could assert "<0x02>M:" but won't
            char b = Serial1.read();
            if (b == 0x02)
            {
                // STX (start of transmission), reset buffer
                str = text;
                *str = 0;
            }
            else
            {
                if (b == 0x0D)
                {
                    // CR, end of message
                    *str = 0;                                         // insert 0 instead of <CR>
                    fusorSetFloatVariableFromString("pin", &text[2]); // skipping "M:"
                }
                else
                {
                    *str++ = b;
                }
            }
        }
    }

    //
    // get the edge-detected Geiger counts
    //
    long now;
    static long last = 0;
    int d2now, d3now;
    float interval;

    now = millis();
    if (now > last + 1000) // update only once per second
    {
        last = now;
        noInterrupts();
        d2now = d2;
        d3now = d3;
        d2 = 0;
        d3 = 0;
        interrupts();

        fusorSetFloatVariable("gc2", d2now);
        fusorSetFloatVariable("gc3", d3now);
    }
}


void ISR2() 
{
  long now = micros();
  if (now > timeLastPulseGc2+4000){
    d2++;
    timeLastPulseGc2 = now;
  }
}

void ISR3()
{
  long now = micros();
  if (now > timeLastPulseGc3+4000){
    d3++;
    timeLastPulseGc3 = now;
  }
}
