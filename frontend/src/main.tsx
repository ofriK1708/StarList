import { Amplify } from 'aws-amplify';
import { createRoot } from "react-dom/client";
import App from "./app/App";
import "./styles/index.css";
import { awsConfig } from "./aws-config";

Amplify.configure(awsConfig);

createRoot(document.getElementById("root")!).render(<App />);