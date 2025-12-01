# Network Topology Learning with GNNs for Distributed FMS

## 1. Overview & Concepts

### What We're Doing
Instead of manually configuring "device A connects to device B," we let the system learn the logical network topology by observing:
- Which alarms co-occur frequently
- Temporal patterns (alarm A always precedes alarm B by 50ms)
- Correlation strength over time
- SNMP OID patterns that indicate device relationships

### Why GNNs?
- Natural graph representation: devices = nodes, relationships = edges
- Learn node embeddings that capture "device role" (router, switch, host, etc.)
- Predict missing edges and edge weights (relationship strength)
- Explainable: can show which alarm patterns led to topology inference

### Output
A dynamic network graph showing:
```
Router-A ──(strong)──> Switch-B ──(weak)──> Host-C
   │
   └──(medium)──> Firewall-D
```

## 2. Architecture Overview

### System Flow
```
SNMP Traps → Kafka Topic
              ↓
         Trap Processor (Java/Ignite)
              ↓
         Alarm Correlation Engine (Existing)
              ↓
    GNN Topology Learner (Python Service)
              ↓
         Topology Graph (Ignite Cache)
              ↓
    Enhanced Correlation Rules
              ↓
         React UI (Visualize Topology)
```

### Key Components

**1. Graph Construction Service (Python)**
- Consumes alarms from Kafka
- Builds temporal interaction matrix
- Updates node/edge features continuously

**2. GNN Model (PyTorch/PyTorch Geometric)**
- Learns latent device representations
- Predicts likely new connections
- Scores edge confidence

**3. Bridge Layer (Python → Java/Ignite)**
- REST API or gRPC to communicate with Ignite
- Writes topology to Ignite cache
- Reads correlation history from Ignite

**4. Ignite Integration**
- Stores: Device nodes, edges, embeddings, confidence scores
- Reads: Historical alarms, correlations
- Exposes: Topology queries to Java components

### Technology Stack

```
Java/Ignite (existing)
    ↓
Python Service (NEW)
    ├─ PyTorch Geometric (GNN)
    ├─ NetworkX (graph utilities)
    ├─ Pandas/NumPy (data processing)
    └─ gRPC/FastAPI (communication with Ignite)
    ↓
React UI (existing, enhanced visualization)
```

## 3. Detailed Implementation Strategy

### Phase 1: Graph Feature Engineering (Weeks 1-2)

**From Alarms to Graph Features**

For each pair of devices (i, j), extract:

1. **Co-occurrence Frequency**
   ```
   co_occur[i,j] = count(alarm_i and alarm_j within 10s window) / total_alarms
   ```

2. **Temporal Causality**
   ```
   causality[i→j] = P(alarm_j follows alarm_i within 100ms)
   ```

3. **Alarm Correlation Strength**
   ```
   From your existing correlation engine:
   strength[i,j] = correlation_score from Ignite cache
   ```

4. **OID Similarity**
   ```
   oid_similarity[i,j] = cosine_distance(oid_vector_i, oid_vector_j)
   Device types often use predictable OID patterns
   ```

5. **Message Content Similarity**
   ```
   text_similarity[i,j] = embedding_cosine_similarity(trap_text_i, trap_text_j)
   Using sentence-transformers for semantic similarity
   ```

**Node Features (Per Device)**
- Alert frequency (how often alarms from this device)
- Alert severity distribution (% critical, warning, info)
- OID pattern hash (device type fingerprint)
- Time-of-day patterns (peaks, quiet periods)
- Geographic region (if available in trap)

Example node feature vector (8D):
```python
node_features = [
    alert_frequency,           # normalized 0-1
    severity_critical_pct,     # 0-1
    severity_warning_pct,      # 0-1
    is_core_device,            # inferred: high connectivity
    is_edge_device,            # inferred: low connectivity
    temporal_volatility,       # std dev of inter-alarm time
    oid_diversity,             # how many different OIDs
    region_id                  # one-hot encoded
]
```

### Phase 2: GNN Architecture (Week 2-3)

**Graph Neural Network Design**

```python
class TopologyGNN(torch.nn.Module):
    def __init__(self, input_dim, hidden_dim, output_dim):
        super().__init__()
        # Graph Attention Network (GAT) - good for inferring importance
        self.gat1 = GATConv(input_dim, hidden_dim, heads=8)
        self.gat2 = GATConv(hidden_dim * 8, output_dim, heads=4)
        
        # Link prediction decoder
        self.mlp = MLP(output_dim * 2, [64, 32, 1])
    
    def forward(self, x, edge_index, edge_weight=None):
        # x: node features [num_nodes, input_dim]
        # edge_index: [2, num_edges]
        # edge_weight: optional edge weights
        
        x = self.gat1(x, edge_index, edge_attr=edge_weight)
        x = F.elu(x)
        x = self.gat2(x, edge_index, edge_attr=edge_weight)
        return x
    
    def predict_link(self, z, edge_index_pos, edge_index_neg):
        # Link prediction: predicts if edges should exist
        pos_score = (z[edge_index_pos[0]] * z[edge_index_pos[1]]).sum(dim=1)
        neg_score = (z[edge_index_neg[0]] * z[edge_index_neg[1]]).sum(dim=1)
        
        # Return edge confidence scores
        return pos_score.sigmoid()
```

**Why GAT (Graph Attention Networks)?**
- Learns which neighbors are important (attention weights)
- Captures that "direct upstream device matters more than distant devices"
- Interpretable: can visualize attention patterns
- Good for heterogeneous networks (routers, switches, hosts differ)

**Alternative: GraphSAGE or GCN**
- GraphSAGE: better for inductive learning (new devices appearing)
- GCN: simpler, if your topology is relatively stable

### Phase 3: Training & Inference (Week 3-4)

**Training Loop**

```python
def train_topology_gnn(alarm_history, model, epochs=100):
    """
    Input: alarm_history from Ignite cache
    - List of (device_a, device_b, correlation_score, timestamp)
    """
    
    # Build temporal windows (last 7 days → training snapshot)
    for window_date in sliding_windows(alarm_history, window_size=7_days):
        
        # Construct graph from this window
        G = build_graph_from_alarms(window_date)
        
        # Split edges: 80% train, 10% val, 10% test
        # IMPORTANT: temporal split, not random!
        # Train on day 1-5, validate on day 6, test on day 7
        
        train_edges = G.edges[:day5]
        val_edges = G.edges[day6]
        test_edges = G.edges[day7]
        
        # Negative sampling (non-existent edges)
        negative_edges = sample_non_edges(G, num=len(train_edges))
        
        # Train
        for epoch in range(epochs):
            z = model(x=node_features, edge_index=train_edges)
            
            # Link prediction loss
            loss = link_prediction_loss(z, train_edges, negative_edges)
            loss.backward()
            optimizer.step()
            
            # Validate
            if epoch % 10 == 0:
                val_auc = evaluate_link_prediction(z, val_edges, negative_edges)
                print(f"Epoch {epoch}: Val AUC = {val_auc:.4f}")
        
        # Final inference on test set
        final_z = model(x=node_features, edge_index=all_edges)
        topology_scores = final_z  # Node embeddings
        edge_confidence = predict_edges(final_z, all_possible_edges)
        
        # Write to Ignite
        write_topology_to_ignite(edge_confidence, final_z)
```

**Why Temporal Split?**
- Realistic: tomorrow's topology should build on yesterday's alarms
- Prevents data leakage: doesn't cheat by seeing future

### Phase 4: Integration with Ignite (Week 4)

**Option A: REST API Bridge (Simpler)**

```python
# Python FastAPI service
from fastapi import FastAPI
import grpc_client_to_ignite

app = FastAPI()

@app.post("/topology/update")
async def update_topology(alarm_batch: List[AlarmEvent]):
    """
    Called by Ignite when new alarms arrive
    """
    # Update graph incrementally
    G.add_nodes_from(extract_devices(alarm_batch))
    G.add_edges_from(extract_correlations(alarm_batch))
    
    # Run GNN inference (lightweight, ~10ms for typical network)
    edge_scores = model.predict_edges(G)
    
    # Write back to Ignite
    await write_to_ignite_cache("topology_graph", edge_scores)
    
    return {"status": "ok", "edges_updated": len(edge_scores)}

@app.get("/topology/graph")
async def get_topology():
    """
    Called by React UI to render network graph
    """
    topology = await read_from_ignite_cache("topology_graph")
    return {
        "nodes": list(topology.nodes()),
        "edges": list(topology.edges()),
        "embeddings": topology.node_embeddings
    }
```

**Option B: gRPC Service (More efficient)**

```protobuf
// topology.proto
syntax = "proto3";

service TopologyService {
  rpc UpdateTopology(AlarmBatch) returns (TopologyUpdate);
  rpc GetTopology(Empty) returns (TopologyGraph);
  rpc PredictCausality(DevicePair) returns (CausalityScore);
}

message TopologyGraph {
  repeated Node nodes = 1;
  repeated Edge edges = 2;
}

message Edge {
  string source = 1;
  string target = 2;
  float confidence = 3;
  float causality_weight = 4;
}
```

**Ignite Cache Structure**

```java
// In your existing Ignite setup
IgniteCache<String, TopologyGraph> topologyCache = ignite.cache("topology");
IgniteCache<String, DeviceEmbedding> embeddingCache = ignite.cache("device_embeddings");

// Example entries
topologyCache.put("latest", new TopologyGraph(
    nodes = [Device("Router-A"), Device("Switch-B"), ...],
    edges = [
        Edge("Router-A", "Switch-B", confidence=0.95, causality=0.87),
        Edge("Switch-B", "Host-C", confidence=0.72, causality=0.61),
        ...
    ],
    timestamp = System.currentTimeMillis()
));
```

## 4. Simulation (No Real Network Data)

### Strategy: Synthetic Alarm Generation

Since you have no real network, we **synthesize realistic alarm patterns** that mimic actual network behavior.

### Synthetic Network Topology

Define a fake but realistic network:

```python
import networkx as nx

def create_synthetic_network():
    """
    Realistic telecom network structure:
    - 2 Core routers (high connectivity)
    - 4 Regional switches (medium connectivity)
    - 8 Edge routers (low connectivity)
    - 16 Hosts (leaf nodes)
    """
    G = nx.DiGraph()
    
    # Core layer (tier 0)
    cores = ["CORE-A", "CORE-B"]
    G.add_nodes_from(cores, layer=0, device_type="core_router")
    G.add_edge("CORE-A", "CORE-B", weight=0.95)  # High connectivity
    G.add_edge("CORE-B", "CORE-A", weight=0.95)
    
    # Regional layer (tier 1)
    regionals = [f"REGION-{i}" for i in range(4)]
    G.add_nodes_from(regionals, layer=1, device_type="switch")
    
    # Connect cores to regionals (fully connected)
    for core in cores:
        for regional in regionals:
            G.add_edge(core, regional, weight=0.85)
    
    # Edge layer (tier 2)
    edges = [f"EDGE-{i}" for i in range(8)]
    G.add_nodes_from(edges, layer=2, device_type="edge_router")
    
    # Regionals to edges (partial mesh)
    for idx, edge in enumerate(edges):
        connected_regionals = regionals[idx % 4:idx % 4 + 2]
        for regional in connected_regionals:
            G.add_edge(regional, edge, weight=0.75)
    
    # Hosts (tier 3)
    hosts = [f"HOST-{i}" for i in range(16)]
    G.add_nodes_from(hosts, layer=3, device_type="host")
    
    # Each host connects to 1-2 edge routers
    for idx, host in enumerate(hosts):
        edge_routers = [edges[idx % len(edges)], edges[(idx + 1) % len(edges)]]
        for edge_router in edge_routers:
            G.add_edge(edge_router, host, weight=0.9)
    
    return G
```

### Synthetic Alarm Generator

```python
import numpy as np
from datetime import datetime, timedelta
import random

class SyntheticAlarmGenerator:
    def __init__(self, network_topology):
        self.G = network_topology
        self.devices = list(self.G.nodes())
        self.alarm_count = 0
        
    def generate_alarm_sequence(self, num_alarms=10000):
        """
        Generate realistic alarm sequences that respect network topology
        """
        alarms = []
        current_time = datetime.now()
        
        for i in range(num_alarms):
            if random.random() < 0.7:
                # 70% of alarms: follow a causal path through network
                alarm = self._generate_causal_alarm(current_time)
            else:
                # 30% of alarms: independent (noise, unrelated issues)
                alarm = self._generate_independent_alarm(current_time)
            
            alarms.append(alarm)
            current_time += timedelta(milliseconds=random.randint(10, 1000))
        
        return alarms
    
    def _generate_causal_alarm(self, start_time):
        """
        Simulate realistic fault propagation:
        Core device failure → affects regionals → affects edges → affects hosts
        
        Example: Link down on CORE-A
        - T+0ms: CORE-A reports link down
        - T+50ms: REGION-1,2,3,4 lose connectivity to CORE-A
        - T+150ms: EDGE-1,2,3 lose connectivity
        - T+300ms: HOST-1-4 lose connectivity
        """
        
        # Pick a root cause device (weighted toward core/regional)
        if random.random() < 0.3:
            root_device = random.choice([d for d in self.devices if "CORE" in d])
        elif random.random() < 0.5:
            root_device = random.choice([d for d in self.devices if "REGION" in d])
        else:
            root_device = random.choice([d for d in self.devices if "EDGE" in d])
        
        # Simulate alarm propagation down the topology
        propagation_path = []
        visited = set()
        queue = [(root_device, 0)]  # (device, depth)
        
        while queue:
            device, depth = queue.pop(0)
            if device in visited:
                continue
            visited.add(device)
            propagation_path.append((device, depth))
            
            # Get downstream devices
            for downstream in self.G.successors(device):
                if downstream not in visited and depth < 3:
                    queue.append((downstream, depth + 1))
        
        # Generate alarm for each device in path with realistic delays
        alarms = []
        for device, depth in propagation_path:
            alarm_delay = 50 * depth + np.random.normal(0, 10)  # 50ms per hop
            alarm_time = start_time + timedelta(milliseconds=alarm_delay)
            
            # Random alarm type for this device
            alarm_type = random.choice([
                "LINK_DOWN",
                "INTERFACE_ERROR",
                "BGPDOWN",
                "CPU_HIGH",
                "MEMORY_HIGH"
            ])
            
            oid = self._get_oid_for_alarm_type(alarm_type)
            
            alarms.append({
                "source_device": device,
                "timestamp": alarm_time,
                "alarm_type": alarm_type,
                "oid": oid,
                "severity": "CRITICAL" if depth < 2 else "WARNING",
                "message": f"{device}: {alarm_type}",
                "is_root_cause": (device == root_device)
            })
        
        # Return primary alarm (can return as batch or individually)
        return alarms[0] if alarms else self._generate_independent_alarm(start_time)
    
    def _generate_independent_alarm(self, time):
        """Random unrelated alarm (noise)"""
        device = random.choice(self.devices)
        alarm_type = random.choice(["CPU_HIGH", "MEMORY_HIGH", "DISK_FULL", "TEMP_HIGH"])
        
        return {
            "source_device": device,
            "timestamp": time,
            "alarm_type": alarm_type,
            "oid": self._get_oid_for_alarm_type(alarm_type),
            "severity": random.choice(["CRITICAL", "WARNING"]),
            "message": f"{device}: {alarm_type}",
            "is_root_cause": False
        }
    
    def _get_oid_for_alarm_type(self, alarm_type):
        """Map alarm types to realistic SNMP OIDs"""
        oid_map = {
            "LINK_DOWN": "1.3.6.1.6.3.1.1.5.3",       # linkDown
            "INTERFACE_ERROR": "1.3.6.1.4.1.8072.4.0.1",
            "BGPDOWN": "1.3.6.1.4.1.3641.2.1.0.1",
            "CPU_HIGH": "1.3.6.1.4.1.2578.2.1.1.0",
            "MEMORY_HIGH": "1.3.6.1.4.1.2578.2.1.2.0",
            "DISK_FULL": "1.3.6.1.4.1.2578.2.1.3.0",
            "TEMP_HIGH": "1.3.6.1.4.1.2578.2.1.4.0"
        }
        return oid_map.get(alarm_type, "1.3.6.1.6.3.1.1.5.4")
```

### Run Synthetic Alarms Through FMS

```python
def simulate_fms_with_synthetic_alarms():
    """
    Generate synthetic alarms and feed them to your existing FMS
    """
    # Create network
    network = create_synthetic_network()
    
    # Create alarm generator
    generator = SyntheticAlarmGenerator(network)
    
    # Generate alarms (simulate 1 week of activity)
    alarms = generator.generate_alarm_sequence(num_alarms=10000)
    
    # Feed to FMS via Kafka (or direct to Ignite)
    kafka_producer = KafkaProducer(
        bootstrap_servers=['localhost:9092'],
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )
    
    for alarm in alarms:
        kafka_producer.send('snmp-traps', value=alarm)
    
    kafka_producer.flush()
    
    # Wait for processing
    time.sleep(60)
    
    # Train GNN on collected alarms
    train_topology_gnn_on_ignite_data()
    
    # Validate learned topology matches reality
    learned_topology = read_topology_from_ignite()
    validate_learned_vs_real(learned_topology, network)
```

### Validation Metrics

```python
def validate_learned_topology(learned_edges, real_edges, network):
    """
    Compare learned topology to ground truth
    """
    
    # 1. Edge Recovery (did we find the real edges?)
    found_edges = set()
    for source, target, confidence in learned_edges:
        if confidence > 0.5:  # Only count confident predictions
            found_edges.add((source, target))
    
    real_edge_set = set(network.edges())
    
    precision = len(found_edges & real_edge_set) / len(found_edges)
    recall = len(found_edges & real_edge_set) / len(real_edge_set)
    f1 = 2 * (precision * recall) / (precision + recall)
    
    print(f"Edge Recovery: Precision={precision:.3f}, Recall={recall:.3f}, F1={f1:.3f}")
    
    # 2. Causality Detection (can we identify root causes?)
    causal_accuracy = evaluate_root_cause_detection(learned_topology, alarms)
    print(f"Causality Accuracy: {causal_accuracy:.3f}")
    
    # 3. Device Clustering (do learned embeddings group similar devices?)
    embedding_quality = evaluate_device_embeddings(learned_embeddings, network)
    print(f"Device Embedding Quality: {embedding_quality:.3f}")
```

## 5. Python ↔ Java/Ignite Integration

### Approach: Microservice Pattern

You can absolutely run Python alongside Java/Ignite. Here's how:

**Architecture:**
```
Java Application (FMS Core)
    ↓ (Kafka)
Python Microservice (GNN Model)
    ↓ (gRPC or REST)
Ignite Cache (Shared Data Store)
```

### Implementation Option 1: REST API

**Python (FastAPI)**
```python
from fastapi import FastAPI
from ignite_client import IgniteClient
import torch
from gnn_model import TopologyGNN

app = FastAPI()
ignite_client = IgniteClient(host="localhost", port=10800)
model = TopologyGNN.load("trained_model.pt")

@app.post("/train/topology")
async def train_topology():
    """Called periodically to retrain the model"""
    
    # 1. Read alarm history from Ignite
    alarms = ignite_client.query("""
        SELECT source_device, target_device, correlation_score, timestamp
        FROM alarms
        WHERE timestamp > ?
        ORDER BY timestamp DESC
    """, [datetime.now() - timedelta(days=7)])
    
    # 2. Build graph
    G = build_graph_from_alarms(alarms)
    
    # 3. Train GNN
    model.train()
    for epoch in range(50):
        loss = train_epoch(model, G)
    
    # 4. Write results back to Ignite
    topology_graph = model.infer_topology(G)
    ignite_client.put_cache("topology", {
        "timestamp": datetime.now(),
        "edges": topology_graph.edges(),
        "node_embeddings": topology_graph.node_embeddings,
        "version": "1.0"
    })
    
    return {"status": "trained", "loss": loss.item()}

@app.get("/topology/suggest-correlation")
async def suggest_correlation(device_a: str, device_b: str):
    """
    Use learned topology to suggest correlation rules
    """
    topology = ignite_client.get_cache("topology")
    
    # Find path between devices
    path = nx.shortest_path(topology["edges"], device_a, device_b)
    confidence = calculate_path_confidence(path, topology["edges"])
    
    return {
        "devices": [device_a, device_b],
        "path": path,
        "confidence": confidence,
        "recommendation": "correlate" if confidence > 0.7 else "monitor"
    }
```

**Java (Call Python Service)**
```java
// In your existing Java code
import org.apache.http.client.methods.HttpPost;
import com.google.gson.JsonObject;

public class TopologyLearner {
    private String pythonServiceUrl = "http://localhost:8000";
    
    public void trainTopologyModel() throws Exception {
        HttpPost post = new HttpPost(pythonServiceUrl + "/train/topology");
        // Execute and handle response
    }
    
    public TopologyGraph getLearnedTopology() throws Exception {
        // GET /topology from Python service
        // Parse JSON response into TopologyGraph object
        // Return to UI
    }
}
```

### Implementation Option 2: gRPC (Better Performance)

**Protobuf Definition**
```protobuf
syntax = "proto3";

package fms.topology;

service TopologyService {
  rpc TrainTopology(TrainRequest) returns (TrainResponse);
  rpc InferTopology(InferRequest) returns (TopologyGraph);
  rpc GetDeviceEmbedding(GetEmbeddingRequest) returns (Embedding);
}

message TrainRequest {
  int64 alarm_history_hours = 1;
  int32 num_epochs = 2;
}

message TrainResponse {
  bool success = 1;
  string message = 2;
  float final_loss = 3;
}

message TopologyGraph {
  repeated Device nodes = 1;
  repeated Edge edges = 2;
  int64 timestamp = 3;
}

message Device {
  string device_id = 1;
  string device_type = 2;
  repeated float embedding = 3;
}

message Edge {
  string source = 1;
  string target = 2;
  float confidence = 3;
  float causality_weight = 4;
}
```

**Python gRPC Server**
```python
import grpc
from concurrent import futures
import topology_pb2
import topology_pb2_grpc

class TopologyServicer(topology_pb2_grpc.TopologyServiceServicer):
    def __init__(self, model, ignite_client):
        self.model = model
        self.ignite = ignite_client
    
    def TrainTopology(self, request, context):
        try:
            alarms = self.ignite.get_recent_alarms(request.alarm_history_hours)
            G = build_graph(alarms)
            
            loss = 0
            for epoch in range(request.num_epochs):
                loss = self._train_epoch(G)
            
            return topology_pb2.TrainResponse(
                success=True,
                message="Training complete",
                final_loss=loss
            )
        except Exception as e:
            return topology_pb2.TrainResponse(
                success=False,
                message=str(e)
            )
    
    def InferTopology(self, request, context):
        topology = self.model.infer()
        
        nodes = [
            topology_pb2.Device(
                device_id=node,
                device_type=self._get_device_type(node),
                embedding=self.model.get_embedding(node).tolist()
            )
            for node in topology.nodes()
        ]
        
        edges = [
            topology_pb2.Edge(
                source=u,
                target=v,
                confidence=data['confidence'],
                causality_weight=data.get('causality', 0.0)
            )
            for u, v, data in topology.edges(data=True)
        ]
        
        return topology_pb2.TopologyGraph(
            nodes=nodes,
            edges=edges,
            timestamp=int(time.time() * 1000)
        )

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    servicer = TopologyServicer(model, ignite_client)
    topology_pb2_grpc.add_TopologyServiceServicer_to_server(servicer, server)
    
    server.add_insecure_port('[::]:50052')  # Different port from FMS gRPC
    server.start()
    server.wait_for_termination()

if __name__ == '__main__':
    serve()
```

**Java gRPC Client**
```java
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import fms.topology.TopologyServiceGrpc;
import fms.topology.TopologyServiceGrpc.TopologyServiceBlockingStub;

public class TopologyClient {
    private final TopologyServiceBlockingStub blockingStub;
    
    public TopologyClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
        blockingStub = TopologyServiceGrpc.newBlockingStub(channel);
    }
    
    public void trainTopology() {
        TrainRequest request = TrainRequest.newBuilder()
            .setAlarmHistoryHours(168)  // Last week
            .setNumEpochs(50)
            .build();
        
        TrainResponse response = blockingStub.trainTopology(request);
        System.out.println("Training result: " + response.getMessage());
    }
    
    public TopologyGraph getTopology() {
        return blockingStub.inferTopology(InferRequest.newBuilder().build());
    }
}
```

## 6. Practical Timeline & Phases

### Phase 1: Foundation (Weeks 1-2)
- [ ] Create synthetic network simulator
- [ ] Build alarm generator
- [ ] Set up Python microservice skeleton
- [ ] Create Ignite ↔ Python bridge (REST or gRPC)

### Phase 2: GNN Implementation (Weeks 2-4)
- [ ] Implement graph feature extraction
- [ ] Build GAT model
- [ ] Implement training loop with temporal splitting
- [ ] Validate on synthetic data (achieve >0.85 F1 score)

### Phase 3: Integration (Week 4-5)
- [ ] Connect Python service to Ignite
- [ ] Write topology results to Ignite cache
- [ ] Add topology queries to Java backend
- [ ] Create REST endpoint for UI

### Phase 4: Visualization (Week 5-6)
- [ ] Enhance React UI with D3/Cytoscape for graph rendering
- [ ] Add topology confidence scores to visualization
- [ ] Add device embeddings visualization (t-SNE/UMAP)
- [ ] Create correlation rule suggestions based on topology

### Phase 5: Real Deployment (Week 6+)
- [ ] Start collecting real network data
- [ ] Retrain with hybrid synthetic + real data
- [ ] Continuous learning loop (retrain weekly/daily)
- [ ] Monitor model drift

## 7. Key Advantages of This Approach

1. **Python-Java Coexistence**: Python runs as separate microservice, Java remains core
2. **Zero Breaking Changes**: Existing Ignite/Kafka setup continues unchanged
3. **Testable**: Synthetic alarms let you validate logic before real network
4. **Iterative**: Can improve model without touching Java code
5. **Scalable**: Python service can handle inference separately
6. **Explainable**: GAT attention weights show why topology was learned

## 8. Expected Results

After training on 7+ days of alarm data (synthetic or real):

- **Topology Accuracy**: 85-95% edge recovery (precision/recall)
- **Latency**: 10-50ms inference per alarm batch
- **Improved Correlations**: 20-40% reduction in false positive correlations
- **RCA Enhancement**: Automatically suggest which alarms are root causes (75-90% accuracy)
