import resources from "../../../../composeApp/src/commonMain/composeResources/values/strings.xml?raw";

// Mobile and moderation UI share the social_* Turkish text resources.
const strings = new Map(Array.from(new DOMParser().parseFromString(resources, "text/xml")
  .querySelectorAll('string[name^="social_"]')).map((entry) => [entry.getAttribute("name")!, entry.textContent ?? ""]));

export function socialText(key: string): string {
  const value = strings.get(`social_${key}`);
  if (value === undefined) throw new Error(`Missing social string: ${key}`);
  return value;
}
