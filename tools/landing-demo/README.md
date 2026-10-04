# Tanıtım ekran görüntüleri için demo ortamı

good4tr.com tanıtım sayfası ve mağaza görselleri, gerçek kullanıcı verisi göstermemek için
yerel Firebase emülatörlerindeki **kurgusal** demo veriyle çekilir. Emülatörler `demo-good4-v2`
projesiyle çalışır; bu proje adı gerçek bir Firebase projesine karşılık gelmez, yani buradaki
hiçbir şey good4tr-v2'ye yazılmaz.

| Dosya | Ne yapar |
|---|---|
| `demo-data.mjs` | Demo öğrenci, topluluklar, etkinlikler, Kampüs Dolabı ilanları, askıda yemekler |
| `make-assets.mjs` | Logo, kapak, etkinlik afişi ve ilan çizimlerini `assets/` altına üretir (Chrome gerekir) |
| `seed.mjs` | Emülatörleri temizler ve demo veriyi yükler |
| `export-screens.py` | `design/landing-screens/*.png` ekran görüntülerini sayfada gösterildikleri boyutta (1x/2x/3x WebP) dışa aktarır |

Yeni bir ekran görüntüsü eklerken tam çözünürlüklü PNG'yi `design/landing-screens/` altına koy ve
`python3 tools/landing-demo/export-screens.py` çalıştır. Görseller tarayıcıda küçültülürse döndürülmüş
telefonlarda yazılar dalgalı görünür; bu yüzden boyutlar CSS'teki genişliklerle birebir eşleşir.

## 1. Emülatörleri başlat

```bash
cd firebase/v2 && npm --prefix functions run build
npx firebase emulators:start --config firebase.landing-demo.json --project demo-good4-v2 --only auth,firestore,functions,storage
```

Portlar testlerle çakışmasın diye ayrıdır: Auth 9399, Firestore 8385, Functions 5305, Storage 9495.

## 2. Demo veriyi yükle

```bash
node tools/landing-demo/seed.mjs            # iOS Simulator için (127.0.0.1)
node tools/landing-demo/seed.mjs 10.0.2.2   # Android emülatörü için
```

Görsel adresleri cihaza göre değiştiği için platform değiştirirken veriyi yeniden yükle.

## 3. Uygulamayı demo modunda aç

Demo modu yalnızca debug derlemelerde ve aşağıdaki ayar verildiğinde açılır; mağaza sürümü etkilenmez.

**iOS** (`iosApp V2`, `DebugV2`, Simulator): uygulamayı ortam değişkenleriyle başlat. İlk açılışta
demo hesabın e-postası ve şifresi (`demo-data.mjs`) verilir, oturum kalıcıdır.

```bash
SIMCTL_CHILD_GOOD4_EMULATOR_HOST=127.0.0.1 \
SIMCTL_CHILD_GOOD4_DEMO_EMAIL=... SIMCTL_CHILD_GOOD4_DEMO_PASSWORD=... \
xcrun simctl launch <simulator> com.good4.iosApp
```

**Android** (`prodDebug`): henüz hazır değil. Aşağıdaki akış için uygulamada debug bağlantısı gerekiyor ve o kısım commit edilmedi.

```bash
adb shell run-as com.good4 sh -c 'printf "host=10.0.2.2\nemail=...\npassword=...\n" > files/good4-emulator.properties'
```

Dosyayı silmek (`adb shell run-as com.good4 rm files/good4-emulator.properties`) normal moda döndürür.

## 4. Durum çubuğu

```bash
xcrun simctl status_bar <simulator> override --time 9:41 --dataNetwork wifi --wifiBars 3 --cellularMode active --cellularBars 4 --batteryState charged --batteryLevel 100
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
```
