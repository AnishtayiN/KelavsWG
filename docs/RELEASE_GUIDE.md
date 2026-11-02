# راهنمای انتشار نسخه (Release Guide)

این سند مراحل انتشار نسخه جدید Kelavs VPN را توضیح می‌دهد.

## 📋 پیش‌نیازها

- دسترسی Admin به مخزن `AnishtayiN/KelavsWG`
- GitHub Secrets تنظیم شده (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
- نسخه SemVer معتبر (مثال: `1.0.0`, `1.2.3-beta.1`)

## 🚀 انتشار از طریق GitHub Actions (روش پیشنهادی)

### 1. رفتن به تب Actions
```
https://github.com/AnishtayiN/KelavsWG/actions
```

### 2. انتخاب Workflow
- **Release Kelavs VPN** را انتخاب کنید

### 3. اجرای Workflow
- دکمه **Run workflow** را بزنید
- Branch را انتخاب کنید (معمولاً `main`)

### 4. وارد کردن اطلاعات
| فیلد | الزامی؟ | مثال | توضیح |
|------|---------|------|-------|
| `version` | ✅ بله | `1.0.1` | نسخه SemVer (بدون پیشوند v) |
| `release_notes` | ❌ خیر | `اصلاح باگ اتصال` | توضیحات نسخه برای Release Notes |

### 5. تأیید و اجرا
- دکمه **Run workflow** سبز را بزنید
- Workflow شروع به کار می‌کند

## 🔄 مراحل اتوماتیک Workflow

### Phase 1: اعتبارسنجی نسخه (`validate-version`)
- بررسی فرمت SemVer
- بررسی تکراری نبودن Tag
- محاسبه `versionCode` از `versionName`
  - فرمول: `Major × 1,000,000 + Minor × 1,000 + Patch`
  - مثال: `1.2.3` → `1,002,003`

### Phase 2: Build و تست (`build`)
1. Checkout کد
2. Setup JDK 17، Android SDK 36، NDK 27
3. Decode Keystore از Base64
4. تولید `gradle.properties` با اطلاعات نسخه و امضا
5. **Build Debug APK**
6. **Run Unit Tests** (تمام ماژول‌های core)
7. **Run Lint**
8. **Build Release APK** (امضای شده)
9. **Verify APK**:
   - بررسی Package Name (`com.kelavs.vpn`)
   - بررسی امضای APK (`apksigner verify`)
10. **Generate SHA-256**
11. **Generate release.json** (متادیتا)
12. Upload Artifacts

### Phase 3: انتشار Release (`release`)
1. دانلود Artifacts
2. ایجاد GitHub Release با Tag `v{version}`
3. آپلود فایل‌ها:
   - `Kelavs-VPN-{version}.apk`
   - `Kelavs-VPN-{version}.apk.sha256`
   - `release.json`

### Phase 4: اعلان (`notify`)
- نمایش وضعیت موفق/ناموفق

## 📦 فایل‌های منتشرشده

### 1. APK
- نام: `Kelavs-VPN-1.0.1.apk`
- امضای شده با Keystore رسمی
- Package Name: `com.kelavs.vpn`
- Min SDK: 28 (Android 9)

### 2. SHA-256 Checksum
- نام: `Kelavs-VPN-1.0.1.apk.sha256`
- محتوای: `<sha256_hash>  Kelavs-VPN-1.0.1.apk`
- برای تأیید یکپارچگی فایل

### 3. Release Metadata (release.json)
```json
{
  "version": "1.0.1",
  "version_code": 1001000,
  "package_name": "com.kelavs.vpn",
  "min_sdk": 28,
  "target_sdk": 36,
  "sha256": "a1b2c3d4...",
  "apk_size": 12345678,
  "build_time": "2026-11-02T10:30:00Z",
  "github_repo": "AnishtayiN/KelavsWG",
  "release_notes": "اصلاح باگ اتصال"
}
```

## ✅ چک‌لیست پس از انتشار

- [ ] Release در GitHub ایجاد شده: `https://github.com/AnishtayiN/KelavsWG/releases/tag/v1.0.1`
- [ ] Tag در مخزن وجود دارد: `git tag -l "v1.0.1"`
- [ ] APK قابل دانلود و نصب است
- [ ] SHA-256 مطابقت دارد: `sha256sum Kelavs-VPN-1.0.1.apk`
- [ ] امضای APK معتبر است: `apksigner verify --print-certs Kelavs-VPN-1.0.1.apk`
- [ ] برنامه در اپدیت داخلی تشخیص می‌دهد (تست در دستگاه)

## 🔧 عیب‌یابی مشکلات رایج

### خطا: "KEYSTORE_BASE64 secret is not set"
- Secrets را در Settings > Secrets and variables > Actions بررسی کنید
- مطمئن شوید نام‌ها دقیقاً: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

### خطا: "Version v1.0.1 already exists"
- Tag قبلاً وجود دارد
- از نسخه جدید استفاده کنید یا Tag قبلی را حذف کنید:
  ```bash
  git tag -d v1.0.1
  git push origin :refs/tags/v1.0.1
  ```

### خطا: "Package name mismatch"
- `applicationId` در `ui/build.gradle.kts` برابر `com.kelavs.vpn` باشد
- `namespace` در `ui/build.gradle.kts` هم برابر باشد

### خطا: "APK signature verification failed"
- Keystore معتبر نیست یا رمز اشتباه است
- `apksigner verify` را به‌صورت دستی تست کنید

### Build Timeout
- در `gradle.properties`: `org.gradle.jvmargs=-Xmx4g`
- در Workflow: `--no-daemon` استفاده شده

## 📝 تغییر Version Code به‌صورت دستی

در `gradle.properties`:
```properties
kelavsVersionName=1.0.1
kelavsVersionCode=1001000
```

فرمول محاسبه:
```
versionCode = major * 1000000 + minor * 1000 + patch
1.0.1      = 1 * 1000000 + 0 * 1000 + 1 = 1000001
1.2.3      = 1 * 1000000 + 2 * 1000 + 3 = 1002003
2.0.0      = 2 * 1000000 + 0 * 1000 + 0 = 2000000
```

## 🔐 نکات امنیتی

1. **هرگز** Keystore یا رمزها را در کد/گیت commit نکنید
2. **فقط** از GitHub Secrets استفاده کنید
3. Keystore فقط در محیط موقت Runner decode می‌شود
4. نسخه‌های بعدی **باید** با همان کلید امضا شوند
5. APK بدون امضا معتبر **هرگز** به عنوان Release انتشار داده نشود

## 📞 پشتیبانی

در صورت مشکل:
1. لاگ‌های Workflow را بررسی کنید
2. Issues گیت‌هاب را چک کنید
3. با ادمین تماس بگیرید: [@Matinkelavs](https://t.me/Matinkelavs)