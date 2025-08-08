// server.js

// Use ES Module import syntax
import { WebSocketServer, WebSocket } from 'ws';
import express from 'express';
import http from 'http';

// Define the port number
const PORT = 3000;

// Create an Express app
const app = express();
// Middleware to parse JSON request bodies
app.use(express.json());

// Create an HTTP server from the Express app
const server = http.createServer(app);

// Attach the WebSocket server to the HTTP server
// Note the change from WebSocket.Server to WebSocketServer
const wss = new WebSocketServer({ server });

// This function will run whenever a new client connects
wss.on('connection', ws => {
    console.log('✅ Client connected.');

    ws.on('close', () => {
        console.log('❌ Client disconnected.');
    });
});

/**
 * Broadcasts a message to all connected WebSocket clients.
 * @param {string} message The message to send.
 */
function broadcast(message) {
    if (!message) {
        console.log("No message to broadcast.");
        return;
    }

    console.log(`Broadcasting: "${message}"`);
    wss.clients.forEach(client => {
        // Note the change to WebSocket.OPEN
        if (client.readyState === WebSocket.OPEN) {
            client.send(message);
        }
    });
}

// --- HTTP ENDPOINT ---
app.post('/notify', (req, res) => {
    const { message } = req.body;

    if (!message) {
        return res.status(400).json({ error: 'Message is required' });
    }

    broadcast(message);
    res.status(200).json({ status: 'Notification sent successfully', message });
});

// Start the HTTP server
server.listen(PORT, () => {
    console.log('🚀 HTTP and WebSocket server is running.');
    console.log(`Listening on http://localhost:${PORT}`);
});