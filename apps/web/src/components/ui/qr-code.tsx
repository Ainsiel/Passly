import QRCode from "qrcode";

export async function QRCodeImage({ value }: { value: string }) {
	const dataUrl = await QRCode.toDataURL(value, { width: 200, margin: 2 });
	return <img src={dataUrl} alt="QR Code" className="rounded-lg" />;
}
