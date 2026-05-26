const { app, BrowserWindow, dialog, Menu } = require('electron')
const { spawn } = require('child_process')
const path = require('path')
const fs = require('fs')
const net = require('net')

let mainWindow = null
let javaProcess = null
let quitting = false
const javaLogs = []

const MAX_LOG_LINES = 200

function collectLog(data) {
  const lines = data.toString().trim().split('\n')
  javaLogs.push(...lines)
  if (javaLogs.length > MAX_LOG_LINES) {
    javaLogs.splice(0, javaLogs.length - MAX_LOG_LINES)
  }
}

function getRecentLogs(maxChars = 2000) {
  const joined = javaLogs.join('\n')
  return joined.length > maxChars ? joined.slice(-maxChars) : joined
}

function findJavaBin(dir) {
  const javaName = process.platform === 'win32' ? 'java.exe' : 'java'
  const candidates = process.platform === 'darwin'
      ? [
        path.join(dir, 'Contents', 'Home', 'bin', javaName),
        path.join(dir, 'bin', javaName),
      ]
      : [
        path.join(dir, 'bin', javaName),
      ]

  for (const p of candidates) {
    if (fs.existsSync(p)) return p
  }
  return null
}

function getBaseDir() {
  return app.isPackaged ? process.resourcesPath : __dirname
}

function getJrePath() {
  const jreDir = path.join(getBaseDir(), 'jre')

  const direct = findJavaBin(jreDir)
  if (direct) return direct

  if (fs.existsSync(jreDir)) {
    for (const entry of fs.readdirSync(jreDir)) {
      const nested = path.join(jreDir, entry)
      if (fs.statSync(nested).isDirectory()) {
        const found = findJavaBin(nested)
        if (found) return found
      }
    }
  }

  return path.join(jreDir, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
}

function getJarPath() {
  return path.join(getBaseDir(), 'runify.jar')
}

function getAvailablePort() {
  return new Promise((resolve, reject) => {
    const server = net.createServer()
    server.on('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const port = server.address().port
      server.close(() => resolve(port))
    })
  })
}

function waitForPort(port, timeout = 60000) {
  return new Promise((resolve, reject) => {
    const startTime = Date.now()
    let settled = false

    function settle(fn) {
      if (settled) return
      settled = true
      fn()
    }

    function tryConnect() {
      if (settled) return

      if (!javaProcess) {
        settle(() => reject(new Error(`Java 进程已退出\n\n日志:\n${getRecentLogs()}`)))
        return
      }

      const socket = new net.Socket()
      socket.setTimeout(2000)

      socket.on('connect', () => {
        socket.destroy()
        settle(() => resolve())
      })

      socket.on('error', () => {
        socket.destroy()
        if (Date.now() - startTime > timeout) {
          settle(() => reject(new Error(`端口 ${port} ${timeout / 1000}s 内未就绪\n\n日志:\n${getRecentLogs()}`)))
        } else {
          setTimeout(tryConnect, 1000)
        }
      })

      socket.on('timeout', () => {
        socket.destroy()
        if (Date.now() - startTime > timeout) {
          settle(() => reject(new Error(`端口 ${port} ${timeout / 1000}s 内未就绪\n\n日志:\n${getRecentLogs()}`)))
        } else {
          setTimeout(tryConnect, 1000)
        }
      })

      socket.connect(port, '127.0.0.1')
    }

    tryConnect()
  })
}

function showErrorAndQuit(title, message) {
  console.error(`[${title}] ${message}`)

  if (app.isReady()) {
    dialog.showErrorBox(title, message)
    app.quit()
  } else {
    app.whenReady().then(() => {
      dialog.showErrorBox(title, message)
      app.quit()
    })
  }
}

function initConfig() {
  const v1Dir = path.join(app.getPath('userData'), 'v1')
  const configDir = path.join(v1Dir, 'config')
  const configPath = path.join(configDir, 'runify.yaml')

  if (!fs.existsSync(configPath)) {
    const { randomBytes } = require('crypto')
    const dataPath = path.join(v1Dir, 'data')
    fs.mkdirSync(configDir, { recursive: true })
    fs.writeFileSync(configPath, [
      'system:',
      '  dataPath: ' + dataPath,
      '  secretKey: ' + randomBytes(32).toString('hex'),
      'database:',
      '  type: SQLITE',
      'cache:',
      '  type: LOCAL',
      'search:',
      '  type: LUCENE',
    ].join('\n') + '\n')
    console.log(`[Electron] Config initialized: ${configPath}`)
  }

  return configPath
}

function killJavaProcess() {
  if (!javaProcess) return

  const proc = javaProcess
  javaProcess = null

  try {
    if (process.platform === 'win32') {
      spawn('taskkill', ['/pid', String(proc.pid), '/f', '/t'])
    } else {
      proc.kill('SIGTERM')
      setTimeout(() => {
        try { proc.kill('SIGKILL') } catch (_) { /* already dead */ }
      }, 5000)
    }
  } catch (_) {
    /* process already exited */
  }
}


app.whenReady().then(async () => {
  let port
  try {
    port = await getAvailablePort()
  } catch (err) {
    showErrorAndQuit('启动失败', `无法获取可用端口:\n${err.message}`)
    return
  }
  console.log(`[Electron] Using port: ${port}`)

  const java = getJrePath()
  const jar = getJarPath()
  const cwd = path.dirname(jar)

  const javaExists = fs.existsSync(java)
  const jarExists = fs.existsSync(jar)

  console.log(`[Electron] java: ${java} (exists: ${javaExists})`)
  console.log(`[Electron] jar: ${jar} (exists: ${jarExists})`)
  console.log(`[Electron] cwd: ${cwd}`)

  if (!javaExists) {
    showErrorAndQuit('启动失败', `找不到 Java:\n${java}`)
    return
  }

  if (!jarExists) {
    showErrorAndQuit('启动失败', `找不到 JAR:\n${jar}`)
    return
  }

  let configPath
  try {
    configPath = initConfig()
  } catch (err) {
    showErrorAndQuit('启动失败', `配置初始化失败:\n${err.message}`)
    return
  }

  const javaArgs = [
    `-Dport=${port}`,
    `-Drunify.config=${configPath}`,
    '-Dpolyglotimpl.DisableMultiReleaseCheck=true',
    '-jar',
    jar
  ]

  console.log(`[Electron] java args: ${javaArgs.join(' ')}`)

  javaProcess = spawn(java, javaArgs, {
    cwd,
    stdio: ['ignore', 'pipe', 'pipe']
  })

  javaProcess.stdout.on('data', (data) => {
    collectLog(data)
    console.log(`[Java] ${data.toString().trim()}`)
  })

  javaProcess.stderr.on('data', (data) => {
    collectLog(data)
    console.error(`[Java] ${data.toString().trim()}`)
  })

  javaProcess.on('error', (err) => {
    showErrorAndQuit('启动失败', `无法启动 Java:\n${err.message}`)
  })

  javaProcess.on('exit', (code) => {
    console.log(`[Java] exited with code ${code}`)
    javaProcess = null

    if (!quitting && mainWindow && !mainWindow.isDestroyed()) {
      showErrorAndQuit('后端异常', `Java 退出 (code: ${code})\n\n日志:\n${getRecentLogs()}`)
    }
  })

  try {
    await waitForPort(port)
  } catch (err) {
    showErrorAndQuit('启动超时', err.message)
    return
  }

  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    title: 'Runify',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    }
  })

  const url = `http://localhost:${port}/admin/`
  console.log(`[Electron] Loading: ${url}`)

  mainWindow.webContents.on('did-finish-load', () => {
    console.log('[Electron] Page loaded successfully')
  })

  mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription) => {
    console.error(`[Electron] Page load failed: ${errorCode} ${errorDescription}`)
  })

  mainWindow.webContents.on('console-message', (event, level, message) => {
    console.log(`[Renderer] ${message}`)
  })

  mainWindow.loadURL(url)

  const menuTemplate = [
    {
      label: 'Runify',
      submenu: [
        {
          label: 'Admin',
          accelerator: 'CmdOrCtrl+1',
          click: () => mainWindow.loadURL(`http://localhost:${port}/admin/`)
        },
        {
          label: 'Conversation',
          accelerator: 'CmdOrCtrl+2',
          click: () => mainWindow.loadURL(`http://localhost:${port}/conversation/`)
        },
        { type: 'separator' },
        { role: 'quit' }
      ]
    },
    {
      label: 'Edit',
      submenu: [
        { role: 'undo' },
        { role: 'redo' },
        { type: 'separator' },
        { role: 'cut' },
        { role: 'copy' },
        { role: 'paste' },
        { role: 'selectAll' }
      ]
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'toggleDevTools' },
        { type: 'separator' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { role: 'resetZoom' },
        { type: 'separator' },
        { role: 'togglefullscreen' }
      ]
    }
  ]

  Menu.setApplicationMenu(Menu.buildFromTemplate(menuTemplate))

  if (process.platform !== 'darwin') {
    mainWindow.setMenuBarVisibility(false)
  }

  mainWindow.on('closed', () => {
    mainWindow = null
  })
})

app.on('window-all-closed', () => {
  app.quit()
})

app.on('before-quit', () => {
  quitting = true
  killJavaProcess()
})