import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import MembershipAgreement from "./MembershipAgreement";
import PrivacyPolicy from "./PrivacyPolicy";
import AccountDeletion from "./AccountDeletion";
import Good4PrivacyPolicy from "./Good4PrivacyPolicy";
import "./styles.css";

const normalizedPath = window.location.pathname.replace(/\/$/, "") || "/";
const isPrivacyPage = normalizedPath === "/gizlilik" || normalizedPath === "/privacy";
const isAgreementPage = normalizedPath === "/uyelik-sozlesmesi" || normalizedPath === "/terms";
const isAccountDeletionPage = normalizedPath === "/hesabimi-sil";
const isStandalonePrivacyPolicy = normalizedPath === "/gizlilik-politikasi";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    {isStandalonePrivacyPolicy ? <Good4PrivacyPolicy /> : isAccountDeletionPage ? <AccountDeletion /> : isAgreementPage ? <MembershipAgreement /> : isPrivacyPage ? <PrivacyPolicy /> : <App />}
  </StrictMode>,
);
