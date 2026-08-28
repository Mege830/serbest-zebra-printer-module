# serbest-zebra-printer-module

Zebra Link-OS yazıcılarla Bluetooth üzerinden iletişim kurup ZPL komutlarıyla etiket bastırmayı sağlayan React Native (Turbo Module) kütüphanesi.

> **Sadece Android desteklenir.** iOS tarafı şu an implemente edilmemiştir.

## Kurulum

```sh
npm install serbest-zebra-printer-module
# veya
yarn add serbest-zebra-printer-module
```

## Gerekli Kurulum Adımları (ÖNEMLİ)

Kütüphaneyi kurduktan sonra, projenizde aşağıdaki iki adımı **mutlaka** tamamlamanız gerekir. Bunlar olmadan bağlantı `Connect Error` hatası verir.

### 1. Android İzinlerini Ekleyin

`android/app/src/main/AndroidManifest.xml` dosyanızın `<manifest>` etiketi içine şu izinleri ekleyin:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

Android 12 (SDK 31) altındaki cihazları da destekleyecekseniz ayrıca:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### 2. Çalışma Zamanı İzinlerini İsteyin

Manifest'te tanımlamak yeterli değildir — Android 12+'ta kullanıcıdan **çalışma zamanında** da izin istemeniz gerekir. `connect()` çağırmadan önce:

```typescript
import { PermissionsAndroid, Platform } from 'react-native';

async function requestBluetoothPermissions(): Promise<boolean> {
  if (Platform.OS === 'android' && Platform.Version >= 31) {
    const results = await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
      PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    ]);
    return Object.values(results).every(
      (r) => r === PermissionsAndroid.RESULTS.GRANTED
    );
  }
  return true;
}
```

## Kullanım

```typescript
import { connect, print, disconnect, checkStatus } from 'serbest-zebra-printer-module';

// Yazıcının Bluetooth MAC adresi - KENDİ YAZICINIZIN adresiyle değiştirin.
// Telefonun Bluetooth ayarlarından eşleştirilmiş cihazlar listesinden bulabilirsiniz.
const PRINTER_MAC = 'XX:XX:XX:XX:XX:XX'; // <-- Bunu değiştirmeyi unutmayın!

await connect(PRINTER_MAC);

const zpl = '^XA^FO50,50^A0N,50,50^FDMerhaba^FS^XZ';
await print(zpl);

await disconnect();
```

> **Not:** `PRINTER_MAC` değeri her yazıcıda farklıdır. Kodu kendi projenize entegre ederken bu değeri mutlaka kendi yazıcınızın gerçek MAC adresiyle değiştirin, aksi halde bağlantı kurulamaz.

## API

| Fonksiyon | Açıklama | Dönüş |
|---|---|---|
| `discoverPrinters()` | Eşleştirilmiş ve yakındaki Bluetooth cihazlarını tarar, sonuçlar `PrinterFound` event'i ile gelir | `Promise<void>` |
| `connect(macAddress: string)` | Belirtilen MAC adresindeki yazıcıya bağlanır | `Promise<boolean>` |
| `print(data: string)` | Ham ZPL verisini yazıcıya gönderir | `Promise<number>` (yazılan byte sayısı) |
| `checkStatus()` | Yazıcının anlık durumunu döner (`READY`, `BUSY`, `DISCONNECTED` vb.) | `Promise<PrinterStatusResult>` |
| `disconnect()` | Bağlantıyı keser | `Promise<boolean>` |

## Lisans

MIT
