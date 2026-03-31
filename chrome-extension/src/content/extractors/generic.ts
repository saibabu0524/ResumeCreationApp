/**
 * Generic job description extractor.
 * Fallback: finds the largest text block on the page.
 */
export function extractGeneric(): string | null {
  // Try common semantic containers first
  const semanticSelectors = [
    '[class*="job-description"]',
    '[class*="jobDescription"]',
    '[class*="job_description"]',
    '[id*="job-description"]',
    '[id*="jobDescription"]',
    "article",
    "main",
    '[role="main"]',
  ];

  for (const sel of semanticSelectors) {
    const el = document.querySelector(sel);
    const text = el?.textContent?.trim();
    if (text && text.length > 200) {
      return text;
    }
  }

  // Fallback: find the largest content block among divs/sections
  const candidates = document.querySelectorAll("div, section");
  let bestText = "";
  let bestLen = 0;

  for (const el of candidates) {
    const text = el.textContent?.trim() ?? "";
    // Skip elements that are too small or too large (likely the whole page)
    if (text.length > 200 && text.length < 15000 && text.length > bestLen) {
      bestLen = text.length;
      bestText = text;
    }
  }

  return bestText || null;
}
