// Trimmed from etkinlik.akdeniz.edu.tr (10.11.2026) so parser tests run offline.
export const akdenizListHtml = `<html><body><main>
<div class="event-card" data-category="Gezi">
                <div class="event-image-container">
                    <img src="/static/uploads/default.jpg" class="event-image" alt="Etkinlik Afişi">
                </div>
                <div class="card-content">
                    <div>
                        <div class="community-info">Akdeniz ve Türk Uygarlıkları Araştırmaları Topluluğu</div>
                        <h3>SILLYON-PERGE ANTIK KENT GEZISI</h3>
                        <div class="card-details">
                            <i class="fas fa-map-marker-alt"></i> Sillyon ve Perge Antik kentleri<br>
                            <i class="far fa-clock"></i> 13.11.2026 | 08:30
                        </div>
                    </div>
                    <a href="/detay/149" class="btn-view">Detayları Gör</a>
                </div>
            </div>
<div class="event-card" data-category="Topluluğa Özgü Faaliyet">
                <div class="event-image-container">
                    <img src="/static/uploads/default.jpg" class="event-image" alt="Etkinlik Afişi">
                </div>
                <div class="card-content">
                    <div>
                        <div class="community-info">Akdeniz Üniversitesi Motosiklet Topluluğu</div>
                        <h3>LÖSEV FARKINDALIK KORTEJI</h3>
                        <div class="card-details">
                            <i class="fas fa-map-marker-alt"></i> Antalya Şehir Merkezi-Üç Kapılar-Yalım Park<br>
                            <i class="far fa-clock"></i> 18.11.2026 | 12:00
                        </div>
                    </div>
                    <a href="/detay/162" class="btn-view">Detayları Gör</a>
                </div>
            </div>
<div class="event-card" data-category="Bisiklet Turu">
                <div class="event-image-container">
                    <img src="/static/uploads/770dc52b_cakrlar_tur_postu.jpeg" class="event-image" alt="Etkinlik Afişi">
                </div>
                <div class="card-content">
                    <div>
                        <div class="community-info">Bisiklet Topluluğu</div>
                        <h3>ÇKIRLAR TURU</h3>
                        <div class="card-details">
                            <i class="fas fa-map-marker-alt"></i> stadyum otoparkı<br>
                            <i class="far fa-clock"></i> 15.11.2026 | 09:30
                        </div>
                    </div>
                    <a href="/detay/155" class="btn-view">Detayları Gör</a>
                </div>
            </div>
</main></body></html>`;

export const akdenizDetail149Html = `<html><body><main class="container" style="margin: 30px auto; max-width: 820px; padding: 0 15px;">
        <div style="background: #fff; border-radius: 16px; border: 1px solid var(--border); overflow: hidden; display: flex; box-shadow: 0 8px 20px rgba(0,0,0,0.04); align-items: stretch;">

            <div style="flex: 0 0 320px; max-width: 320px; background: #f8fafc; display: flex; align-items: center; justify-content: center; padding: 20px; border-right: 1px solid var(--border);">
                <img src="/static/uploads/default.jpg" style="width: 100%; max-height: 280px; object-fit: contain; border-radius: 10px;" alt="SILLYON-PERGE ANTIK KENT GEZISI">
            </div>

            <div style="flex: 1; padding: 25px 30px; display: flex; flex-direction: column; justify-content: center;">

                <div style="margin-bottom: 8px;">
                    <span style="background: var(--accent); color: var(--primary); padding: 3px 10px; border-radius: 15px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase;">
                        Gezi
                    </span>
                    <span style="color: #64748b; font-size: 0.8rem; margin-left: 8px; font-weight: 600;">
                        <i class="fas fa-users"></i> Akdeniz ve Türk Uygarlıkları Araştırmaları Topluluğu
                    </span>
                </div>

                <h2 style="color: var(--primary); font-size: 1.45rem; font-weight: 800; margin-bottom: 16px; line-height: 1.25;">
                    SILLYON-PERGE ANTIK KENT GEZISI
                </h2>

                <div style="display: grid; grid-template-columns: 1fr; gap: 10px; margin-bottom: 18px;">
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div style="width: 34px; height: 34px; background: #f8fafc; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--accent); font-size: 0.9rem;">
                            <i class="far fa-calendar-alt"></i>
                        </div>
                        <div>
                            <span style="display: block; font-size: 0.7rem; color: #94a3b8; font-weight: 700; text-transform: uppercase;">Tarih Aralığı</span>
                            <span style="font-weight: 700; color: var(--primary); font-size: 0.85rem;">
                                13 November 2026
                                 - 13 November 2026
                            </span>
                        </div>
                    </div>

                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div style="width: 34px; height: 34px; background: #f8fafc; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--accent); font-size: 0.9rem;">
                            <i class="far fa-clock"></i>
                        </div>
                        <div>
                            <span style="display: block; font-size: 0.7rem; color: #94a3b8; font-weight: 700; text-transform: uppercase;">Etkinlik Saatleri</span>
                            <span style="font-weight: 700; color: var(--primary); font-size: 0.85rem;">
                                08:30  — 18:00
                            </span>
                        </div>
                    </div>

                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div style="width: 34px; height: 34px; background: #f8fafc; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--accent); font-size: 0.9rem;">
                            <i class="fas fa-map-marker-alt"></i>
                        </div>
                        <div>
                            <span style="display: block; font-size: 0.7rem; color: #94a3b8; font-weight: 700; text-transform: uppercase;">Etkinlik Mekanı</span>
                            <span style="font-weight: 700; color: var(--primary); font-size: 0.85rem;">Sillyon ve Perge Antik kentleri</span>
                        </div>
                    </div>
                </div>

                <div style="border-top: 1px solid var(--border); padding-top: 14px; margin-bottom: 15px;">
                    <h4 style="color: var(--primary); margin-bottom: 6px; font-size: 0.85rem; font-weight: 800;">Etkinlik Hakkında</h4>
                    <p style="line-height: 1.5; color: #475569; font-size: 0.85rem; margin: 0;">
                        Resmi tatil dolayısıyla dün evrak yükleme yapamadım bundan dolayı bugün evrak yükleme yapabiliyorum. Gezi tarihi 12 kasım 2026 Perşembe günüdür.
                    </p>
                </div>

                

                <div style="margin-top: 20px; display: flex; gap: 10px;">
                    <a href="/" class="btn-view" style="display: inline-flex; align-items: center; gap: 6px; padding: 9px 18px; border-radius: 8px; font-size: 0.85rem; font-weight: 700; text-decoration: none; background: #0f172a; color: white;">
                        <i class="fas fa-chevron-left"></i> Listeye Dön
                    </a>
                </div>
            </div>
        </div>
    </main></body></html>`;

export const akdenizDetail162Html = `<html><body><main class="container" style="margin: 30px auto; max-width: 820px; padding: 0 15px;">
        <div style="background: #fff; border-radius: 16px; border: 1px solid var(--border); overflow: hidden; display: flex; box-shadow: 0 8px 20px rgba(0,0,0,0.04); align-items: stretch;">

            <div style="flex: 0 0 320px; max-width: 320px; background: #f8fafc; display: flex; align-items: center; justify-content: center; padding: 20px; border-right: 1px solid var(--border);">
                <img src="/static/uploads/default.jpg" style="width: 100%; max-height: 280px; object-fit: contain; border-radius: 10px;" alt="LÖSEV FARKINDALIK KORTEJI">
            </div>

            <div style="flex: 1; padding: 25px 30px; display: flex; flex-direction: column; justify-content: center;">

                <div style="margin-bottom: 8px;">
                    <span style="background: var(--accent); color: var(--primary); padding: 3px 10px; border-radius: 15px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase;">
                        Topluluğa Özgü Faaliyet
                    </span>
                    <span style="color: #64748b; font-size: 0.8rem; margin-left: 8px; font-weight: 600;">
                        <i class="fas fa-users"></i> Akdeniz Üniversitesi Motosiklet Topluluğu
                    </span>
                </div>

                <h2 style="color: var(--primary); font-size: 1.45rem; font-weight: 800; margin-bottom: 16px; line-height: 1.25;">
                    LÖSEV FARKINDALIK KORTEJI
                </h2>

                <div style="display: grid; grid-template-columns: 1fr; gap: 10px; margin-bottom: 18px;">
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div style="width: 34px; height: 34px; background: #f8fafc; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--accent); font-size: 0.9rem;">
                            <i class="far fa-calendar-alt"></i>
                        </div>
                        <div>
                            <span style="display: block; font-size: 0.7rem; color: #94a3b8; font-weight: 700; text-transform: uppercase;">Tarih Aralığı</span>
                            <span style="font-weight: 700; color: var(--primary); font-size: 0.85rem;">
                                18 November 2026
                                 - 18 November 2026
                            </span>
                        </div>
                    </div>

                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div style="width: 34px; height: 34px; background: #f8fafc; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--accent); font-size: 0.9rem;">
                            <i class="far fa-clock"></i>
                        </div>
                        <div>
                            <span style="display: block; font-size: 0.7rem; color: #94a3b8; font-weight: 700; text-transform: uppercase;">Etkinlik Saatleri</span>
                            <span style="font-weight: 700; color: var(--primary); font-size: 0.85rem;">
                                12:00  — 17:00
                            </span>
                        </div>
                    </div>

                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div style="width: 34px; height: 34px; background: #f8fafc; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: var(--accent); font-size: 0.9rem;">
                            <i class="fas fa-map-marker-alt"></i>
                        </div>
                        <div>
                            <span style="display: block; font-size: 0.7rem; color: #94a3b8; font-weight: 700; text-transform: uppercase;">Etkinlik Mekanı</span>
                            <span style="font-weight: 700; color: var(--primary); font-size: 0.85rem;">Antalya Şehir Merkezi-Üç Kapılar-Yalım Park</span>
                        </div>
                    </div>
                </div>

                <div style="border-top: 1px solid var(--border); padding-top: 14px; margin-bottom: 15px;">
                    <h4 style="color: var(--primary); margin-bottom: 6px; font-size: 0.85rem; font-weight: 800;">Etkinlik Hakkında</h4>
                    <p style="line-height: 1.5; color: #475569; font-size: 0.85rem; margin: 0;">
                        Kortej başlangıç noktası Üç Kapılar, varış noktası Yalım park olmak üzere LÖSEV haftasında farkındalığı arttırmak amacıyla düzenlenecektir. ETKİNLİĞİN YAPILACAĞI TARİH 7 KASIM CUMARTESİ GÜNÜDÜR. ANCAK SİSTEMİN BELİRLEDİĞİ SÜREYE YETİŞTİRİLEMEDİĞİ İÇİN 18 KASIM SEÇİLMEK ZORUNDA KALMIŞTIR.
                    </p>
                </div>

                

                <div style="margin-top: 20px; display: flex; gap: 10px;">
                    <a href="/" class="btn-view" style="display: inline-flex; align-items: center; gap: 6px; padding: 9px 18px; border-radius: 8px; font-size: 0.85rem; font-weight: 700; text-decoration: none; background: #0f172a; color: white;">
                        <i class="fas fa-chevron-left"></i> Listeye Dön
                    </a>
                </div>
            </div>
        </div>
    </main></body></html>`;
