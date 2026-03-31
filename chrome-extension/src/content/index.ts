/**
 * Content script entry point.
 * Runs on supported job board pages to extract job descriptions.
 * Communicates with the popup/background via chrome.runtime messaging.
 */
import { extractJobDescription, type ExtractionResult } from "./extractors";

// Extract JD on page load
const result = extractJobDescription();

// Store extraction result for the popup to retrieve
chrome.storage.session.set({ lastExtraction: result });

// Listen for extraction requests from popup or background
chrome.runtime.onMessage.addListener(
  (
    message: { type: string },
    _sender: chrome.runtime.MessageSender,
    sendResponse: (response: ExtractionResult) => void,
  ) => {
    if (message.type === "EXTRACT_JD") {
      const freshResult = extractJobDescription();
      chrome.storage.session.set({ lastExtraction: freshResult });
      sendResponse(freshResult);
    }
    return true; // Keep the message channel open for async response
  },
);

// Notify background about extraction result
chrome.runtime.sendMessage({
  type: result.jobDescription ? "JD_EXTRACTED" : "JD_NOT_FOUND",
  data: result,
});
