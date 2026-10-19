// This is a public chatbot embed identifier, never a server-side Dify API key.
export function initDifyEmbed(config = import.meta.env) {
  if (config.VITE_DIFY_ENABLED !== 'true') return
  const token = config.VITE_DIFY_EMBED_TOKEN?.trim()
  const address = config.VITE_DIFY_BASE_URL?.trim()
  if (!token || !address) {
    console.warn('Dify 挂件未启用：请填写地址和公开的挂件应用标识。')
    return
  }
  let baseUrl
  try {
    const url = new URL(address)
    if (!['http:', 'https:'].includes(url.protocol)) throw new Error('Invalid protocol')
    if (url.username || url.password || url.search || url.hash) throw new Error('Invalid URL')
    baseUrl = url.href.replace(/\/$/, '')
  } catch {
    console.warn('Dify 挂件地址必须是有效的 HTTP 或 HTTPS 地址。')
    return
  }
  if (document.querySelector('script[data-fraud-trace-dify]')) return
  window.difyChatbotConfig = { token, baseUrl, inputs: {}, systemVariables: {}, userVariables: {} }
  const script = document.createElement('script')
  script.src = `${baseUrl}/embed.min.js`
  // Dify identifies the application from the embed script id.
  script.id = token
  script.dataset.fraudTraceDify = 'true'
  script.defer = true
  script.onerror = () => console.warn('Dify 挂件加载失败，请检查服务地址。')
  document.body.appendChild(script)
}
