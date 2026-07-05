import request from '@/config/axios'
import { guid } from '@/views/visualized/data/dataset/form/util.js'
import { ElMessage } from 'element-plus-secondary'

const staticResourcePath = '/static-resource/'
const supportedImageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'svg']
export const STATIC_IMAGE_FILE_ACCEPT = supportedImageExtensions.map(ext => `.${ext}`).join(',')

function fileExtension(fileName = '') {
  const lastDotIndex = fileName.lastIndexOf('.')
  if (lastDotIndex < 0 || lastDotIndex === fileName.length - 1) {
    return ''
  }
  return fileName.substring(lastDotIndex + 1).toLowerCase()
}

export function isSupportedStaticImage(file) {
  const extension = fileExtension(file?.name)
  const mimeType = file?.type || ''
  const isSupportedExtension = supportedImageExtensions.includes(extension)
  return isSupportedExtension && (!mimeType || mimeType.startsWith('image/'))
}

export const uploadFile = (fileId: number | string, param) =>
  request.post({
    url: '/static-resource/upload/' + fileId,
    headersType: 'multipart/form-data',
    loading: true,
    data: param
  })

export function beforeUploadCheck(file) {
  const isImage = isSupportedStaticImage(file)
  const isSizeValid = file.size / 1024 / 1024 < 15 // 15MB

  if (!isImage) {
    ElMessage.error('仅支持 JPG、JPEG、PNG、GIF、SVG 图片')
    return false
  }
  if (!isSizeValid) {
    ElMessage.error('图片大小不能超过15M')
    return false
  }
  return true
}

export function uploadFileResult(file, callback) {
  const fileId = guid()
  const fileName = file.name
  const newFileName = fileId + fileName.substr(fileName.lastIndexOf('.'), fileName.length)
  const fileUrl = staticResourcePath + newFileName
  const param = new FormData()
  param.append('file', file)
  return uploadFile(fileId, param)
    .then(() => {
      callback(fileUrl, null)
    })
    .catch(error => {
      callback(null, error)
    })
}

export function resourceBase64(params) {
  return request.post({
    url: '/static-resource/resource-base64',
    data: params
  })
}
