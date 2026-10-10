import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import MembershipAgreement from "./MembershipAgreement";
import PrivacyPolicy from "./PrivacyPolicy";
import AccountDeletion from "./AccountDeletion";
import Good4PrivacyPolicy from "./Good4PrivacyPolicy";
import CommunityApplicationPage from "./CommunityApplication";
import DownloadPage from "./DownloadPage";
import LandingPage from "./LandingPage";
import CampusEmailLinkPage from "./CampusEmailLinkPage";
import "./styles.css";

const normalizedPath = window.location.pathname.replace(/\/$/, "") || "/";
const isPrivacyPage = normalizedPath === "/gizlilik" || normalizedPath === "/privacy";
const isAgreementPage = normalizedPath === "/uyelik-sozlesmesi" || normalizedPath === "/terms";
const isAccountDeletionPage = normalizedPath === "/hesabimi-sil";
const isStandalonePrivacyPolicy = normalizedPath === "/gizlilik-politikasi";
const isCommunityApplicationPage = normalizedPath === "/topluluk-basvuru";
const isDownloadPage = normalizedPath === "/indir";
const isCampusEmailLinkPage = normalizedPath === "/campus-email-verification";
// Community managers have their own entrance at /topluluk-paneli, on any host.
const isCommunityPortalPage = normalizedPath === "/topluluk-paneli" || normalizedPath.startsWith("/topluluk-paneli/");
// The staff panel lives on panel.good4tr.com, under /admin, and on admin.* hosts;
// every other unknown address, including the bare domain, shows the landing page.
const isAdminPage = !isCommunityPortalPage && (normalizedPath === "/admin" || normalizedPath.startsWith("/admin/")
  || window.location.hostname === "panel.good4tr.com"
  || window.location.hostname.startsWith("admin."));
if (isAdminPage) document.title = "Good4 Yönetim Paneli";
if (isCommunityPortalPage) document.title = "Good4 Topluluk Paneli";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    {isCampusEmailLinkPage ? <CampusEmailLinkPage /> : isDownloadPage ? <DownloadPage /> : isCommunityApplicationPage ? <CommunityApplicationPage /> : isStandalonePrivacyPolicy ?<Good4PrivacyPolicy /> : isAccountDeletionPage ? <AccountDeletion /> : isAgreementPage ? <MembershipAgreement /> : isPrivacyPage ? <PrivacyPolicy /> : isCommunityPortalPage ? <App portal="community" /> : isAdminPage ? <App /> : <LandingPage />}
  </StrictMode>,
);
