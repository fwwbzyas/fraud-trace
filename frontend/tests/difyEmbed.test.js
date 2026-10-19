import test from 'node:test'
import assert from 'node:assert/strict'
import { initDifyEmbed } from '../src/config/difyEmbed.js'

test('disabled Dify does not access the DOM or load a script', () => {
  initDifyEmbed({ VITE_DIFY_ENABLED: 'false' })
  initDifyEmbed({})
})

test('missing or invalid embed settings do not load a script', () => {
  const previous = console.warn
  console.warn = () => {}
  try {
    initDifyEmbed({ VITE_DIFY_ENABLED: 'true' })
    for (const url of ['javascript:alert(1)', 'not-a-url', 'https://user:password@example.com']) {
      initDifyEmbed({ VITE_DIFY_ENABLED: 'true', VITE_DIFY_BASE_URL: url, VITE_DIFY_EMBED_TOKEN: 'demo-id' })
    }
  } finally { console.warn = previous }
})

test('configured embed loads once and uses the public application id', () => {
  const scripts = []
  globalThis.window = {}
  globalThis.document = {
    querySelector: () => scripts[0] || null,
    createElement: () => ({ dataset: {} }),
    body: { appendChild: script => scripts.push(script) }
  }
  try {
    const config = {
      VITE_DIFY_ENABLED: 'true',
      VITE_DIFY_BASE_URL: 'https://dify.example.com/',
      VITE_DIFY_EMBED_TOKEN: 'demo-id'
    }
    initDifyEmbed(config)
    initDifyEmbed(config)
    assert.equal(scripts.length, 1)
    assert.equal(scripts[0].src, 'https://dify.example.com/embed.min.js')
    assert.equal(scripts[0].id, 'demo-id')
    assert.equal(window.difyChatbotConfig.token, 'demo-id')
  } finally {
    delete globalThis.window
    delete globalThis.document
  }
})
