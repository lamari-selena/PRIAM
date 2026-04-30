import express, { Request, Response } from "express";
import session from "express-session";
import httpProxy from "http-proxy";
import cors from "cors";
import pgSession from "connect-pg-simple";
import { keycloakAuth } from './middleware';

const app = express();
const apiProxy = httpProxy.createProxyServer();


const frontUrl = process.env.REACT_APP_FRONT_URL || "http://localhost:3000/";
app.use(
  cors({
    origin: frontUrl.slice(0, -1), // remove trailing slash
    methods: ["POST", "PUT", "GET", "DELETE", "OPTIONS", "HEAD"],
    credentials: true
  })
);


//////////////////// MICROSERVICES ROUTING ///////////////////////////////////

//app.use("/api/ms-user",function(req: Request,res: Response) {
  //console.log("proxy to ms-user")
  //console.log("URL IS : " + req.url)
  //apiProxy.web(req, res, { target: 'http://ms-user:' + process.env.MS_PORT + "/api" }, (err) => {console.log(err);})
//});

app.use("/data", keycloakAuth,(req: Request, res: Response) => {
  console.log("proxy to Data Service");
  console.log("URL IS : " + req.url);
  // enlève le préfixe /data avant de proxy
  req.url = req.url.replace(/^\/data/, "");
  apiProxy.web(req, res, { target: process.env.CUSTOM_DATA_URL }, (err) => console.log(err));
});

app.use("/right", keycloakAuth,(req: Request, res: Response) => {
  console.log("proxy to Right Service");
  console.log("URL IS : " + req.url);
  req.url = req.url.replace(/^\/right/, "");
  apiProxy.web(req, res, { target: process.env.CUSTOM_RIGHT_URL }, (err) => console.log(err));
});

app.use("/actor", keycloakAuth,(req: Request, res: Response) => {
  console.log("proxy to Actor Service");
  console.log("URL IS : " + req.url);
  req.url = req.url.replace(/^\/actor/, "");
  apiProxy.web(req, res, { target: process.env.CUSTOM_ACTOR_URL }, (err) => console.log(err));
});

app.use("/provider", keycloakAuth,(req: Request, res: Response) => {
  console.log("proxy to Provider Service");
  console.log("URL IS : " + req.url);
  req.url = req.url.replace(/^\/provider/, "");
  apiProxy.web(req, res, { target: process.env.CUSTOM_PROVIDER_URL }, (err) => console.log(err));
});

app.use("/cdp", keycloakAuth, (req: Request, res: Response) => {
  console.log("proxy to Consent Service");
  console.log("URL IS : " + req.url);
  req.url = req.url.replace(/^\/cdp/, "");
  apiProxy.web(req, res, { target: process.env.CUSTOM_CDP_URL }, (err) => console.log(err));
});

//////////////////// START SERVER ///////////////////////////////////

const PORT = process.env.GATEWAY_PORT || 8090;
app.listen(PORT, () => {
  console.log(`ShellOnYou **API Gateway** listening on port ${PORT}`);
});
