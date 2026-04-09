/**
 * Returns the current local date as a YYYY-MM-DD string.
 *
 * Do NOT use `new Date().toISOString().split('T')[0]` — that returns the UTC
 * date, which rolls over to the next calendar day for users in negative UTC
 * offsets (e.g. US timezones) when it is after ~7–11 pm local time.
 */
export function localDateString(date: Date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}
