import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import CampusEmailLinkPage from "./CampusEmailLinkPage";
import "./campusEmailPage.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode><CampusEmailLinkPage /></StrictMode>,
);
