import { FieldValue, type Firestore } from "firebase-admin/firestore";

// Central Antalya campus of Akdeniz University.
export const CAMPUS_LATITUDE = 36.898;
export const CAMPUS_LONGITUDE = 30.651;
export const CAMPUS_WEATHER_DOC = "app_config/campus_weather";

// MET Norway requires an identifying User-Agent with contact details.
const USER_AGENT = "Good4/1.0 (https://good4tr-v2.web.app; cannklnc7@gmail.com)";
const FORECAST_URL =
  `https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=${CAMPUS_LATITUDE}&lon=${CAMPUS_LONGITUDE}`;

export type FetchLike = (url: string, init: { headers: Record<string, string> }) => Promise<{
  status: number;
  headers: { get(name: string): string | null };
  json(): Promise<unknown>;
}>;

/** Turkish label for a MET Norway symbol code such as "partlycloudy_day" or "lightrain". */
export function weatherLabel(symbolCode: string): string {
  const base = symbolCode.split("_")[0] ?? "";
  if (base === "clearsky") return "Açık";
  if (base === "fair") return "Az bulutlu";
  if (base === "partlycloudy") return "Parçalı bulutlu";
  if (base === "cloudy") return "Bulutlu";
  if (base === "fog") return "Sisli";
  if (base.includes("thunder")) return "Gök gürültülü";
  if (base.includes("sleet")) return "Karla karışık yağmurlu";
  if (base.includes("snow")) return "Karlı";
  if (base.includes("rain")) return "Yağmurlu";
  return "Hava durumu";
}

interface Forecast {
  properties?: {
    timeseries?: Array<{
      data?: {
        instant?: { details?: { air_temperature?: number } };
        next_1_hours?: { summary?: { symbol_code?: string } };
        next_6_hours?: { summary?: { symbol_code?: string } };
      };
    }>;
  };
}

/**
 * Fetches the campus forecast and stores the current reading for the app.
 * Honours If-Modified-Since so unchanged data is not downloaded again.
 */
export async function refreshCampusWeatherService(
  database: Firestore,
  fetchImpl: FetchLike = fetch as unknown as FetchLike,
  now = Date.now(),
): Promise<"updated" | "not_modified"> {
  const ref = database.doc(CAMPUS_WEATHER_DOC);
  const previous = await ref.get();
  const headers: Record<string, string> = { "User-Agent": USER_AGENT };
  const lastModified = previous.get("lastModified");
  if (typeof lastModified === "string" && lastModified) headers["If-Modified-Since"] = lastModified;

  const response = await fetchImpl(FORECAST_URL, { headers });
  if (response.status === 304) {
    await ref.set({ checkedAtMillis: now }, { merge: true });
    return "not_modified";
  }
  if (response.status !== 200) {
    throw new Error(`MET Norway responded with ${response.status}`);
  }

  const forecast = await response.json() as Forecast;
  const current = forecast.properties?.timeseries?.[0]?.data;
  const temperature = current?.instant?.details?.air_temperature;
  const symbolCode = current?.next_1_hours?.summary?.symbol_code
    ?? current?.next_6_hours?.summary?.symbol_code
    ?? "";
  if (typeof temperature !== "number") {
    throw new Error("MET Norway forecast has no air temperature");
  }

  await ref.set({
    temperature: Math.round(temperature),
    symbolCode,
    label: weatherLabel(symbolCode),
    source: "MET Norway",
    lastModified: response.headers.get("last-modified") ?? "",
    updatedAtMillis: now,
    checkedAtMillis: now,
    updatedAt: FieldValue.serverTimestamp(),
  });
  return "updated";
}
