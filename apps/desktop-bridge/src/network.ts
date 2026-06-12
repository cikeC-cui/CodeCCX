import os from "node:os";

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
  return addresses.filter(isVirtualPrivateAddress);
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
