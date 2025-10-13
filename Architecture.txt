Current Architecture (v0.1.0):
┌─────────────┐
│SNMP Network │
│  Devices    │
└──────┬──────┘
       │
       v
┌─────────────────┐
│SnmpTrapReceiver │
│   (Port 10162)  │
└──────┬──────────┘
       │
       v
┌─────────────────┐
│  Kafka Topic    │
│  'fms-events'   │
└──────┬──────────┘
       │
       v
┌─────────────────┐     ┌─────────────────┐
│EventConsumer    │────>│Ignite Cluster   │
│(Node 1, 2, ...N)│     │(Distributed)    │
└─────────────────┘     └─────────────────┘
       │                         │
       v                         v
┌─────────────────────────────────────┐
│  Deduplication & Correlation Logic  │
└─────────────────────────────────────┘