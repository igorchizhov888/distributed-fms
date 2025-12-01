# Distributed Fault Management System with AI/ML Topology Learning

A geo-distributed, high-performance fault management system for telecommunications networks, featuring **Graph Neural Network (GNN) based network topology learning**. Built on Apache Ignite, Apache Kafka, PyTorch Geometric, and React.

## 🎯 Problem Statement
Traditional centralized fault management systems cannot handle modern network event volumes and geographic distribution requirements. Moreover, they lack intelligent topology discovery and relationship learning capabilities. This project implements distributed, edge-based fault management with **AI-powered network topology learning** that automatically discovers network relationships from alarm patterns.

## ✨ Key Features

### Core Fault Management
- **Event-Driven Architecture**: Ingests events via Apache Kafka message bus
- **Geographic Distribution**: Process events at network edge locations using custom affinity functions
- **Real-time Processing**: Handle thousands of events per second with sub-millisecond response times
- **Advanced Alarm Correlation**: Automatically groups related alarms into parent/child relationships with root cause identification
- **Distributed Caching**: Events stored in distributed Apache Ignite cache
- **Active-Active Clustering**: Automatic failover with zero data loss
- **Universal Network Support**: Monitor any network type with software adapters

### 🧠 AI/ML Topology Learning (NEW in v0.2.0)
- **Graph Neural Networks**: Uses Graph Attention Networks (GAT) to learn network topology from alarm co-occurrence patterns
- **Automatic Topology Discovery**: Learns device relationships without manual configuration
- **Confidence Scoring**: Each learned edge has a confidence score (0.0-1.0) indicating reliability
- **Multi-Feature Learning**: Combines temporal, spatial, and causal alarm relationships
- **Real-time Training**: Train the model on historical alarm data (configurable window)
- **Interactive Visualization**: Force-directed graph with 30+ nodes, 870+ edges

### 🎨 Modern Web UI
- **Dual-Tab Interface**: 
  - **Alarms Tab**: Real-time alarm monitoring with parent/child grouping
  - **Network Topology Tab**: Interactive graph visualization of learned network
- **Force-Directed Graph**: Intuitive network layout with color-coded device types
- **Confidence Filtering**: Slider to adjust edge visibility based on confidence
- **Real-time Updates**: gRPC-Web streaming for live data
- **Responsive Design**: Professional UI with dark theme

### 🏗️ Multi-Language Architecture
- **Java Backend**: FMS server with Ignite and Kafka integration
- **Python ML Service**: PyTorch Geometric GNN for topology learning
- **React Frontend**: Modern web UI with interactive visualizations
- **gRPC Integration**: Seamless Java ↔ Python ↔ React communication

## 🏛️ Architecture
```
                    ┌─────────────────────────────────────┐
                    │     React Web UI (Port 3000)       │
                    │  ┌──────────┐  ┌─────────────────┐ │
                    │  │  Alarms  │  │Network Topology │ │
                    │  │   Tab    │  │   Tab (GNN)     │ │
                    │  └──────────┘  └─────────────────┘ │
                    └──────────────┬──────────────────────┘
                                   │ gRPC-Web
                    ┌──────────────▼──────────────────────┐
                    │    Envoy Proxy (Port 8080)          │
                    │  - gRPC-Web Translation             │
                    │  - Smart Routing                    │
                    └──────┬──────────────────┬───────────┘
                           │                  │
            ┌──────────────▼─────┐  ┌────────▼──────────────┐
            │   FMS Server        │  │  Topology Service     │
            │   (Java/gRPC)       │  │  (Python/gRPC)        │
            │   Port 50051        │  │  Port 50052           │
            │                     │  │                       │
            │  ┌───────────────┐  │  │  ┌─────────────────┐ │
            │  │ Alarm         │  │  │  │ Graph Attention │ │
            │  │ Correlation   │  │  │  │ Networks (GAT)  │ │
            │  │ Engine        │  │  │  │ PyTorch Geo     │ │
            │  └───────────────┘  │  │  └─────────────────┘ │
            │  ┌───────────────┐  │  │                       │
            │  │ Apache        │  │  │  Learns topology from │
            │  │ Ignite Cache  │  │  │  alarm patterns       │
            │  └───────────────┘  │  │                       │
            └──────────▲───────────┘  └───────────────────────┘
                       │
            ┌──────────┴───────────┐
            │   Apache Kafka       │
            │   (Message Bus)      │
            └──────────▲───────────┘
                       │
            ┌──────────┴───────────┐
            │  SNMP Trap Receiver  │
            │  gNMI Simulator      │
            │  (Port 10162)        │
            └──────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Python 3.8+ with pip
- Docker and Docker Compose
- Node.js 18+

### Option 1: Automated Startup (Recommended)

1. **Start all Docker services:**
```bash
docker compose up -d
```

2. **Start Python topology service** (in separate terminal):
```bash
cd topology-service
python3 -m venv topology_env
source topology_env/bin/activate  # On Windows: topology_env\Scripts\activate
pip install -r requirements.txt
python topology_grpc_server.py
```

3. **Start React UI** (in another terminal):
```bash
cd ui/fms-ui
npm install
npm start
```

4. **Generate alarms:**
```bash
docker compose restart gnmi-simulator
```

5. **Open browser:** http://localhost:3000
   - View alarms in "Alarms" tab
   - Click "Network Topology" tab → "Train Model" button
   - Adjust confidence slider to see different edge densities

### Option 2: Step-by-Step Manual Setup

See [Startup/Shutdown Guide](#startuprestart-procedures) below for detailed commands.

## 🎮 Using the System

### Viewing Alarms
1. Open http://localhost:3000
2. Click **Alarms** tab
3. See real-time alarm updates (1 parent + 4 children typically)
4. Click ▶ to expand parent alarms and see child alarms

### Training Topology Model
1. Click **Network Topology** tab
2. Click **🎓 Train Model** button
3. Wait 5-10 seconds for training to complete
4. Graph automatically displays with learned topology

### Exploring Topology
- **Adjust Confidence Slider**: Filter edges by confidence (0.0 - 1.0)
  - Lower threshold (0.30): Shows 870 edges (dense network)
  - Higher threshold (0.55): Shows 0-50 edges (high-confidence only)
- **Zoom**: Mouse wheel
- **Pan**: Click and drag
- **Node Details**: Hover over nodes to see device type
- **Link Details**: Hover over edges to see confidence, causality, co-occurrence

### Device Type Color Coding
- 🔴 **Red**: Core Routers
- 🔵 **Blue**: Switches
- 🟢 **Green**: Routers
- 🟠 **Orange**: Hosts
- ⚫ **Gray**: Unknown devices

## 📊 System Capabilities

### Alarm Management
- ✅ Real-time ingestion (SNMP traps, gNMI)
- ✅ Distributed storage (Apache Ignite)
- ✅ Advanced correlation (dedup, parent-child, clear events, RCA)
- ✅ 15+ tracked alarm fields
- ✅ Hierarchical UI display with expand/collapse

### Topology Learning
- ✅ GNN training on 24-hour alarm history (configurable)
- ✅ 30 nodes, 870 edges learned from synthetic data
- ✅ Confidence scores (0.30 - 0.54 range observed)
- ✅ Device type classification
- ✅ Causal relationship detection
- ✅ Graph Attention Networks (2 layers, 8 heads)

### Performance Metrics
- **Alarm Processing**: <1 second latency
- **Topology Training**: 5-10 seconds (50 epochs, 30 nodes)
- **Graph Rendering**: <1 second (870 edges)
- **UI Responsiveness**: Real-time slider updates

## 🛠️ Technology Stack

### Backend
- **Java 17** - FMS server
- **Spring Boot 2.x** - Application framework
- **Apache Ignite 2.17** - Distributed cache
- **Apache Kafka 3.7** - Message broker
- **gRPC 1.64** - RPC framework

### Machine Learning
- **Python 3.8+** - ML service runtime
- **PyTorch 2.1** - Deep learning framework
- **PyTorch Geometric 2.4** - Graph neural networks
- **Graph Attention Networks (GAT)** - Topology learning model

### Frontend
- **React 18** - UI framework
- **react-force-graph-2d 1.25** - Graph visualization
- **grpc-web 1.4** - Browser gRPC client

### Infrastructure
- **Docker & Docker Compose** - Containerization
- **Envoy 1.28** - gRPC-Web proxy
- **Kafka + Zookeeper** - Message bus

## 📁 Project Structure
```
distributed-fms/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── grpc/              # gRPC service implementations
│   │   │   ├── core/              # Alarm processing, correlation
│   │   │   └── topology/          # Topology integration
│   │   ├── proto/
│   │   │   └── FMS.proto          # gRPC service definitions
│   │   └── resources/
│   └── test/
├── topology-service/              # 🆕 Python GNN service
│   ├── topology_grpc_server.py    # gRPC server
│   ├── topology_learner.py        # GNN model (GAT)
│   ├── FMS_pb2_grpc.py           # Generated gRPC stubs
│   ├── requirements.txt           # Python dependencies
│   └── TOPOLOGY_QUICKSTART.md     # Quick start guide
├── ui/fms-ui/                     # React frontend
│   ├── src/
│   │   ├── App.js                 # Main app with tabs
│   │   ├── TopologyView.js        # 🆕 Topology visualization
│   │   ├── TopologyView.css       # 🆕 Topology styling
│   │   ├── AlarmClient.js         # gRPC-Web client
│   │   └── generated/             # Generated gRPC-Web stubs
│   └── package.json
├── docker-compose.yml             # Multi-container orchestration
├── envoy.yaml                     # Envoy proxy config
├── .env                           # Environment variables
├── CHANGELOG.md                   # Version history
└── README.md                      # This file
```

## 🔄 Startup/Restart Procedures

### Complete Startup Sequence
```bash
# 1. Start Docker services
cd ~/path/to/distributed-fms
docker compose up -d
sleep 30  # Wait for Kafka to be ready

# 2. Start Python topology service (Terminal 1)
cd topology-service
source topology_env/bin/activate
python topology_grpc_server.py

# 3. Start React UI (Terminal 2)
cd ui/fms-ui
docker compose stop fms-ui  # Stop Docker UI if running
npm start

# 4. Generate alarms
docker compose restart gnmi-simulator

# 5. Open browser: http://localhost:3000
```

### Complete Shutdown
```bash
# Terminal 1 & 2: Press Ctrl+C to stop Python/React

# Stop Docker services
docker compose down

# Optional: Remove volumes (wipes all data)
# docker compose down -v
```

### Quick Restart (After Code Changes)
```bash
# Rebuild FMS server
export APP_VERSION=0.1.0-SNAPSHOT
mvn clean package -DskipTests
docker compose up -d --build fms-server

# Restart alarm generation
docker compose restart gnmi-simulator
```

## 📖 Alarm Fields

| Field | Description |
|-------|-------------|
| Alarm ID | Unique identifier |
| Device ID | Source device |
| Node Alias | Network node identifier |
| Severity | 1=INFO, 2=WARNING, 3=CRITICAL |
| Alarm Group | Category classification |
| Probable Cause | Root cause analysis |
| Summary | Brief summary |
| Description | Detailed description |
| Status | ACTIVE, CLEARED, etc. |
| Event Type | Network event type |
| Geographic Region | Device location |
| Tally Count | Duplicate occurrences |
| Correlation ID | Group identifier |
| Root Cause ID | Parent alarm ID |
| First Occurrence | Initial timestamp |
| Last Occurrence | Latest timestamp |
| IID | Ignite cache key |

## 🐛 Troubleshooting

### No alarms appearing?
```bash
# Check if alarms were sent
docker compose logs gnmi-simulator | grep "Published"

# Check FMS server processing
docker compose logs fms-server | tail -50

# Verify Kafka is running
docker compose ps kafka
```

### Topology training fails?
```bash
# Check Python service logs
# (in Python service terminal, look for errors)

# Verify Envoy routing
curl http://localhost:9901/config_dump | grep TopologyService

# Check if services can communicate
docker compose logs envoy | grep topology
```

### UI not loading?
```bash
# Check if React dev server is running
ps aux | grep "react-scripts"

# Check browser console (F12) for errors

# Verify ports are available
lsof -i:3000  # React UI
lsof -i:8080  # Envoy proxy
```

## 📚 Additional Documentation

- [CHANGELOG.md](CHANGELOG.md) - Version history and release notes
- [topology-service/TOPOLOGY_QUICKSTART.md](topology-service/TOPOLOGY_QUICKSTART.md) - Topology service quick start
- [CLEAR_CORRELATION_SUMMARY.md](CLEAR_CORRELATION_SUMMARY.md) - Clear event handling
- [docs/DEMO.md](docs/DEMO.md) - Demo branch guide

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is open source and available under the Apache License 2.0. See [LICENSE](LICENSE) for details.

## 🏆 Project Achievements

**v0.2.0-topology** (Latest)
- 🧠 Graph Neural Network topology learning
- 📊 Interactive force-directed graph visualization
- 🔗 Java-Python-React multi-language integration
- 📈 16,000+ lines of code added
- ⚡ Production-ready performance (<10s training, <1s rendering)

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

---

**Built with ❤️ for telecommunications network operations**
