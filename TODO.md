### Issues
- [x] Fix RSSI (arduino uint16 serialization/deserialization)
- [x] gracefully close actor system + app if unable to connect or error
- [ ] check closing of serial port on CTRL-C

### Features
- [ ] add driver monitoring (throughput, queue size, message wait time in queue, ack delay)
- [x] initialize the gateway with settings (network id, gateway id)
- [ ] add OTA programing
- [x] ~~add protobuf support~~ now use `scodec` with custom protocol
- [ ] add reset node feature
- [ ] make gateway write its mac (unique id)
- [ ] write last seen timestamp each time node is seen (network)


Each node handler should implement a trait providing following actions:
- declare its name
- declare its (spray) api (should always acts on a group (1**n) of nodes)
- createMeasure()
- updateState()
- getState()
