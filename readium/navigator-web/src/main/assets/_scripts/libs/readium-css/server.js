const http = require("http");
const express = require("express");

const app = express();
const httpPort = 8000;
const httpServer = http.createServer(app);

httpServer.listen(httpPort, (err) => {
  if (err) {
    console.error(err);
  } else {
    console.log("http server listening on port " + httpServer.address().port);
  }
});

app.use("/tests", express.static(__dirname + "/tests"));
app.use("/css/dist", express.static(__dirname + "/css/dist"));