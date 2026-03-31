/**
 * LinkedIn job description extractor.
 * Targets the job detail panel on linkedin.com/jobs/* pages.
 */
export function extractLinkedIn(): string | null {
  // Primary: the expanded job description container
  const selectors = [
    ".jobs-description__content",
    ".jobs-description-content__text",
    '[class*="jobs-description"]',
    ".job-details-jobs-unified-top-card__job-insight",
    "#job-details",
  ];

  for (const sel of selectors) {
    const el = document.querySelector(sel);
    if (el?.textContent?.trim()) {
      return el.textContent.trim();
    }
  }

  // Fallback: look for the "About the job" section
  const sections = document.querySelectorAll("section");
  for (const section of sections) {
    const heading = section.querySelector("h2");
    if (heading?.textContent?.toLowerCase().includes("about the job")) {
      return section.textContent?.trim() ?? null;
    }
  }

  return null;
}
