const { app, BrowserWindow, dialog, Menu } = require('electron')
const { spawn } = require('child_process')
const path = require('path')
const fs = require('fs')
const net = require('net')

let mainWindow = null
let javaProcess = null

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

function waitForPort(port, timeout = 60000) {
  const javaLogs = []

  return new Promise((resolve, reject) => {
    const startTime = Date.now()

    if (javaProcess) {
      javaProcess.stdout.on('data', (data) => javaLogs.push(data.toString()))
      javaProcess.stderr.on('data', (data) => javaLogs.push(data.toString()))
    }

    function tryConnect() {
      if (!javaProcess) {
        reject(new Error(`Java 进程已退出\n\n日志:\n${javaLogs.join('')}`))
        return
      }

      const socket = new net.Socket()
      socket.setTimeout(2000)

      socket.on('connect', () => {
        socket.destroy()
        resolve()
      })

      socket.on('error', () => {
        socket.destroy()
        if (Date.now() - startTime > timeout) {
          reject(new Error(`端口 ${port} ${timeout / 1000}s 内未就绪\n\n日志:\n${javaLogs.join('').slice(-2000)}`))
        } else {
          setTimeout(tryConnect, 1000)
        }
      })

      socket.on('timeout', () => {
        socket.destroy()
        if (Date.now() - startTime > timeout) {
          reject(new Error(`端口 ${port} ${timeout / 1000}s 内未就绪\n\n日志:\n${javaLogs.join('').slice(-2000)}`))
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

function isPortInUse(port) {
  return new Promise((resolve) => {
    const socket = new net.Socket()
    socket.setTimeout(2000)

    socket.on('connect', () => {
      socket.destroy()
      resolve(true)
    })

    socket.on('error', () => {
      socket.destroy()
      resolve(false)
    })

    socket.on('timeout', () => {
      socket.destroy()
      resolve(false)
    })

    socket.connect(port, '127.0.0.1')
  })
}

app.whenReady().then(async () => {
  const alreadyRunning = await isPortInUse(8080)

  if (alreadyRunning) {
    const { response } = await dialog.showMessageBox({
      type: 'warning',
      title: '端口冲突',
      message: '端口 8080 已被占用',
      detail: '可能是其他程序或已启动的后端服务。点击"继续"尝试连接，点击"退出"关闭程序。',
      buttons: ['继续', '退出'],
      defaultId: 0,
      cancelId: 1
    })

    if (response === 1) {
      app.quit()
      return
    }

    console.log('[Electron] Backend already running on port 8080, attempting to connect')
  } else {
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

    const javaArgs = [
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
      console.log(`[Java] ${data.toString().trim()}`)
    })

    javaProcess.stderr.on('data', (data) => {
      console.error(`[Java] ${data.toString().trim()}`)
    })

    javaProcess.on('error', (err) => {
      showErrorAndQuit('启动失败', `无法启动 Java:\n${err.message}`)
    })

    javaProcess.on('exit', (code) => {
      console.log(`[Java] exited with code ${code}`)
      javaProcess = null

      if (mainWindow && !mainWindow.isDestroyed()) {
        showErrorAndQuit('后端异常', `Java 退出 (code: ${code})`)
      }
    })

    try {
      await waitForPort(8080)
    } catch (err) {
      showErrorAndQuit('启动超时', err.message)
      return
    }
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

  const url = 'http://localhost:8080/admin/'
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
          click: () => mainWindow.loadURL('http://localhost:8080/admin/')
        },
        {
          label: 'Conversation',
          accelerator: 'CmdOrCtrl+2',
          click: () => mainWindow.loadURL('http://localhost:8080/conversation/')
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
  if (javaProcess) {
    javaProcess.kill()
    javaProcess = null
  }
})