<div align="center">

# 🛡️ Kelavs VPN

**WireGuard کلاینت اختصاصی، سریع و امن برای اینترنت ایران**  
ساخته شده بر پایه موتور رسمی WireGuard Android

[![GitHub Release](https://img.shields.io/github/v/release/AnishtayiN/KelavsWG?style=for-the-badge&logo=github&label=نسخه&color=006DFF)](https://github.com/AnishtayiN/KelavsWG/releases/latest)
[![Android](https://img.shields.io/badge/Android-9%2B-3DDC84?style=for-the-badge&logo=android)](https://developer.android.com/about/versions/pie)
[![WireGuard](https://img.shields.io/badge/Engine-WireGuard%20Official-FF6B35?style=for-the-badge&logo=wireguard)](https://www.wireguard.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

---

[📥 دانلود آخرین نسخه](https://github.com/AnishtayiN/KelavsWG/releases/latest) •
[💬 پشتیبانی تلگرام](https://t.me/Matinkelavs) •
[🤖 خرید اشتراک](https://t.me/Kelavs_bot)

</div>

---

## ✨ چرا Kelavs VPN؟

| ویژگی | توضیح |
|----------|--------|
| **موتور واقعی WireGuard** | بدون شبیه‌سازی، بدون پروتکل اختصاصی — مستقیماً از کد رسمی WireGuard Android |
| **ورود یک‌مرحله** | فقط لینک اشتراک را بزنید؛ بقیه کارها خودکار انجام می‌شود |
| **انتخاب هوشمند سرور** | بر اساس Handshake واقعی WireGuard، نه فقط Ping |
| **بهینه برای ایران** | MTU خودکار، پیشگیری از DNS Leak، بازیابی اتصال در تغییر شبکه |
| **آپدیت داخلی** | بررسی، دانلود (با پیشرفت ۰–۱۰۰٪)، تأیید امضا و نصب — همه در برنامه |
| **حریم خصوصی کامل** | بدون تبلیغات، آنالیتیکس، لاگ‌گیری یا ارسال داده‌های حساس |

---

## 🖼️ پیش‌نمایش برنامه

<div align="center">

| ورود و اشتراک | صفحه اصلی | مدیریت سرورها |
|:---:|:---:|:---:|
| ![Onboarding](.github/assets/onboarding.png) | ![Dashboard](.github/assets/dashboard.png) | ![Servers](.github/assets/servers.png) |

| اشتراک و آپدیت | تنظیمات | عیب‌یابی |
|:---:|:---:|:---:|
| ![Subscription](.github/assets/subscription.png) | ![Settings](.github/assets/settings.png) | ![Troubleshooting](.github/assets/troubleshoot.png) |

</div>

---

## 🚀 قابلیت‌های اصلی

### 📡 سیستم اشتراک هوشمند
- **فرمت‌های پشتیبانی‌شده**: `.conf` مستقیم، متن ساده WireGuard، Base64، اشتراک چند کانفیگی، لیست URLها
- **پردازش خودکار**: دانلود → اعتبارسنجی → حذف تکراری‌ها → وارد کردن به موتور WireGuard
- **امنیت**: فقط HTTPS، اعتبارسنجی گواهی، جلوگیری از SSRF، محدودیت ۱۰ مگابایت

### 🎯 انتخاب خودکار سرور
- معیار اصلی: **Handshake موفق WireGuard** (نه باز بودن پورت UDP)
- متریک‌ها: نرخ موفقیت، تأخیر، اخیر بودن Handshake، تعداد خطا
- جلوگیری از تغییر مدام سرور (کول‌داون ۳۰ ثانیه)
- قابلیت انتخاب دستی و غیرفعال‌سازی انتخاب خودکار

### 🇮🇷 بهینه‌سازی برای شبکه‌های ایران
| بهینه‌سازی | وضعیت |
|-------------|-------|
| MTU خودکار / دستی با تشخیص فرگمنتاسیون | ✅ |
| پیشگیری از DNS Leak | ✅ |
| مدیریت مسیر IPv4/IPv6 و Split Tunneling | ✅ |
| بازیابی اتصال در تغییر WiFi ⇄ دیتا / حالت هواپیما | ✅ |
| حفظ PersistentKeepalive ارائه‌دهنده | ✅ |
| reconnect هوشمند با بک‌آف نمایی | ✅ |

### 🎨 رابط کاربری مدرن
- **Material 3** با طراحی مینیمال
- **Dark / Light Mode** کامل
- **RTL و فارسی** به‌صورت پیش‌فرض
- انیمیشن‌های روان، کارت‌های وضعیت زنده
- منوی پایین: خانه، سرورها، اشتراک، تنظیمات

### 🔄 سیستم آپدیت داخلی
1. بررسی خودکار `GitHub Releases` در اجرا
2. نمایش اعلان نسخه جدید با تشریح تغییرات
3. دانلود APK با نوار پیشرفت واقعی (۰–۱۰۰٪)
4. تأیید **SHA-256** + **امضای APK** + **Package Name**
5. نصب از طریق `FileProvider` + `Intent` استاندارد اندروید

### 🤖 یکپارچگی تلگرام
- دکمه‌های مستقیم: **خرید اشتراک** | **تمدید** | **پشتیبانی**
- باز کردن در تلگرام (اگر نصب باشد) یا مرورگر

---

## 📥 نصب

### روش ۱: GitHub Releases (پیشنهاد)
1. به [صفحه ریلیزها](https://github.com/AnishtayiN/KelavsWG/releases/latest) بروید
2. فایل `Kelavs-VPN-<version>.apk` را دانلود کنید
3. (اختیاری) چک‌سام SHA-256 را با فایل `.sha256` مقایسه کنید
4. فایل را نصب کنید

### روش ۲: آپدیت داخلی برنامه
برنامه به‌صورت خودکار نسخه جدید را بررسی کرده و با یک دکمه دانلود و نصب می‌کند.

> **نکته**: برای نصب از منابع ناشناس، اندروید اجازه می‌خواهد. این فقط یک‌بار لازم است.

---

## 🔧 تنظیمات پیشرفته (اختیاری)

| تنظیم | مکان | پیش‌فرض |
|---------|-------|---------|
| MTU | تنظیمات → بهینه‌سازی شبکه | خودکار |
| DNS سفارشی | تنظیمات → بهینه‌سازی شبکه | DNS کانفیگ |
| IPv6 | تنظیمات → بهینه‌سازی شبکه | فعال |
| اتصال مجدد خودکار | تنظیمات → رفتار اتصال | فعال |
| مدیریت تغییر شبکه | تنظیمات → رفتار اتصال | فعال |
| تم | تنظیمات → نمایش | سیستمی |
| زبان | تنظیمات → نمایش | فارسی |
| بررسی خودکار آپدیت | تنظیمات → به‌روزرسانی | فعال |

---

## 🛡️ امنیت و حریم خصوصی

- **بدون تبلیغات** — هیچ SDK تبلیغاتی وجود ندارد
- **بدون آنالیتیکس** — هیچ ردیابی رفتاری
- **بدون لاگ حساس** — Private Key، Subscription URL هرگز لاگ نمی‌شوند
- **HTTPS اجباری** — تمام ارتباطات رمزنگاری شده
- **Android Keystore** — کلیدها و لینک اشتراک به‌صورت امن ذخیره می‌شوند
- **امضای برنامه** — همه نسخه‌ها با کلید واحد امضا می‌شوند
- **Open Source** — کد کامل در GitHub قابل بازبینی است

---

## 📋 پیش‌نیازها

- **Android 9 (API 28)** یا بالاتر
- معماری **ARM64-v8a** یا **armeabi-v7a**
- اینترنت برای دریافت اشتراک و بررسی آپدیت

---

## 🤝 پشتیبانی و ارتباط

| کانال | لینک |
|--------|------|
| 💬 پشتیبانی و سوالات | [@Matinkelavs](https://t.me/Matinkelavs) |
| 🤖 خرید و تمدید اشتراک | [@Kelavs_bot](https://t.me/Kelavs_bot) |
| 🐛 گزارش باگ / درخواست قابلیت | [GitHub Issues](https://github.com/AnishtayiN/KelavsWG/issues) |
| 🔒 گزارش امنیتی | security@kelavs.vpn |

---

## 📄 مجوز

```
Copyright 2026 Kelavs VPN Team
Copyright 2017-2025 WireGuard LLC

Licensed under the Apache License, Version 2.0
```

موتور WireGuard تحت مجوز **GPLv2** منتشر شده است. این پروژه تغییرات اختصاصی Kelavs را بر پایه WireGuard Android پیاده‌سازی کرده است.

---

<div align="center">

**ساخته شده با ❤️ برای کاربران ایرانی**

[⬆️ بالا برگردید](#-kelavs-vpn)

</div>