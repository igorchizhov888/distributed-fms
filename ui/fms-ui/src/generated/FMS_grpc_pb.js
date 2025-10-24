// GENERATED CODE -- DO NOT EDIT!

'use strict';
var grpc = require('@grpc/grpc-js');
var FMS_pb = require('./FMS_pb.js');

function serialize_com_distributedFMS_AlarmMessage(arg) {
  if (!(arg instanceof FMS_pb.AlarmMessage)) {
    throw new Error('Expected argument of type com.distributedFMS.AlarmMessage');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_com_distributedFMS_AlarmMessage(buffer_arg) {
  return FMS_pb.AlarmMessage.deserializeBinary(new Uint8Array(buffer_arg));
}

function serialize_com_distributedFMS_QueryAlarmsRequest(arg) {
  if (!(arg instanceof FMS_pb.QueryAlarmsRequest)) {
    throw new Error('Expected argument of type com.distributedFMS.QueryAlarmsRequest');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_com_distributedFMS_QueryAlarmsRequest(buffer_arg) {
  return FMS_pb.QueryAlarmsRequest.deserializeBinary(new Uint8Array(buffer_arg));
}

function serialize_com_distributedFMS_SubmitEventRequest(arg) {
  if (!(arg instanceof FMS_pb.SubmitEventRequest)) {
    throw new Error('Expected argument of type com.distributedFMS.SubmitEventRequest');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_com_distributedFMS_SubmitEventRequest(buffer_arg) {
  return FMS_pb.SubmitEventRequest.deserializeBinary(new Uint8Array(buffer_arg));
}

function serialize_com_distributedFMS_SubmitEventResponse(arg) {
  if (!(arg instanceof FMS_pb.SubmitEventResponse)) {
    throw new Error('Expected argument of type com.distributedFMS.SubmitEventResponse');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_com_distributedFMS_SubmitEventResponse(buffer_arg) {
  return FMS_pb.SubmitEventResponse.deserializeBinary(new Uint8Array(buffer_arg));
}


// The FMS service definition.
var ManagementServiceService = exports.ManagementServiceService = {
  // Submits an event to the FMS.
submitEvent: {
    path: '/com.distributedFMS.ManagementService/SubmitEvent',
    requestStream: false,
    responseStream: false,
    requestType: FMS_pb.SubmitEventRequest,
    responseType: FMS_pb.SubmitEventResponse,
    requestSerialize: serialize_com_distributedFMS_SubmitEventRequest,
    requestDeserialize: deserialize_com_distributedFMS_SubmitEventRequest,
    responseSerialize: serialize_com_distributedFMS_SubmitEventResponse,
    responseDeserialize: deserialize_com_distributedFMS_SubmitEventResponse,
  },
};

exports.ManagementServiceClient = grpc.makeGenericClientConstructor(ManagementServiceService, 'ManagementService');
// The Alarm Query service definition.
var AlarmServiceService = exports.AlarmServiceService = {
  // Queries for alarms, with optional filtering.
queryAlarms: {
    path: '/com.distributedFMS.AlarmService/QueryAlarms',
    requestStream: false,
    responseStream: true,
    requestType: FMS_pb.QueryAlarmsRequest,
    responseType: FMS_pb.AlarmMessage,
    requestSerialize: serialize_com_distributedFMS_QueryAlarmsRequest,
    requestDeserialize: deserialize_com_distributedFMS_QueryAlarmsRequest,
    responseSerialize: serialize_com_distributedFMS_AlarmMessage,
    responseDeserialize: deserialize_com_distributedFMS_AlarmMessage,
  },
};

exports.AlarmServiceClient = grpc.makeGenericClientConstructor(AlarmServiceService, 'AlarmService');
