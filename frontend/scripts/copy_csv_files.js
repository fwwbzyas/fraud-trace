import { copyFileSync, mkdirSync, existsSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const source = process.argv[2]
if (!source) {
  console.error('Usage: node scripts/copy_csv_files.js <source-directory>')
  process.exit(1)
}
const target = resolve(dirname(fileURLToPath(import.meta.url)), '../public/data')
const groups = [
  { names: ['account.csv', 'accounts.csv', 'account(1).csv'], target: 'account.csv' },
  { names: ['logins.csv', 'logins(1).csv'], target: 'logins.csv' },
  { names: ['transactions.csv'], target: 'transactions.csv' }
]
const files = groups.map(group => {
  const name = group.names.find(name => existsSync(resolve(source, name)))
  if (!name) throw new Error(`Missing CSV: ${group.names.join(' / ')}`)
  return [resolve(source, name), resolve(target, group.target)]
})
mkdirSync(target, { recursive: true })
for (const [from, to] of files) {
  if (from !== to) copyFileSync(from, to)
  console.log(`Copied ${to}`)
}
