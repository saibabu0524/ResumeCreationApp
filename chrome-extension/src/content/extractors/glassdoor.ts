/**
 * Glassdoor job description extractor.
 */
export function extractGlassdoor(): string | null {
  const selectors = [
    ".jobDescriptionContent",
    '[class*="JobDescription"]',
    '[class*="jobDescription"]',
    ".desc",
    "#JobDescriptionContainer",
  ];

  for (const sel of selectors) {
    const el = document.querySelector(sel);
    if (el?.textContent?.trim()) {
      return el.textContent.trim();
    }
  }

  return null;
}
