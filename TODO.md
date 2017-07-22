### Modules
- Compteur électricité (triphasé)
- Compteur pulsation eau
- Capteur d'ambiance (température, humidité, luminosité)
- Capteur météo (température, luminosité, pluviométrie, pression barométrique)
- Capteur de présence / mouvement
- Capteur ouverture / fermeture de porte
- Controle volet roulant
- Controle ambiance salon (bandeau led RGB)
- Controle luminaire

### Alarme
- I/O controle (siren, lamp, etc)
- SIM900 SMS/MMS + photos


### Issues
- [x] Fix RSSI (arduino uint16 serialization/deserialization)
- [x] gracefully close actor system + app if unable to connect or error
- [ ] check closing of serial port on CTRL-C

### Features
- [x] initialize the gateway with settings (network id, gateway id)
- [x] ~~add protobuf support~~ now use `scodec` with custom protocol
- [x] make gateway write its mac (unique id)
- [x] write last seen timestamp each time node is seen (network)

- [ ] (gateway) add driver monitoring (throughput, queue size, message wait time in queue, ack delay)

- [ ] (protocol) add OTA programing

- [ ] (protocol) a node can send battery update in any data payload
  battery voltage should be wrapped in payload and automatically unwrapped/handled gateway side
  a enableBatteryUpdate() method must be available in the clib to enable this feature

- [ ] (clib) node can enable active listening
  any call to messageAvailable() should block

- [ ] (protocol) handle set state value / get state value
  in case of a getValue call, the value is returned via the ack
  any double or long type can be used

- [ ] (protocol) node can request clock
  achieved via a getClock call

- [ ] (protocol) gateway can trigger a heartbeat (for active node only)

- [ ] (protocol) gateway can reset a node (for active node only)
