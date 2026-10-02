/** Only the latest request in the current auth session may update the screen. */
export function createAccountRequestScope() {
  let accountUid: string | null = null;
  let generation = 0;

  return {
    setAccount(uid: string | null) {
      accountUid = uid;
      generation += 1;
    },
    start(uid: string): (() => boolean) | null {
      // A late submission callback from an old form must not cancel the new
      // account's request or fetch its data using the old form's context.
      if (uid !== accountUid) return null;
      const requestGeneration = ++generation;
      return () => uid === accountUid && requestGeneration === generation;
    },
  };
}
