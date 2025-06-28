# WhatsApp Status Paths - Comprehensive Guide

This document lists all possible WhatsApp status paths across different device manufacturers and Android versions.

## Standard WhatsApp Paths

### Android 10+ (API 29+)
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`

### Android 9 and below (API 28-)
- `WhatsApp/Media/.Statuses`
- `WhatsApp/Media/WhatsApp Statuses`
- `WhatsApp/Media/Statuses`
- `WhatsApp/Statuses`

## Manufacturer-Specific Paths

### Samsung
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`
- `Samsung/WhatsApp/Media/.Statuses`
- `Samsung/WhatsApp/Statuses`

### Xiaomi
- `MIUI/Media/WhatsApp/Media/.Statuses`
- `MIUI/Media/WhatsApp/Statuses`
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`

### OnePlus
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`
- `OnePlus/WhatsApp/Media/.Statuses`
- `OnePlus/WhatsApp/Statuses`

### Huawei
- `Huawei/Media/WhatsApp/Media/.Statuses`
- `Huawei/Media/WhatsApp/Statuses`
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`

### Oppo/Vivo
- `ColorOS/Media/WhatsApp/Media/.Statuses`
- `ColorOS/Media/WhatsApp/Statuses`
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`

### Motorola
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`
- `Motorola/WhatsApp/Media/.Statuses`
- `Motorola/WhatsApp/Statuses`

### LG
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`
- `LG/WhatsApp/Media/.Statuses`
- `LG/WhatsApp/Statuses`

### Sony
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`
- `Sony/WhatsApp/Media/.Statuses`
- `Sony/WhatsApp/Statuses`

### Nokia
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`
- `Nokia/WhatsApp/Media/.Statuses`
- `Nokia/WhatsApp/Statuses`

### Realme
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`
- `Realme/WhatsApp/Media/.Statuses`
- `Realme/WhatsApp/Statuses`

### Poco
- `Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `Android/data/com.whatsapp/files/Statuses`
- `Poco/WhatsApp/Media/.Statuses`
- `Poco/WhatsApp/Statuses`

## WhatsApp Business Paths

### Android 10+ (API 29+)
- `Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses`
- `Android/data/com.whatsapp.w4b/files/Statuses`

### Android 9 and below (API 28-)
- `WhatsApp Business/Media/.Statuses`
- `WhatsApp Business/Media/Statuses`
- `WhatsApp Business/Statuses`

### Modern paths
- `Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses`
- `Android/data/com.whatsapp.w4b/files/Statuses`

## GBWhatsApp Paths

### Android 10+ (API 29+)
- `Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses`
- `Android/data/com.gbwhatsapp/files/Statuses`

### Android 9 and below (API 28-)
- `GBWhatsApp/Media/.Statuses`
- `GBWhatsApp/Media/Statuses`
- `GBWhatsApp/Statuses`

### Modern paths
- `Android/media/com.gbwhatsapp/GBWhatsApp/Media/Statuses`
- `Android/data/com.gbwhatsapp/files/Statuses`

## YoWhatsApp Paths

### Android 10+ (API 29+)
- `Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses`
- `Android/data/com.yowhatsapp/files/Statuses`

### Android 9 and below (API 28-)
- `YoWhatsApp/Media/.Statuses`
- `YoWhatsApp/Media/Statuses`
- `YoWhatsApp/Statuses`

### Modern paths
- `Android/media/com.yowhatsapp/YoWhatsApp/Media/Statuses`
- `Android/data/com.yowhatsapp/files/Statuses`

## Additional WhatsApp Mods

### FMWhatsApp
- `FMWhatsApp/Media/.Statuses`
- `Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses`
- `Android/data/com.fmwhatsapp/files/Statuses`

### WhatsApp Plus
- `WhatsApp Plus/Media/.Statuses`
- `Android/media/com.whatsapp.plus/WhatsApp Plus/Media/.Statuses`
- `Android/data/com.whatsapp.plus/files/Statuses`

### Fouad WhatsApp
- `Fouad WhatsApp/Media/.Statuses`
- `Android/media/com.fouad.whatsapp/Fouad WhatsApp/Media/.Statuses`
- `Android/data/com.fouad.whatsapp/files/Statuses`

## Storage Variations

### Internal Storage
- `0/WhatsApp/Media/.Statuses`
- `0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `0/Android/data/com.whatsapp/files/Statuses`

### SD Card (if available)
- `1/WhatsApp/Media/.Statuses`
- `1/Android/media/com.whatsapp/WhatsApp/Media/.Statuses`
- `1/Android/data/com.whatsapp/files/Statuses`

## Notes

1. **Android Version Differences**: Android 10+ uses scoped storage, so paths are typically under `Android/media/` or `Android/data/`
2. **Manufacturer Variations**: Different manufacturers may use their own folder structures
3. **Storage Location**: Statuses can be stored on internal storage or SD card
4. **App Variants**: Different WhatsApp mods may use different package names and paths
5. **Folder Names**: Some devices use `.Statuses` while others use `Statuses` or `WhatsApp Statuses`

## Detection Strategy

The app uses the following strategy to find status paths:

1. **Check if WhatsApp variants are installed** using package manager
2. **Scan all possible paths** for the installed variants
3. **Prioritize paths that exist and contain files**
4. **Fallback to empty but existing paths** if no files found
5. **Show appropriate messages** if no paths found or no statuses available

## Error Handling

- **WhatsApp not installed**: Shows message asking user to install appropriate app
- **No statuses found**: Shows message indicating no statuses available at this time
- **Path detection failed**: Falls back to manual folder selection
- **Permission issues**: Guides user to grant necessary permissions 