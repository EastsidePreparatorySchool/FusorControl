//
// Fusor project - fusor.h - shared Arduino code
//

#define FDEBUG false

#define FUSOR_LED_ON() digitalWrite(LED_BUILTIN, HIGH);
#define FUSOR_LED_OFF() digitalWrite(LED_BUILTIN, LOW);

#define FUSOR_CMDLENGTH 127
#define FUSOR_RESPONSE_MAX 511
#define FUSOR_NAME_LENGTH 16
#define FUSOR_VAR_LENGTH 16
#define FUSOR_MAX_VARIABLES 8

#define FUSOR_VARTYPE_STR 0
#define FUSOR_VARTYPE_INT 1
#define FUSOR_VARTYPE_FLOAT 2
#define FUSOR_VARTYPE_BOOL 3

struct FusorVariable
{
  char name[FUSOR_NAME_LENGTH];
  int type; //0: string; 1:int; 2: float: 3: bool
  int intValue;
  float floatValue;
  bool boolValue;
  char value[FUSOR_VAR_LENGTH];
  bool updated;
  long timestamp;
};

static char fusorName[FUSOR_NAME_LENGTH];
static char fusorCmdBuffer[FUSOR_CMDLENGTH + 1] = "";
static int fusorCmdBufpos = 0;
static char fusorResponseBuffer[FUSOR_RESPONSE_MAX + 1];
static int fusorNumVars = 0;
static FusorVariable fusorVariables[FUSOR_MAX_VARIABLES];
static bool _fusorAutoStatus = false;
static long _fusorLastStatus = 0;
static char _buffer[16];


static const char *_fusorCmd = "CMD[";
static const char *_fusorRsp = "RSP[";
static const char *_fusorEnd = "]END";
#define FUSOR_FIX_LENGTH_CMD 4
#define FUSOR_FIX_LENGTH_END 4

#ifdef BLUETOOTH
BluetoothSerial SerialBT;
#define FSERIAL SerialBT
#else
#define FSERIAL Serial
#endif

void fusorSendResponse(const char *msg);
void fusorStartResponse(const char *response);
void fusorAddResponse(const char *response);

void fusorInitWithBaudRate(const char *name, long baudRate);
void fusorInit(const char *name);
void fusorLoop();

int _fusorReadToCmdBuffer();
char *_fusorGetCommand(char *sCommand);
char *_fusorSkipCommand(char *current);
char *_fusorParseCommand(char *full, char **command, char **var, char **val);
void _fusorCmdExecute(char *sCmd, char *sVar, char *sVal);
void _fusorCmdGetAll();
void _fusorCmdAutoStatusOn();
void _fusorCmdAutoStatusOff();

struct FusorVariable *_fusorGetVariableEntry(const char *var);
void _fusorCmdSetVariable(char *var, char *val);
void _fusorCmdGetVariable(char *var);

void fusorAddVariable(const char *var, int type);
bool fusorVariableUpdated(const char *var);

int fusorGetIntVariable(const char *var);
char *fusorGetStrVariable(const char *var);
float fusorGetFloatVariable(const char *var);
bool fusorGetBoolVariable(const char *var);

void fusorSetIntVariable(const char *var, int val);
void fusorSetStrVariable(const char *var, char *val);
void fusorSetFloatVariable(const char *var, float val);
void fusorSetBoolVariable(const char *var, bool val);

//================

//
// response API
//

void fusorStartResponse(const char *response)
{
  strcpy(fusorResponseBuffer, _fusorRsp);
  if (response != NULL)
  {
    fusorAddResponse(response);
  }
}

void fusorAddResponse(const char *response)
{
  strncat(fusorResponseBuffer, response, FUSOR_RESPONSE_MAX - strlen(fusorResponseBuffer));
  fusorResponseBuffer[FUSOR_RESPONSE_MAX] = 0;
}

void fusorSendResponse(const char *msg)
{
  // if msg present, frame it
  if (msg != NULL)
  {
    fusorStartResponse(msg);
  }

  // make sure to not leave any markers in the message unaltered,
  // so that the host doesn't get confused
  char *start = strstr(fusorResponseBuffer, _fusorCmd);
  if (start != NULL)
  {
    strncpy(start, "cmd<", FUSOR_FIX_LENGTH_CMD);
  }

  char *end = strstr(fusorResponseBuffer, _fusorEnd);
  if (end != NULL)
  {
    strncpy(end, ">end", FUSOR_FIX_LENGTH_END);
  }

  // add the real end marker
  fusorAddResponse(_fusorEnd);
  fusorAddResponse("\n");
  FSERIAL.write((const uint8_t *)fusorResponseBuffer, strlen(fusorResponseBuffer));
}

//
// reading, parsing and executing commands
//

char *_fusorCompactCmdBuffer(char *newStart)
{
  if (newStart != NULL)
  {

    // compact buffer
    memmove(fusorCmdBuffer, newStart, strlen(newStart) + 1);
    fusorCmdBufpos = strlen(fusorCmdBuffer);
  }
  return fusorCmdBuffer;
}

int _fusorReadToCmdBuffer()
{
  while (FSERIAL.available() > 0 && fusorCmdBufpos < FUSOR_CMDLENGTH - 1)
  {
    fusorCmdBuffer[fusorCmdBufpos] = FSERIAL.read();
    fusorCmdBufpos++;
  }
  fusorCmdBuffer[fusorCmdBufpos] = 0;
  return strlen(fusorCmdBuffer);
}

void fusorClearCommandQueue()
{
  if (_fusorReadToCmdBuffer() > 0)
  {
    char *sCommand = NULL;
    while (sCommand = _fusorGetCommand(sCommand))
    {
      //fusorSendResponse("Got cmd");
      char *sCmd;
      char *sVar;
      char *sVal;
      int len = strlen(sCommand);

      sCommand = _fusorParseCommand(sCommand, &sCmd, &sVar, &sVal);
      _fusorCompactCmdBuffer(sCommand);
    }
  }
}

char *_fusorGetCommand(char *sCommand)
{
  char *sEnd;
  
  // start from beginning if indicated
  if (sCommand == NULL)
  {
    sCommand = fusorCmdBuffer;
  }

  // let's parse
  sCommand = strstr(sCommand, _fusorCmd);
  if (sCommand != NULL)
  {
    // look for end of command
    sEnd = strstr(sCommand, _fusorEnd);
    if (sEnd != NULL && sEnd > sCommand)
    {
      // found complete command, compact, terminate appropriately, return start
      // found keyword, skip, compact
      sCommand += FUSOR_FIX_LENGTH_CMD;
      sCommand = _fusorCompactCmdBuffer(sCommand);

      // reestablish where the end is
      sEnd = strstr(sCommand, _fusorEnd);
      *sEnd = 0;
      return sCommand;
    }
  } 
  else 
  {
    // no valid CMD found. check for ends, so we can compact and get rid of garbage
    sEnd = strstr(fusorCmdBuffer, _fusorEnd);
    if (sEnd != NULL) {
      // found end, get rid of the whole front of the buffer.
      sEnd += FUSOR_FIX_LENGTH_END;
      _fusorCompactCmdBuffer(sEnd);
      return NULL;
    }
  }

  // didn't find the right thing, keep looking
  return NULL;
}

char *_fusorParseCommand(char *full, char **command, char **var, char **val)
{
  char *next;
  int len = strlen(full);
  char *nextCmd = full + len + FUSOR_FIX_LENGTH_END;

  *command = full;
  *var = NULL;
  *val = NULL;

  // check if variable name present
  next = strstr(full, ":");
  if (next == NULL)
  {
    return nextCmd;
  }
  *next = 0;

  // mark if requested
  if (var != NULL)
  {
    *var = next + 1;
  }

  // check if value present
  next = strstr(next + 1, ":");
  if (next == NULL)
  {
    return nextCmd;
  }
  *next = 0;

  // mark if requested
  if (val != NULL)
  {
    *val = next + 1;
  }

  // all good, terminate and advance
  return nextCmd;
}

void _fusorCmdExecute(char *sCmd, char *sVar, char *sVal)
{
  // handle special case of identify first
  if (strcmp(sCmd, "IDENTIFY") == 0)
  {
    fusorStartResponse("IDENTIFY:");
    fusorAddResponse(fusorName);
    fusorSendResponse(NULL);
  }
  //fusorStartResponse("handling cmd:");
  //fusorAddResponse(sCmd);
  //fusorSendResponse(NULL);
  else if (strcmp(sCmd, "SET") == 0)
  {
    _fusorCmdSetVariable(sVar, sVal);
  }
  else if (strcmp(sCmd, "GET") == 0)
  {
    _fusorCmdGetVariable(sVar);
  }
  else if (strcmp(sCmd, "GETALL") == 0)
  {
    _fusorCmdGetAll();
  }
  else if (strcmp(sCmd, "AUTOSTATUSON") == 0)
  {
    _fusorCmdAutoStatusOn();
  }
  else if (strcmp(sCmd, "AUTOSTATUSOFF") == 0)
  {
    _fusorCmdAutoStatusOff();
  }

  sCmd[0] = 0;
}

void _fusorCmdGetAll()
{
  int skip = 0;
  fusorStartResponse("STATUS:{");

  for (int i = 0; i < fusorNumVars; i++)
  {
    fusorAddResponse("\"");
    FusorVariable *pfv = &fusorVariables[i];
    fusorAddResponse(pfv->name);
    fusorAddResponse("\":{");

    fusorAddResponse("\"value\":");
    switch (pfv->type)
    {
      case FUSOR_VARTYPE_STR:
        fusorAddResponse("\"");
        fusorAddResponse(pfv->value);
        fusorAddResponse("\"");
        break;
      case FUSOR_VARTYPE_INT:
        itoa(pfv->intValue, _buffer, 10);
        fusorAddResponse(_buffer);
        break;
      case FUSOR_VARTYPE_FLOAT:
        // we processed the float earlier, just send the string
        if (pfv->value[0] == 0) {
          strcpy(pfv->value, "0.0");
        }
        fusorAddResponse(pfv->value); 
        break;
      case FUSOR_VARTYPE_BOOL:
        fusorAddResponse((char *)(pfv->boolValue ? "true" : "false"));
        break;
      default:
        fusorAddResponse("<?>");
        break;
    }
    fusorAddResponse(",\"vartime\":");
    ltoa(pfv->timestamp, _buffer, 10);
    fusorAddResponse(_buffer);

    fusorAddResponse("}");
    fusorAddResponse(",");
  }
  fusorAddResponse("\"devicetime\":");
  ltoa(millis(), _buffer, 10);
  fusorAddResponse(_buffer);

  fusorAddResponse("}");
  fusorSendResponse(NULL);
  _fusorLastStatus = millis();
}

void _fusorDoAutoStatus()
{
  if (_fusorAutoStatus)
  {
    if ((millis() - _fusorLastStatus) > 100)
    {
      _fusorCmdGetAll();
    }
  }
}

void fusorDelay(int ms)
{
  long start = millis();
  while (millis() < (start + ms))
  {
    //    _fusorDoAutoStatus();
    //    _fusorReadToCmdBuffer();
    fusorLoop();
    delayMicroseconds(100);
  }
}

void fusorDelayMicroseconds(int us)
{
  long start = micros();
  while (micros() < (start + us))
  {
    //    _fusorDoAutoStatus();
    //    _fusorReadToCmdBuffer();
    fusorLoop();
  }
}

struct FusorVariable *_fusorGetVariableEntry(const char *name)
{
  FusorVariable *pfv = fusorVariables;
  for (int i = 0; i < fusorNumVars; i++)
  {
    if (strcmp(pfv->name, name) == 0)
    {
      return pfv;
    }
    pfv++;
  }
  return NULL;
}

void _fusorCmdSetVariable(char *var, char *val)
{
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);
  if (pfv != NULL)
  {
    strncpy(pfv->value, val, FUSOR_VAR_LENGTH - 1);
    pfv->value[FUSOR_VAR_LENGTH - 1] = 0;
    pfv->updated = true;
    pfv->timestamp = millis();
    fusorStartResponse("SET:");
    fusorAddResponse(var);
    fusorAddResponse(":");
    fusorAddResponse(val);
    fusorSendResponse(NULL);
    switch (pfv->type)
    {
      case FUSOR_VARTYPE_STR:
        break;
      case FUSOR_VARTYPE_INT:
        pfv->intValue = atoi(val);
        break;
      case FUSOR_VARTYPE_FLOAT:
        pfv->floatValue = atof(val);
        break;
      case FUSOR_VARTYPE_BOOL:
        pfv->boolValue = (strcmp(val, "true") == 0);
        break;
      default:
        break;
    }
  }
  else
  {
    fusorStartResponse("?:");
    fusorAddResponse(var);
    fusorSendResponse(NULL);
  }
}

void _fusorCmdGetVariable(char *var)
{
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);
  if (pfv != NULL)
  {
    fusorStartResponse("GET:");
    fusorAddResponse(var);
    fusorAddResponse(":");
    fusorAddResponse(pfv->value);
    fusorSendResponse(NULL);
  }
  else
  {
    fusorStartResponse("?:");
    fusorAddResponse(var);
    fusorSendResponse(NULL);
  }
}

void _fusorCmdAutoStatusOn()
{
  fusorSendResponse("AUTOSTATUSON");
  _fusorCmdGetAll();
  _fusorAutoStatus = true;
}

void _fusorCmdAutoStatusOff()
{
  fusorSendResponse("AUTOSTATUSOFF");
  _fusorAutoStatus = false;
}

//
// getting variables from the main code (during loop or after init)
// and "updated" status
//

bool fusorVariableUpdated(const char *var)
{
  FusorVariable *pfv;
  bool result = false;

  pfv = _fusorGetVariableEntry(var);
  if (pfv != NULL) {
    result = pfv->updated;
    pfv->updated = false;
  }
  return result;
}

int fusorGetIntVariable(const char *var)
{
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);

  return (pfv->intValue);
}

float fusorGetFloatVariable(const char *var)
{
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);

  return (pfv->floatValue);
}

bool fusorGetBoolVariable(const char *var)
{
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);

  return (pfv->boolValue);
}

char *fusorGetStrVariable(const char *var)
{
  FusorVariable *pfv;

  pfv = _fusorGetVariableEntry(var);
  return pfv->value;
}

//
// setting variables from the main code (during loop or after init)
//

bool fusorStrVariableEquals(const char *var, char *test)
{
  FusorVariable *pfv;

  pfv = _fusorGetVariableEntry(var);
  return strcmp(pfv->value, test) == 0;
}

void fusorSetIntVariable(const char *var, int val)
{
  FusorVariable *pfv;

  pfv = _fusorGetVariableEntry(var);
  pfv->intValue = val;
  //pfv->updated = true;
  pfv->timestamp = millis();
}

void fusorSetStrVariable(const char *var, char *val)
{
  FusorVariable *pfv;

  pfv = _fusorGetVariableEntry(var);
  strncpy(pfv->value, val, FUSOR_VAR_LENGTH - 1);
  pfv->value[FUSOR_VAR_LENGTH - 1] = 0;
  //pfv->updated = true;
  pfv->timestamp = millis();
}

void fusorSetBoolVariable(const char *var, bool val)
{
  FusorVariable *pfv;

  pfv = _fusorGetVariableEntry(var);
  pfv->boolValue = val;
  //pfv->updated = true;
  pfv->timestamp = millis();
}

void fusorSetFloatVariable(const char *var, float val)
{
  FusorVariable *pfv;
  int skip;

  pfv = _fusorGetVariableEntry(var);

  // make it into a string right here
  dtostrf(val, 15, 8, _buffer);
  skip = 0;
  while (_buffer[skip] == ' ')
  {
      skip++;
  }
  strncpy(pfv->value, _buffer+skip, FUSOR_VAR_LENGTH - 1);
  pfv->value[FUSOR_VAR_LENGTH - 1] = 0;

  //pfv->updated = true;
  pfv->timestamp = millis();
}

// alternate "set float" for data that is already in string format
// (e.g. received from serial connection)
void fusorSetFloatVariableFromString(const char *var, const char * str)
{
  FusorVariable *pfv;

  pfv = _fusorGetVariableEntry(var);

  strncpy(pfv->value, str, FUSOR_VAR_LENGTH - 1);
  pfv->value[FUSOR_VAR_LENGTH - 1] = 0;

  //pfv->updated = true;
  pfv->timestamp = millis();
}

//
// initialization - init, add variable
//

void fusorAddVariable(const char *name, int type)
{
  FusorVariable *pfv = &fusorVariables[fusorNumVars];
  strncpy(pfv->name, name, FUSOR_NAME_LENGTH - 1);
  pfv->name[FUSOR_NAME_LENGTH - 1] = 0;
  pfv->type = type;
  pfv->updated = false;
  pfv->value[0] = 0;
  pfv->floatValue = 0.0f;
  pfv->intValue = 0;
  pfv->boolValue = 0;
  pfv->timestamp = 0;
  fusorNumVars++;
}

void fusorInit(const char *name)
{
  fusorInitWithBaudRate(name, 115200);
}

void fusorInitWithBaudRate(const char *name, long baudRate)
{
#ifdef BLUETOOTH
  SerialBT.begin(name);
#else
  Serial.begin(baudRate);
#endif

  // light for hope
  pinMode(LED_BUILTIN, OUTPUT); // pin 13

  strncpy(fusorName, name, FUSOR_NAME_LENGTH);
  fusorName[FUSOR_NAME_LENGTH - 1] = 0;
  fusorCmdBuffer[0] = 0;
  fusorCmdBufpos = 0;
  fusorNumVars = 0;
  _fusorAutoStatus = false;
}

//
// loop
//

void fusorLoop()
{

  bool didGetAll = false;
  _fusorDoAutoStatus();

  // reset all "updated" values
  //  for (int i = 0; i < fusorNumVars; i++)
  //  {
  //    fusorVariables[i].updated = false;
  //  }

  //collects serial messages from the hardware buffer
  if (_fusorReadToCmdBuffer() > 0)
  {
    if (FDEBUG)
    {
      fusorStartResponse("ECHO:");
      fusorAddResponse(fusorCmdBuffer);
      fusorSendResponse(NULL);
    }
    // got message, let's parse
    char *sCommand = NULL;
    while (sCommand = _fusorGetCommand(sCommand))
    {
      //fusorSendResponse("Got cmd");
      char *sCmd;
      char *sVar;
      char *sVal;
      int len = strlen(sCommand);

      sCommand = _fusorParseCommand(sCommand, &sCmd, &sVar, &sVal);
      // {
      //   // make sure that GETALL only runs once this loop
      //   if (didGetAll)
      //   {
      //     continue;
      //   }
      //   didGetAll = true;
      // }

      _fusorCmdExecute(sCmd, sVar, sVal);
      _fusorCompactCmdBuffer(sCommand);
      // fusorStartResponse("Executed cmd, next up:");
      // fusorAddResponse(fusorCmdBuffer);
      // fusorSendResponse(NULL);
    }
  }
}

// ================================================================
