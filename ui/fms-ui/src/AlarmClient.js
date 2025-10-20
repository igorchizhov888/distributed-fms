import { AlarmServiceClient } from './generated/FMS_grpc_web_pb.js';
import { QueryAlarmsRequest } from './generated/FMS_pb';

const client = new AlarmServiceClient('http://localhost:8080');

export const subscribeToAlarms = (callback) => {
  const request = new QueryAlarmsRequest();
  const stream = client.queryAlarms(request, {});

  stream.on('data', (response) => {
    callback(response.toObject());
  });

  stream.on('end', () => {
    console.log('Stream ended');
  });

  stream.on('error', (err) => {
    console.error(`Unexpected stream error: code = ${err.code}, message = ${err.message}`);
  });

  return () => {
    stream.cancel();
  };
};