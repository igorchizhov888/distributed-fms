import React, { useState, useEffect } from 'react';
import { subscribeToAlarms } from './AlarmClient';
import './App.css';

function App() {
  console.log('App component rendering');
  const [alarms, setAlarms] = useState([]);

  useEffect(() => {
    const unsubscribe = subscribeToAlarms((newAlarm) => {
      setAlarms((prevAlarms) => [...prevAlarms, newAlarm]);
    });
  //
    return () => {
      unsubscribe();
    };
  }, []);

  console.log('App component render');

  return (
    <div className="App">
      <header className="App-header">
        <h1>FMS Alarms</h1>
      </header>
      {/* <div className="App-content">
        <table>
          <thead>
            <tr>
              <th>Alarm ID</th>
              <th>Device ID</th>
              <th>Severity</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            {alarms.map((alarm) => (
              <tr key={alarm.alarmId}>
                <td>{alarm.alarmId}</td>
                <td>{alarm.deviceId}</td>
                <td>{alarm.severity}</td>
                <td>{alarm.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div> */}
    </div>
  );
}

export default App;
