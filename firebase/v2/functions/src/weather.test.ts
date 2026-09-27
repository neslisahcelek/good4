import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { db, legacyTestDb } from "./firebase.js";
import { CAMPUS_WEATHER_DOC, type FetchLike, refreshCampusWeatherService, weatherLabel } from "./weather.js";

beforeEach(async () => {
  await db.recursiveDelete(db.collection("app_config"));
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

function fakeFetch(status: number, body: unknown, lastModified = "Thu, 25 Sep 2026 10:00:00 GMT") {
  const calls: Array<{ url: string; headers: Record<string, string> }> = [];
  const impl: FetchLike = async (url, init) => {
    calls.push({ url, headers: init.headers });
    return {
      status,
      headers: { get: (name: string) => (name.toLowerCase() === "last-modified" ? lastModified : null) },
      json: async () => body,
    };
  };
  return { impl, calls };
}

const forecast = {
  properties: {
    timeseries: [{
      data: {
        instant: { details: { air_temperature: 25.6 } },
        next_1_hours: { summary: { symbol_code: "partlycloudy_day" } },
      },
    }],
  },
};

test("stores the current campus temperature and a Turkish label", async () => {
  const { impl, calls } = fakeFetch(200, forecast);
  assert.equal(await refreshCampusWeatherService(db, impl, 1_000), "updated");

  const stored = await db.doc(CAMPUS_WEATHER_DOC).get();
  assert.equal(stored.get("temperature"), 26);
  assert.equal(stored.get("label"), "Parçalı bulutlu");
  assert.equal(stored.get("source"), "MET Norway");
  assert.equal(stored.get("updatedAtMillis"), 1_000);
  assert.match(calls[0]?.headers["User-Agent"] ?? "", /^Good4\/1\.0 \(.+\)$/);
  assert.match(calls[0]?.url ?? "", /lat=36\.898&lon=30\.651$/);
});

test("sends If-Modified-Since and keeps the reading on 304", async () => {
  await refreshCampusWeatherService(db, fakeFetch(200, forecast).impl, 1_000);
  const notModified = fakeFetch(304, null);
  assert.equal(await refreshCampusWeatherService(db, notModified.impl, 2_000), "not_modified");

  assert.equal(notModified.calls[0]?.headers["If-Modified-Since"], "Thu, 25 Sep 2026 10:00:00 GMT");
  const stored = await db.doc(CAMPUS_WEATHER_DOC).get();
  assert.equal(stored.get("temperature"), 26);
  assert.equal(stored.get("updatedAtMillis"), 1_000);
  assert.equal(stored.get("checkedAtMillis"), 2_000);
});

test("keeps the last reading when MET Norway fails", async () => {
  await refreshCampusWeatherService(db, fakeFetch(200, forecast).impl, 1_000);
  await assert.rejects(refreshCampusWeatherService(db, fakeFetch(503, null).impl, 2_000));
  assert.equal((await db.doc(CAMPUS_WEATHER_DOC).get()).get("temperature"), 26);
});

test("maps MET symbol codes to Turkish labels", () => {
  assert.equal(weatherLabel("clearsky_night"), "Açık");
  assert.equal(weatherLabel("lightrainshowers_day"), "Yağmurlu");
  assert.equal(weatherLabel("heavysnow"), "Karlı");
  assert.equal(weatherLabel("rainandthunder"), "Gök gürültülü");
  assert.equal(weatherLabel("lightsleet"), "Karla karışık yağmurlu");
  assert.equal(weatherLabel("fog"), "Sisli");
});
