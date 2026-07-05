import path from 'path'
import { opendirSync, type Dirent } from 'fs'

const MAX_PAGE_DIRECTORIES = 200

export interface Page {
  name: string
  path?: string // 如'pages/dev'
  template?: string
}

function readPages(srcDir: string): Page[] {
  // nosemgrep: javascript.lang.security.audit.path-traversal.path-join-resolve-traversal.path-join-resolve-traversal
  const pagesDir = path.resolve(srcDir, 'pages')
  const pageEntries = readDirectoryEntries(pagesDir, MAX_PAGE_DIRECTORIES, 'Page directory')
  let pages: Page[] = pageEntries
    .filter(o => o.isDirectory() && !/^[._]/.test(o.name) && !/^lib/.test(o.name))
    // nosemgrep: javascript.lang.security.audit.path-traversal.path-join-resolve-traversal.path-join-resolve-traversal
    .map(o => ({ name: o.name, path: path.join('pages', o.name) }))
  if (!pages.length) {
    pages = [
      {
        name: 'index',
        path: ''
      }
    ]
  }

  return pages
}

function readDirectoryEntries(dir: string, maxEntries: number, label: string): Dirent[] {
  const directory = opendirSync(dir)
  const entries: Dirent[] = []
  try {
    for (;;) {
      const entry = directory.readSync()
      if (!entry) {
        return entries
      }
      entries.push(entry)
      if (entries.length > maxEntries) {
        throw new Error(`${label} count exceeds ${maxEntries}`)
      }
    }
  } finally {
    directory.closeSync()
  }
}
export const ROOT_DIR = path.resolve(__dirname, '../')

export const PAGES = readPages(path.resolve(ROOT_DIR, 'src'))
