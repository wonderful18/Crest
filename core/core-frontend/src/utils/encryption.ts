import CryptoJS from 'crypto-js/crypto-js'
import JSEncrypt from 'jsencrypt'
import { Base64 } from 'js-base64'
import { sm2, sm3, sm4 } from 'sm-crypto'
import { useCache } from '@/hooks/web/useCache'
import { useAppStoreWithOut } from '@/store/modules/app'

const appStore = useAppStoreWithOut()

const { wsCache } = useCache()

const rsaKey = '-pk_separator-'
const crypt = new JSEncrypt()
const smSuitePublicKeyPrefix = 'sm-suite:v1:sm2-public:'
const sm2CipherPrefix = 'sm-suite:v1:sm2:'
const sm4CipherPrefix = 'sm-suite:v1:sm4:'
const aesCtrHmacPrefix = 'aes-ctr-hmac:v1:'

const aesDecrypt = (word, keyStr) => {
  const keyHex = CryptoJS.enc.Utf8.parse(keyStr)
  if (typeof word === 'string' && word.startsWith(aesCtrHmacPrefix)) {
    return aesCtrHmacDecrypt(word, keyHex)
  }
  const ivHex = CryptoJS.enc.Utf8.parse('0000000000000000')
  const decrypt = CryptoJS.AES.decrypt(word, keyHex, {
    iv: ivHex,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypt.toString(CryptoJS.enc.Utf8)
}

export const rsaEncryp = word => {
  const clientKey = wsCache.get(appStore.getClientKey)
  if (typeof clientKey === 'string' && clientKey.startsWith(smSuitePublicKeyPrefix)) {
    const publicKey = clientKey.substring(smSuitePublicKeyPrefix.length)
    return sm2CipherPrefix + sm2.doEncrypt(word, publicKey, 1)
  }
  const separator = Base64.encodeURI(rsaKey) + '='
  const keyArray = clientKey.split(separator)
  const k1 = keyArray[0]
  const k2 = keyArray[1]
  const pk = aesDecrypt(k1, k2)
  crypt.setKey(pk)
  return crypt.encrypt(word)
}

export const symmetricDecrypt = (data, keyStr) => {
  if (typeof data === 'string' && data.startsWith(sm4CipherPrefix)) {
    const [iv, ciphertext, mac] = data.substring(sm4CipherPrefix.length).split(':')
    const keyHex = CryptoJS.enc.Base64.parse(keyStr).toString(CryptoJS.enc.Hex)
    if (mac && !constantTimeEqualsHex(sm3(`${iv}:${ciphertext}`, { key: keyHex }), mac)) {
      throw new Error('SM4 payload integrity check failed')
    }
    return sm4.decrypt(ciphertext, keyHex, {
      mode: 'cbc',
      iv
    })
  }
  const key = CryptoJS.enc.Base64.parse(keyStr)
  if (typeof data === 'string' && data.startsWith(aesCtrHmacPrefix)) {
    return aesCtrHmacDecrypt(data, key)
  }
  const iv = CryptoJS.enc.Utf8.parse('0000000000000000')
  const decodedCiphertext = CryptoJS.enc.Base64.parse(data)
  const decrypted = CryptoJS.AES.decrypt({ ciphertext: decodedCiphertext }, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypted.toString(CryptoJS.enc.Utf8)
}

const aesCtrHmacDecrypt = (data: string, key) => {
  const [ivText, ciphertextText, mac] = data.substring(aesCtrHmacPrefix.length).split(':')
  if (!ivText || !ciphertextText || !mac) {
    throw new Error('AES payload is invalid')
  }
  const expectedMac = CryptoJS.HmacSHA256(`${ivText}:${ciphertextText}`, key).toString(CryptoJS.enc.Hex)
  if (!constantTimeEqualsHex(expectedMac, mac)) {
    throw new Error('AES payload integrity check failed')
  }
  const iv = base64UrlToWordArray(ivText)
  const ciphertext = base64UrlToWordArray(ciphertextText)
  const decrypted = CryptoJS.AES.decrypt({ ciphertext }, key, {
    iv,
    mode: CryptoJS.mode.CTR,
    padding: CryptoJS.pad.NoPadding
  })
  return decrypted.toString(CryptoJS.enc.Utf8)
}

const base64UrlToWordArray = (value: string) => {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
  return CryptoJS.enc.Base64.parse(padded)
}

const constantTimeEqualsHex = (left: string, right: string) => {
  if (left.length !== right.length) {
    return false
  }
  let result = 0
  for (let index = 0; index < left.length; index++) {
    result |= left.charCodeAt(index) ^ right.charCodeAt(index)
  }
  return result === 0
}
