import React, { useState, useEffect, useRef } from 'react';
import ForceGraph2D from 'react-force-graph-2d';
import { TopologyServiceClient } from './generated/FMS_grpc_web_pb';
import { 
  GetTopologyRequest,
  TrainTopologyRequest 
} from './generated/FMS_pb';
import './TopologyView.css';

const TopologyView = () => {
  const [graphData, setGraphData] = useState({ nodes: [], links: [] });
  const [loading, setLoading] = useState(false);
  const [training, setTraining] = useState(false);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({ nodes: 0, edges: 0, timestamp: '' });
  const [minConfidence, setMinConfidence] = useState(0.6);
  const graphRef = useRef();

  const client = new TopologyServiceClient('http://localhost:8080', null, null);

  const trainTopology = () => {
    setTraining(true);
    setError(null);

    const request = new TrainTopologyRequest();
    request.setAlarmHistoryHours(24);
    request.setNumEpochs(50);

    client.trainTopology(request, {}, (err, response) => {
      setTraining(false);
      
      if (err) {
        setError(`Training failed: ${err.message}`);
        console.error(err);
        return;
      }

      if (response.getSuccess()) {
        console.log('Training completed:', response.getMessage());
        fetchTopology();
      } else {
        setError(response.getMessage());
      }
    });
  };

  const fetchTopology = () => {
    setLoading(true);
    setError(null);

    const request = new GetTopologyRequest();
    request.setMinConfidence(minConfidence);

    client.getTopology(request, {}, (err, response) => {
      setLoading(false);
      
      if (err) {
        setError(`Failed to fetch topology: ${err.message}`);
        console.error(err);
        return;
      }

      const nodes = response.getNodesList().map(node => ({
        id: node.getDeviceId(),
        name: node.getDeviceId(),
        type: node.getDeviceType(),
        val: 8
      }));

      const links = response.getEdgesList().map(edge => ({
        source: edge.getSourceDevice(),
        target: edge.getDestinationDevice(),
        confidence: edge.getConfidence(),
        causality: edge.getCausalityWeight(),
        count: edge.getCoOccurrenceCount(),
        value: edge.getConfidence() * 3
      }));

      setGraphData({ nodes, links });
      setStats({
        nodes: response.getTotalNodes(),
        edges: response.getTotalEdges(),
        timestamp: response.getTopologyTimestamp()
      });
    });
  };

  useEffect(() => {
    fetchTopology();
  }, [minConfidence]);

  const getNodeColor = (node) => {
    const typeColors = {
      'core_router': '#ff4444',
      'switch': '#4444ff',
      'router': '#44ff44',
      'host': '#ffaa44',
      'unknown': '#888888'
    };
    return typeColors[node.type] || typeColors.unknown;
  };

  const getLinkColor = (link) => {
    const conf = link.confidence;
    if (conf > 0.8) return '#00ff00';
    if (conf > 0.6) return '#ffff00';
    return '#ff6600';
  };

  return (
    <div className="topology-container">
      <div className="topology-header">
        <h2>🗺️ Network Topology (GNN Learned)</h2>
        
        <div className="topology-controls">
          <button 
            onClick={trainTopology} 
            disabled={training}
            className="btn-train"
          >
            {training ? '⏳ Training...' : '🎓 Train Model'}
          </button>
          
          <button 
            onClick={fetchTopology} 
            disabled={loading}
            className="btn-refresh"
          >
            {loading ? '⏳ Loading...' : '🔄 Refresh'}
          </button>

          <div className="confidence-slider">
            <label>Min Confidence: {minConfidence.toFixed(2)}</label>
            <input
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={minConfidence}
              onChange={(e) => setMinConfidence(parseFloat(e.target.value))}
            />
          </div>
        </div>

        {error && <div className="error-message">❌ {error}</div>}
      </div>

      <div className="topology-stats">
        <div className="stat-box">
          <div className="stat-value">{stats.nodes}</div>
          <div className="stat-label">Nodes</div>
        </div>
        <div className="stat-box">
          <div className="stat-value">{stats.edges}</div>
          <div className="stat-label">Edges</div>
        </div>
        <div className="stat-box">
          <div className="stat-value">{minConfidence.toFixed(2)}</div>
          <div className="stat-label">Min Confidence</div>
        </div>
      </div>

      <div className="graph-container">
        {graphData.nodes.length > 0 ? (
          <ForceGraph2D
            ref={graphRef}
            graphData={graphData}
            nodeLabel={node => `${node.name} (${node.type})`}
            nodeColor={getNodeColor}
            nodeRelSize={3}
            nodeCanvasObjectMode={() => "after"}
            nodeCanvasObject={(node, ctx, globalScale) => {
              const label = node.name;
              const fontSize = 12/globalScale;
              ctx.font = `${fontSize}px Sans-Serif`;
              ctx.textAlign = "center";
              ctx.textBaseline = "top";
              ctx.fillStyle = "white";
              ctx.fillText(label, node.x, node.y + 5);
            }}
            linkColor={getLinkColor}
            linkWidth={link => link.value}
            linkDirectionalParticles={2}
            linkDirectionalParticleWidth={4}
            linkLabel={link => 
              `Confidence: ${link.confidence.toFixed(3)}\n` +
              `Causality: ${link.causality.toFixed(3)}\n` +
              `Co-occurrence: ${link.count}`
            }
            onNodeClick={(node) => {
              console.log('Clicked node:', node);
            }}
            width={1200}
            height={600}
          />
        ) : (
          <div className="no-data">
            {loading ? '⏳ Loading topology...' : '📊 No topology data. Train the model first!'}
          </div>
        )}
      </div>

      <div className="topology-legend">
        <h4>Legend</h4>
        <div className="legend-item">
          <span className="legend-color" style={{backgroundColor: '#ff4444'}}></span>
          Core Router
        </div>
        <div className="legend-item">
          <span className="legend-color" style={{backgroundColor: '#4444ff'}}></span>
          Switch
        </div>
        <div className="legend-item">
          <span className="legend-color" style={{backgroundColor: '#44ff44'}}></span>
          Router
        </div>
        <div className="legend-item">
          <span className="legend-color" style={{backgroundColor: '#ffaa44'}}></span>
          Host
        </div>
        <div className="legend-separator"></div>
        <div className="legend-item">
          <span className="legend-line" style={{backgroundColor: '#00ff00'}}></span>
          High Confidence (&gt;0.8)
        </div>
        <div className="legend-item">
          <span className="legend-line" style={{backgroundColor: '#ffff00'}}></span>
          Medium (0.6-0.8)
        </div>
        <div className="legend-item">
          <span className="legend-line" style={{backgroundColor: '#ff6600'}}></span>
          Low (&lt;0.6)
        </div>
      </div>
    </div>
  );
};

export default TopologyView;
