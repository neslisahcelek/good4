import { writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const events = [];
const add = (id, title, startDate, endDate, faculty, category, sourcePage, description = "") => {
  events.push({ id, title, description, startDate, endDate, faculty, category, sourcePage });
};
const one = (id, title, date, faculty, category, page, description = "") =>
  add(id, title, date, date, faculty, category, page, description);

// Ön lisans / lisans yabancı dil hazırlık takvimi — PDF s.1
add("prep-transfer-placement", "Yatay geçiş öğrencileri hazırlık düzey belirleme ve yeterlik sınavı", "2026-09-15", "2026-09-16", "Yabancı Diller Yüksekokulu", "Sınav", 1);
one("prep-transfer-results", "Yatay geçiş öğrencileri hazırlık sınav sonuçlarının duyurulması", "2026-09-17", "Yabancı Diller Yüksekokulu", "Sonuç", 1);
one("prep-placement-written", "Hazırlık düzey belirleme ve yeterlik sınavı", "2026-09-08", "Yabancı Diller Yüksekokulu", "Sınav", 1, "Yazma ve test bölümü.");
one("prep-placement-speaking", "Hazırlık düzey belirleme ve yeterlik sınavı", "2026-09-09", "Yabancı Diller Yüksekokulu", "Sınav", 1, "Konuşma bölümü.");
one("prep-placement-results", "Hazırlık sınav sonuçlarının duyurulması", "2026-09-11", "Yabancı Diller Yüksekokulu", "Sonuç", 1);
one("prep-module-one-start", "I. Modül Hazırlık Programı Derslerinin Başlaması", "2026-09-14", "Yabancı Diller Yüksekokulu", "Ders", 1);
one("prep-module-one-end", "I. Modül Hazırlık Programı Derslerinin Sona Ermesi", "2027-01-15", "Yabancı Diller Yüksekokulu", "Ders", 1);
one("prep-module-two-start", "II. Modül Hazırlık Programı Derslerinin Başlaması", "2027-02-01", "Yabancı Diller Yüksekokulu", "Ders", 1);
one("prep-module-two-end", "II. Modül Hazırlık Programı Derslerinin Sona Ermesi", "2027-05-28", "Yabancı Diller Yüksekokulu", "Ders", 1);
add("prep-year-end-exam", "Yabancı dil hazırlık yıl sonu sınavı", "2027-06-01", "2027-06-02", "Yabancı Diller Yüksekokulu", "Sınav", 1);
one("prep-year-end-results", "Yabancı dil hazırlık yıl sonu sınav sonuçları için son gün", "2027-06-04", "Yabancı Diller Yüksekokulu", "Sonuç", 1, "Sonuçların otomasyon sistemine girilmesi için son tarih.");
one("prep-second-exam", "Yabancı dil hazırlık ikinci sınavı", "2027-06-08", "Yabancı Diller Yüksekokulu", "Sınav", 1);
one("prep-second-results", "Yabancı dil hazırlık ikinci sınav sonuçları için son gün", "2027-06-11", "Yabancı Diller Yüksekokulu", "Sonuç", 1, "Sonuçların otomasyon sistemine girilmesi için son tarih.");

// Tıp Fakültesi — PDF s.1-2
add("medicine-transfer-app", "Yatay geçiş başvuruları", "2026-08-01", "2026-08-15", "Tıp Fakültesi", "Başvuru", 1, "Dönem 1, 2, 3, 4, 5 ve 6.");
add("medicine-transfer-correction", "Yatay geçiş eksik veya yanlış evrak düzeltme", "2026-08-17", "2026-08-18", "Tıp Fakültesi", "Başvuru", 1);
one("medicine-transfer-results", "Yatay geçiş sonuçlarının duyurulması", "2026-08-28", "Tıp Fakültesi", "Sonuç", 1, "Dönem 1, 2, 3, 4, 5 ve 6.");
add("medicine-transfer-registration", "Yatay geçiş kesin kayıtları", "2026-08-31", "2026-09-02", "Tıp Fakültesi", "Kayıt", 1, "Dönem 1, 2, 3, 4, 5 ve 6.");
add("medicine-transfer-reserve-d45", "Yatay geçiş yedek kayıtları", "2026-09-03", "2026-09-04", "Tıp Fakültesi", "Kayıt", 1, "Dönem 4 ve 5.");
add("medicine-transfer-reserve-d23", "Yatay geçiş yedek kayıtları", "2026-09-07", "2026-09-08", "Tıp Fakültesi", "Kayıt", 1, "Dönem 2 ve 3.");
one("medicine-d6-special", "Özel öğrenci başvurusu için son gün", "2026-06-19", "Tıp Fakültesi", "Başvuru", 1, "Dönem 6; gelen ve giden öğrenciler.");
add("medicine-d6-registration", "Katkı payı, öğrenim ücreti ve ders kayıtları", "2026-06-22", "2026-06-26", "Tıp Fakültesi", "Kayıt", 1, "Dönem 6, 1. taksit.");
add("medicine-d6-advisor", "Danışman onayı", "2026-06-22", "2026-06-28", "Tıp Fakültesi", "Kayıt", 1, "Dönem 6.");
one("medicine-d6-leave", "Öğrenime ara izni başvurusu için son gün", "2026-06-26", "Tıp Fakültesi", "Başvuru", 1, "Dönem 6.");
one("medicine-d6-education-start", "Eğitim Öğretimin Başlaması", "2026-07-01", "Tıp Fakültesi", "Ders", 1, "Dönem 6.");
one("medicine-d6-education-end", "Eğitim Öğretimin Sona Ermesi", "2027-06-30", "Tıp Fakültesi", "Ders", 2, "Dönem 6.");
add("medicine-d6-social", "Sosyal transkript etkinlik başvuruları", "2026-09-14", "2027-06-30", "Tıp Fakültesi", "Başvuru", 2, "Dönem 6.");
add("medicine-d6-add-drop", "Ders ekleme-bırakma ve mazeretli ders kaydı", "2026-06-29", "2026-07-03", "Tıp Fakültesi", "Kayıt", 2, "Dönem 6.");
add("medicine-d6-add-drop-approval", "Ekle-bırak danışman onayı", "2026-06-29", "2026-07-05", "Tıp Fakültesi", "Kayıt", 2, "Dönem 6.");
one("medicine-d45-special", "Özel öğrenci başvurusu için son gün", "2026-08-21", "Tıp Fakültesi", "Başvuru", 2, "Dönem 4 ve 5; gelen ve giden öğrenciler.");
add("medicine-d45-registration", "Katkı payı, öğrenim ücreti ve ders kayıtları", "2026-08-31", "2026-09-04", "Tıp Fakültesi", "Kayıt", 2, "Dönem 4 ve 5, 1. taksit.");
add("medicine-d45-advisor", "Danışman onayı", "2026-08-31", "2026-09-06", "Tıp Fakültesi", "Kayıt", 2, "Dönem 4 ve 5.");
one("medicine-d45-leave", "Öğrenime ara izni başvurusu için son gün", "2026-09-04", "Tıp Fakültesi", "Başvuru", 2, "Dönem 4 ve 5.");
one("medicine-d45-education-start", "Eğitim Öğretimin Başlaması", "2026-09-07", "Tıp Fakültesi", "Ders", 2, "Dönem 4 ve 5.");
one("medicine-d45-education-end", "Eğitim Öğretimin Sona Ermesi", "2027-06-11", "Tıp Fakültesi", "Ders", 2, "Dönem 4 ve 5.");
add("medicine-d45-social", "Sosyal transkript etkinlik başvuruları", "2026-09-14", "2027-06-11", "Tıp Fakültesi", "Başvuru", 2, "Dönem 4 ve 5.");
add("medicine-d45-add-drop", "Ders ekleme-bırakma ve mazeretli ders kaydı", "2026-09-07", "2026-09-11", "Tıp Fakültesi", "Kayıt", 2, "Dönem 4 ve 5.");
add("medicine-d45-add-drop-approval", "Ekle-bırak danışman onayı", "2026-09-07", "2026-09-13", "Tıp Fakültesi", "Kayıt", 2, "Dönem 4 ve 5.");
one("medicine-d123-special", "Özel öğrenci başvurusu için son gün", "2026-08-21", "Tıp Fakültesi", "Başvuru", 2, "Dönem 1, 2 ve 3; gelen ve giden öğrenciler.");
add("medicine-d123-registration", "Katkı payı, öğrenim ücreti ve ders kayıtları", "2026-09-07", "2026-09-11", "Tıp Fakültesi", "Kayıt", 2, "Dönem 1, 2 ve 3, 1. taksit.");
add("medicine-d123-advisor", "Danışman onayı", "2026-09-07", "2026-09-13", "Tıp Fakültesi", "Kayıt", 2, "Dönem 1, 2 ve 3.");
one("medicine-d123-leave", "Öğrenime ara izni başvurusu için son gün", "2026-09-11", "Tıp Fakültesi", "Başvuru", 2, "Dönem 1, 2 ve 3.");
one("medicine-d123-education-start", "Eğitim Öğretimin Başlaması", "2026-09-14", "Tıp Fakültesi", "Ders", 2, "Dönem 1, 2 ve 3.");
one("medicine-d123-education-end", "Eğitim Öğretimin Sona Ermesi", "2027-06-11", "Tıp Fakültesi", "Ders", 2, "Dönem 1, 2 ve 3.");
add("medicine-d123-social", "Sosyal transkript etkinlik başvuruları", "2026-09-14", "2027-06-11", "Tıp Fakültesi", "Başvuru", 2, "Dönem 1, 2 ve 3.");
add("medicine-d123-add-drop", "Ders ekleme-bırakma ve mazeretli ders kaydı", "2026-09-14", "2026-09-18", "Tıp Fakültesi", "Kayıt", 2, "Dönem 1, 2 ve 3.");
add("medicine-d123-add-drop-approval", "Ekle-bırak danışman onayı", "2026-09-14", "2026-09-20", "Tıp Fakültesi", "Kayıt", 2, "Dönem 1, 2 ve 3.");
add("medicine-second-installment", "Katkı payı ve öğrenim ücreti 2. taksit ödeme süresi", "2027-01-25", "2027-01-29", "Tıp Fakültesi", "Kayıt", 2, "Dönem 1, 2, 3, 4, 5 ve 6.");

// Diş Hekimliği Fakültesi — PDF s.2-3
const dentistryCommon = (prefix, scope, page) => {
  one(`${prefix}-special`, "Özel öğrenci başvurusu için son gün", "2026-08-21", "Diş Hekimliği Fakültesi", "Başvuru", page, `${scope}; gelen ve giden öğrenciler.`);
  add(`${prefix}-registration-fall`, "Katkı payı, öğrenim ücreti ve ders kayıtları", "2026-09-07", "2026-09-11", "Diş Hekimliği Fakültesi", "Kayıt", page, `${scope}; güz yarıyılı.`);
  add(`${prefix}-registration-spring`, "Katkı payı, öğrenim ücreti ve ders kayıtları", "2027-01-18", "2027-01-22", "Diş Hekimliği Fakültesi", "Kayıt", page, `${scope}; bahar yarıyılı.`);
  add(`${prefix}-advisor-fall`, "Danışman onayı", "2026-09-07", "2026-09-13", "Diş Hekimliği Fakültesi", "Kayıt", page, `${scope}; güz yarıyılı.`);
  add(`${prefix}-advisor-spring`, "Danışman onayı", "2027-01-18", "2027-01-24", "Diş Hekimliği Fakültesi", "Kayıt", page, `${scope}; bahar yarıyılı.`);
  one(`${prefix}-leave`, "Öğrenime ara izni başvurusu için son gün", "2026-09-11", "Diş Hekimliği Fakültesi", "Başvuru", page, scope);
  one(`${prefix}-start-fall`, "Derslerin başlaması", "2026-09-14", "Diş Hekimliği Fakültesi", "Ders", page, `${scope}; güz yarıyılı.`);
  one(`${prefix}-start-spring`, "Derslerin başlaması", "2027-01-25", "Diş Hekimliği Fakültesi", "Ders", page, `${scope}; bahar yarıyılı.`);
  add(`${prefix}-social`, "Sosyal transkript etkinlik başvuruları", "2026-09-14", "2027-05-28", "Diş Hekimliği Fakültesi", "Başvuru", page, scope);
  add(`${prefix}-add-drop-fall`, "Ders ekleme-bırakma ve mazeretli ders kaydı", "2026-09-15", "2026-09-18", "Diş Hekimliği Fakültesi", "Kayıt", page, `${scope}; güz yarıyılı.`);
  add(`${prefix}-add-drop-spring`, "Ders ekleme-bırakma ve mazeretli ders kaydı", "2027-01-25", "2027-01-29", "Diş Hekimliği Fakültesi", "Kayıt", page, `${scope}; bahar yarıyılı.`);
  add(`${prefix}-approval-fall`, "Ekle-bırak danışman onayı", "2026-09-15", "2026-09-20", "Diş Hekimliği Fakültesi", "Kayıt", page, `${scope}; güz yarıyılı.`);
  add(`${prefix}-approval-spring`, "Ekle-bırak danışman onayı", "2027-01-25", "2027-01-31", "Diş Hekimliği Fakültesi", "Kayıt", page, `${scope}; bahar yarıyılı.`);
  add(`${prefix}-midterm-fall`, "Ara sınavlar", "2026-11-09", "2026-11-20", "Diş Hekimliği Fakültesi", "Sınav", page, `${scope}; güz yarıyılı.`);
  add(`${prefix}-midterm-spring`, "Ara sınavlar", "2027-03-29", "2027-04-09", "Diş Hekimliği Fakültesi", "Sınav", page, `${scope}; bahar yarıyılı.`);
  one(`${prefix}-midterm-results-fall`, "Ara sınav sonuçlarının otomasyona girişi için son gün", "2026-11-27", "Diş Hekimliği Fakültesi", "Sonuç", page, `${scope}; güz yarıyılı.`);
  one(`${prefix}-midterm-results-spring`, "Ara sınav sonuçlarının otomasyona girişi için son gün", "2027-04-16", "Diş Hekimliği Fakültesi", "Sonuç", page, `${scope}; bahar yarıyılı.`);
  add(`${prefix}-makeup-fall`, "Mazeret sınavları", "2026-12-02", "2026-12-03", "Diş Hekimliği Fakültesi", "Sınav", page, `${scope}; güz yarıyılı.`);
  add(`${prefix}-makeup-spring`, "Mazeret sınavları", "2027-04-20", "2027-04-21", "Diş Hekimliği Fakültesi", "Sınav", page, `${scope}; bahar yarıyılı.`);
  one(`${prefix}-end-fall`, "Derslerin sona ermesi", "2027-01-03", "Diş Hekimliği Fakültesi", "Ders", page, `${scope}; güz yarıyılı.`);
  one(`${prefix}-end-spring`, "Derslerin sona ermesi", "2027-05-28", "Diş Hekimliği Fakültesi", "Ders", page, `${scope}; bahar yarıyılı.`);
  add(`${prefix}-final-fall`, "Yıl/yarıyıl sonu sınavları", "2027-01-04", "2027-01-08", "Diş Hekimliği Fakültesi", "Sınav", page, `${scope}; güz yarıyılı.`);
  add(`${prefix}-final-spring`, "Yıl/yarıyıl sonu sınavları", "2027-05-31", "2027-06-18", "Diş Hekimliği Fakültesi", "Sınav", page, `${scope}; bahar yarıyılı.`);
  one(`${prefix}-final-results-fall`, "Yıl/yarıyıl sonu sınav sonuçları için son gün", "2027-01-14", "Diş Hekimliği Fakültesi", "Sonuç", page, `${scope}; güz yarıyılı.`);
};
dentistryCommon("dentistry-d123", "Dönem 1, 2 ve 3", 2);
one("dentistry-d123-final-results-spring", "Yıl/yarıyıl sonu sınav sonuçları için son gün", "2027-06-25", "Diş Hekimliği Fakültesi", "Sonuç", 2, "Dönem 1, 2 ve 3; bahar yarıyılı.");
add("dentistry-d123-second-exam", "Yıl sonu ikinci sınavları", "2027-07-05", "2027-07-16", "Diş Hekimliği Fakültesi", "Sınav", 3, "Dönem 1, 2 ve 3.");
one("dentistry-d123-second-results", "Yıl sonu ikinci sınav sonuçları için son gün", "2027-07-23", "Diş Hekimliği Fakültesi", "Sonuç", 3, "Dönem 1, 2 ve 3.");
one("dentistry-d123-single-course", "Tek ders sınıf geçme sınavı", "2027-07-26", "Diş Hekimliği Fakültesi", "Sınav", 3, "Dönem 1, 2 ve 3.");
one("dentistry-d123-single-course-results", "Tek ders sınav sonuçları için son gün", "2027-07-28", "Diş Hekimliği Fakültesi", "Sonuç", 3, "Dönem 1, 2 ve 3.");
dentistryCommon("dentistry-d45", "Dönem 4 ve 5", 3);
one("dentistry-d45-final-results-spring", "Yıl/yarıyıl sonu sınav sonuçları için son gün", "2027-06-24", "Diş Hekimliği Fakültesi", "Sonuç", 3, "Dönem 4 ve 5; bahar yarıyılı.");
add("dentistry-d45-second-exam", "Yıl sonu ikinci sınavları", "2027-07-05", "2027-07-16", "Diş Hekimliği Fakültesi", "Sınav", 3, "Dönem 4 ve 5.");
one("dentistry-d45-second-results", "Yıl sonu ikinci sınav sonuçları için son gün", "2027-07-30", "Diş Hekimliği Fakültesi", "Sonuç", 3, "Dönem 4 ve 5.");
one("dentistry-d45-single-course", "Tek ders sınıf geçme sınavı", "2027-08-03", "Diş Hekimliği Fakültesi", "Sınav", 3, "Dönem 4 ve 5.");
one("dentistry-d45-single-course-results", "Tek ders sınav sonuçları için son gün", "2027-08-05", "Diş Hekimliği Fakültesi", "Sonuç", 3, "Dönem 4 ve 5.");
one("dentistry-clinical-start-fall", "Klinik eğitimin başlaması", "2026-09-14", "Diş Hekimliği Fakültesi", "Ders", 3, "Dönem 4 ve 5; güz yarıyılı.");
one("dentistry-clinical-start-spring", "Klinik eğitimin başlaması", "2027-01-04", "Diş Hekimliği Fakültesi", "Ders", 3, "Dönem 4 ve 5; bahar yarıyılı.");
one("dentistry-clinical-d4-end-fall", "Klinik eğitimin sona ermesi", "2026-12-31", "Diş Hekimliği Fakültesi", "Ders", 3, "4. sınıf; güz yarıyılı.");
one("dentistry-clinical-d4-end-spring", "Klinik eğitimin sona ermesi", "2027-06-04", "Diş Hekimliği Fakültesi", "Ders", 3, "4. sınıf; bahar yarıyılı.");
one("dentistry-clinical-d5-end", "Klinik eğitimin sona ermesi", "2027-06-25", "Diş Hekimliği Fakültesi", "Ders", 3, "5. sınıf.");
add("dentistry-clinical-d4-repeat", "Tek klinik eğitim tekrarı", "2027-06-14", "2027-07-09", "Diş Hekimliği Fakültesi", "Ders", 3, "4. sınıf.");
add("dentistry-clinical-d5-repeat", "Tek klinik eğitim tekrarı", "2027-07-05", "2027-07-30", "Diş Hekimliği Fakültesi", "Ders", 3, "5. sınıf.");

// Hukuk Fakültesi — PDF s.3-4
one("law-special", "Özel öğrenci başvurusu için son gün", "2026-08-21", "Hukuk Fakültesi", "Başvuru", 3, "Gelen ve giden öğrenciler.");
add("law-registration-fall", "Katkı payı, öğrenim ücreti ve kayıt yenileme", "2026-09-07", "2026-09-11", "Hukuk Fakültesi", "Kayıt", 3, "Güz yarıyılı.");
add("law-advisor-fall", "Danışman onayı", "2026-09-07", "2026-09-13", "Hukuk Fakültesi", "Kayıt", 3, "Güz yarıyılı.");
one("law-start-fall", "Derslerin başlaması", "2026-09-14", "Hukuk Fakültesi", "Ders", 3, "Güz yarıyılı.");
add("law-social-fall", "Sosyal transkript etkinlik başvuruları", "2026-09-14", "2026-12-20", "Hukuk Fakültesi", "Başvuru", 3, "Güz yarıyılı.");
add("law-add-drop-fall", "Ders ekleme-bırakma ve mazeretli ders kaydı", "2026-09-15", "2026-09-18", "Hukuk Fakültesi", "Kayıt", 3, "Güz yarıyılı.");
add("law-approval-fall", "Ekle-bırak danışman onayı", "2026-09-15", "2026-09-20", "Hukuk Fakültesi", "Kayıt", 3, "Güz yarıyılı.");
one("law-withdraw-fall", "Dersten çekilme için son gün", "2026-10-02", "Hukuk Fakültesi", "Kayıt", 3, "Güz yarıyılı.");
one("law-assessment-results-fall", "Ara sınav ve diğer ölçme sonuçları için son gün", "2026-12-20", "Hukuk Fakültesi", "Sonuç", 3, "Yarıyıllık dersler; otomasyon sistemine giriş.");
one("law-end-fall", "Derslerin sona ermesi", "2026-12-20", "Hukuk Fakültesi", "Ders", 3, "Güz yarıyılı.");
add("law-finals-fall", "Yıllık ara sınavları ve yarıyıllık final sınavları", "2026-12-21", "2027-01-08", "Hukuk Fakültesi", "Sınav", 3, "Üç hafta.");
one("law-final-results-fall", "Yarıyıllık final sonuçları için son gün", "2027-01-11", "Hukuk Fakültesi", "Sonuç", 4, "Otomasyon sistemine giriş.");
add("law-resit-app-fall", "Yarıyıllık bütünleme başvuruları", "2027-01-12", "2027-01-16", "Hukuk Fakültesi", "Başvuru", 4);
add("law-resits-fall", "Yarıyıllık bütünleme sınavları", "2027-01-18", "2027-01-22", "Hukuk Fakültesi", "Sınav", 4);
one("law-resit-results-fall", "Yarıyıllık bütünleme sonuçları için son gün", "2027-01-24", "Hukuk Fakültesi", "Sonuç", 4, "Otomasyon sistemine giriş.");
add("law-annual-makeup", "Yıllık derslerin ara sınav mazeret sınavları", "2027-02-10", "2027-02-12", "Hukuk Fakültesi", "Sınav", 4, "Sonuçların otomasyon sistemine girişi dahildir.");
add("law-registration-spring", "Katkı payı, öğrenim ücreti ve kayıt yenileme", "2027-01-25", "2027-01-29", "Hukuk Fakültesi", "Kayıt", 4, "Bahar yarıyılı.");
add("law-advisor-spring", "Danışman onayı", "2027-01-25", "2027-01-31", "Hukuk Fakültesi", "Kayıt", 4, "Bahar yarıyılı.");
one("law-start-spring", "Derslerin başlaması", "2027-02-01", "Hukuk Fakültesi", "Ders", 4, "Bahar yarıyılı.");
add("law-social-spring", "Sosyal transkript etkinlik başvuruları", "2027-02-01", "2027-05-14", "Hukuk Fakültesi", "Başvuru", 4, "Bahar yarıyılı.");
one("law-annual-midterm-results", "Yıllık ders ara sınav sonuçları için son gün", "2027-03-21", "Hukuk Fakültesi", "Sonuç", 4, "Otomasyon sistemine giriş.");
one("law-assessment-results-spring", "Ara sınav ve diğer ölçme sonuçları için son gün", "2027-05-14", "Hukuk Fakültesi", "Sonuç", 4, "Yarıyıllık dersler; otomasyon sistemine giriş.");
one("law-end-spring", "Derslerin sona ermesi", "2027-05-14", "Hukuk Fakültesi", "Ders", 4, "Bahar yarıyılı.");
add("law-finals-spring", "Yıllık ve yarıyıllık final sınavları", "2027-05-31", "2027-06-18", "Hukuk Fakültesi", "Sınav", 4, "Üç hafta.");
one("law-final-results-spring", "Yıl/yarıyıl sonu sınav sonuçları için son gün", "2027-06-21", "Hukuk Fakültesi", "Sonuç", 4, "Otomasyon sistemine giriş.");
add("law-resit-app-spring", "Yıl/yarıyıl sonu bütünleme başvuruları", "2027-06-19", "2027-06-26", "Hukuk Fakültesi", "Başvuru", 4);
add("law-resits-spring", "Yıllık ve yarıyıllık bütünleme sınavları", "2027-06-28", "2027-07-02", "Hukuk Fakültesi", "Sınav", 4);
one("law-resit-results-spring", "Yıl/yarıyıl sonu bütünleme sonuçları için son gün", "2027-07-05", "Hukuk Fakültesi", "Sonuç", 4, "Otomasyon sistemine giriş.");

// Yarıyıllık eğitim veren birimler — PDF s.4-5
const semesterRows = [
  ["assignments", "Ders görevlendirmelerinin Rektörlüğe bildirilmesi için son gün", "2026-08-14", "2026-08-14", "2026-12-31", "2026-12-31", "Ders"],
  ["special", "Özel öğrenci başvurusu için son gün", "2026-08-21", "2026-08-21", "2027-01-15", "2027-01-15", "Başvuru"],
  ["registration", "Katkı payı, öğrenim ücreti ve kayıt yenileme", "2026-09-07", "2026-09-11", "2027-01-25", "2027-01-29", "Kayıt"],
  ["advisor", "Danışman onayı", "2026-09-07", "2026-09-13", "2027-01-25", "2027-01-31", "Kayıt"],
  ["leave", "Öğrenime ara izni başvurusu için son gün", "2026-09-11", "2026-09-11", "2027-01-29", "2027-01-29", "Başvuru"],
  ["classes-start", "Derslerin başlaması", "2026-09-14", "2026-09-14", "2027-02-01", "2027-02-01", "Ders"],
  ["social", "Sosyal transkript etkinlik başvuruları", "2026-09-14", "2026-12-20", "2027-02-01", "2027-05-14", "Başvuru"],
  ["section-notice", "Kapatılacak veya şubelere ayrılacak derslerin bildirimi", "2026-09-14", "2026-09-14", "2027-02-01", "2027-02-01", "Ders"],
  ["add-drop", "Ders ekleme-bırakma ve mazeretli ders kaydı", "2026-09-15", "2026-09-18", "2027-02-02", "2027-02-05", "Kayıt"],
  ["approval", "Ekle-bırak danışman onayı", "2026-09-15", "2026-09-20", "2027-02-02", "2027-02-07", "Kayıt"],
  ["withdraw", "Dersten çekilme için son gün", "2026-10-02", "2026-10-02", "2027-02-19", "2027-02-19", "Kayıt"],
  ["assessment-results", "Ara sınav ve diğer ölçme sonuçları için son gün", "2026-12-20", "2026-12-20", "2027-05-14", "2027-05-14", "Sonuç"],
  ["classes-end", "Derslerin sona ermesi", "2026-12-20", "2026-12-20", "2027-05-14", "2027-05-14", "Ders"],
  ["finals", "Yarıyıl sonu sınavları", "2026-12-21", "2026-12-31", "2027-05-24", "2027-06-04", "Sınav"],
  ["final-results", "Yarıyıl sonu sınav sonuçları için son gün", "2027-01-04", "2027-01-04", "2027-06-07", "2027-06-07", "Sonuç"],
  ["resit-app", "Bütünleme sınavı başvuruları", "2027-01-02", "2027-01-09", "2027-06-05", "2027-06-12", "Başvuru"],
  ["resits", "Bütünleme sınavları", "2027-01-11", "2027-01-15", "2027-06-14", "2027-06-18", "Sınav"],
  ["resit-results", "Bütünleme sınav sonuçları için son gün", "2027-01-18", "2027-01-18", "2027-06-21", "2027-06-21", "Sonuç"],
];
for (const [id, title, fs, fe, ss, se, category] of semesterRows) {
  add(`general-${id}-fall`, title, fs, fe, "Genel", category, 4, "Güz yarıyılı; yarıyıllık eğitim veren birimler.");
  add(`general-${id}-spring`, title, ss, se, "Genel", category, 5, "Bahar yarıyılı; yarıyıllık eğitim veren birimler.");
}

// Uzaktan öğretim — PDF s.5-6 (sınav tarihleri dışındaki satırlar genel takvimle aynıdır)
const remoteRows = semesterRows.filter(([id]) => id !== "section-notice" && id !== "finals" && id !== "resits");
for (const [id, title, fs, fe, ss, se, category] of remoteRows) {
  add(`remote-${id}-fall`, title, fs, fe, "Uzaktan Eğitim", category, 5, "Güz yarıyılı; uzaktan öğretim diploma programları.");
  add(`remote-${id}-spring`, title, ss, se, "Uzaktan Eğitim", category, 5, "Bahar yarıyılı; uzaktan öğretim diploma programları.");
}
add("remote-finals-fall", "Yarıyıl sonu sınavları", "2026-12-26", "2026-12-27", "Uzaktan Eğitim", "Sınav", 5, "Güz yarıyılı.");
add("remote-finals-spring", "Yarıyıl sonu sınavları", "2027-05-29", "2027-05-30", "Uzaktan Eğitim", "Sınav", 5, "Bahar yarıyılı.");
one("remote-resits-fall", "Bütünleme sınavı", "2027-01-10", "Uzaktan Eğitim", "Sınav", 5, "Güz yarıyılı.");
one("remote-resits-spring", "Bütünleme sınavı", "2027-06-13", "Uzaktan Eğitim", "Sınav", 5, "Bahar yarıyılı.");

// Yaz okulu — PDF s.6
one("summer-assignments", "Ders görevlendirmeleri ve ders şubeleri için son gün", "2027-06-25", "Yaz Okulu", "Ders", 6, "Otomasyon sistemine giriş.");
one("summer-courses-announced", "Açılacak derslerin duyurulması", "2027-07-02", "Yaz Okulu", "Ders", 6);
add("summer-registration", "Öğrenim ücreti ve kesin kayıtlar", "2027-07-05", "2027-07-09", "Yaz Okulu", "Kayıt", 6);
add("summer-education", "Yaz okulu eğitim öğretim dönemi", "2027-07-12", "2027-08-13", "Yaz Okulu", "Ders", 6, "Beş hafta.");
one("summer-midterm-results", "Ara sınav sonuçları için son gün", "2027-08-13", "Yaz Okulu", "Sonuç", 6, "Otomasyon sistemine giriş.");
add("summer-finals", "Yaz okulu dönem sonu sınavları", "2027-08-16", "2027-08-20", "Yaz Okulu", "Sınav", 6);
one("summer-results", "Yaz okulu sınav sonuçları için son gün", "2027-08-27", "Yaz Okulu", "Sonuç", 6, "Otomasyon sistemine giriş.");

// 2547/44-c ek sınav takvimi — PDF s.6
const extraExamRows = [
  ["first-app", "1. ek sınav başvuruları", "2027-01-19", "2027-01-21", "2027-08-31", "2027-09-02", "Başvuru"],
  ["first-exam", "1. ek sınavlar", "2027-01-25", "2027-01-29", "2027-09-06", "2027-09-10", "Sınav"],
  ["first-results", "1. ek sınav sonuçları için son gün", "2027-02-01", "2027-02-01", "2027-09-13", "2027-09-13", "Sonuç"],
  ["second-app", "2. ek sınav başvuruları", "2027-02-02", "2027-02-04", "2027-09-14", "2027-09-16", "Başvuru"],
  ["second-exam", "2. ek sınavlar", "2027-02-08", "2027-02-12", "2027-09-20", "2027-09-24", "Sınav"],
  ["second-results", "2. ek sınav sonuçları için son gün", "2027-02-15", "2027-02-15", "2027-09-27", "2027-09-27", "Sonuç"],
  ["registration", "Ek sınavlar sonrası ders kayıtları", "2027-02-16", "2027-02-19", "2027-09-28", "2027-10-01", "Kayıt"],
];
for (const [id, title, fs, fe, ss, se, category] of extraExamRows) {
  add(`extra-${id}-fall`, title, fs, fe, "2547 Ek Sınav", category, 6, "Güz yarıyılı takvimi; ön lisans ve lisans programları.");
  add(`extra-${id}-spring`, title, ss, se, "2547 Ek Sınav", category, 6, "Bahar yarıyılı takvimi; ön lisans ve lisans programları.");
}

// Lisansüstü eğitim — PDF s.6-8
const instituteApplication = (prefix, label, fallStart, fallEnd, prelim, interviewStart, interviewEnd, result, mainStart, mainEnd, page) => {
  add(`${prefix}-application-fall`, "Lisansüstü yeni aday ve yatay geçiş başvuruları", fallStart, fallEnd, "Lisansüstü Eğitim", "Başvuru", page, `${label}; online, güz yarıyılı.`);
  add(`${prefix}-application-spring`, "Lisansüstü yeni aday ve yatay geçiş başvuruları", "2026-12-21", "2026-12-31", "Lisansüstü Eğitim", "Başvuru", page, `${label}; online, bahar yarıyılı.`);
  add(`${prefix}-correction-fall`, "Lisansüstü başvuru evrak düzeltme", "2026-07-28", "2026-07-29", "Lisansüstü Eğitim", "Başvuru", page, `${label}; güz yarıyılı.`);
  add(`${prefix}-correction-spring`, "Lisansüstü başvuru evrak düzeltme", "2027-01-04", "2027-01-05", "Lisansüstü Eğitim", "Başvuru", page, `${label}; bahar yarıyılı.`);
  one(`${prefix}-prelim-fall`, "Tezli yüksek lisans ön eleme sonuçları için son gün", prelim, "Lisansüstü Eğitim", "Sonuç", page, `${label}; güz yarıyılı.`);
  one(`${prefix}-prelim-spring`, "Tezli yüksek lisans ön eleme sonuçları için son gün", "2027-01-12", "Lisansüstü Eğitim", "Sonuç", page, `${label}; bahar yarıyılı.`);
  add(`${prefix}-interview-fall`, "Lisansüstü bilim ve mülakat sınavları", interviewStart, interviewEnd, "Lisansüstü Eğitim", "Sınav", page, `${label}; güz yarıyılı.`);
  add(`${prefix}-interview-spring`, "Lisansüstü bilim ve mülakat sınavları", "2027-01-14", "2027-01-15", "Lisansüstü Eğitim", "Sınav", page, `${label}; bahar yarıyılı.`);
  one(`${prefix}-results-fall`, "Lisansüstü sınav ve yatay geçiş sonuçları için son gün", result, "Lisansüstü Eğitim", "Sonuç", page, `${label}; güz yarıyılı.`);
  one(`${prefix}-results-spring`, "Lisansüstü sınav ve yatay geçiş sonuçları için son gün", "2027-01-22", "Lisansüstü Eğitim", "Sonuç", page, `${label}; bahar yarıyılı.`);
  add(`${prefix}-main-registration-fall`, "Lisansüstü asıl aday kesin kayıtları", mainStart, mainEnd, "Lisansüstü Eğitim", "Kayıt", page, `${label}; güz yarıyılı.`);
  add(`${prefix}-main-registration-spring`, "Lisansüstü asıl aday kesin kayıtları", "2027-01-25", "2027-01-29", "Lisansüstü Eğitim", "Kayıt", page, `${label}; bahar yarıyılı.`);
  add(`${prefix}-main-course-fall`, "Lisansüstü asıl aday ders kayıtları", prefix === "grad-fen" ? "2026-08-31" : "2026-09-07", prefix === "grad-fen" ? "2026-09-04" : "2026-09-11", "Lisansüstü Eğitim", "Kayıt", page, `${label}; güz yarıyılı.`);
  add(`${prefix}-main-course-spring`, "Lisansüstü asıl aday ders kayıtları", "2027-01-25", "2027-01-29", "Lisansüstü Eğitim", "Kayıt", page, `${label}; bahar yarıyılı.`);
  add(`${prefix}-reserve-registration-fall`, "Lisansüstü yedek aday kesin kayıtları", "2026-09-07", "2026-09-11", "Lisansüstü Eğitim", "Kayıt", page, `${label}; güz yarıyılı.`);
  add(`${prefix}-reserve-registration-spring`, "Lisansüstü yedek aday kesin kayıtları", "2027-02-01", "2027-02-03", "Lisansüstü Eğitim", "Kayıt", page, `${label}; bahar yarıyılı.`);
  add(`${prefix}-reserve-course-fall`, "Lisansüstü yedek aday ders kayıtları", "2026-09-07", "2026-09-11", "Lisansüstü Eğitim", "Kayıt", page, `${label}; güz yarıyılı.`);
  add(`${prefix}-reserve-course-spring`, "Lisansüstü yedek aday ders kayıtları", "2027-02-01", "2027-02-03", "Lisansüstü Eğitim", "Kayıt", page, `${label}; bahar yarıyılı.`);
};
instituteApplication("grad-common", "Sosyal, Eğitim, Akdeniz Uygarlıkları, Sağlık ve Organ Nakli enstitüleri", "2026-07-13", "2026-07-27", "2026-08-06", "2026-08-10", "2026-08-11", "2026-08-21", "2026-08-31", "2026-09-04", 6);
instituteApplication("grad-fen", "Fen Bilimleri Enstitüsü", "2026-07-06", "2026-07-24", "2026-08-06", "2026-08-10", "2026-08-11", "2026-08-21", "2026-08-31", "2026-09-04", 7);
instituteApplication("grad-fine", "Güzel Sanatlar Enstitüsü", "2026-08-03", "2026-08-14", "2026-08-21", "2026-08-26", "2026-08-27", "2026-08-31", "2026-09-01", "2026-09-04", 7);
const graduateRows = [
  ["assignments", "Ders görevlendirmeleri ve ders şubeleri için son gün", "2026-08-14", "2026-08-14", "2026-12-31", "2026-12-31", "Ders"],
  ["registration", "Katkı payı, öğrenim ücreti ve kayıt yenileme", "2026-09-07", "2026-09-11", "2027-01-25", "2027-01-29", "Kayıt"],
  ["advisor", "Danışman onayı", "2026-09-07", "2026-09-13", "2027-01-25", "2027-01-31", "Kayıt"],
  ["classes-start", "Derslerin başlaması", "2026-09-14", "2026-09-14", "2027-02-01", "2027-02-01", "Ders"],
  ["social", "Sosyal transkript etkinlik başvuruları", "2026-09-14", "2026-12-20", "2027-02-01", "2027-05-14", "Başvuru"],
  ["add-drop", "Ders ekleme-bırakma ve mazeretli ders kaydı", "2026-09-14", "2026-09-18", "2027-02-01", "2027-02-05", "Kayıt"],
  ["approval", "Ekle-bırak danışman onayı", "2026-09-14", "2026-09-20", "2027-02-01", "2027-02-07", "Kayıt"],
  ["classes-end", "Derslerin sona ermesi", "2026-12-20", "2026-12-20", "2027-05-14", "2027-05-14", "Ders"],
  ["project-submit", "Tezsiz yüksek lisans dönem projesi teslimi için son gün", "2027-01-08", "2027-01-08", "2027-05-24", "2027-05-24", "Proje"],
  ["project-evaluate", "Tezsiz yüksek lisans dönem projesi değerlendirmesi için son gün", "2027-01-18", "2027-01-18", "2027-06-04", "2027-06-04", "Proje"],
  ["thesis-submit", "Tez savunması öncesi tez teslimi için son gün", "2027-01-05", "2027-01-05", "2027-06-15", "2027-06-15", "Tez"],
  ["finals", "Yarıyıl sonu sınavları", "2026-12-21", "2026-12-31", "2027-05-24", "2027-06-04", "Sınav"],
  ["final-results", "Yarıyıl sonu sınav sonuçları için son gün", "2027-01-04", "2027-01-04", "2027-06-07", "2027-06-07", "Sonuç"],
  ["resit-app", "Bütünleme sınavı başvuruları", "2027-01-02", "2027-01-09", "2027-06-05", "2027-06-12", "Başvuru"],
  ["resits", "Bütünleme sınavları", "2027-01-11", "2027-01-15", "2027-06-14", "2027-06-18", "Sınav"],
  ["resit-results", "Bütünleme sınav sonuçları için son gün", "2027-01-18", "2027-01-18", "2027-06-21", "2027-06-21", "Sonuç"],
  ["doctorate", "Doktora yeterlik sınavları", "2026-09-14", "2027-01-22", "2027-02-01", "2027-06-25", "Sınav"],
];
for (const [id, title, fs, fe, ss, se, category] of graduateRows) {
  add(`graduate-${id}-fall`, title, fs, fe, "Lisansüstü Eğitim", category, 7, "Güz yarıyılı; tüm enstitüler.");
  add(`graduate-${id}-spring`, title, ss, se, "Lisansüstü Eğitim", category, 8, "Bahar yarıyılı; tüm enstitüler.");
}

// Özel yetenek — PDF s.8-9
add("fine-arts-application", "Özel yetenek sınavı başvuruları", "2026-08-03", "2026-08-14", "Güzel Sanatlar Fakültesi", "Başvuru", 8, "Online başvuru.");
add("fine-arts-exam", "Özel yetenek sınavı", "2026-08-18", "2026-08-20", "Güzel Sanatlar Fakültesi", "Sınav", 8);
one("fine-arts-results", "Özel yetenek sınav sonuçlarının duyurulması", "2026-08-21", "Güzel Sanatlar Fakültesi", "Sonuç", 8);
add("fine-arts-appeal", "Özel yetenek sınav sonucuna itiraz", "2026-08-24", "2026-08-25", "Güzel Sanatlar Fakültesi", "Başvuru", 8, "Sonuç ilanından itibaren iki iş günü.");
add("fine-arts-main-registration", "Özel yetenek asıl aday kesin kayıtları", "2026-08-31", "2026-09-02", "Güzel Sanatlar Fakültesi", "Kayıt", 8);
add("fine-arts-reserve-1", "Özel yetenek 1. yedek kesin kayıtları", "2026-09-03", "2026-09-04", "Güzel Sanatlar Fakültesi", "Kayıt", 8);
add("fine-arts-reserve-2", "Özel yetenek 2. yedek kesin kayıtları", "2026-09-07", "2026-09-08", "Güzel Sanatlar Fakültesi", "Kayıt", 8);
add("fine-arts-reserve-3", "Özel yetenek 3. yedek kesin kayıtları", "2026-09-09", "2026-09-10", "Güzel Sanatlar Fakültesi", "Kayıt", 8);
one("fine-arts-reserve-4", "Özel yetenek 4. yedek kesin kayıtları", "2026-09-11", "Güzel Sanatlar Fakültesi", "Kayıt", 8);
add("conservatory-application", "Konservatuvar özel yetenek başvuruları", "2026-07-27", "2026-08-07", "Antalya Devlet Konservatuvarı", "Başvuru", 8, "Online başvuru.");
add("conservatory-exams", "Konservatuvar özel yetenek sınavları", "2026-08-13", "2026-08-28", "Antalya Devlet Konservatuvarı", "Sınav", 8);
add("conservatory-main-registration", "Konservatuvar asıl aday kesin kayıtları", "2026-09-02", "2026-09-09", "Antalya Devlet Konservatuvarı", "Kayıt", 8);
add("conservatory-reserve-registration", "Konservatuvar yedek aday kesin kayıtları", "2026-09-10", "2026-09-11", "Antalya Devlet Konservatuvarı", "Kayıt", 8);
add("conservatory-parttime-prereg", "Yarı zamanlı müzik ve bale ön kayıtları", "2026-07-27", "2026-08-07", "Antalya Devlet Konservatuvarı", "Başvuru", 8);
one("conservatory-music-stage1", "Müzik bölümü özel yetenek sınavı", "2026-08-17", "Antalya Devlet Konservatuvarı", "Sınav", 8, "I. aşama.");
one("conservatory-music-stage2", "Müzik bölümü özel yetenek sınavı", "2026-08-18", "Antalya Devlet Konservatuvarı", "Sınav", 8, "II. aşama.");
one("conservatory-music-results", "Müzik bölümü sınav sonuçlarının duyurulması", "2026-08-21", "Antalya Devlet Konservatuvarı", "Sonuç", 8);
add("conservatory-music-main-registration", "Müzik bölümü asıl aday kayıtları", "2026-08-24", "2026-08-28", "Antalya Devlet Konservatuvarı", "Kayıt", 9);
add("conservatory-music-reserve-registration", "Müzik bölümü yedek aday kayıtları", "2026-09-02", "2026-09-04", "Antalya Devlet Konservatuvarı", "Kayıt", 9);
one("conservatory-ballet-exam", "Bale ana sanat dalı özel yetenek sınavı", "2026-08-24", "Antalya Devlet Konservatuvarı", "Sınav", 9, "Baraj ve kesin kabul aşamaları.");
one("conservatory-ballet-results", "Bale ana sanat dalı sınav sonuçlarının duyurulması", "2026-08-28", "Antalya Devlet Konservatuvarı", "Sonuç", 9);
add("conservatory-ballet-main-registration", "Bale ana sanat dalı asıl aday kayıtları", "2026-08-31", "2026-09-04", "Antalya Devlet Konservatuvarı", "Kayıt", 9);
add("conservatory-ballet-reserve-registration", "Bale ana sanat dalı yedek aday kayıtları", "2026-09-07", "2026-09-08", "Antalya Devlet Konservatuvarı", "Kayıt", 9);
add("arts-highschool-prereg", "Müzik ve Sahne Sanatları Lisesi ön kayıtları", "2026-07-27", "2026-08-07", "Antalya Devlet Konservatuvarı", "Başvuru", 9);
one("arts-highschool-music-exam", "Müzik bölümü özel yetenek sınavı", "2026-08-19", "Antalya Devlet Konservatuvarı", "Sınav", 9, "Müzik ve Sahne Sanatları Lisesi.");
one("arts-highschool-music-results", "Müzik bölümü sınav sonuçlarının duyurulması", "2026-08-21", "Antalya Devlet Konservatuvarı", "Sonuç", 9, "Müzik ve Sahne Sanatları Lisesi.");
one("arts-highschool-ballet-exam", "Bale ana sanat dalı özel yetenek sınavı", "2026-08-21", "Antalya Devlet Konservatuvarı", "Sınav", 9, "Müzik ve Sahne Sanatları Lisesi.");
one("arts-highschool-ballet-results", "Bale ana sanat dalı sınav sonuçlarının duyurulması", "2026-08-24", "Antalya Devlet Konservatuvarı", "Sonuç", 9, "Müzik ve Sahne Sanatları Lisesi.");
add("arts-highschool-main-registration", "Müzik ve Sahne Sanatları Lisesi asıl aday kayıtları", "2026-08-25", "2026-08-28", "Antalya Devlet Konservatuvarı", "Kayıt", 9);
add("arts-highschool-reserve-registration", "Müzik ve Sahne Sanatları Lisesi yedek aday kayıtları", "2026-09-01", "2026-09-03", "Antalya Devlet Konservatuvarı", "Kayıt", 9);

// Yatay geçiş, değişim, çift anadal/yandal — PDF s.9-10
add("transfer-ek1-application", "EK Madde 1 yatay geçiş başvuruları", "2026-08-01", "2026-08-15", "Yatay Geçiş ve ÇAP", "Başvuru", 9, "Tıp Fakültesi hariç ön lisans ve lisans birimleri.");
add("transfer-ek1-correction", "EK Madde 1 yatay geçiş evrak düzeltme", "2026-08-17", "2026-08-18", "Yatay Geçiş ve ÇAP", "Başvuru", 9);
one("transfer-ek1-results", "EK Madde 1 yatay geçiş sonuçlarının duyurulması", "2026-08-28", "Yatay Geçiş ve ÇAP", "Sonuç", 9);
add("transfer-ek1-registration", "EK Madde 1 yatay geçiş asıl aday kayıtları", "2026-08-31", "2026-09-04", "Yatay Geçiş ve ÇAP", "Kayıt", 9);
const ganoRows = [
  ["application", "GANO ile yatay geçiş başvuruları", "2026-08-01", "2026-08-15", "2027-01-04", "2027-01-15", "Başvuru"],
  ["correction", "GANO ile yatay geçiş evrak düzeltme", "2026-08-17", "2026-08-18", "2027-01-18", "2027-01-19", "Başvuru"],
  ["results", "GANO ile yatay geçiş sonuçlarının duyurulması", "2026-08-28", "2026-08-28", "2027-01-22", "2027-01-22", "Sonuç"],
  ["main", "GANO ile yatay geçiş asıl aday kayıtları", "2026-08-31", "2026-09-04", "2027-01-25", "2027-01-27", "Kayıt"],
  ["reserve", "GANO ile yatay geçiş yedek aday kayıtları", "2026-09-07", "2026-09-11", "2027-01-28", "2027-01-29", "Kayıt"],
];
for (const [id, title, fs, fe, ss, se, category] of ganoRows) {
  add(`transfer-gano-${id}-fall`, title, fs, fe, "Yatay Geçiş ve ÇAP", category, 9, "Güz yarıyılı; Tıp Fakültesi hariç ön lisans ve lisans birimleri.");
  add(`transfer-gano-${id}-spring`, title, ss, se, "Yatay Geçiş ve ÇAP", category, 9, "Bahar yarıyılı; ön lisans birimleri.");
}
one("exchange-free-mover-fall", "Free Mover gelen öğrenci başvurusu için son gün", "2026-08-29", "Değişim Programları", "Başvuru", 10, "Güz yarıyılı.");
one("exchange-free-mover-spring", "Free Mover gelen öğrenci başvurusu için son gün", "2026-12-31", "Değişim Programları", "Başvuru", 10, "Bahar yarıyılı.");
add("double-major-application-fall", "Çift anadal ve yandal başvuruları", "2026-09-07", "2026-09-11", "Yatay Geçiş ve ÇAP", "Başvuru", 10, "Güz yarıyılı.");
add("double-major-application-spring", "Çift anadal ve yandal başvuruları", "2027-01-25", "2027-01-29", "Yatay Geçiş ve ÇAP", "Başvuru", 10, "Bahar yarıyılı.");
one("double-major-results-fall", "Çift anadal ve yandal sonuçlarının duyurulması", "2026-09-14", "Yatay Geçiş ve ÇAP", "Sonuç", 10, "Güz yarıyılı.");
one("double-major-results-spring", "Çift anadal ve yandal sonuçlarının duyurulması", "2027-02-01", "Yatay Geçiş ve ÇAP", "Sonuç", 10, "Bahar yarıyılı.");
add("double-major-registration-fall", "Çift anadal ve yandal kesin kayıtları", "2026-09-15", "2026-09-18", "Yatay Geçiş ve ÇAP", "Kayıt", 10, "Güz yarıyılı.");
add("double-major-registration-spring", "Çift anadal ve yandal kesin kayıtları", "2027-02-02", "2027-02-05", "Yatay Geçiş ve ÇAP", "Kayıt", 10, "Bahar yarıyılı.");

// Resmî tatiller — PDF s.10
one("holiday-republic-eve", "Cumhuriyet Bayramı arifesi", "2026-10-28", "Genel", "Tatil", 10, "Öğleden sonra resmî tatil.");
one("holiday-republic", "Cumhuriyet Bayramı", "2026-10-29", "Genel", "Tatil", 10);
one("holiday-new-year", "Yılbaşı", "2027-01-01", "Genel", "Tatil", 10);
one("holiday-ramadan-eve", "Ramazan Bayramı arifesi", "2027-03-08", "Genel", "Tatil", 10, "Öğleden sonra resmî tatil.");
add("holiday-ramadan", "Ramazan Bayramı", "2027-03-09", "2027-03-11", "Genel", "Tatil", 10);
one("holiday-april23", "Ulusal Egemenlik ve Çocuk Bayramı", "2027-04-23", "Genel", "Tatil", 10);
one("holiday-may1", "Emek ve Dayanışma Günü", "2027-05-01", "Genel", "Tatil", 10);
one("holiday-sacrifice-eve", "Kurban Bayramı arifesi", "2027-05-15", "Genel", "Tatil", 10, "Öğleden sonra resmî tatil.");
add("holiday-sacrifice", "Kurban Bayramı", "2027-05-16", "2027-05-19", "Genel", "Tatil", 10);
one("holiday-may19", "Atatürk'ü Anma, Gençlik ve Spor Bayramı", "2027-05-19", "Genel", "Tatil", 10);
one("holiday-july15", "Demokrasi ve Millî Birlik Günü", "2027-07-15", "Genel", "Tatil", 10);
one("holiday-august30", "Zafer Bayramı", "2027-08-30", "Genel", "Tatil", 10);

// Kullanıcıya gösterilen başlıklar PDF'deki etkinlik metinleridir. Satır sonları
// PDF yerleşiminden kaynaklandığı için tek boşluğa indirilir; sözcükler değiştirilmez.
const exactById = new Map();
const exact = (ids, title) => ids.split(" ").forEach((id) => exactById.set(id, title));

exact("prep-transfer-placement", "Yatay Geçiş ile Gelen Öğrencilerin Hazırlık Programı Yabancı Dil Düzey Belirleme ve Yeterlik Sınavı");
exact("prep-transfer-results", "Yatay Geçiş ile Gelen Öğrencilerin Hazırlık Programı Yabancı Dil Düzey Belirleme ve Yeterlik Sınavı Sonuçlarının Duyurulması");
exact("prep-placement-written", "Hazırlık Programı Yabancı Dil Düzey Belirleme ve Yeterlik Sınavı (Yazma ve Test Bölümü)");
exact("prep-placement-speaking", "Hazırlık Programı Yabancı Dil Düzey Belirleme ve Yeterlik Sınavı (Konuşma Bölümü)");
exact("prep-placement-results", "Hazırlık Programı Yabancı Dil Düzey Belirleme ve Yeterlik Sınavı Sonuçlarının Duyurulması");
exact("prep-year-end-exam", "Yabancı Dil Hazırlık Sınıfları için Yıl Sonu Sınavı");
exact("prep-year-end-results", "Yabancı Dil Hazırlık Sınıfları için Yıl Sonu Sınavı Sonuçlarının Otomasyon Sistemine Girilmesinin Son Tarihi");
exact("prep-second-exam", "Yabancı Dil Hazırlık Sınıfları için Yıl/Yarıyıl Sonu İkinci Sınavı");
exact("prep-second-results", "Yabancı Dil Hazırlık Sınıfları için Yıl/Yarıyıl Sonu İkinci Sınavı Sonuçlarının Otomasyon Sistemine Girilmesinin Son Tarihi");

exact("medicine-transfer-app", "Dönem 1 (*),2,3,4,5 ve 6 (*) için Yatay Geçiş Başvuru Tarihleri");
exact("medicine-transfer-correction", "Yatay Geçiş Başvurularında Eksik veya Yanlış Evrak Düzeltme Tarihi");
exact("medicine-transfer-results", "Dönem 1 (*),2,3,4,5 ve 6 (*) için Yatay Geçiş Sonuçlarının Duyurulması");
exact("medicine-transfer-registration", "Dönem 1,2,3,4,5 ve 6 için Yatay Geçiş Kesin Kayıt Tarihleri");
exact("medicine-transfer-reserve-d45", "Dönem 4 ve 5 için Yatay Geçiş Yedek Kayıt Tarihleri");
exact("medicine-transfer-reserve-d23", "Dönem 2 ve 3 için Yatay Geçiş Yedek Kayıt Tarihleri");
exact("medicine-d6-special", "Özel Öğrenci (Gelen ve Giden) Son Başvuru Tarihi");
exact("medicine-d45-special medicine-d123-special", "Özel Öğrenci (Gelen ve Giden) Son Başvuru Tarihi");
exact("medicine-d6-registration medicine-d45-registration medicine-d123-registration", "Öğrenci Katkı Payı/Öğrenim Ücretleri 1. Taksitleri Yatırma Süresi ve Ders Kayıtları");
exact("medicine-d6-advisor medicine-d45-advisor medicine-d123-advisor", "Danışman Onayı");
exact("medicine-d6-leave medicine-d45-leave medicine-d123-leave", "Öğrenime Ara İzni Başvurusu için Son Gün");
exact("medicine-d6-social medicine-d45-social medicine-d123-social", "Sosyal Transkript Etkinlik Başvuru Tarihleri");
exact("medicine-d6-add-drop medicine-d45-add-drop medicine-d123-add-drop", "Ders Bırakma ve Ders Ekleme Süresi (Ekle-Çıkar)/Mazeretli Ders Kaydı");
exact("medicine-d6-add-drop-approval medicine-d45-add-drop-approval medicine-d123-add-drop-approval", "Ders Bırakma ve Ders Ekleme (Ekle-Çıkar) Danışman Onayı");
exact("medicine-second-installment", "Katkı Payı/Öğrenim Ücretleri 2. Taksitlerinin Yatırılma Süresi");

const dentistryExact = {
  special: "Özel Öğrenci (Gelen ve Giden) Başvurularının Son Günü",
  "registration-fall": "Katkı Payı/Öğrenim Ücretleri Yatırma Süresi ve Ders Kayıtları",
  "registration-spring": "Katkı Payı/Öğrenim Ücretleri Yatırma Süresi ve Ders Kayıtları",
  "advisor-fall": "Danışman Onayı", "advisor-spring": "Danışman Onayı",
  leave: "Öğrenime Ara İzni Başvurusu İçin Son Gün",
  "start-fall": "Derslerin Başlaması", "start-spring": "Derslerin Başlaması",
  social: "Sosyal Transkript Etkinlik Başvuru Tarihleri",
  "add-drop-fall": "Ders Bırakma ve Ders Ekleme Süresi (Ekle-Çıkar)/Mazeretli Ders Kaydı",
  "add-drop-spring": "Ders Bırakma ve Ders Ekleme Süresi (Ekle-Çıkar)/Mazeretli Ders Kaydı",
  "approval-fall": "Ders Bırakma ve Ders Ekleme (Ekle-Çıkar) Danışman Onayı",
  "approval-spring": "Ders Bırakma ve Ders Ekleme (Ekle-Çıkar) Danışman Onayı",
  "midterm-fall": "Ara Sınav Tarihleri", "midterm-spring": "Ara Sınav Tarihleri",
  "midterm-results-fall": "Ara Sınav Sonuçlarının Otomasyona Girişinin Son Günü",
  "midterm-results-spring": "Ara Sınav Sonuçlarının Otomasyona Girişinin Son Günü",
  "makeup-fall": "Mazeret Sınavları", "makeup-spring": "Mazeret Sınavları",
  "end-fall": "Derslerin Sona Ermesi", "end-spring": "Derslerin Sona Ermesi",
  "final-fall": "Yıl/Yarıyıl Sonu Sınavları", "final-spring": "Yıl/Yarıyıl Sonu Sınavları",
  "final-results-fall": "Yıl/Yarıyıl Sonu Sınav Sonuçlarının Otomasyona Girişinin Son Günü",
  "final-results-spring": "Yıl/Yarıyıl Sonu Sınav Sonuçlarının Otomasyona Girişinin Son Günü",
};
for (const scope of ["d123", "d45"]) for (const [suffix, title] of Object.entries(dentistryExact)) exact(`dentistry-${scope}-${suffix}`, title);
exact("dentistry-d123-second-exam dentistry-d45-second-exam", "Yıl Sonu İkinci Sınavları");
exact("dentistry-d123-second-results", "Yıl Sonu İkinci Sınav Sonuçlarının Otomasyona Girişinin Son Günü");
exact("dentistry-d45-second-results", "Yıl Sonu İkinci Sınavlarının Otomasyon Sistemine Girilmesinin Son Günü");
exact("dentistry-d123-single-course dentistry-d45-single-course", "Tek Ders Sınıf Geçme Sınavı");
exact("dentistry-d123-single-course-results dentistry-d45-single-course-results", "Tek Ders Sınıf Geçme Sınavı Sonuçlarının Otomasyona Girişinin Son Günü");
exact("dentistry-clinical-start-fall dentistry-clinical-start-spring", "Klinik Eğitim Başlaması");
exact("dentistry-clinical-d4-end-fall dentistry-clinical-d4-end-spring", "Klinik Eğitim Sona Ermesi (4. Sınıf)");
exact("dentistry-clinical-d5-end", "Klinik Eğitim Sona Ermesi (5. Sınıf)");
exact("dentistry-clinical-d4-repeat", "4. Sınıf Tek Klinik Eğitim Tekrarı Başlaması ve Sona Ermesi");
exact("dentistry-clinical-d5-repeat", "5. Sınıf Tek Klinik Eğitim Tekrarı Başlaması ve Sona Ermesi");

const semesterExact = {
  assignments: "Ders Görevlendirmelerinin Rektörlüğe Bildirilmesinin Son Günü",
  special: "Özel Öğrenci Başvurusu (gelen ve giden) İçin Son Gün",
  registration: "Katkı Payı/Öğrenim Ücretleri Yatırma ve Kayıt Yenileme Süresi",
  advisor: "Danışman Onayı", leave: "Öğrenime Ara İzni Başvurusu İçin Son Gün",
  "classes-start": "Derslerin Başlaması", social: "Sosyal Transkript Etkinlik Başvuru Tarihleri",
  "section-notice": "Kapatılacak veya Şubelere Ayrılan Derslerin Rektörlüğe Bildirilmesinin Son Günü*",
  "add-drop": "Ders Bırakma ve Ders Ekleme Süresi (Ekle-Çıkar) / Mazeretli Ders Kaydı",
  approval: "Ders Bırakma ve Ders Ekleme (Ekle-Çıkar) Danışman Onayı",
  withdraw: "Dersten Çekilmenin Son Günü",
  "assessment-results": "Ara Sınav Sonuçlarının ve Diğer Yıl/Yarıyıl İçi Ölçme Araçları Sonuçlarının Otomasyon Sistemine Girilmesinin Son Tarihi",
  "classes-end": "Derslerin Sona Ermesi", finals: "Yarıyıl Sonu Sınavları",
  "final-results": "Yarıyıl Sonu Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
  "resit-app": "Yıl/Yarıyıl Sonu İkinci Sınavı (Bütünleme) Başvuru Tarihleri",
  resits: "Yıl/Yarıyıl Sonu İkinci Sınavı (Bütünleme) Tarihleri",
  "resit-results": "Yıl/Yarıyıl Sonu İkinci Sınavı (Bütünleme) Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
};
for (const group of ["general", "remote"]) for (const [key, title] of Object.entries(semesterExact)) for (const term of ["fall", "spring"]) exact(`${group}-${key}-${term}`, title);

const lawExact = {
  special: "Özel Öğrenci (Gelen ve Giden) Başvurusu İçin Son Gün",
  "registration-fall": "Katkı Payı/Öğrenim Ücretleri Yatırma ve Kayıt Yenileme Süresi",
  "advisor-fall": "Danışman Onayı", "start-fall": "Derslerin Başlaması",
  "social-fall": "Sosyal Transkript Etkinlik Başvuru Tarihleri",
  "add-drop-fall": "Ders Bırakma ve Ders Ekleme Süresi (Ekle-Çıkar)/Mazeretli Ders Kaydı",
  "approval-fall": "Ders Bırakma ve Ders Ekleme (Ekle-Çıkar) Danışman Onayı",
  "withdraw-fall": "Dersten Çekilmenin Son Günü",
  "assessment-results-fall": "Yarıyıllık (Dönemlik) Derslerin Ara Sınav Sonuçlarının ve Diğer Yıl/Yarıyıl İçi Ölçme Araçları Sonuçlarının Otomasyon Sistemine Girilmesinin Son Tarihi",
  "end-fall": "Derslerin Sona Ermesi",
  "finals-fall": "Yıllık Derslerin Ara Sınavları- Yarıyıllık (Dönemlik) Derslerin Yarıyıl Sonu Sınavları (3 Hafta)",
  "final-results-fall": "Yarıyıllık (Dönemlik) Derslerin Yarıyıl Sonu Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
  "resit-app-fall": "Yarıyıllık (Dönemlik) Derslerin Yarıyıl Sonu İkinci Sınavı (Bütünleme) Başvuru Tarihleri",
  "resits-fall": "Yarıyıllık (Dönemlik) Derslerin Yarıyıl Sonu İkinci Sınavı (Bütünleme) Tarihleri",
  "resit-results-fall": "Yarıyıllık (Dönemlik) Derslerin Yarıyıl Sonu İkinci Sınavı (Bütünleme) Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
  "annual-makeup": "Yıllık Derslerin Ara Sınavlarının Mazeret Sınavları ve Sonuçlarının Otomasyon Sistemine Girilmesi",
  "registration-spring": "Katkı Payı/Öğrenim Ücretleri Yatırma ve Kayıt Yenileme Süresi",
  "advisor-spring": "Danışman Onayı", "start-spring": "Derslerin Başlaması",
  "social-spring": "Sosyal Transkript Etkinlik Başvuru Tarihleri",
  "annual-midterm-results": "Yıllık Derslerin Ara Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
  "assessment-results-spring": "Yarıyıllık (Dönemlik) Derslerin Ara Sınav ve Diğer Yıl/Yarıyıl İçi Ölçme Araçları Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
  "end-spring": "Derslerin Sona Ermesi",
  "finals-spring": "Yıllık ve Yarıyıllık (Dönemlik) Derslerin Yıl/yarıyıl Sonu Sınavı Tarihleri (3 Hafta)",
  "final-results-spring": "Yıllık ve Yarıyıllık (Dönemlik) Derslerin Yıl/yarıyıl Sonu Sınavı Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
  "resit-app-spring": "Yıl/Yarıyıl Sonu İkinci Sınavı (Bütünleme) Başvuru Tarihleri",
  "resits-spring": "Yıllık ve Yarıyıllık (Dönemlik) Derslerin Yıl/yarıyıl Sonu İkinci Sınavı Tarihleri",
  "resit-results-spring": "Yıllık ve Yarıyıllık (Dönemlik) Derslerin Yıl/yarıyıl Sonu İkinci Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
};
for (const [key, title] of Object.entries(lawExact)) exact(`law-${key}`, title);

const graduateExact = {
  assignments: "Ders Görevlendirmelerinin Rektörlüğe Bildirilmesi ve Ders Şubelerinin Otomasyon Sistemine Girilmesinin Son Günü",
  registration: "Katkı Payı/Öğrenim Ücretleri Yatırma ve Kayıt Yenileme Süresi", advisor: "Danışman Onayı",
  "classes-start": "Derslerin Başlaması", social: "Sosyal Transkript Etkinlik Başvuru Tarihleri",
  "add-drop": "Ders Bırakma ve Ders Ekleme Süresi (Ekle-Çıkar)/Mazeretli Ders Kaydı",
  approval: "Ders Bırakma ve Ders Ekleme (Ekle-Çıkar) Danışman Onayı", "classes-end": "Derslerin Sona Ermesi",
  "project-submit": "Tezsiz Yüksek Lisans Dönem Projelerinin Ana Bilim Dalı Dönem Projesi Komisyonuna Tesliminin Son Günü",
  "project-evaluate": "Tezsiz Yüksek Lisans Dönem Projelerinin Dönem Projesi Komisyonu Tarafından Değerlendirilmesinin ve Enstitüye Tesliminin Son Günü",
  "thesis-submit": "Tez Savunma Öncesi Tezlerin Enstitüye Tesliminin Son Günü", finals: "Yarıyıl Sonu Sınav Tarihleri",
  "final-results": "Yarıyıl Sonu Sınavları Sonuçların Otomasyon Sistemine Girilmesinin Son Günü",
  "resit-app": "Yıl/Yarıyıl Sonu İkinci Sınavı (Bütünleme) Başvuru Tarihleri",
  resits: "Yıl/Yarıyıl Sonu İkinci Sınav Tarihleri",
  "resit-results": "Yıl/Yarıyıl Sonu İkinci Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü",
  doctorate: "Doktora Yeterlik Sınavı Tarihleri",
};
for (const [key, title] of Object.entries(graduateExact)) for (const term of ["fall", "spring"]) exact(`graduate-${key}-${term}`, title);

exact("fine-arts-application", "Başvuru Tarihleri (Online)");
exact("fine-arts-exam", "Sınav Tarihi");
exact("fine-arts-results", "Sınav Sonuçlarının Duyurulması");
exact("fine-arts-appeal", "Sınav Sonuçlarına İtiraz (Sınav Sonuçlarının İlanından İtibaren 2(iki) İş Günü)");
exact("fine-arts-main-registration", "Asıl Adayların Kesin Kayıt Tarihleri");
exact("fine-arts-reserve-1", "Yedek Adayların Kesin Kayıt Tarihleri (1. Yedek)");
exact("fine-arts-reserve-2", "Yedek Adayların Kesin Kayıt Tarihleri (2. Yedek)");
exact("fine-arts-reserve-3", "Yedek Adayların Kesin Kayıt Tarihleri (3. Yedek)");
exact("fine-arts-reserve-4", "Yedek Adayların Kesin Kayıt Tarihleri (4. Yedek)");
exact("conservatory-application", "Başvuru Tarihleri (Online)");
exact("conservatory-exams", "Sınav Tarihleri");
exact("conservatory-main-registration conservatory-music-main-registration conservatory-ballet-main-registration arts-highschool-main-registration", "Asıl Adayların Kesin Kayıt Tarihleri");
exact("conservatory-reserve-registration conservatory-music-reserve-registration conservatory-ballet-reserve-registration arts-highschool-reserve-registration", "Yedek Adayların Kesin Kayıt Tarihleri");
exact("conservatory-parttime-prereg arts-highschool-prereg", "Ön Kayıt Tarihleri");
exact("conservatory-music-stage1 conservatory-music-stage2", "Müzik Bölümü Özel Yetenek Sınav Tarihleri");
exact("conservatory-music-results arts-highschool-music-results", "Müzik Bölümü Sınav Sonuçlarının Duyurulması");
exact("conservatory-ballet-exam", "Bale Ana Sanat Dalı Özel Yetenek Sınav Tarihi (1. aşama-baraj aşaması-2.aşama-kesin kabul aşaması)");
exact("conservatory-ballet-results arts-highschool-ballet-results", "Bale Ana Sanat Dalı Sınav Sonuçlarının Duyurulması");
exact("arts-highschool-music-exam", "Müzik Bölümü Özel Yetenek Sınav Tarihi");
exact("arts-highschool-ballet-exam", "Bale Ana Sanat Dalı Özel Yetenek Sınav Tarihi");

exact("transfer-ek1-application transfer-gano-application-fall transfer-gano-application-spring", "Yatay Geçiş Başvuru Tarihleri");
exact("transfer-ek1-correction transfer-gano-correction-fall transfer-gano-correction-spring", "Yatay Geçiş Başvurularında Eksik veya Yanlış Evrak Düzeltme Tarihleri");
exact("transfer-ek1-results transfer-gano-results-fall transfer-gano-results-spring", "Yatay Geçiş Sonuçlarının Birimlerce Duyurulması");
exact("transfer-ek1-registration transfer-gano-main-fall transfer-gano-main-spring", "Yatay Geçiş Başvurusu Kabul Edilen Asıl Adayların Kesin Kayıt Tarihleri");
exact("transfer-gano-reserve-fall transfer-gano-reserve-spring", "Yatay Geçiş Başvurusu Kabul Edilen Yedek Adayların Kesin Kayıt Tarihleri (Yedek adaylar ilgili birim tarafından ilan edilecek sırayla çağrılır)");
exact("summer-assignments", "Ders Görevlendirmelerinin Rektörlüğe Bildirilmesi ve Ders Şubelerinin Otomasyon Sistemine Girilmesinin Son Günü");
for (const prefix of ["grad-common", "grad-fen", "grad-fine"]) {
  exact(`${prefix}-reserve-course-fall ${prefix}-reserve-course-spring`, "Yeni Kazanan Yedek Adayların ve Yatay Geçiş ile Kabul Edilen Yedek Adayların Ders Kayıt Tarihleri");
}
exact("remote-add-drop-fall remote-add-drop-spring", "Ders Bırakma ve Ders Ekleme Süresi (Ekle-Çıkar)/Mazeretli Ders Kaydı");

const simpleExact = new Map([
  ["1. ek sınav başvuruları", "1. Ek Sınav Başvuru tarihleri"], ["1. ek sınavlar", "1. Ek Sınav tarihleri"],
  ["1. ek sınav sonuçları için son gün", "1. Ek Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü"],
  ["2. ek sınav başvuruları", "2. Ek Sınav Başvuru tarihleri"], ["2. ek sınavlar", "2. Ek Sınav Tarihleri"],
  ["2. ek sınav sonuçları için son gün", "2. Ek Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü"],
  ["Ek sınavlar sonrası ders kayıtları", "Ek Sınavlar Sonrası Ders Kayıt Tarihleri"],
  ["Lisansüstü yeni aday ve yatay geçiş başvuruları", "Yeni Adayların ve Yatay Geçiş Başvurusu Yapacak Adaylar için Başvuru Tarihleri (Online)"],
  ["Lisansüstü başvuru evrak düzeltme", "Başvurularda Eksik veya Yanlış Evrak Düzeltme Tarihi"],
  ["Tezli yüksek lisans ön eleme sonuçları için son gün", "Tezli Yüksek Lisans Ön Eleme Sonuçlarının Duyurulması Son Günü"],
  ["Lisansüstü bilim ve mülakat sınavları", "Yeni Adayların Bilim / Mülakat Sınavı Tarihleri"],
  ["Lisansüstü sınav ve yatay geçiş sonuçları için son gün", "Yeni Adayların Sınav Sonuçları ile Yatay Geçiş Başvuru Sonuçlarının Duyurulmasının Son Günü"],
  ["Lisansüstü asıl aday kesin kayıtları", "Yeni Kazanan Asıl Adayların ve Yatay Geçiş ile Kabul Edilen Asıl Adayların Kesin Kayıt Tarihleri"],
  ["Lisansüstü asıl aday ders kayıtları", "Yeni Kazanan Asıl Adayların ve Yatay Geçiş ile Kabul Edilen Asıl Adayların Ders Kayıt Tarihleri"],
  ["Lisansüstü yedek aday kesin kayıtları", "Yeni Kazanan Yedek Adayların ve Yatay Geçiş ile Kabul Edilen Yedek Adayların Kesin Kayıt Tarihleri"],
  ["Açılacak derslerin duyurulması", "Açılacak Derslerin Duyurulması"],
  ["Öğrenim ücreti ve kesin kayıtlar", "Öğrenim Ücreti Yatırılması ve Kesin Kayıt Tarihleri"],
  ["Yaz okulu eğitim öğretim dönemi", "Eğitim Öğretim Dönemi (5 Hafta)"],
  ["Ara sınav sonuçları için son gün", "Ara Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü"],
  ["Yaz okulu dönem sonu sınavları", "Dönem Sonu Sınav Tarihleri"],
  ["Yaz okulu sınav sonuçları için son gün", "Sınav Sonuçlarının Otomasyon Sistemine Girilmesinin Son Günü"],
  ["Çift anadal ve yandal başvuruları", "Başvuru Tarihleri"],
  ["Çift anadal ve yandal sonuçlarının duyurulması", "Başvurusu Kabul Edilen Öğrencilerin Birimlerce Duyurulması"],
  ["Çift anadal ve yandal kesin kayıtları", "Kesin Kayıt Tarihleri"],
  ["Free Mover gelen öğrenci başvurusu için son gün", "Free Mover Programı Gelen Öğrenci Başvuru Tarihleri"],
  ["Cumhuriyet Bayramı arifesi", "Cumhuriyet Bayramı Yarım gün"], ["Ramazan Bayramı arifesi", "Ramazan Bayramı Arefe Yarım Gün"],
  ["Kurban Bayramı arifesi", "Kurban Bayramı Arefe Yarım Gün"], ["Demokrasi ve Millî Birlik Günü", "Demokrasi ve Milli Birlik Günü"],
]);
for (const event of events) event.title = exactById.get(event.id) ?? simpleExact.get(event.title) ?? event.title;

const seen = new Set();
for (const event of events) {
  if (seen.has(event.id)) throw new Error(`Tekrarlanan kayıt kimliği: ${event.id}`);
  if (event.startDate > event.endDate) throw new Error(`Geçersiz tarih aralığı: ${event.id}`);
  seen.add(event.id);
}
events.sort((a, b) => a.startDate.localeCompare(b.startDate) || a.faculty.localeCompare(b.faculty) || a.title.localeCompare(b.title));
const outputPath = resolve(process.cwd(), "data/academic-calendar-2026-2027.json");
await writeFile(outputPath, `${JSON.stringify(events, null, 2)}\n`, "utf8");
console.log(`${events.length} akademik takvim kaydı üretildi.`);
