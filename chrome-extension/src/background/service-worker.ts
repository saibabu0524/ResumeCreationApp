// Background service worker for the Chrome extension (MV3)

chrome.runtime.onInstalled.addListener(() => {
  console.log("Resume Tailor extension installed");
});

// Listen for JD extraction results from content scripts
chrome.runtime.onMessage.addListener((message, sender) => {
  if (message.type === "JD_EXTRACTED" && sender.tab?.id) {
    // Set badge to indicate a JD was found
    chrome.action.setBadgeText({ text: "JD", tabId: sender.tab.id });
    chrome.action.setBadgeBackgroundColor({ color: "#2563EB", tabId: sender.tab.id });
  }

  if (message.type === "JD_NOT_FOUND" && sender.tab?.id) {
    chrome.action.setBadgeText({ text: "", tabId: sender.tab.id });
  }
});

// Clear badge when navigating away
chrome.tabs.onUpdated.addListener((tabId, changeInfo) => {
  if (changeInfo.status === "loading") {
    chrome.action.setBadgeText({ text: "", tabId });
  }
});
