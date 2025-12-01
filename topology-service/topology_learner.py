"""
Network Topology Learning with GNNs for Distributed FMS
Starter Implementation - Ready for Ignite Integration

Usage:
    python topology_learner.py --mode simulate --num-alarms 10000
    python topology_learner.py --mode train --days 7
    python topology_learner.py --mode server  # Start FastAPI service
"""

import numpy as np
import torch
import torch.nn.functional as F
from torch_geometric.nn import GATConv
from torch_geometric.data import Data
import networkx as nx
import json
from datetime import datetime, timedelta
from typing import List, Dict, Tuple
import logging
from dataclasses import dataclass, asdict
import random
import argparse
from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# ============================================================================
# DATA MODELS
# ============================================================================

@dataclass
class AlarmEvent:
    """Represents a single SNMP trap/alarm"""
    source_device: str
    timestamp: str
    alarm_type: str
    oid: str
    severity: str
    message: str
    is_root_cause: bool = False
    
    def to_dict(self):
        return asdict(self)


@dataclass
class TopologyEdge:
    """Edge in learned topology with confidence"""
    source: str
    target: str
    confidence: float
    causality_weight: float
    co_occurrence_count: int
    last_observed: str


class TopologyGraph:
    """Represents learned network topology"""
    def __init__(self):
        self.nodes = {}
        self.edges = {}
        self.timestamp = datetime.now()
        self.node_embeddings = {}
    
    def add_node(self, device_id: str, device_type: str, embedding: np.ndarray):
        self.nodes[device_id] = {
            "device_type": device_type,
            "embedding": embedding.tolist() if isinstance(embedding, np.ndarray) else embedding
        }
    
    def add_edge(self, source: str, target: str, confidence: float, 
                 causality: float, co_occur: int):
        key = (source, target)
        self.edges[key] = {
            "confidence": float(confidence),
            "causality_weight": float(causality),
            "co_occurrence_count": int(co_occur),
            "last_observed": datetime.now().isoformat()
        }
    
    def to_dict(self):
        return {
            "timestamp": self.timestamp.isoformat(),
            "nodes": self.nodes,
            "edges": {f"{k[0]}->{k[1]}": v for k, v in self.edges.items()},
            "num_nodes": len(self.nodes),
            "num_edges": len(self.edges)
        }


# ============================================================================
# SYNTHETIC ALARM GENERATOR
# ============================================================================

class NetworkTopologyGenerator:
    """Creates realistic synthetic network topology"""
    
    @staticmethod
    def create_synthetic_network() -> nx.DiGraph:
        """Create a realistic telecom network structure"""
        G = nx.DiGraph()
        
        # Core layer (2 devices)
        cores = ["CORE-A", "CORE-B"]
        for core in cores:
            G.add_node(core, layer=0, device_type="core_router")
        G.add_edge("CORE-A", "CORE-B", weight=0.95)
        G.add_edge("CORE-B", "CORE-A", weight=0.95)
        
        # Regional layer (4 devices)
        regionals = [f"REGION-{i}" for i in range(4)]
        for regional in regionals:
            G.add_node(regional, layer=1, device_type="switch")
        
        # Connect cores to regionals
        for core in cores:
            for regional in regionals:
                G.add_edge(core, regional, weight=0.85)
        
        # Edge layer (8 devices)
        edges = [f"EDGE-{i}" for i in range(8)]
        for edge in edges:
            G.add_node(edge, layer=2, device_type="edge_router")
        
        # Connect regionals to edges
        for idx, edge in enumerate(edges):
            connected_regionals = regionals[idx % 4:idx % 4 + 2]
            for regional in connected_regionals:
                G.add_edge(regional, edge, weight=0.75)
        
        # Hosts (16 devices)
        hosts = [f"HOST-{i}" for i in range(16)]
        for host in hosts:
            G.add_node(host, layer=3, device_type="host")
        
        # Connect hosts to edge routers
        for idx, host in enumerate(hosts):
            edge_routers = [edges[idx % len(edges)], edges[(idx + 1) % len(edges)]]
            for edge_router in edge_routers:
                G.add_edge(edge_router, host, weight=0.9)
        
        logger.info(f"Created synthetic network: {len(G.nodes())} nodes, {len(G.edges())} edges")
        return G


class SyntheticAlarmGenerator:
    """Generates realistic alarm sequences"""
    
    def __init__(self, network_topology: nx.DiGraph):
        self.G = network_topology
        self.devices = list(self.G.nodes())
        self.oid_map = {
            "LINK_DOWN": "1.3.6.1.6.3.1.1.5.3",
            "INTERFACE_ERROR": "1.3.6.1.4.1.8072.4.0.1",
            "BGPDOWN": "1.3.6.1.4.1.3641.2.1.0.1",
            "CPU_HIGH": "1.3.6.1.4.1.2578.2.1.1.0",
            "MEMORY_HIGH": "1.3.6.1.4.1.2578.2.1.2.0",
            "DISK_FULL": "1.3.6.1.4.1.2578.2.1.3.0",
            "TEMP_HIGH": "1.3.6.1.4.1.2578.2.1.4.0"
        }
    
    def generate_alarm_sequence(self, num_alarms: int = 1000) -> List[AlarmEvent]:
        """Generate realistic alarm sequences"""
        alarms = []
        current_time = datetime.now() - timedelta(days=7)
        
        logger.info(f"Generating {num_alarms} synthetic alarms...")
        
        for i in range(num_alarms):
            if random.random() < 0.7:
                # 70%: causal alarms (follow network topology)
                alarm_batch = self._generate_causal_alarm(current_time)
                alarms.extend(alarm_batch)
            else:
                # 30%: independent alarms (noise)
                alarm = self._generate_independent_alarm(current_time)
                alarms.append(alarm)
            
            # Increment time
            current_time += timedelta(milliseconds=random.randint(50, 500))
            
            if (i + 1) % 1000 == 0:
                logger.info(f"Generated {i + 1} alarms...")
        
        return alarms
    
    def _generate_causal_alarm(self, start_time: datetime) -> List[AlarmEvent]:
        """Simulate fault propagation through network"""
        
        # Pick root cause (weighted toward higher-level devices)
        if random.random() < 0.3:
            root = random.choice([d for d in self.devices if "CORE" in d])
        elif random.random() < 0.6:
            root = random.choice([d for d in self.devices if "REGION" in d])
        else:
            root = random.choice([d for d in self.devices if "EDGE" in d])
        
        # Find propagation path
        propagation = []
        visited = set()
        queue = [(root, 0)]
        
        while queue:
            device, depth = queue.pop(0)
            if device in visited or depth >= 3:
                continue
            visited.add(device)
            propagation.append((device, depth))
            
            for downstream in self.G.successors(device):
                if downstream not in visited:
                    queue.append((downstream, depth + 1))
        
        # Generate alarms for each device
        alarms = []
        for device, depth in propagation:
            alarm_delay = 50 * depth + np.random.normal(0, 10)
            alarm_time = start_time + timedelta(milliseconds=max(0, alarm_delay))
            
            alarm_type = random.choice(["LINK_DOWN", "INTERFACE_ERROR", "BGPDOWN", "CPU_HIGH"])
            
            alarms.append(AlarmEvent(
                source_device=device,
                timestamp=alarm_time.isoformat(),
                alarm_type=alarm_type,
                oid=self.oid_map[alarm_type],
                severity="CRITICAL" if depth < 2 else "WARNING",
                message=f"{device}: {alarm_type}",
                is_root_cause=(device == root)
            ))
        
        return alarms
    
    def _generate_independent_alarm(self, time: datetime) -> AlarmEvent:
        """Random independent alarm"""
        device = random.choice(self.devices)
        alarm_type = random.choice(["CPU_HIGH", "MEMORY_HIGH", "DISK_FULL", "TEMP_HIGH"])
        
        return AlarmEvent(
            source_device=device,
            timestamp=time.isoformat(),
            alarm_type=alarm_type,
            oid=self.oid_map[alarm_type],
            severity=random.choice(["CRITICAL", "WARNING"]),
            message=f"{device}: {alarm_type}",
            is_root_cause=False
        )


# ============================================================================
# GRAPH FEATURE ENGINEERING
# ============================================================================

class GraphFeatureExtractor:
    """Extract graph features from alarm history"""
    
    def __init__(self, time_window_seconds: int = 10):
        self.time_window = time_window_seconds
    
    def extract_features(self, alarms: List[AlarmEvent], 
                        devices: List[str]) -> Tuple[np.ndarray, List[Tuple]]:
        """
        Extract node features and edges from alarms
        
        Returns:
            node_features: [num_devices, feature_dim]
            edges: [(source, target, weight), ...]
        """
        
        device_to_idx = {dev: i for i, dev in enumerate(devices)}
        num_devices = len(devices)
        
        # Initialize feature matrix
        node_features = np.zeros((num_devices, 8))
        edge_counts = np.zeros((num_devices, num_devices))
        edge_causality = np.zeros((num_devices, num_devices))
        
        # Process alarms
        for i, alarm_i in enumerate(alarms):
            idx_i = device_to_idx.get(alarm_i.source_device)
            if idx_i is None:
                continue
            
            # Update node features
            node_features[idx_i, 0] += 1  # Alert frequency
            
            if alarm_i.severity == "CRITICAL":
                node_features[idx_i, 1] += 1  # Critical alerts
            else:
                node_features[idx_i, 2] += 1  # Warnings
            
            # Find correlated alarms within time window
            alarm_i_time = datetime.fromisoformat(alarm_i.timestamp)
            
            for j, alarm_j in enumerate(alarms[i+1:i+50]):  # Look ahead
                idx_j = device_to_idx.get(alarm_j.source_device)
                if idx_j is None:
                    continue
                
                alarm_j_time = datetime.fromisoformat(alarm_j.timestamp)
                time_diff = (alarm_j_time - alarm_i_time).total_seconds()
                
                if time_diff <= self.time_window:
                    # Co-occurrence
                    edge_counts[idx_i, idx_j] += 1
                    
                    # Causality (if alarm_i precedes alarm_j)
                    if 0 < time_diff < 0.2:  # 200ms window
                        edge_causality[idx_i, idx_j] += 1
        
        # Normalize node features
        for i in range(num_devices):
            total_alerts = node_features[i, 0] + 1e-6
            node_features[i, 1] /= total_alerts  # Critical ratio
            node_features[i, 2] /= total_alerts  # Warning ratio
            node_features[i, 3] = np.sum(edge_counts[i, :]) / (num_devices * total_alerts + 1e-6)  # Connectivity
            node_features[i, 4] = np.sum(edge_causality[i, :]) / (num_devices * total_alerts + 1e-6)  # Causality
        
        # Extract edges with weights
        edges = []
        for i in range(num_devices):
            for j in range(num_devices):
                if i != j:
                    co_occur = edge_counts[i, j]
                    causality = edge_causality[i, j]
                    
                    if co_occur > 0:
                        weight = (co_occur + causality) / max(1, np.sum(edge_counts[i, :]))
                        edges.append((
                            i, j, float(weight),
                            float(causality), int(co_occur),
                            devices[i], devices[j]
                        ))
        
        logger.info(f"Extracted features: {num_devices} nodes, {len(edges)} edges")
        return node_features, edges


# ============================================================================
# GRAPH NEURAL NETWORK
# ============================================================================

class TopologyGNN(torch.nn.Module):
    """Graph Attention Network for topology learning"""
    
    def __init__(self, input_dim: int, hidden_dim: int = 64, output_dim: int = 32):
        super().__init__()
        self.gat1 = GATConv(input_dim, hidden_dim, heads=8, dropout=0.2)
        self.gat2 = GATConv(hidden_dim * 8, output_dim, heads=4, dropout=0.2)
        
        # Link prediction MLP
        self.mlp = torch.nn.Sequential(
            torch.nn.Linear(output_dim * 2 * 4, 64),
            torch.nn.ReLU(),
            torch.nn.Dropout(0.2),
            torch.nn.Linear(64, 32),
            torch.nn.ReLU(),
            torch.nn.Linear(32, 1)
        )
    
    def forward(self, x: torch.Tensor, edge_index: torch.Tensor, 
                edge_weight: torch.Tensor = None) -> torch.Tensor:
        """Forward pass - returns node embeddings"""
        x = self.gat1(x, edge_index, edge_attr=edge_weight)
        x = F.elu(x)
        x = F.dropout(x, p=0.2, training=self.training)
        x = self.gat2(x, edge_index, edge_attr=edge_weight)
        return x
    
    def predict_edges(self, z: torch.Tensor, 
                     edge_index: torch.Tensor) -> torch.Tensor:
        """Predict edge existence from embeddings"""
        src, dst = edge_index
        edge_z = torch.cat([z[src], z[dst]], dim=1)
        return torch.sigmoid(self.mlp(edge_z)).squeeze()


# ============================================================================
# TRAINING PIPELINE
# ============================================================================

class TopologyLearner:
    """Main training and inference engine"""
    
    def __init__(self, device: str = "cpu"):
        self.device = torch.device(device)
        self.model = None
        self.topology = TopologyGraph()
    
    def train_from_alarms(self, alarms: List[AlarmEvent], 
                         devices: List[str], 
                         epochs: int = 50) -> TopologyGraph:
        """Train GNN on alarm history"""
        
        logger.info(f"Training topology from {len(alarms)} alarms...")
        
        # Extract features
        extractor = GraphFeatureExtractor(time_window_seconds=10)
        node_features, edges_data = extractor.extract_features(alarms, devices)
        
        # Convert to tensors
        x = torch.from_numpy(node_features).float().to(self.device)
        
        # Build edge index
        edge_indices = [(int(e[0]), int(e[1])) for e in edges_data]
        edge_weights = [e[2] for e in edges_data]
        
        if edge_indices:
            edge_index = torch.tensor(edge_indices, dtype=torch.long).t().contiguous().to(self.device)
            edge_weight = torch.tensor(edge_weights, dtype=torch.float).to(self.device)
        else:
            logger.warning("No edges found in data!")
            return self.topology
        
        # Initialize model
        input_dim = node_features.shape[1]
        self.model = TopologyGNN(input_dim, hidden_dim=64, output_dim=32).to(self.device)
        optimizer = torch.optim.Adam(self.model.parameters(), lr=0.001)
        
        # Training loop
        self.model.train()
        for epoch in range(epochs):
            optimizer.zero_grad()
            
            # Forward pass
            z = self.model(x, edge_index, edge_weight)
            
            # Link prediction loss
            pos_edge_pred = self.model.predict_edges(z, edge_index)
            pos_loss = F.binary_cross_entropy(pos_edge_pred, torch.ones_like(pos_edge_pred))
            
            # Negative sampling
            neg_edge_idx = self._sample_negative_edges(len(devices), len(edge_indices))
            neg_edge_pred = self.model.predict_edges(z, neg_edge_idx)
            neg_loss = F.binary_cross_entropy(neg_edge_pred, torch.zeros_like(neg_edge_pred))
            
            loss = pos_loss + neg_loss
            loss.backward()
            optimizer.step()
            
            if (epoch + 1) % 10 == 0:
                logger.info(f"Epoch {epoch+1}/{epochs}, Loss: {loss.item():.4f}")
        
        # Build topology from learned edges
        self.model.eval()
        with torch.no_grad():
            z = self.model(x, edge_index, edge_weight)
            
            # Store node embeddings
            for i, device in enumerate(devices):
                device_type = self._infer_device_type(device)
                self.topology.add_node(device, device_type, z[i].cpu().numpy())
            
            # Predict all edges
            all_edges = self._generate_all_possible_edges(len(devices))
            edge_pred = self.model.predict_edges(z, all_edges)
            
            # Add edges above threshold
            for (i, j), score in zip(all_edges.t().tolist(), edge_pred.cpu().numpy()):
                if score > 0.0:
                    # Find corresponding edge data for causality/co-occurrence
                    matching = [e for e in edges_data if e[0] == i and e[1] == j]
                    if matching:
                        e = matching[0]
                        causality = e[3] / max(1, e[4])
                        self.topology.add_edge(devices[i], devices[j], 
                                             float(score), causality, int(e[4]))
        
        logger.info(f"Topology learned: {len(self.topology.nodes)} nodes, {len(self.topology.edges)} edges")
        return self.topology
    
    @staticmethod
    def _infer_device_type(device_name: str) -> str:
        """Infer device type from name"""
        if "CORE" in device_name:
            return "core_router"
        elif "REGION" in device_name:
            return "switch"
        elif "EDGE" in device_name:
            return "edge_router"
        else:
            return "host"
    
    @staticmethod
    def _sample_negative_edges(num_nodes: int, num_pos_edges: int) -> torch.Tensor:
        """Sample non-existent edges for training"""
        neg_edges = []
        for _ in range(num_pos_edges):
            i = random.randint(0, num_nodes - 1)
            j = random.randint(0, num_nodes - 1)
            if i != j:
                neg_edges.append([i, j])
        return torch.tensor(neg_edges, dtype=torch.long).t().contiguous()
    
    @staticmethod
    def _generate_all_possible_edges(num_nodes: int) -> torch.Tensor:
        """Generate all possible edges for inference"""
        edges = []
        for i in range(num_nodes):
            for j in range(num_nodes):
                if i != j:
                    edges.append([i, j])
        return torch.tensor(edges, dtype=torch.long).t().contiguous()


# ============================================================================
# FASTAPI SERVICE (for Ignite integration)
# ============================================================================

app = FastAPI(title="FMS Topology Learner")

class TrainRequest(BaseModel):
    alarm_history_hours: int = 24
    num_epochs: int = 50

class TopologyResponse(BaseModel):
    timestamp: str
    nodes: int
    edges: int
    status: str

@app.post("/train")
async def train_topology(request: TrainRequest):
    """Train topology model from historical alarms"""
    try:
        # In real integration, fetch alarms from Ignite
        # For now, use synthetic data
        network = NetworkTopologyGenerator.create_synthetic_network()
        generator = SyntheticAlarmGenerator(network)
        alarms = generator.generate_alarm_sequence(num_alarms=5000)
        devices = list(network.nodes())
        
        learner = TopologyLearner(device="cpu")
        topology = learner.train_from_alarms(alarms, devices, epochs=request.num_epochs)
        
        return TopologyResponse(
            timestamp=topology.timestamp.isoformat(),
            nodes=len(topology.nodes),
            edges=len(topology.edges),
            status="trained"
        )
    except Exception as e:
        logger.error(f"Training failed: {e}")
        return {"status": "error", "message": str(e)}

@app.get("/topology")
async def get_topology():
    """Get current learned topology"""
    return {"topology": "Not yet trained"}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=["simulate", "train", "server"], default="train")
    parser.add_argument("--num-alarms", type=int, default=10000)
    parser.add_argument("--days", type=int, default=7)
    parser.add_argument("--epochs", type=int, default=50)
    
    args = parser.parse_args()
    
    if args.mode == "simulate":
        # Generate and save synthetic alarms
        network = NetworkTopologyGenerator.create_synthetic_network()
        generator = SyntheticAlarmGenerator(network)
        alarms = generator.generate_alarm_sequence(args.num_alarms)
        
        with open("synthetic_alarms.json", "w") as f:
            json.dump([a.to_dict() for a in alarms], f)
        logger.info(f"Saved {len(alarms)} alarms to synthetic_alarms.json")
    
    elif args.mode == "train":
        # Train GNN on synthetic data
        network = NetworkTopologyGenerator.create_synthetic_network()
        generator = SyntheticAlarmGenerator(network)
        alarms = generator.generate_alarm_sequence(args.num_alarms)
        devices = list(network.nodes())
        
        learner = TopologyLearner(device="cpu")
        topology = learner.train_from_alarms(alarms, devices, epochs=args.epochs)
        
        # Save results
        with open("learned_topology.json", "w") as f:
            json.dump(topology.to_dict(), f, indent=2)
        logger.info("Saved topology to learned_topology.json")
        
        # Validate
        logger.info(f"\nTopology Results:")
        logger.info(f"  Real edges in network: {len(network.edges())}")
        logger.info(f"  Learned edges: {len(topology.edges)}")
        logger.info(f"  Learned nodes: {len(topology.nodes)}")
    
    elif args.mode == "server":
        # Start FastAPI service
        uvicorn.run(app, host="0.0.0.0", port=8000)
