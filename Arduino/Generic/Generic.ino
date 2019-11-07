//
// Fusor project - Generic template code for Arduino
//
// #define BLUETOOTH


//
// Fusor project - fusor.h - shared Arduino code
//

#define FUSOR_LED_ON()  digitalWrite(LED_BUILTIN, HIGH);
#define FUSOR_LED_OFF() digitalWrite(LED_BUILTIN, LOW);

#define FUSOR_CMDLENGTH  127
#define FUSOR_RESPONSE_MAX  511
static char fusorCmdBuffer[FUSOR_CMDLENGTH+1] = "";
static int fusorCmdBufpos = 0;
static char fusorResponseBuffer[FUSOR_RESPONSE_MAX+1];

#ifdef BLUETOOTH
  #define SERIAL SerialBT
#else
  #define SERIAL Serial
#endif


void fusorReadCommands() {
  do{
    int start = fusorCmdBufpos;
    while (SERIAL.available() > 0 && fusorCmdBufpos < FUSOR_CMDLENGTH)
    {
      fusorCmdBuffer[fusorCmdBufpos] = SERIAL.read();
      //SERIAL.write(fusorCmdBuffer[fusorCmdBufpos]);
      fusorCmdBufpos++;
    }
    fusorCmdBuffer[fusorCmdBufpos] = 0;
      
  } while(strstr(fusorCmdBuffer, "]END") == NULL);
  //SERIAL.write('+');
}

char *fusorGetCommand(char*sCommand) {
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

char *fusorSkipCommand(char *current) {
  return current+strlen(current)+4; // 4 = strlen("]END")  
}

void fusorSendResponse(char *msg) {
  if(msg != NULL) {
    fusorStartResponse(msg);
  }
  fusorAddResponse("]END");
  SERIAL.write(fusorResponseBuffer, strlen(fusorResponseBuffer));
}

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

//char *fusorGetResponse() {
//  return fusorResponseBuffer;
//}

bool fusorParseCommand(char *full, char **command, char ** var, char **val) {
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

#define FUSOR_VAR_LENGTH 28

typedef struct FusorVariable {
  char  *name;
  char  value[FUSOR_VAR_LENGTH];
  bool  updated;
};

FusorVariable *fusorVariables = NULL;
int fusorNumVars = 0;

void fusorInit(struct FusorVariable *fvs, int numVars) {
    #ifdef BLUETOOTH
      SerialBT.begin("FusorGenericArduino");
    #else
      Serial.begin(9600);
    #endif

    fusorCmdBuffer[0] = 0;
    fusorCmdBufpos = 0;
    fusorVariables = fvs;
    fusorNumVars = numVars;
}

void fusorLoop() {
  // reset all "updated" values
  for (int i =0; i<fusorNumVars; i++) {
    fusorVariables[i].updated = false;
  }

  
  //collects serial messages from the hardware buffer
  fusorReadCommands();

  // got message, let's parse
  char *sCommand = NULL;
  while(sCommand = fusorGetCommand(sCommand)) {
    char *sCmd;
    char *sVar;
    char *sVal;
    fusorParseCommand(sCommand, &sCmd, &sVar, &sVal);
    fusorCmdExecute(sCmd, sVar, sVal);
    sCommand = fusorSkipCommand(sCommand);
  }

}

void fusorCmdExecute(char *sCmd, char* sVar, char *sVal) {
  // handle special case of identify first
  if (strcmp(sCmd, "IDENTIFY") == 0) {
    //SERIAL.write('*');
    fusorSendResponse("IDENTIFY:GENERIC");
  }
  fusorStartResponse("handling cmd:");
  fusorAddResponse(sCmd);
  fusorSendResponse(NULL);
  if (strcmp(sCmd, "SET") == 0) fusorCmdSetVariable(sVar,sVal);
  if (strcmp(sCmd, "GET") == 0) fusorCmdGetVariable(sVar);
  if (strcmp(sCmd, "GETALL") == 0) fusorCmdGetAll();
    
  FUSOR_LED_ON();
  delay(50);
  FUSOR_LED_OFF();
}

void fusorCmdGetAll() {
  fusorStartResponse("{");
  for (int i =0; i<fusorNumVars; i++) {
    fusorAddResponse("\"");
    FusorVariable *pfv = &fusorVariables[i];
    fusorAddResponse(pfv->name);
    fusorAddResponse("\":\"");
    fusorAddResponse(pfv->value);
    fusorAddResponse("\"");
    if (i< fusorNumVars-1) {
      fusorAddResponse(",");
    }
  }
  fusorAddResponse("}");
  fusorSendResponse(NULL);
}

struct FusorVariable *fusorGetVariableEntry(char *name) {
  FusorVariable *pfv = fusorVariables;
  for (int i =0; i<fusorNumVars; i++) {
    if (strcmp(pfv->name, name) == 0) {
      return pfv;
    }
    pfv++;
  }
  return NULL;
}

void fusorCmdSetVariable(char *var, char *val) {
  FusorVariable *pfv;
  pfv = fusorGetVariableEntry(var);
  if (pfv != NULL) {
    strncpy (pfv->value, val, FUSOR_VAR_LENGTH-1);
    pfv->value[FUSOR_VAR_LENGTH-1] = 0;
    pfv->updated = true;
    fusorStartResponse(var);
    fusorAddResponse(":");
    fusorAddResponse(val);
    fusorSendResponse(NULL);
  } else {
    fusorStartResponse("unknown variable:");
    fusorAddResponse(var);
    fusorSendResponse(NULL);
  }
}

void fusorCmdGetVariable(char *var) {
  FusorVariable *pfv;
  pfv = fusorGetVariableEntry(var);
  if (pfv != NULL) {
    fusorStartResponse(var);
    fusorAddResponse(":");
    fusorAddResponse(pfv->value);
    fusorSendResponse(NULL);
  } else {
    fusorStartResponse("unknown variable:");
    fusorAddResponse(var);
    fusorSendResponse(NULL);
  }
}


void fusorSetVariable(char * var, char *sVal, int *iVal, float *fVal) {
  char buffer[20];
  FusorVariable *pfv;
  
  pfv = fusorGetVariableEntry(var);
  if (pfv != NULL) {
    pfv->value[0] = 0;
    if (sVal != NULL) {
      strncat(pfv->value, sVal, FUSOR_VAR_LENGTH-1);
      pfv->value[FUSOR_VAR_LENGTH-1] = 0;
    }
    if (iVal != NULL) {
      itoa(*iVal, buffer, 10);
      strncat(pfv->value, buffer, FUSOR_VAR_LENGTH-1-strlen(buffer));
      pfv->value[FUSOR_VAR_LENGTH-1] = 0;
    }
    if (fVal != NULL) {
      sprintf(buffer, "%f", *fVal);
      strncat(pfv->value, buffer, FUSOR_VAR_LENGTH-1-strlen(buffer));
      pfv->value[FUSOR_VAR_LENGTH-1] = 0;
    }
  }
}



// ================================================================


FusorVariable fvs[] = {
  //name,   value,  updated
  {"foo",   "",     false},
  {"bar",   "",     false} 
};



#define delayMicros 1000


void setup(){
  // light for hope
  pinMode(LED_BUILTIN, OUTPUT);  // pin 13

  fusorInit(fvs, 2);
  
  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}


void loop() {
  fusorLoop();
  updateAll();
  delay(5);
}

void updateAll() {
  static int count = 0;
  count++;
  
  fusorSetVariable("foo", NULL, &count, NULL);
  fusorSetVariable("bar", "This is test iteration ", &count, NULL);
}
