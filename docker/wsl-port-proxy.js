const net = require("net");

const [, , listenHost, listenPortRaw, targetHost, targetPortRaw] = process.argv;

if (!listenHost || !listenPortRaw || !targetHost || !targetPortRaw) {
  console.error("Usage: node wsl-port-proxy.js <listenHost> <listenPort> <targetHost> <targetPort>");
  process.exit(1);
}

const listenPort = Number(listenPortRaw);
const targetPort = Number(targetPortRaw);

const server = net.createServer((clientSocket) => {
  const targetSocket = net.createConnection({ host: targetHost, port: targetPort });

  clientSocket.pipe(targetSocket);
  targetSocket.pipe(clientSocket);

  const closeBoth = () => {
    clientSocket.destroy();
    targetSocket.destroy();
  };

  clientSocket.on("error", closeBoth);
  targetSocket.on("error", closeBoth);
  clientSocket.on("close", closeBoth);
  targetSocket.on("close", closeBoth);
});

server.on("error", (error) => {
  console.error(error.stack || error);
  process.exit(1);
});

server.listen(listenPort, listenHost);
