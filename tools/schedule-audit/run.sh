#!/usr/bin/env bash
# Ders programlarını derlenmiş koddan dışa aktarır, çökme ve bütünlük kontrolü yapar.
# Kullanım: bash tools/schedule-audit/run.sh [çıktı klasörü]
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
here="$root/tools/schedule-audit"
out="${1:-${TMPDIR:-/tmp}/good4-schedule-audit}"
mkdir -p "$out"
cd "$root"
./gradlew -q :composeApp:compileV2DebugKotlinAndroid
stdlib="$(find ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib -name 'kotlin-stdlib-2*.jar' ! -name '*sources*' | sort | tail -1)"
cp="$root/composeApp/build/tmp/kotlin-classes/v2Debug:$stdlib"
javac -d "$out" "$here/Dump.java" "$here/Combos.java"
python3 "$here/combos.py" composeApp/src/commonMain/kotlin/com/good4/user/presentation/accountsettings/AccountSettingsScreen.kt > "$out/combos.tsv"
java -cp "$out:$cp" Dump "$out/all.tsv" "$out/combos.tsv"
echo "== Seçilebilen tüm kombinasyonlar (çökme/eksik program)"
java -cp "$out:$cp" Combos "$out/combos.tsv" | tail -20
echo "== Bütünlük kontrolleri"
python3 "$here/analyze.py" "$out/all.tsv"
echo "Dışa aktarılan veri: $out/all.tsv"
