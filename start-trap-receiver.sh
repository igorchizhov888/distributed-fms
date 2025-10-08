#!/bin/bash
java -cp target/distributed-fms-0.1.0-SNAPSHOT.jar com.distributedFMS.agent.SnmpTrapReceiver &> trap.log &
