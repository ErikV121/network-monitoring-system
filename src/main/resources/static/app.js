var stompClient1 = null;

// Variables for Chart.js
let uploadChart, downloadChart;
let uploadData = [];
let downloadData = [];

// Variables for calculating averages
let totalUpload = 0;
let totalDownload = 0;
let uploadCount = 0;
let downloadCount = 0;

// Initialize Charts
let packetLossChart;
let packetLossData = []; // Array to store packet loss data

function initializeCharts() {
    const ctxUpload = document.getElementById('uploadChart').getContext('2d');
    const ctxDownload = document.getElementById('downloadChart').getContext('2d');
    const ctxPacketLoss = document.getElementById('packetLossChart').getContext('2d');

    uploadChart = new Chart(ctxUpload, {
        type: 'line',
        data: {
            labels: [], // Time labels
            datasets: [{
                label: 'Upload (Mbps)',
                data: uploadData,
                borderColor: 'rgba(75, 192, 192, 1)',
                borderWidth: 1,
                fill: false
            }]
        },
        options: {
            scales: {
                x: {title: {display: true, text: 'Time'}},
                y: {title: {display: true, text: 'Upload Speed (Mbps)'}}
            }
        }
    });

    downloadChart = new Chart(ctxDownload, {
        type: 'line',
        data: {
            labels: [], // Time labels
            datasets: [{
                label: 'Download (Mbps)',
                data: downloadData,
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1,
                fill: false
            }]
        },
        options: {
            scales: {
                x: {title: {display: true, text: 'Time'}},
                y: {title: {display: true, text: 'Download Speed (Mbps)'}}
            }
        }
    });

    packetLossChart = new Chart(ctxPacketLoss, {
        type: 'line',
        data: {
            labels: [], // Time labels
            datasets: [{
                label: 'Packet Loss (%)',
                data: packetLossData,
                borderColor: 'rgba(255, 99, 132, 1)',
                borderWidth: 1,
                fill: false
            }]
        },
        options: {
            scales: {
                x: {
                    title: {display: true, text: 'Time'}
                },
                y: {
                    title: {display: true, text: 'Packet Loss (%)'},
                    min: 0 // Prevent negative values on the y-axis
                }
            }
        }
    });

}


// Connect to WebSocket and Listen for Messages
function connect() {
    const socket1 = new SockJS('/ws');
    stompClient1 = Stomp.over(socket1);

    stompClient1.connect({}, function (frame) {
        console.log('Connected to /main/test1: ' + frame);
        stompClient1.subscribe('/main/test1', function (message) {
            processMessage(JSON.parse(message.body).content);
        });
    });
}

function disconnect() {
    if (stompClient1 !== null) stompClient1.disconnect();
    console.log("Disconnected");
}

function pauseUpdates() {
    isPaused = true;
    console.log("Updates paused.");
}

function playUpdates() {
    isPaused = false;
    console.log("Updates resumed.");
}


function processMessage(message) {
    if (isPaused) return;

    const match = message.match(/Upload: ([\d.]+) Mbps, Download: ([\d.]+) Mbps, Packet Loss: ([\d.]+)%/);
    if (match) {
        const upload = parseFloat(match[1]);
        const download = parseFloat(match[2]);
        const packetLoss = parseFloat(match[3]);
        const timeLabel = new Date().toLocaleTimeString();

        // Update data for upload and download charts
        uploadData.push(upload);
        downloadData.push(download);
        uploadChart.data.labels.push(timeLabel);
        downloadChart.data.labels.push(timeLabel);

        // Update data for packet loss chart
        packetLossData.push(packetLoss);
        packetLossChart.data.labels.push(timeLabel);

        // Keep only the last 20 data points
        if (uploadData.length > 20) {
            uploadData.shift();
            uploadChart.data.labels.shift();
        }
        if (downloadData.length > 20) {
            downloadData.shift();
            downloadChart.data.labels.shift();
        }
        if (packetLossData.length > 20) {
            packetLossData.shift();
            packetLossChart.data.labels.shift();
        }

        // Calculate sliding window averages for upload and download
        const avgUpload = (uploadData.reduce((sum, value) => sum + value, 0) / uploadData.length).toFixed(2);
        const avgDownload = (downloadData.reduce((sum, value) => sum + value, 0) / downloadData.length).toFixed(2);

        // Update average values in the DOM
        document.getElementById('uploadAvg').innerText = `Average Upload: ${avgUpload} Mbps`;
        document.getElementById('downloadAvg').innerText = `Average Download: ${avgDownload} Mbps`;

        uploadChart.update();
        downloadChart.update();
        packetLossChart.update();
    }
}

initializeCharts();
