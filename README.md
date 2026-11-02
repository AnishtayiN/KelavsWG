# Kelavs VPN

[![Build Status](https://github.com/AnishtayiN/KelavsWG/workflows/Release%20Kelavs%20VPN/badge.svg)](https://github.com/AnishtayiN/KelavsWG/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-9%2B-green.svg)](https://developer.android.com/about/versions/pie)
[![WireGuard](https://img.shields.io/badge/WireGuard-Official%20Engine-orange.svg)](https://www.wireguard.com/)

**Kelavs VPN** یک کلاینت WireGuard اختصاصی و حرفه‌ای برای اندروید است که با تمرکز بر بهینه‌سازی اتصال برای اینترنت ایران، سیستم اشتراک خودکار، و آپدیت داخلی طراحی شده است.

## ✨ ویژگی‌ها

### 🔐 موتور WireGuard رسمی
- استفاده از موتور واقعی WireGuard Android
- پیاده‌سازی رمزنگاری اصلی WireGuard (Curve25519, ChaCha20-Poly1305, BLAKE2s)
- حفظ معماری مدیریت Tunnel پروژه اصلی

### 📡 سیستم اشتراک هوشمند
- **ورود ساده**: فقط لینک اشتراک در اولین اجرا
- **پشتیبانی فرمت‌های متعدد**:
  - فایل `.conf` مستقیم
  - متن ساده WireGuard
  - Base64 encoded
  - اشتراک چند کانفیگی
  - لیست URLها
  - لینک مستقیم دانلود
- **پردازش خودکار**: دانلود، اعتبارسنجی، حذف تکراری‌ها، انتخاب بهترین سرور

### 🎯 انتخاب خودکار سرور
- اولویت با **Handshake واقعی WireGuard** (نه فقط Ping)
- معیارها: تأخیر، پایداری، تاریخچه قطعی، وضعیت DNS
- جلوگیری از تغییر مدام سرور
- انتخاب دستی و غیرفعال‌سازی انتخاب خودکار

### 🇮🇷 بهینه‌سازی برای ایران
- **MTU**: خودکار/يدوي با تشخیص فرگمنتاسیون
- **DNS**: حفظ DNS کانفیگ، DNS سفارشی، جلوگیری از DNS Leak
- **IPv4/IPv6**: مدیریت صحیح AllowedIPs و Split Tunneling
- **تغییر شبکه**: بازیابی اتصال در تغییر Wi-Fi/دیتا/حالت هواپیما
- **صفحه عیب‌یابی**: وضعیت کامل اتصال، Handshake، ترافیک، خطاها

### 🎨 رابط کاربری مدرن
- **Material 3** با طراحی مینیمال
- **Dark/Light Mode** کامل
- **RTL و فارسی** به‌صورت پیش‌فرض
- انیمیشن‌های روان، کارت‌های وضعیت زنده
- منوی پایین: خانه، سرورها، اشتراک، تنظیمات

### 📱 مدیریت اشتراک
- ذخیره امن با Android Keystore
- به‌روزرسانی دوره‌ای با WorkManager
- نمایش وضعیت، لینک مخفی، زمان‌ها، تعداد کانفیگ
- دکمه‌های به‌روزرسانی و حذف

### 🤖 پشتیبانی تلگرام
- دکمه‌های مستقیم: خرید اشتراک، تمدید، پشتیبانی
- باز کردن در تلگرام یا مرورگر

### 🔄 سیستم آپدیت داخلی
- بررسی GitHub Releases در اجرا
- دانلود با نوار پیشرفت واقعی (۰-۱۰۰٪)
- تأیید SHA-256، امضای APK، Package Name
- نصب از طریق FileProvider و Intent استاندارد

### 🏗️ CI/CD با GitHub Actions
- Workflow با `workflow_dispatch` و ورودی نسخه
- Build، Test، Lint، امضا، انتشار Release
- فایل‌های: APK، SHA-256، release.json
- امضای امن با GitHub Secrets

## 📋 پیش‌نیازها

- Android 9 (API 28) یا بالاتر
- معماری ARM64-v8a یا armeabi-v7a
- اینترنت برای دریافت اشتراک و آپدیت

## 🛠️ ساخت (Build)

### پیش‌نیازها
- JDK 17
- Android SDK 36
- NDK 27
- Gradle 8.5+

### مراحل ساخت
```bash
# کلون مخزن
git clone https://github.com/AnishtayiN/KelavsWG.git
cd KelavsWG

# تنظیم متغیرهای محیطی برای امضا (اختیاری برای Debug)
export KEYSTORE_BASE64="..."
export KEYSTORE_PASSWORD="..."
export KEY_ALIAS="..."
export KEY_PASSWORD="..."

# ساخت Debug
./gradlew :ui:assembleDebug

# ساخت Release (نیاز به Keystore دارد)
./gradlew :ui:assembleRelease
```

### خروجی
- Debug: `ui/build/outputs/apk/debug/ui-debug.apk`
- Release: `ui/build/outputs/apk/release/Kelavs-VPN-<version>.apk`

## 🚀 انتشار نسخه جدید

### از طریق GitHub Actions (پیشنهادی)
1. به تب **Actions** بروید
2. **Release Kelavs VPN** را انتخاب کنید
3. **Run workflow** بزنید
4. نسخه را وارد کنید (مثال: `1.0.1`)
5. توضیحات نسخه را اضافه کنید (اختیاری)
6. **Run workflow** بزنید

### به‌صورت دستی
```bash
# تنظیم نسخه در gradle.properties
kelavsVersionName=1.0.1
kelavsVersionCode=1001000

# ساخت و امضا
./gradlew :ui:assembleRelease

# آپلود دستی به GitHub Releases
```

## 🔐 تنظیم Secrets (برای امضای Release)

در Settings > Secrets and variables > Actions مخزن، موارد زیر را اضافه کنید:

| Secret | توضیحات |
|--------|---------|
| `KEYSTORE_BASE64` | فایل Keystore کد شده در Base64 |
| `KEYSTORE_PASSWORD` | رمز Keystore |
| `KEY_ALIAS` | نام مستعار کلید (Alias) |
| `KEY_PASSWORD` | رمز کلید |

**تولید KEYSTORE_BASE64:**
```bash
base64 -i keystore.jks -o keystore.base64
cat keystore.base64
```

## 📁 ساختار پروژه

```
KelavsVPN/
├── .github/workflows/release.yml    # GitHub Actions
├── app/                             # ماژول اصلی (ui)
├── core/
│   ├── subscription/                # پردازش اشتراک
│   ├── update/                      # سیستم آپدیت
│   ├── network/                     # شبکه و کیفیت اتصال
│   └── storage/                     # Room، DataStore، Keystore
├── feature/
│   ├── onboarding/                  # ورود اولیه
│   ├── dashboard/                   # صفحه اصلی
│   ├── servers/                     # مدیریت سرورها
│   ├── subscription/                # صفحه اشتراک
│   └── settings/                    # تنظیمات
├── tunnel/                          # موتور WireGuard (فork شده)
├── docs/
│   ├── RELEASE_GUIDE.md
│   └── SECURITY.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 🧪 تست

```bash
# تست‌های واحد تمام ماژول‌ها
./gradlew test

# تست‌های ماژول خاص
./gradlew :core:subscription:test
./gradlew :core:update:test
./gradlew :core:network:test
./gradlew :core:storage:test

# Lint
./gradlew lint
```

## 🔒 امنیت

- **بدون تبلیغات و آنالیتیکس**
- **بدون ارسال PrivateKey** به سرور خارجی
- **بدون ثبت اطلاعات حساس** در Log
- **HTTPS اجباری** برای تمام ارتباطات
- **اعتبارسنجی URL** و جلوگیری از SSRF
- **امضای APK** با کلید ثابت
- **چک‌سام SHA-256** برای فایل‌های انتشار
- **Android Keystore** برای ذخیره امن کلیدها

## 📄 مجوز

```
Copyright 2026 Kelavs VPN Team
Copyright 2017-2025 WireGuard LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
```

موتور WireGuard تحت مجوز GPLv2 منتشر شده است. این پروژه تغییرات اختصاصی Kelavs را بر پایه WireGuard Android پیاده‌سازی کرده است.

## 🤝 مشارکت

1. Fork کنید
2. Branch جدید بسازید (`git checkout -b feature/amazing-feature`)
3. Commit کنید (`git commit -m 'Add amazing feature'`)
4. Push کنید (`git push origin feature/amazing-feature`)
5. Pull Request باز کنید

## 📞 پشتیبانی

- **تلگرام ادمین**: [@Matinkelavs](https://t.me/Matinkelavs)
- **ربات خرید**: [@Kelavs_bot](https://t.me/Kelavs_bot)
- **گیت‌هاب**: [Issues](https://github.com/AnishtayiN/KelavsWG/issues)

## 🙏 تشکر

- [WireGuard](https://www.wireguard.com/) - موتور VPN محترم
- [WireGuard Android](https://git.zx2c4.com/wireguard-android) - پروژه پایه
- تمام مشارکت‌کنندگان Open Source