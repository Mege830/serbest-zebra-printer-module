import { useState } from 'react';
import { View, Text, Button, Alert, PermissionsAndroid, Platform } from 'react-native';
import { connect, print, disconnect } from 'serbest-zebra-printer';

const PRINTER_MAC = '90:75:DE:18:2C:C3';
const testZpl =
  `! U1 setvar "device.languages" "zpl"\r\n` +
  `~SD15\r\n` +
  `^XA\r\n` +
  `^FO50,50\r\n` +
  `^A0N,50,50\r\n` +
  `^FDSerbest ZPL Test^FS\r\n` +
  `^XZ\r\n`;
export default function App() {
  const [status, setStatus] = useState('Bağlı değil');

  const requestBluetoothPermissions = async () => {
    if (Platform.OS === 'android' && Platform.Version >= 31) {
      const granted = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
      ]);

      const isScanGranted = granted[PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN] === PermissionsAndroid.RESULTS.GRANTED;
      const isConnectGranted = granted[PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT] === PermissionsAndroid.RESULTS.GRANTED;

      return isScanGranted && isConnectGranted;
    }
    return true;
  };

  const handleConnect = async () => {
    try {
      setStatus('İzinler bekleniyor...');

      const hasPermission = await requestBluetoothPermissions();

      if (!hasPermission) {
        setStatus('İzin reddedildi');
        Alert.alert('Hata', 'Bluetooth izinleri verilmeden yazıcıya bağlanılamaz.');
        return;
      }

      setStatus('Bağlanıyor...');
      await connect(PRINTER_MAC);
      setStatus('Bağlandı');
    } catch (e: any) {
      setStatus('Bağlantı hatası');
      Alert.alert('Connect Error', e?.message ?? String(e));
    }
  };

  const handlePrint = async () => {
    try {
      const bytesWritten = await print(testZpl);
      Alert.alert('Yazdırıldı', `${bytesWritten} byte gönderildi`);
    } catch (e: any) {
      Alert.alert('Print Error', e?.message ?? String(e));
    }
  };

  const handleDisconnect = async () => {
    try {
      await disconnect();
      setStatus('Bağlı değil');
    } catch (e: any) {
      Alert.alert('Disconnect Error', e?.message ?? String(e));
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', padding: 20 }}>
      <Text style={{ fontSize: 18, marginBottom: 20 }}>Durum: {status}</Text>
      <View style={{ marginBottom: 10 }}>
        <Button title="Bağlan" onPress={handleConnect} />
      </View>
      <View style={{ marginBottom: 10 }}>
        <Button title="Test Yazdır" onPress={handlePrint} />
      </View>
      <View style={{ marginBottom: 10 }}>
        <Button title="Bağlantıyı Kes" onPress={handleDisconnect} />
      </View>
    </View>
  );
}