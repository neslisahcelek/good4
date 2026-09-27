/** Stable identifiers shared by the callable validation and community web form. */
export const EVENT_CATEGORIES = [
  { id: "academic-science", label: "Akademik ve Bilim" },
  { id: "career-entrepreneurship", label: "Kariyer ve Girişimcilik" },
  { id: "technology", label: "Teknoloji" },
  { id: "culture-arts", label: "Kültür ve Sanat" },
  { id: "sports-nature", label: "Spor ve Doğa" },
  { id: "social-entertainment", label: "Sosyal ve Eğlence" },
  { id: "volunteering", label: "Gönüllülük" },
  { id: "other", label: "Diğer" },
] as const;

export type EventCategoryId = (typeof EVENT_CATEGORIES)[number]["id"];
