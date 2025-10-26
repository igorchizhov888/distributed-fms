import React, { useState, useEffect } from 'react';
import { subscribeToAlarms } from './AlarmClient';
import './App.css';

function App() {
  console.log('App component rendering');
  const [alarms, setAlarms] = useState([]);

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

  return (
    <div className="App">
      <header className="App-header">
        <h1>FMS Alarms</h1>
      </header>
      <div className="App-content">
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
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
              </tr>
            </thead>
            <tbody>
              {alarms.map((alarm) => (
                <tr key={alarm.alarmId}>
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
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default App;
