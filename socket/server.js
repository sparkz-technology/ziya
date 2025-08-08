// server.js - Enhanced WebSocket Server for Ziya Notification Service

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
const wss = new WebSocketServer({ server });

// Store client connections with metadata
const clients = new Map();
let clientIdCounter = 0;

// This function will run whenever a new client connects
wss.on('connection', (ws, request) => {
    const clientId = ++clientIdCounter;
    const clientInfo = {
        id: clientId,
        ws: ws,
        connectedAt: new Date(),
        ip: request.socket.remoteAddress
    };
    
    clients.set(clientId, clientInfo);
    
    console.log(`✅ Client ${clientId} connected from ${clientInfo.ip}. Total clients: ${clients.size}`);
    
    // Send welcome message to new client
    ws.send(`Welcome! You are client #${clientId}`);

    // Handle client disconnection
    ws.on('close', () => {
        clients.delete(clientId);
        console.log(`❌ Client ${clientId} disconnected. Total clients: ${clients.size}`);
    });

    // Handle errors
    ws.on('error', (error) => {
        console.log(`⚠️ Client ${clientId} error:`, error.message);
        clients.delete(clientId);
    });

    // Handle messages from client (optional)
    ws.on('message', (data) => {
        try {
            const message = data.toString();
            console.log(`📨 Message from client ${clientId}: ${message}`);
            
            // Echo back to sender
            ws.send(`Echo: ${message}`);
        } catch (error) {
            console.log(`❌ Error processing message from client ${clientId}:`, error.message);
        }
    });
});

/**
 * Broadcasts a message to all connected WebSocket clients.
 * @param {string} message The message to send.
 * @param {number} excludeClientId Optional client ID to exclude from broadcast
 */
function broadcast(message, excludeClientId = null) {
    if (!message) {
        console.log("❌ No message to broadcast.");
        return;
    }

    console.log(`📢 Broadcasting to ${clients.size} clients: "${message}"`);
    let successCount = 0;
    let failureCount = 0;

    clients.forEach((clientInfo, clientId) => {
        if (excludeClientId && clientId === excludeClientId) {
            return; // Skip excluded client
        }

        if (clientInfo.ws.readyState === WebSocket.OPEN) {
            try {
                clientInfo.ws.send(message);
                successCount++;
            } catch (error) {
                console.log(`❌ Failed to send to client ${clientId}:`, error.message);
                failureCount++;
            }
        } else {
            console.log(`⚠️ Client ${clientId} not ready (state: ${clientInfo.ws.readyState})`);
            failureCount++;
        }
    });

    console.log(`📊 Broadcast result: ${successCount} successful, ${failureCount} failed`);
}

// --- HTTP ENDPOINTS ---

// Send notification to all clients
app.post('/notify', (req, res) => {
    const { message } = req.body;

    if (!message) {
        return res.status(400).json({ error: 'Message is required' });
    }

    broadcast(message);
    res.status(200).json({ 
        status: 'Notification sent successfully', 
        message,
        clientCount: clients.size 
    });
});

// Get connected clients info
app.get('/clients', (req, res) => {
    const clientsInfo = Array.from(clients.values()).map(client => ({
        id: client.id,
        connectedAt: client.connectedAt,
        ip: client.ip,
        readyState: client.ws.readyState
    }));

    res.json({
        totalClients: clients.size,
        clients: clientsInfo
    });
});

// Send notification to specific client
app.post('/notify/:clientId', (req, res) => {
    const clientId = parseInt(req.params.clientId);
    const { message } = req.body;

    if (!message) {
        return res.status(400).json({ error: 'Message is required' });
    }

    const clientInfo = clients.get(clientId);
    if (!clientInfo) {
        return res.status(404).json({ error: `Client ${clientId} not found` });
    }

    if (clientInfo.ws.readyState === WebSocket.OPEN) {
        try {
            clientInfo.ws.send(message);
            console.log(`📤 Message sent to client ${clientId}: "${message}"`);
            res.json({ 
                status: 'Message sent successfully', 
                clientId,
                message 
            });
        } catch (error) {
            console.log(`❌ Failed to send to client ${clientId}:`, error.message);
            res.status(500).json({ error: 'Failed to send message' });
        }
    } else {
        res.status(400).json({ error: `Client ${clientId} is not connected` });
    }
});

// Test endpoint
app.get('/test', (req, res) => {
    const testMessage = `Test notification at ${new Date().toLocaleTimeString()}`;
    broadcast(testMessage);
    res.json({ 
        status: 'Test notification sent', 
        message: testMessage,
        clientCount: clients.size 
    });
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'Server is running',
        uptime: process.uptime(),
        clients: clients.size,
        timestamp: new Date().toISOString()
    });
});

// Serve simple web interface
app.get('/', (req, res) => {
    res.send(`
        <html>
            <head><title>Ziya WebSocket Server</title></head>
            <body style="font-family: Arial, sans-serif; margin: 40px;">
                <h1>Ziya WebSocket Notification Server</h1>
                <p><strong>Status:</strong> Running</p>
                <p><strong>WebSocket URL:</strong> ws://localhost:${PORT}</p>
                <p><strong>Connected Clients:</strong> <span id="clientCount">${clients.size}</span></p>
                
                <h2>Send Test Notification</h2>
                <input type="text" id="messageInput" placeholder="Enter message..." style="width: 300px; padding: 5px;">
                <button onclick="sendNotification()" style="padding: 5px 10px;">Send</button>
                
                <h2>API Endpoints</h2>
                <ul>
                    <li><code>POST /notify</code> - Send notification to all clients</li>
                    <li><code>GET /clients</code> - Get connected clients info</li>
                    <li><code>POST /notify/:clientId</code> - Send to specific client</li>
                    <li><code>GET /test</code> - Send test notification</li>
                    <li><code>GET /health</code> - Server health check</li>
                </ul>
                
                <script>
                    function sendNotification() {
                        const message = document.getElementById('messageInput').value;
                        if (!message) return;
                        
                        fetch('/notify', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ message })
                        }).then(response => response.json())
                          .then(data => {
                              alert('Notification sent: ' + data.message);
                              document.getElementById('messageInput').value = '';
                          });
                    }
                    
                    // Update client count every 5 seconds
                    setInterval(() => {
                        fetch('/clients')
                            .then(response => response.json())
                            .then(data => {
                                document.getElementById('clientCount').textContent = data.totalClients;
                            });
                    }, 5000);
                </script>
            </body>
        </html>
    `);
});

// Start the HTTP server
server.listen(PORT, () => {
    console.log('🚀 Ziya WebSocket Notification Server started');
    console.log(`📱 WebSocket URL: ws://localhost:${PORT}`);
    console.log(`🌐 Web interface: http://localhost:${PORT}`);
    console.log(`📡 API base URL: http://localhost:${PORT}`);
    console.log('');
    console.log('Ready to accept connections...');
});