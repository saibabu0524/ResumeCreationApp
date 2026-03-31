/**
 * Job description extractor dispatcher.
 * Detects the current site and calls the appropriate extractor.
 */
import { extractLinkedIn } from "./linkedin";
import { extractIndeed } from "./indeed";
import { extractGlassdoor } from "./glassdoor";
import { extractNaukri } from "./naukri";
import { extractGeneric } from "./generic";

export interface ExtractionResult {
  source: string;
  jobDescription: string | null;
  url: string;
  title: string;
}

export function extractJobDescription(): ExtractionResult {
  const url = window.location.href;
  const hostname = window.location.hostname;
  const title = document.title;

  let source = "generic";
  let jobDescription: string | null = null;

  if (hostname.includes("linkedin.com")) {
    source = "linkedin";
    jobDescription = extractLinkedIn();
  } else if (hostname.includes("indeed.com")) {
    source = "indeed";
    jobDescription = extractIndeed();
  } else if (hostname.includes("glassdoor.com") || hostname.includes("glassdoor.co.in")) {
    source = "glassdoor";
    jobDescription = extractGlassdoor();
  } else if (hostname.includes("naukri.com")) {
    source = "naukri";
    jobDescription = extractNaukri();
  }

  // Fallback to generic if site-specific extractor found nothing
  if (!jobDescription) {
    jobDescription = extractGeneric();
    if (source !== "generic" && jobDescription) {
      source = `${source}+generic`;
    }
  }

  return { source, jobDescription, url, title };
}
