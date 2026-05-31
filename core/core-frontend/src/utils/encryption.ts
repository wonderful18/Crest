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

const aesDecrypt = (word, keyStr) => {
  const keyHex = CryptoJS.enc.Utf8.parse(keyStr) //
  const ivHex = CryptoJS.enc.Utf8.parse('0000000000000000')
  const decrypt = CryptoJS.AES.decrypt(word, keyHex, {
    iv: ivHex,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypt.toString(CryptoJS.enc.Utf8)
}

export const rsaEncryp = word => {
  const dekey = wsCache.get(appStore.getDekey)
  if (typeof dekey === 'string' && dekey.startsWith(smSuitePublicKeyPrefix)) {
    const publicKey = dekey.substring(smSuitePublicKeyPrefix.length)
    return sm2CipherPrefix + sm2.doEncrypt(word, publicKey, 1)
  }
  const separator = Base64.encodeURI(rsaKey) + '='
  const keyArray = dekey.split(separator)
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
  const iv = CryptoJS.enc.Utf8.parse('0000000000000000')
  const key = CryptoJS.enc.Base64.parse(keyStr)
  const decodedCiphertext = CryptoJS.enc.Base64.parse(data)
  const decrypted = CryptoJS.AES.decrypt({ ciphertext: decodedCiphertext }, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypted.toString(CryptoJS.enc.Utf8)
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
