#!/usr/bin/env python3
"""gRPC Server for Network Topology Learning Service"""

import grpc
from concurrent import futures
import time
import logging
from datetime import datetime

import FMS_pb2
import FMS_pb2_grpc
from topology_learner import TopologyLearner, NetworkTopologyGenerator, SyntheticAlarmGenerator, AlarmEvent

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TopologyServiceImpl(FMS_pb2_grpc.TopologyServiceServicer):
    
    def __init__(self):
        self.learner = TopologyLearner(device="cpu")
        self.topology = None
        logger.info("✅ TopologyService initialized")
    
    def TrainTopology(self, request, context):
        """Train with synthetic data (backward compatibility)"""
        try:
            logger.info(f"📊 Training (synthetic): {request.alarm_history_hours}h, {request.num_epochs} epochs")
            
            network = NetworkTopologyGenerator.create_synthetic_network()
            generator = SyntheticAlarmGenerator(network)
            alarms = generator.generate_alarm_sequence(num_alarms=10000)
            devices = list(network.nodes())
            
            start = time.time()
            self.topology = self.learner.train_from_alarms(alarms, devices, epochs=request.num_epochs)
            duration = time.time() - start
            
            logger.info(f"✅ Training done in {duration:.2f}s: {len(self.topology.nodes)} nodes, {len(self.topology.edges)} edges")
            
            return FMS_pb2.TrainTopologyResponse(
                success=True,
                message=f"Training completed in {duration:.2f}s",
                num_nodes=len(self.topology.nodes),
                num_edges=len(self.topology.edges),
                training_loss=1.0,
                training_timestamp=datetime.now().isoformat()
            )
        except Exception as e:
            logger.error(f"❌ Training failed: {e}", exc_info=True)
            return FMS_pb2.TrainTopologyResponse(success=False, message=str(e))
    
    def TrainTopologyWithAlarms(self, request, context):
        """Train with real alarms from FMS (NEW)"""
        try:
            logger.info(f"📊 Training with REAL alarms: {len(request.alarms)} alarms, {request.num_epochs} epochs")
            
            # Convert protobuf AlarmData to AlarmEvent objects
            alarms = []
            for alarm_data in request.alarms:
                alarm = AlarmEvent()
                alarm.device_id = alarm_data.device_id
                alarm.timestamp = datetime.fromtimestamp(alarm_data.timestamp / 1000.0)
                alarm.severity = alarm_data.severity
                alarm.event_type = alarm_data.event_type
                alarm.description = alarm_data.description
                alarms.append(alarm)
            
            devices = list(request.device_ids)
            
            logger.info(f"Converted {len(alarms)} alarms from {len(devices)} devices")
            
            start = time.time()
            self.topology = self.learner.train_from_alarms(alarms, devices, epochs=request.num_epochs)
            duration = time.time() - start
            
            logger.info(f"✅ REAL DATA training done in {duration:.2f}s: {len(self.topology.nodes)} nodes, {len(self.topology.edges)} edges")
            
            return FMS_pb2.TrainTopologyResponse(
                success=True,
                message=f"Trained on {len(alarms)} real alarms in {duration:.2f}s",
                num_nodes=len(self.topology.nodes),
                num_edges=len(self.topology.edges),
                training_loss=1.0,
                training_timestamp=datetime.now().isoformat()
            )
        except Exception as e:
            logger.error(f"❌ Real alarm training failed: {e}", exc_info=True)
            return FMS_pb2.TrainTopologyResponse(success=False, message=str(e))
    
    def GetTopology(self, request, context):
        try:
            if self.topology is None:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details("No topology. Train first.")
                return FMS_pb2.GetTopologyResponse()
            
            min_conf = request.min_confidence or 0.5
            
            nodes = [FMS_pb2.TopologyNode(
                device_id=did, 
                device_type=nd['device_type'], 
                embedding=nd['embedding']
            ) for did, nd in self.topology.nodes.items()]
            
            edges = []
            for (src, dst), ed in self.topology.edges.items():
                if ed['confidence'] >= min_conf:
                    edges.append(FMS_pb2.TopologyEdge(
                        source_device=src,
                        destination_device=dst,
                        confidence=ed['confidence'],
                        causality_weight=ed['causality_weight'],
                        co_occurrence_count=ed['co_occurrence_count'],
                        last_observed=ed.get('last_observed', '')
                    ))
            
            logger.info(f"📤 Returning: {len(nodes)} nodes, {len(edges)} edges")
            return FMS_pb2.GetTopologyResponse(
                nodes=nodes, edges=edges,
                topology_timestamp=self.topology.timestamp.isoformat(),
                total_nodes=len(nodes), total_edges=len(edges)
            )
        except Exception as e:
            logger.error(f"❌ GetTopology failed: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            return FMS_pb2.GetTopologyResponse()
    
    def PredictConnection(self, request, context):
        try:
            if self.topology is None:
                return FMS_pb2.PredictConnectionResponse(connected=False, confidence=0.0, explanation="No model")
            
            edge_data = self.topology.edges.get((request.device_a, request.device_b)) or \
                       self.topology.edges.get((request.device_b, request.device_a))
            
            if edge_data is None:
                return FMS_pb2.PredictConnectionResponse(connected=False, confidence=0.0, explanation="No edge")
            
            return FMS_pb2.PredictConnectionResponse(
                connected=edge_data['confidence'] > 0.5,
                confidence=edge_data['confidence'],
                causality_weight=edge_data['causality_weight'],
                explanation=f"Confidence: {edge_data['confidence']:.3f}"
            )
        except Exception as e:
            logger.error(f"❌ PredictConnection failed: {e}")
            return FMS_pb2.PredictConnectionResponse(connected=False, confidence=0.0, explanation=str(e))
    
    def GetDeviceConnections(self, request, context):
        try:
            if self.topology is None:
                return FMS_pb2.GetDeviceConnectionsResponse(edges=[], total_connections=0)
            
            min_conf = request.min_confidence or 0.5
            device_edges = []
            
            for (src, dst), ed in self.topology.edges.items():
                if ed['confidence'] >= min_conf and (src == request.device_id or dst == request.device_id):
                    device_edges.append(FMS_pb2.TopologyEdge(
                        source_device=src, destination_device=dst,
                        confidence=ed['confidence'],
                        causality_weight=ed['causality_weight'],
                        co_occurrence_count=ed['co_occurrence_count']
                    ))
            
            return FMS_pb2.GetDeviceConnectionsResponse(edges=device_edges, total_connections=len(device_edges))
        except Exception as e:
            logger.error(f"❌ GetDeviceConnections failed: {e}")
            return FMS_pb2.GetDeviceConnectionsResponse(edges=[], total_connections=0)


def serve(port=50052):
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    FMS_pb2_grpc.add_TopologyServiceServicer_to_server(TopologyServiceImpl(), server)
    server.add_insecure_port(f'[::]:{port}')
    server.start()
    
    logger.info("")
    logger.info("=" * 60)
    logger.info("  🚀 Topology gRPC Server Started")
    logger.info("=" * 60)
    logger.info(f"  Port: {port}")
    logger.info("  ✨ NEW: Accepts real alarm data from FMS")
    logger.info("=" * 60)
    logger.info("")
    
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("\n⏹️ Shutting down...")
        server.stop(0)


if __name__ == '__main__':
    serve()
