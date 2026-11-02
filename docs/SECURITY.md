# سیاست امنیت (Security Policy)

## 🔒 گزارش آسیب‌پذیری

اگر آسیب‌پذیری امنیتی کشف کردید، **لطفاً به‌صورت خصوصی** گزارش دهید:

- **ایمیل**: security@kelavs.vpn
- **تلگرام**: [@Matinkelavs](https://t.me/Matinkelavs) (پیام مستقیم)

**نکات مهم:**
- در Issues عمومی گزارش ندهید
- جزئیات فنی کامل ارائه دهید
- PoC (اثبات مفهوم) اگر ممکن است
- زمان پاسخ‌دهی: حداکثر ۷۲ ساعت

## 🛡️ معماری امنیتی

### موتور WireGuard
- استفاده از **موتور رسمی WireGuard Android**
- رمزنگاری: **Curve25519** (Key Exchange), **ChaCha20-Poly1305** (Encryption), **BLAKE2s** (Hashing)
- بدون پیاده‌سازی رمزنگاری اختصاصی
- بدون تغییر در پروتکل اصلی WireGuard

### مدیریت کلیدها
- **Private Keys**: فقط در حافظه پروسه، هرگز در لاگ/فایل/شبکه
- **Android Keystore**: برای رمزنگاری داده‌های حساس محلی
- **Keystore امضا**: فقط در GitHub Actions (موقت)، هرگز در مخزن

### ارتباطات شبکه
- **HTTPS اجباری** برای تمام درخواست‌های خروجی
- **اعتبارسنجی گواهی** (Certificate Pinning برای APIهای حیاتی)
- **جلوگیری از SSRF**: مسدود کردن IPهای داخلی/خصوصی
- **حدودية حجم پاسخ**: حداکثر ۱۰ مگابایت برای اشتراک
- **Timeout**: ۳۰ ثانیه برای درخواست‌ها

### ذخیره‌سازی داده‌ها
| داده | مکانیزم | رمزنگاری |
|------|---------|----------|
| لینک اشتراک | DataStore + Keystore | AES-256-GCM |
| کانفیگ‌های سرور | Room Database | SQLCipher (اختیاری) |
| تاریخچه اتصال | Room Database | بدون رمزنگاری (غیر حساس) |
| تنظیمات کاربر | DataStore | بدون رمزنگاری |

### لاگ‌گیری
- **هیچ PrivateKey** در لاگ ثبت نمی‌شود
- **هیچ Subscription URL** در لاگ ثبت نمی‌شود
- لاگ‌ها در Build Release غیرفعال هستند
- فقط خطاهای عمومی (بدون داده حساس) ثبت می‌شوند

## 📱 امنیت برنامه (App Hardening)

### Anti-Tampering
- امضای APK با کلید رسمی
- بررسی امضا در آپدیت داخلی
- بررسی SHA-256 قبل از نصب
- بررسی Package Name

### ProGuard/R8
- Minify و Shrink در Release
- Obuscation کدهای حساس
- حذف کدهای Debug

### Permissions
- حداقل مجوزها: `INTERNET`, `FOREGROUND_SERVICE`, `BIND_VPN_SERVICE`, `REQUEST_INSTALL_PACKAGES`
- بدون مجوزهای خطرناک: `READ_CONTACTS`, `ACCESS_FINE_LOCATION`, `CAMERA` (مجزا برای QR)

### Network Security Config
```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.github.com</domain>
        <domain includeSubdomains="true">github.com</domain>
        <domain includeSubdomains="true">objects.githubusercontent.com</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

## 🔄 سیستم آپدیت امن

### فرآیند آپدیت
1. بررسی `api.github.com/repos/.../releases/latest` (HTTPS، Certificate Validation)
2. مقایسه `versionName` و `versionCode`
3. نمایش اعلان در برنامه
4. دانلود APK از `github.com/.../releases/download/...` (HTTPS)
5. **تأیید SHA-256** (از فایل `.sha256` در Release)
6. **تأیید امضای APK** (`apksigner verify`)
7. **تأیید Package Name** (`aapt dump badging`)
8. نصب از طریق `FileProvider` + `ACTION_INSTALL_PACKAGE`

### Rollback
- در صورت شکست هر مرحله، آپدیت لغو می‌شود
- کانفیگ‌های قبلی حفظ می‌شوند
- کاربر به نسخه فعلی باقی می‌ماند

## 🔑 مدیریت Keystore

### تولید Keystore
```bash
keytool -genkeypair \
  -alias kelavs-vpn \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -keystore kelavs-release.jks \
  -storepass <strong_password> \
  -keypass <strong_password> \
  -dname "CN=Kelavs VPN, OU=Engineering, O=Kelavs VPN, L=Tehran, ST=Tehran, C=IR"
```

### ذخیره امن
- فایل `kelavs-release.jks` **هرگز** در گیت commit نشود
- فقط در **GitHub Secrets** (Base64 encoded) ذخیره شود
- رمزها: `KEYSTORE_PASSWORD`, `KEY_PASSWORD` در Secrets
- Alias: `KEY_ALIAS` در Secrets

### Rotate Key (تغییر کلید)
**⚠️ هشدار:** تغییر کلید باعث می‌شود کاربران نتوانند آپدیت کنند (نیاز به Uninstall/Reinstall).
- فقط در صورت نشت کلید انجام شود
- برنامه قبلی باید حذف و نسخه جدید نصب شود

## 🧪 تست‌های امنیتی

### Static Analysis
```bash
# Lint Security
./gradlew lint

# Dependency Check (OWASP)
./gradlew dependencyCheckAnalyze
```

### Dynamic Analysis
- تست ترافیک شبکه با Wireshark/mitmproxy
- تست باینری با MobSF
- تست ربات‌های خودکار (فرازیابی)

### Penetration Testing
- تست سالانه توسط تیم امنیتی
- باگ بانتی در برنامه‌های رسمی

## 📋 چک‌لیست امنیتی هر Release

- [ ] امضای APK با کلید رسمی تأیید شده
- [ ] SHA-256 منتشر شده و مطابقت دارد
- [ ] ProGuard/R8 فعال در Release
- [ ] Network Security Config اعمال شده
- [ ] Keystore در Secrets (نه در کد)
- [ ] هیچ PrivateKey/URL در لاگ‌ها نیست
- [ ] HTTPS برای تمام endpointها
- [ ] Certificate Validation فعال
- [ ] SSRF Protection در Subscription Parser
- [ ] Input Validation در تمام نقاط ورودی
- [ ] Dependency های به‌روزی (بدون CVEهای Critical)

## 🚨 سناریوهای تهدید و 대응

| تهدید | خطر | 대응 |
|--------|-----|------|
| نشت Keystore | بالا | Rotate Key، انقضای نسخه‌های قبلی |
| MITM در آپدیت | متوسط | Certificate Pinning، SHA-256 |
| SSRF در اشتراک | متوسط | Private IP Blocking، URL Validation |
| تزریق کانفیگ مخرب | پایین | اعتبارسنجی کامل کانفیگ، Base64 Safe Decode |
| Reverse Engineering | پایین | ProGuard، Native Library (Go) |

## 📞 تماس امنیتی

- **PGP Key**: موجود در `security@kelavs.vpn`
- **Response Time**: ۷۲ ساعت برای تriage
- **Disclosure Policy**: Coordinated Disclosure (۹۰ روز)

## 📄 مراجع

- [WireGuard Security](https://www.wireguard.com/protocol/)
- [Android Security Best Practices](https://developer.android.com/topic/security)
- [OWASP MASVS](https://mas.owasp.org/)
- [Google Play Security Requirements](https://play.google.com/console/about/developer-content-policy/)