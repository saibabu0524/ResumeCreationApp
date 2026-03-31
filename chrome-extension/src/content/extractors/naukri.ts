/**
 * Naukri.com job description extractor.
 */
export function extractNaukri(): string | null {
  const selectors = [
    ".styles_jd-container__cOXMq",
    '[class*="jd-container"]',
    '[class*="job-desc"]',
    ".dang-inner-html",
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
