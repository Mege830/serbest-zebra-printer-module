import { NativeEventEmitter } from 'react-native';
import SerbestZebraPrinter from './NativeSerbestZebraPrinter';
import type { PrinterStatusResult } from './NativeSerbestZebraPrinter';

export function discoverPrinters(): Promise<void> {
  return SerbestZebraPrinter.discoverPrinters();
}

export function connect(macAddress: string): Promise<boolean> {
  return SerbestZebraPrinter.connect(macAddress);
}

export function print(data: string): Promise<number> {
  return SerbestZebraPrinter.print(data);
}

export function checkStatus(): Promise<PrinterStatusResult> {
  return SerbestZebraPrinter.checkStatus();
}

export function disconnect(): Promise<boolean> {
  return SerbestZebraPrinter.disconnect();
}

export type { PrinterStatusResult };

export const SerbestZebraPrinterEmitter = new NativeEventEmitter(SerbestZebraPrinter as any);