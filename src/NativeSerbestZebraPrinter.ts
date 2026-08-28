import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

// checkStatus'un dönebileceği durumlar - eski koddaki 4 farklı status string'e karşılık geliyor
export interface PrinterStatusResult {
  status: string; // "DISCONNECTED" | "READY" | "BUSY" | "CONNECTED" | "CONNECTED_UNKNOWN"
  isReady?: boolean;
}

export interface Spec extends TurboModule {
  // Bluetooth taraması başlatır, sonuçlar "PrinterFound" event'i ile gelir (return değeri yok, void)
  discoverPrinters(): Promise<void>;

  // MAC adresine bağlanır, başarılıysa true döner
  connect(macAddress: string): Promise<boolean>;

  // Ham veriyi yazıcıya yollar, yazılan byte sayısını döner
  print(data: string): Promise<number>;

  // Yazıcının anlık durumunu sorgular
  checkStatus(): Promise<PrinterStatusResult>;

  // Bağlantıyı keser
  disconnect(): Promise<boolean>;

  // NativeEventEmitter'ın zorunlu tuttuğu iki metod - "PrinterFound" ve
  // "PrinterStatusChanged" event'lerini dinleyebilmek için gerekli, gövdeleri
  // Kotlin tarafında boş kalabilir
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('SerbestZebraPrinter');