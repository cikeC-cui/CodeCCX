import { createHash, randomBytes, randomUUID, timingSafeEqual } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname } from "node:path";

export type DeviceRecord = {
  id: string;
  name: string;
  tokenHash: string;
  createdAt: string;
  lastSeenAt?: string;
  revokedAt?: string;
};

type AuthStoreShape = {
  devices: DeviceRecord[];
};

export class AuthStore {
  private data: AuthStoreShape = { devices: [] };

  constructor(private readonly filePath: string) {}

  async load(): Promise<void> {
    try {
      const raw = await readFile(this.filePath, "utf8");
      this.data = JSON.parse(raw) as AuthStoreShape;
    } catch {
      this.data = { devices: [] };
      await this.save();
    }
  }

  async createDevice(name: string): Promise<{ deviceId: string; authToken: string }> {
    const authToken = randomBytes(32).toString("base64url");
    const device: DeviceRecord = {
      id: randomUUID(),
      name: name.trim() || "Android device",
      tokenHash: hashToken(authToken),
      createdAt: new Date().toISOString()
    };
    this.data.devices.push(device);
    await this.save();
    return { deviceId: device.id, authToken };
  }

  async verifyToken(authToken: string | undefined): Promise<DeviceRecord | null> {
    if (!authToken) return null;
    const incoming = Buffer.from(hashToken(authToken));
    const device = this.data.devices.find((candidate) => {
      if (candidate.revokedAt) return false;
      const expected = Buffer.from(candidate.tokenHash);
      return expected.length === incoming.length && timingSafeEqual(expected, incoming);
    });
    if (!device) return null;
    device.lastSeenAt = new Date().toISOString();
    await this.save();
    return device;
  }

  async revokeDevice(deviceId: string): Promise<boolean> {
    const device = this.data.devices.find((candidate) => candidate.id === deviceId);
    if (!device || device.revokedAt) return false;
    device.revokedAt = new Date().toISOString();
    await this.save();
    return true;
  }

  listDevices(): DeviceRecord[] {
    return [...this.data.devices].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }

  private async save(): Promise<void> {
    await mkdir(dirname(this.filePath), { recursive: true });
    await writeFile(this.filePath, JSON.stringify(this.data, null, 2), "utf8");
  }
}

export function createPairToken(): { pairToken: string; expiresAt: Date } {
  return {
    pairToken: randomBytes(18).toString("base64url"),
    expiresAt: new Date(Date.now() + 10 * 60 * 1000)
  };
}

function hashToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}
