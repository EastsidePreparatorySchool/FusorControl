//
// Fusor project - fusor.h - shared Arduino code
//

#define FUSOR_LED_ON()  digitalWrite(LED_BUILTIN, HIGH);
#define FUSOR_LED_OFF() digitalWrite(LED_BUILTIN, LOW);

#define FUSOR_CMDLENGTH  127
#define FUSOR_RESPONSE_MAX  511
#define FUSOR_NAME_LENGTH 16
#define FUSOR_VAR_LENGTH 16
#define FUSOR_MAX_VARIABLES 8

#define FUSOR_VARTYPE_STR 0
#define FUSOR_VARTYPE_INT 1
#define FUSOR_VARTYPE_FLOAT 2
#define FUSOR_VARTYPE_BOOL 3

typedef struct FusorVariable {
  char  name[FUSOR_NAME_LENGTH];
  int   type; //0: string; 1:int; 2: float: 3: bool
  int   intValue;
  float floatValue;
  bool  boolValue;
  char  value[FUSOR_VAR_LENGTH];
  bool  updated;
  long  timestamp;
};


static char fusorName[FUSOR_NAME_LENGTH];
static char fusorCmdBuffer[FUSOR_CMDLENGTH+1] = "";
static int fusorCmdBufpos = 0;
static char fusorResponseBuffer[FUSOR_RESPONSE_MAX+1];
static int fusorNumVars = 0;
static FusorVariable fusorVariables[FUSOR_MAX_VARIABLES];




#ifdef BLUETOOTH
  BluetoothSerial SerialBT;
  #define SERIAL SerialBT
#else
  #define SERIAL Serial
#endif


void _fusorReadCommands();
char *_fusorGetCommand(char*sCommand);
char *_fusorSkipCommand(char *current);

void fusorSendResponse(char *msg);
void fusorStartResponse(char *response);
void fusorAddResponse(char *response);

void fusorInitWithBaudRate(char * name, long baudRate);
void fusorInit(char * name);
void fusorLoop();

bool _fusorParseCommand(char *full, char **command, char ** var, char **val);
void _fusorCmdExecute(char *sCmd, char* sVar, char *sVal);
void _fusorCmdGetAll();
struct FusorVariable *_fusorGetVariableEntry(char *name);
void _fusorCmdSetVariable(char *var, char *val);
void _fusorCmdGetVariable(char *var);

void fusorAddVariable (char * name, int type);
bool fusorVariableUpdated(char* var);

int fusorGetIntVariable(char* var);
char *fusorGetStrVariable(char* var);
float fusorGetFloatVariable(char* var);
bool fusorGetBoolVariable(char* var);

void fusorSetIntVariable(char *name, int val);
void fusorSetStrVariable(char* var, char *val);
void fusorSetFloatVariable(char* var, float val);
void fusorSetBoolVariable(char* var, bool val);


//================


void _fusorReadCommands() {
    int start = fusorCmdBufpos;
    while (SERIAL.available() > 0 && fusorCmdBufpos < FUSOR_CMDLENGTH) {
       fusorCmdBuffer[fusorCmdBufpos] = SERIAL.read();
      //SERIAL.write(fusorCmdBuffer[fusorCmdBufpos]);
      fusorCmdBufpos++;
    }
    fusorCmdBuffer[fusorCmdBufpos] = 0;
}

char *_fusorGetCommand(char*sCommand) {
  if (sCommand == NULL) {
    sCommand = fusorCmdBuffer;
  }
  
  // got message, let's parse
  sCommand = strstr(sCommand, "FusorCommand[");
  if (sCommand != NULL) {
    sCommand += 13;
    char *sEnd = strstr(sCommand, "]END");
  
    if (sEnd != NULL) {
      // found at least one more command
      // terminate command string at "]END"
      *sEnd = 0;
    }
 
    // compact buffer
    memmove(fusorCmdBuffer, sCommand, strlen(sCommand)+1);
    sCommand = fusorCmdBuffer;
  }
  fusorCmdBufpos = 0;
  return sCommand;
}

//
// response API
//

void fusorStartResponse(char *response) {
  strcpy(fusorResponseBuffer,"FusorResponse[");
  if (response != NULL) {
    fusorAddResponse(response);
  }
}

void fusorAddResponse(char *response) {
  strncat(fusorResponseBuffer, response, FUSOR_RESPONSE_MAX-strlen(fusorResponseBuffer));
  fusorResponseBuffer[FUSOR_RESPONSE_MAX] = 0;
}

void fusorSendResponse(char *msg) {
  if(msg != NULL) {
    fusorStartResponse(msg);
  }
  fusorAddResponse("]END");
  SERIAL.write((const uint8_t *)fusorResponseBuffer, strlen(fusorResponseBuffer));
}


//
// parsing and executing commands
//


bool _fusorParseCommand(char *full, char **command, char ** var, char **val) {
  char * next;
  
  *command = full;
  *var = NULL;
  *val = NULL;

  // check if variable name present
  next = strstr(full, ":");
  if (next == NULL) {
    return true;
  }
  *next = 0;
  
  // mark if requested
  if (var != NULL) {
    *var = next+1;
  }

  // check if value present
  next = strstr(next+1, ":");
  if (next == NULL) {
    return true;
  }
  *next = 0;
  
  // mark if requested
  if (val != NULL) {
    *val = next+1;
  }

  // check for malformed commands
  next = strstr(next+1, ":");
  if (next != NULL) {
    return false;
  }

  // all good
  return true;
}


void _fusorCmdExecute(char *sCmd, char* sVar, char *sVal) {
  // handle special case of identify first
  if (strcmp(sCmd, "IDENTIFY") == 0) {
    //SERIAL.write('*');
    fusorStartResponse("IDENTIFY:");
    fusorAddResponse(fusorName);
    fusorSendResponse(NULL);
  }
  //fusorStartResponse("handling cmd:");
  //fusorAddResponse(sCmd);
  //fusorSendResponse(NULL);
  if (strcmp(sCmd, "SET") == 0) _fusorCmdSetVariable(sVar,sVal);
  if (strcmp(sCmd, "GET") == 0) _fusorCmdGetVariable(sVar);
  if (strcmp(sCmd, "GETALL") == 0) _fusorCmdGetAll();
    
  FUSOR_LED_ON();
  delay(50);
  FUSOR_LED_OFF();
}

void _fusorCmdGetAll() {
  static char buffer[16];
  int skip = 0;
  fusorStartResponse("STATUS:{");
  fusorAddResponse("\"devicetime\":");
  ltoa(millis(), buffer, 10);
  fusorAddResponse(buffer);
  fusorAddResponse(","); 
  
  for (int i =0; i<fusorNumVars; i++) {
    fusorAddResponse("\"");
    FusorVariable *pfv = &fusorVariables[i];
    fusorAddResponse(pfv->name);
    fusorAddResponse("\":{\"vartime\":");
    ltoa(pfv->timestamp, buffer, 10);
    fusorAddResponse(buffer);
    fusorAddResponse(",\"value\":");
    switch(pfv->type) {
      case FUSOR_VARTYPE_STR:
        fusorAddResponse("\"");
        fusorAddResponse(pfv->value);
        fusorAddResponse("\"");
        break;
      case FUSOR_VARTYPE_INT:
        itoa(pfv->intValue, buffer, 10);
        fusorAddResponse(buffer);
        break;
      case FUSOR_VARTYPE_FLOAT:
        dtostrf(pfv->floatValue,15,8,buffer);
        skip = 0;
        while(buffer[skip] == ' ') {
          skip++;
        }
        fusorAddResponse(buffer+skip);
        break;
      case FUSOR_VARTYPE_BOOL:
        fusorAddResponse((char *)(pfv->boolValue?"true":"false"));
        break;
      default:
        fusorAddResponse("<unknown type>");
        break;
    }
    fusorAddResponse("}");
    if (i< fusorNumVars-1) {
      fusorAddResponse(",");
    }
  }
  fusorAddResponse("}");
  fusorSendResponse(NULL);
}

struct FusorVariable *_fusorGetVariableEntry(char *name) {
  FusorVariable *pfv = fusorVariables;
  for (int i =0; i<fusorNumVars; i++) {
    if (strcmp(pfv->name, name) == 0) {
      return pfv;
    }
    pfv++;
  }
  return NULL;
}

void _fusorCmdSetVariable(char *var, char *val) {
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);
  if (pfv != NULL) {
    strncpy (pfv->value, val, FUSOR_VAR_LENGTH-1);
    pfv->value[FUSOR_VAR_LENGTH-1] = 0;
    pfv->updated = true;
    pfv->timestamp = millis();
    fusorStartResponse("SET:");
    fusorAddResponse(var);
    fusorAddResponse(":");
    fusorAddResponse(val);
    fusorSendResponse(NULL);
    switch(pfv->type) {
      case FUSOR_VARTYPE_STR:
        break;
      case FUSOR_VARTYPE_INT:
        pfv->intValue = atoi(val);
        break;
      case FUSOR_VARTYPE_FLOAT:
        pfv->floatValue = atof(val);
        break;
      case FUSOR_VARTYPE_BOOL:
        pfv->boolValue = (strcmp(val, "true")==0);
        break;
      default:
        break;
    }
  } else {
    fusorStartResponse("ERROR: unknown variable:");
    fusorAddResponse(var);
    fusorSendResponse(NULL);
  }
}


void _fusorCmdGetVariable(char *var) {
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);
  if (pfv != NULL) {
    fusorStartResponse("GET:");
    fusorAddResponse(var);
    fusorAddResponse(":");
    fusorAddResponse(pfv->value);
    fusorSendResponse(NULL);
  } else {
    fusorStartResponse("ERROR: unknown variable:");
    fusorAddResponse(var);
    fusorSendResponse(NULL);
  }
}








//
// getting variables from the main code (during loop or after init)
// and "updated" status
//

bool fusorVariableUpdated(char* var) {
  FusorVariable *pfv;
  
  pfv = _fusorGetVariableEntry(var);
  return pfv->updated;
}


int fusorGetIntVariable(char* var) {
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);
  
  return (pfv->intValue);
}

float fusorGetFloatVariable(char* var) {
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);
  
  return (pfv->floatValue);
}

bool fusorGetBoolVariable(char* var) {
  FusorVariable *pfv;
  pfv = _fusorGetVariableEntry(var);
  
  return (pfv->boolValue);
}

char * fusorGetStrVariable(char* var) {
  FusorVariable *pfv;
  
  pfv = _fusorGetVariableEntry(var);
  return pfv->value;
}



//
// setting variables from the main code (during loop or after init)
//

bool fusorStrVariableEquals(char* var, char*test) {
  FusorVariable *pfv;
  
  pfv = _fusorGetVariableEntry(var);
  return strcmp(pfv->value,test) == 0;
}

void fusorSetIntVariable(char *var, int val) {
  FusorVariable *pfv;
  
  pfv = _fusorGetVariableEntry(var);
  pfv->intValue = val;
  pfv->updated = true;
  pfv->timestamp = millis();
}


void fusorSetStrVariable(char *var, char *val) {
  FusorVariable *pfv;
  
  pfv = _fusorGetVariableEntry(var);
  strncpy(pfv->value, val, FUSOR_VAR_LENGTH-1);
  pfv->value[FUSOR_VAR_LENGTH-1] = 0;
  pfv->updated = true;
  pfv->timestamp = millis();
}


void fusorSetBoolVariable(char *var, bool val) {
  FusorVariable *pfv;
  
  pfv = _fusorGetVariableEntry(var);
  pfv->boolValue = val;
  pfv->updated = true;
  pfv->timestamp = millis();
}


void fusorSetFloatVariable(char *var, float val) {
  FusorVariable *pfv;
  
  pfv = _fusorGetVariableEntry(var);
  pfv->floatValue = val;
  pfv->updated = true;
  pfv->timestamp = millis();
}


//
// initialization - init, add variable
//

void fusorAddVariable (char * name, int type) {
  FusorVariable *pfv = &fusorVariables[fusorNumVars];
  strncpy(pfv->name, name, FUSOR_NAME_LENGTH-1);
  pfv->name[FUSOR_NAME_LENGTH-1] = 0;
  pfv->type = type;
  pfv->updated = false;
  pfv->value[0] = 0;
  pfv->floatValue = 0.0;
  pfv->intValue = 0;
  pfv->boolValue = 0;
  pfv->timestamp = 0;
  fusorNumVars++;
}

void fusorInit(char * name) {
  fusorInitWithBaudRate(name, 115200);
}

void fusorInitWithBaudRate(char * name, long baudRate) {
    #ifdef BLUETOOTH
      SerialBT.begin(name);
    #else
      Serial.begin(baudRate);
    #endif

    // light for hope
    pinMode(LED_BUILTIN, OUTPUT);  // pin 13


    strncpy(fusorName, name, FUSOR_NAME_LENGTH);
    fusorName[FUSOR_NAME_LENGTH-1] = 0;
    fusorCmdBuffer[0] = 0;
    fusorCmdBufpos = 0;
    fusorNumVars = 0;
}

//
// loop
//

void fusorLoop() {
  bool didGetAll = false;

  // reset all "updated" values
  for (int i =0; i<fusorNumVars; i++) {
    fusorVariables[i].updated = false;
  }

  
  //collects serial messages from the hardware buffer
  _fusorReadCommands();
  if (strstr(fusorCmdBuffer, "]END") == NULL) {
    // nothing to see here
    return;
  }

  // got message, let's parse
  char *sCommand = NULL;
  while(sCommand = _fusorGetCommand(sCommand)) {
    char *sCmd;
    char *sVar;
    char *sVal;
    int len = strlen(sCommand);

    _fusorParseCommand(sCommand, &sCmd, &sVar, &sVal);
    if (strcmp(sCmd, "GETALL")==0) {
      // make sure that GETALL only runs once this loop
      if (didGetAll) {
        sCommand += len;
        continue;
      }
      didGetAll = true;
    }

    _fusorCmdExecute(sCmd, sVar, sVal);
    sCommand += len;
  }
}


// ================================================================
