/**
 * Indeed job description extractor.
 * Targets indeed.com job detail pages.
 */
export function extractIndeed(): string | null {
  const selectors = [
    "#jobDescriptionText",
    ".jobsearch-jobDescriptionText",
    '[id="jobDescriptionText"]',
    ".job-description",
  ];

  for (const sel of selectors) {
    const el = document.querySelector(sel);
    if (el?.textContent?.trim()) {
      return el.textContent.trim();
    }
  }

  return null;
}
