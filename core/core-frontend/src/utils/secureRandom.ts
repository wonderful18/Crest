export function secureRandomInt(maxExclusive: number): number {
  if (!Number.isInteger(maxExclusive) || maxExclusive <= 0) {
    throw new Error('maxExclusive must be a positive integer')
  }
  if (!globalThis.crypto?.getRandomValues) {
    throw new Error('crypto.getRandomValues is unavailable')
  }
  const values = new Uint32Array(1)
  const limit = Math.floor(0x100000000 / maxExclusive) * maxExclusive
  do {
    globalThis.crypto.getRandomValues(values)
  } while (values[0] >= limit)
  return values[0] % maxExclusive
}

export function secureRandomChar(characters: string): string {
  return characters.charAt(secureRandomInt(characters.length))
}

export function secureShuffle(value: string): string {
  const chars = value.split('')
  for (let i = chars.length - 1; i > 0; i--) {
    const j = secureRandomInt(i + 1)
    const current = chars[i]
    chars[i] = chars[j]
    chars[j] = current
  }
  return chars.join('')
}
