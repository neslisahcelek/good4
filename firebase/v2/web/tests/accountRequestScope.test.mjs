import assert from "node:assert/strict";
import { test } from "node:test";
import { createAccountRequestScope } from "../src/accountRequestScope.ts";

test("a delayed response from account A cannot replace account B's result", async () => {
  const scope = createAccountRequestScope();
  let resolveA;
  const responseA = new Promise((resolve) => { resolveA = resolve; });
  let displayed;
  scope.setAccount("A");
  const currentA = scope.start("A");
  const requestA = responseA.then((result) => { if (currentA()) displayed = result; });

  scope.setAccount("B");
  const currentB = scope.start("B");
  if (currentB()) displayed = "B's application";
  resolveA("A's application");
  await requestA;

  assert.equal(displayed, "B's application");
  assert.equal(currentB(), true);
});

test("signing out invalidates pending responses even when the same account signs back in", () => {
  const scope = createAccountRequestScope();
  scope.setAccount("A");
  const previousSession = scope.start("A");
  scope.setAccount(null);
  assert.equal(previousSession(), false);
  scope.setAccount("A");
  assert.equal(previousSession(), false);
  assert.equal(scope.start("A")(), true);
});

test("a newer refresh invalidates older results and error callbacks", () => {
  const scope = createAccountRequestScope();
  scope.setAccount("A");
  const older = scope.start("A");
  const newer = scope.start("A");
  assert.equal(older(), false);
  assert.equal(newer(), true);
});

test("a late submission from an old account leaves the new account's request active", () => {
  const scope = createAccountRequestScope();
  scope.setAccount("B");
  const currentB = scope.start("B");
  assert.equal(scope.start("A"), null);
  assert.equal(currentB(), true);
});
