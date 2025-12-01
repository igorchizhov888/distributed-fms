import React, { useState, useEffect } from 'react';
import { subscribeToAlarms } from './AlarmClient';
import TopologyView from './TopologyView';
import './App.css';

function App() {
  console.log('App component rendering');
  const [alarms, setAlarms] = useState([]);
  const [expandedGroups, setExpandedGroups] = useState(new Set());
  const [activeTab, setActiveTab] = useState('alarms');

  // Helper function - Convert severity to number
  const getSeverityLevel = (severity) => {
    const map = {
      'INFO': '1',
      'WARNING': '2',
      'CRITICAL': '3'
    };
    return map[severity] || severity;
  };

  // Helper function - Extract message from OID data
  const getDescriptionText = (description) => {
    try {
      const data = JSON.parse(description);
      if (Array.isArray(data) && data.length > 0) {
        return data[data.length - 1].value || description;
      }
    } catch (e) {
      // If not JSON, return as is
    }
    return description;
  };

  // Group alarms by correlation
  const groupAlarms = () => {
    const grouped = {};
    const standalone = [];

    alarms.forEach(alarm => {
      if (alarm.correlationId && alarm.rootCauseAlarmId) {
        if (!grouped[alarm.correlationId]) {
          grouped[alarm.correlationId] = {
            parent: null,
            children: []
          };
        }
        
        // Check if this alarm is the root cause
        if (alarm.alarmId === alarm.rootCauseAlarmId) {
          grouped[alarm.correlationId].parent = alarm;
        } else {
          grouped[alarm.correlationId].children.push(alarm);
        }
      } else {
        standalone.push(alarm);
      }
    });

    return { grouped, standalone };
  };

  const toggleGroup = (correlationId) => {
    setExpandedGroups(prev => {
      const newSet = new Set(prev);
      if (newSet.has(correlationId)) {
        newSet.delete(correlationId);
      } else {
        newSet.add(correlationId);
      }
      return newSet;
    });
  };

  useEffect(() => {
    const unsubscribe = subscribeToAlarms((newAlarm) => {
      console.log('Received alarm:', newAlarm);
      setAlarms((prevAlarms) => {
        const exists = prevAlarms.some((a) => a.alarmId === newAlarm.alarmId);
        if (exists) {
          return prevAlarms.map((a) =>
            a.alarmId === newAlarm.alarmId ? newAlarm : a
          );
        }
        return [...prevAlarms, newAlarm];
      });
    });

    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, []);

  const { grouped, standalone } = groupAlarms();

  return (
    <div className="App">
      <header className="App-header">
        <h1>🚨 Distributed FMS</h1>
        <div className="tab-navigation">
          <button 
            className={activeTab === 'alarms' ? 'tab-active' : 'tab-inactive'}
            onClick={() => setActiveTab('alarms')}
          >
            📋 Alarms
          </button>
          <button 
            className={activeTab === 'topology' ? 'tab-active' : 'tab-inactive'}
            onClick={() => setActiveTab('topology')}
          >
            🗺️ Network Topology
          </button>
        </div>
      </header>

      {activeTab === 'topology' ? (
        <TopologyView />
      ) : (
        <div className="App-content">
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Expand</th>
                  <th>Alarm ID</th>
                  <th>Device ID</th>
                  <th>Node Alias</th>
                  <th>Severity</th>
                  <th>Alarm Group</th>
                  <th>Probable Cause</th>
                  <th>Summary</th>
                  <th>Description</th>
                  <th>Status</th>
                  <th>Event Type</th>
                  <th>Geographic Region</th>
                  <th>Tally Count</th>
                  <th>First Occurrence</th>
                  <th>Last Occurrence</th>
                  <th>IID</th>
                  <th>Correlation ID</th>
                  <th>Root Cause ID</th>
                </tr>
              </thead>
              <tbody>
                {/* Render grouped alarms (parent + children) */}
                {Object.entries(grouped).map(([correlationId, group]) => {
                  const isExpanded = expandedGroups.has(correlationId);
                  const childCount = group.children.length;
                  const parent = group.parent;

                  if (!parent) return null;

                  return (
                    <React.Fragment key={correlationId}>
                      <tr className="parent-alarm">
                        <td>
                          {childCount > 0 && (
                            <button 
                              onClick={() => toggleGroup(correlationId)}
                              className="expand-btn"
                            >
                              {isExpanded ? '▼' : '▶'} ({childCount})
                            </button>
                          )}
                        </td>
                        <td><strong>{parent.alarmId}</strong></td>
                        <td>{parent.deviceId}</td>
                        <td>{parent.nodeAlias || '-'}</td>
                        <td>{getSeverityLevel(parent.severity)}</td>
                        <td>{parent.alarmGroup || '-'}</td>
                        <td>{parent.probableCause || '-'}</td>
                        <td>{parent.summary || '-'}</td>
                        <td><strong>{getDescriptionText(parent.description)}</strong></td>
                        <td>{parent.status}</td>
                        <td>{parent.eventType}</td>
                        <td>{parent.geographicRegion}</td>
                        <td>{parent.tallyCount}</td>
                        <td>{new Date(parent.firstOccurrence).toLocaleString()}</td>
                        <td>{new Date(parent.lastOccurrence).toLocaleString()}</td>
                        <td>{parent.iid || '-'}</td>
                        <td>{parent.correlationId || '-'}</td>
                        <td>{parent.rootCauseAlarmId || '-'}</td>
                      </tr>
                      
                      {isExpanded && group.children.map((alarm) => (
                        <tr key={alarm.alarmId} className="child-alarm">
                          <td></td>
                          <td style={{paddingLeft: '30px'}}>{alarm.alarmId}</td>
                          <td>{alarm.deviceId}</td>
                          <td>{alarm.nodeAlias || '-'}</td>
                          <td>{getSeverityLevel(alarm.severity)}</td>
                          <td>{alarm.alarmGroup || '-'}</td>
                          <td>{alarm.probableCause || '-'}</td>
                          <td>{alarm.summary || '-'}</td>
                          <td>{getDescriptionText(alarm.description)}</td>
                          <td>{alarm.status}</td>
                          <td>{alarm.eventType}</td>
                          <td>{alarm.geographicRegion}</td>
                          <td>{alarm.tallyCount}</td>
                          <td>{new Date(alarm.firstOccurrence).toLocaleString()}</td>
                          <td>{new Date(alarm.lastOccurrence).toLocaleString()}</td>
                          <td>{alarm.iid || '-'}</td>
                          <td>{alarm.correlationId || '-'}</td>
                          <td>{alarm.rootCauseAlarmId || '-'}</td>
                        </tr>
                      ))}
                    </React.Fragment>
                  );
                })}

                {standalone.map((alarm) => (
                  <tr key={alarm.alarmId} className="standalone-alarm">
                    <td></td>
                    <td>{alarm.alarmId}</td>
                    <td>{alarm.deviceId}</td>
                    <td>{alarm.nodeAlias || '-'}</td>
                    <td>{getSeverityLevel(alarm.severity)}</td>
                    <td>{alarm.alarmGroup || '-'}</td>
                    <td>{alarm.probableCause || '-'}</td>
                    <td>{alarm.summary || '-'}</td>
                    <td>{getDescriptionText(alarm.description)}</td>
                    <td>{alarm.status}</td>
                    <td>{alarm.eventType}</td>
                    <td>{alarm.geographicRegion}</td>
                    <td>{alarm.tallyCount}</td>
                    <td>{new Date(alarm.firstOccurrence).toLocaleString()}</td>
                    <td>{new Date(alarm.lastOccurrence).toLocaleString()}</td>
                    <td>{alarm.iid || '-'}</td>
                    <td>{alarm.correlationId || '-'}</td>
                    <td>{alarm.rootCauseAlarmId || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
