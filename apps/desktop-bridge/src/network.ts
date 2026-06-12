import os from "node:os";
import { execFileSync } from "node:child_process";

export function getPrivateAddresses(): string[] {
  const addresses = new Set<string>();
  for (const entries of Object.values(os.networkInterfaces())) {
    for (const entry of entries ?? []) {
      if (entry.family !== "IPv4" || entry.internal) continue;
      if (isLikelyPrivate(entry.address)) addresses.add(entry.address);
    }
  }
  return [...addresses].sort();
}

export function getLanAddresses(addresses = getPrivateAddresses()): string[] {
  return addresses.filter(isLanPrivateAddress);
}

export function getVirtualAddresses(addresses = getPrivateAddresses()): string[] {
  return uniqueAddresses([...addresses.filter(isVirtualPrivateAddress), ...getTailscaleCliAddresses()]);
}

export function isLikelyPrivate(address: string): boolean {
  return (
    address.startsWith("10.") ||
    address.startsWith("192.168.") ||
    /^172\.(1[6-9]|2\d|3[0-1])\./.test(address) ||
    address.startsWith("100.")
  );
}

export function isLanPrivateAddress(address: string): boolean {
  return (
    address.startsWith("10.") ||
    address.startsWith("192.168.") ||
    /^172\.(1[6-9]|2\d|3[0-1])\./.test(address)
  );
}

export function isVirtualPrivateAddress(address: string): boolean {
  return address.startsWith("100.");
}

export function normalizeBaseUrl(input: string): string {
  const trimmed = input.trim().replace(/\/+$/, "");
  if (!trimmed) return trimmed;
  if (/^https?:\/\//i.test(trimmed)) return trimmed;
  return `http://${trimmed}`;
}

function getTailscaleCliAddresses(): string[] {
  try {
    const output = execFileSync("tailscale", ["ip", "-4"], {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
      timeout: 1500,
      windowsHide: true
    });
    return output
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => isVirtualPrivateAddress(line));
  } catch {
    return [];
  }
}

function uniqueAddresses(addresses: string[]): string[] {
  return [...new Set(addresses)].sort();
}
