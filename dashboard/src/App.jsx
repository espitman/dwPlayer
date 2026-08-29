import React, { useState, useEffect } from 'react'
import axios from 'axios'
import {
  Download,
  Play,
  Pause,
  Trash2,
  HardDrive,
  Folder,
  Server,
  Plus,
  Tv,
  CheckCircle2,
  AlertCircle,
  Clock,
  Zap,
  RefreshCw,
  FolderOpen,
  ArrowRight,
  ExternalLink
} from 'lucide-react'

export default function App() {
  const [activeTab, setActiveTab] = useState('downloads') // 'downloads' | 'smb' | 'storage'
  
  // Download Form State
  const [url, setUrl] = useState('')
  const [customName, setCustomName] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [message, setMessage] = useState(null)

  // Live Downloads State
  const [tasks, setTasks] = useState([])
  const [summary, setSummary] = useState({ total: 0, active: 0, completed: 0 })
  const [loadingTasks, setLoadingTasks] = useState(false)

  // SMB State
  const [smbShares, setSmbShares] = useState([])
  const [smbName, setSmbName] = useState('')
  const [smbHost, setSmbHost] = useState('')
  const [smbShareName, setSmbShareName] = useState('')
  const [smbUser, setSmbUser] = useState('')
  const [smbPass, setSmbPass] = useState('')
  const [smbDomain, setSmbDomain] = useState('')
  const [isTestingSmb, setIsTestingSmb] = useState(false)
  const [smbTestResult, setSmbTestResult] = useState(null)
  
  // SMB Browser State
  const [currentShare, setCurrentShare] = useState(null)
  const [currentPath, setCurrentPath] = useState('')
  const [browseItems, setBrowseItems] = useState([])
  const [isBrowsing, setIsBrowsing] = useState(false)

  // Storage State
  const [storageInfo, setStorageInfo] = useState({ freeSpace: '0 GB', totalSpace: '0 GB', usedPercent: 0, path: '' })

  // Poll tasks & status
  useEffect(() => {
    fetchTasks()
    fetchStorage()
    fetchSmbShares()

    const interval = setInterval(() => {
      fetchTasksSilently()
    }, 2000)
    return () => clearInterval(interval)
  }, [])

  const showToast = (type, text) => {
    setMessage({ type, text })
    setTimeout(() => setMessage(null), 5000)
  }

  const fetchTasks = async () => {
    setLoadingTasks(true)
    try {
      const res = await axios.get('/api/status/live')
      if (res.data && res.data.tasks) {
        setTasks(res.data.tasks)
        if (res.data.summary) setSummary(res.data.summary)
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoadingTasks(false)
    }
  }

  const fetchTasksSilently = async () => {
    try {
      const res = await axios.get('/api/status/live')
      if (res.data && res.data.tasks) {
        setTasks(res.data.tasks)
        if (res.data.summary) setSummary(res.data.summary)
      }
    } catch (e) {
      // silent
    }
  }

  const fetchStorage = async () => {
    try {
      const res = await axios.get('/api/storage/info')
      if (res.data) setStorageInfo(res.data)
    } catch (e) {
      console.error(e)
    }
  }

  const fetchSmbShares = async () => {
    try {
      const res = await axios.get('/api/smb/shares')
      if (Array.isArray(res.data)) setSmbShares(res.data)
    } catch (e) {
      console.error(e)
    }
  }

  const handleAddDownload = async (e) => {
    e.preventDefault()
    if (!url.trim()) return
    setIsSubmitting(true)
    try {
      const res = await axios.post('/api/download', {
        url: url.trim(),
        fileName: customName.trim() || undefined
      })
      if (res.data && res.data.status === 'success') {
        showToast('success', 'Download task added successfully to dwPlayer!')
        setUrl('')
        setCustomName('')
        fetchTasks()
      } else {
        showToast('error', res.data.message || 'Failed to add download.')
      }
    } catch (e) {
      showToast('error', e.response?.data?.message || e.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handlePause = async (id) => {
    try {
      await axios.post(`/api/downloads/${id}/pause`)
      fetchTasks()
    } catch (e) {
      showToast('error', 'Failed to pause task')
    }
  }

  const handleResume = async (id) => {
    try {
      await axios.post(`/api/downloads/${id}/resume`)
      fetchTasks()
    } catch (e) {
      showToast('error', 'Failed to resume task')
    }
  }

  const handleDelete = async (id, deleteFile = true) => {
    try {
      await axios.delete(`/api/downloads/${id}?deleteFile=${deleteFile}`)
      showToast('success', 'Task removed')
      fetchTasks()
      fetchStorage()
    } catch (e) {
      showToast('error', 'Failed to delete task')
    }
  }

  const handlePlayOnTv = async (id) => {
    try {
      await axios.post(`/api/downloads/${id}/play`)
      showToast('success', 'Playback requested on TV!')
    } catch (e) {
      showToast('error', 'Failed to start playback on TV')
    }
  }

  const handleTestSmb = async () => {
    if (!smbHost || !smbShareName) {
      showToast('error', 'Host and Share name are required')
      return
    }
    setIsTestingSmb(true)
    setSmbTestResult(null)
    try {
      const res = await axios.post('/api/smb/test', {
        host: smbHost,
        shareName: smbShareName,
        username: smbUser,
        password: smbPass,
        domain: smbDomain
      })
      setSmbTestResult(res.data)
    } catch (e) {
      setSmbTestResult({ status: 'error', message: e.response?.data?.message || e.message })
    } finally {
      setIsTestingSmb(false)
    }
  }

  const handleSaveSmbShare = async (e) => {
    e.preventDefault()
    if (!smbHost || !smbShareName) return
    try {
      await axios.post('/api/smb/shares', {
        name: smbName || `${smbHost}/${smbShareName}`,
        host: smbHost,
        shareName: smbShareName,
        username: smbUser,
        password: smbPass,
        domain: smbDomain
      })
      showToast('success', 'SMB Share saved successfully')
      setSmbName('')
      setSmbHost('')
      setSmbShareName('')
      setSmbUser('')
      setSmbPass('')
      setSmbDomain('')
      setSmbTestResult(null)
      fetchSmbShares()
    } catch (e) {
      showToast('error', 'Failed to save SMB share')
    }
  }

  const handleDeleteSmbShare = async (id) => {
    try {
      await axios.delete(`/api/smb/shares/${id}`)
      showToast('success', 'SMB Share removed')
      fetchSmbShares()
    } catch (e) {
      showToast('error', 'Failed to delete SMB share')
    }
  }

  const handleBrowseSmb = async (share, path = '') => {
    setCurrentShare(share)
    setCurrentPath(path)
    setIsBrowsing(true)
    try {
      const res = await axios.get('/api/smb/browse', {
        params: { shareId: share.id, path: path }
      })
      setBrowseItems(res.data.items || [])
    } catch (e) {
      showToast('error', 'Failed to browse folder')
    } finally {
      setIsBrowsing(false)
    }
  }

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  return (
    <div className="min-h-screen bg-[#0B0F19] text-slate-100 flex flex-col">
      {/* Header */}
      <header className="border-b border-white/10 bg-[#131A2B]/80 backdrop-blur-xl sticky top-0 z-40">
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-blue-500/25">
              <Tv className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-xl font-black tracking-tight text-white flex items-center gap-2">
                dwPlayer <span className="text-xs px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-400 border border-blue-500/30 font-semibold">TV Companion</span>
              </h1>
              <p className="text-xs text-slate-400">Android TV Remote Manager & Downloader</p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <nav className="flex space-x-1 bg-black/30 p-1 rounded-xl border border-white/5">
              <button
                onClick={() => setActiveTab('downloads')}
                className={`px-4 py-2 rounded-lg text-xs font-bold transition flex items-center gap-2 ${
                  activeTab === 'downloads'
                    ? 'bg-blue-600 text-white shadow-md'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Download className="w-4 h-4" /> Downloads ({summary.active})
              </button>
              <button
                onClick={() => setActiveTab('smb')}
                className={`px-4 py-2 rounded-lg text-xs font-bold transition flex items-center gap-2 ${
                  activeTab === 'smb'
                    ? 'bg-blue-600 text-white shadow-md'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                <Server className="w-4 h-4" /> SMB Shares ({smbShares.length})
              </button>
            </nav>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-6xl mx-auto px-4 py-8 flex-1 w-full">
        {/* Toast Alert */}
        {message && (
          <div
            className={`mb-6 p-4 rounded-xl flex items-center gap-3 border animate-in fade-in slide-in-from-top-4 duration-300 ${
              message.type === 'success'
                ? 'bg-emerald-950/60 border-emerald-500/30 text-emerald-300'
                : 'bg-rose-950/60 border-rose-500/30 text-rose-300'
            }`}
          >
            {message.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-400" />
            ) : (
              <AlertCircle className="w-5 h-5 flex-shrink-0 text-rose-400" />
            )}
            <span className="text-sm font-medium">{message.text}</span>
          </div>
        )}

        {/* Tab 1: Downloads */}
        {activeTab === 'downloads' && (
          <div className="space-y-8">
            {/* Quick Add Download Card */}
            <div className="glass-panel p-6 rounded-2xl relative overflow-hidden">
              <div className="absolute top-0 right-0 w-80 h-80 bg-blue-500/10 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20" />
              <h2 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
                <Plus className="w-5 h-5 text-blue-400" /> Add New Movie / Video URL
              </h2>
              <p className="text-xs text-slate-400 mb-6">
                Paste any direct HTTP/HTTPS download link. The Android TV will download it in the background using multi-segment acceleration.
              </p>

              <form onSubmit={handleAddDownload} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                    Video Download Link (URL) *
                  </label>
                  <input
                    type="url"
                    required
                    placeholder="https://example.com/movies/interstellar.2014.1080p.mkv"
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    className="w-full bg-[#0E1524] border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition"
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Custom File Name (Optional)
                    </label>
                    <input
                      type="text"
                      placeholder="Interstellar.2014.mkv (leave blank to auto-detect)"
                      value={customName}
                      onChange={(e) => setCustomName(e.target.value)}
                      className="w-full bg-[#0E1524] border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition"
                    />
                  </div>

                  <div className="flex items-end">
                    <button
                      type="submit"
                      disabled={isSubmitting || !url.trim()}
                      className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-bold py-3 px-6 rounded-xl shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 disabled:opacity-50 transition"
                    >
                      {isSubmitting ? (
                        <RefreshCw className="w-4 h-4 animate-spin" />
                      ) : (
                        <Download className="w-4 h-4" />
                      )}
                      <span>Send to Android TV</span>
                    </button>
                  </div>
                </div>
              </form>
            </div>

            {/* Storage Info Banner */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="glass-card p-4 rounded-xl flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="p-3 bg-blue-500/10 rounded-lg text-blue-400">
                    <HardDrive className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs text-slate-400 font-semibold">TV Storage Space</div>
                    <div className="text-sm font-bold text-white">{storageInfo.freeSpace} Free / {storageInfo.totalSpace}</div>
                  </div>
                </div>
                <div className="text-xs font-bold text-blue-400">{storageInfo.usedPercent}% Used</div>
              </div>

              <div className="glass-card p-4 rounded-xl flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="p-3 bg-amber-500/10 rounded-lg text-amber-400">
                    <Zap className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs text-slate-400 font-semibold">Active Downloads</div>
                    <div className="text-sm font-bold text-white">{summary.active} tasks in progress</div>
                  </div>
                </div>
              </div>

              <div className="glass-card p-4 rounded-xl flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="p-3 bg-emerald-500/10 rounded-lg text-emerald-400">
                    <CheckCircle2 className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs text-slate-400 font-semibold">Completed</div>
                    <div className="text-sm font-bold text-white">{summary.completed} downloaded files</div>
                  </div>
                </div>
              </div>
            </div>

            {/* Tasks List */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Download className="w-4 h-4 text-blue-400" /> Active & Saved Tasks ({tasks.length})
                </h3>
                <button
                  onClick={fetchTasks}
                  className="text-xs text-slate-400 hover:text-white flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 border border-white/5 transition"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${loadingTasks ? 'animate-spin' : ''}`} /> Refresh
                </button>
              </div>

              {tasks.length === 0 ? (
                <div className="glass-panel p-12 rounded-2xl text-center">
                  <Download className="w-12 h-12 text-slate-600 mx-auto mb-3" />
                  <p className="text-sm font-semibold text-slate-400">No downloads yet.</p>
                  <p className="text-xs text-slate-500 mt-1">Paste a video link above to start downloading on your TV.</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-4">
                  {tasks.map((task) => (
                    <div
                      key={task.id}
                      className="glass-card p-5 rounded-2xl hover:border-white/20 transition duration-200"
                    >
                      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                        {/* Title & Metadata */}
                        <div className="space-y-1 max-w-xl">
                          <div className="flex items-center gap-2">
                            <span
                              className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded-md ${
                                task.status === 'ACTIVE'
                                  ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                                  : task.status === 'COMPLETED'
                                  ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                                  : task.status === 'PAUSED'
                                  ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                                  : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                              }`}
                            >
                              {task.status}
                            </span>
                            <h4 className="text-sm font-bold text-white truncate max-w-md" title={task.fileName}>
                              {task.fileName}
                            </h4>
                          </div>

                          <p className="text-xs text-slate-500 truncate font-mono" title={task.url}>
                            {task.url}
                          </p>

                          {/* Speed & Size metrics */}
                          <div className="flex items-center gap-4 text-xs text-slate-400 pt-1">
                            <span>
                              {formatBytes(task.downloadedBytes)} / {task.totalBytes > 0 ? formatBytes(task.totalBytes) : 'Unknown'}
                            </span>
                            {task.speed && (
                              <>
                                <span>•</span>
                                <span className="text-blue-400 font-semibold flex items-center gap-1">
                                  <Zap className="w-3 h-3" /> {task.speed}
                                </span>
                              </>
                            )}
                            {task.timeRemaining && (
                              <>
                                <span>•</span>
                                <span className="flex items-center gap-1">
                                  <Clock className="w-3 h-3" /> {task.timeRemaining} left
                                </span>
                              </>
                            )}
                          </div>
                        </div>

                        {/* Progress and Actions */}
                        <div className="flex items-center gap-4">
                          <div className="w-32 hidden md:block">
                            <div className="flex justify-between text-xs font-semibold mb-1">
                              <span className="text-slate-400">Progress</span>
                              <span className="text-blue-400">{task.progress}%</span>
                            </div>
                            <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                              <div
                                className={`h-full rounded-full transition-all duration-300 ${
                                  task.status === 'COMPLETED'
                                    ? 'bg-emerald-500'
                                    : 'bg-gradient-to-r from-blue-500 to-indigo-500'
                                }`}
                                style={{ width: `${task.progress}%` }}
                              />
                            </div>
                          </div>

                          {/* Control Buttons */}
                          <div className="flex items-center gap-2">
                            {task.status === 'ACTIVE' ? (
                              <button
                                onClick={() => handlePause(task.id)}
                                className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 hover:text-white transition"
                                title="Pause"
                              >
                                <Pause className="w-4 h-4" />
                              </button>
                            ) : task.status === 'PAUSED' || task.status === 'FAILED' ? (
                              <button
                                onClick={() => handleResume(task.id)}
                                className="p-2 rounded-xl bg-blue-500/20 hover:bg-blue-500/30 text-blue-400 transition"
                                title="Resume"
                              >
                                <Play className="w-4 h-4" />
                              </button>
                            ) : null}

                            {task.status === 'COMPLETED' && (
                              <button
                                onClick={() => handlePlayOnTv(task.id)}
                                className="px-3 py-1.5 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-400 text-xs font-bold flex items-center gap-1.5 transition"
                                title="Play on TV"
                              >
                                <Play className="w-3.5 h-3.5 fill-current" /> Play on TV
                              </button>
                            )}

                            <button
                              onClick={() => handleDelete(task.id, true)}
                              className="p-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 transition"
                              title="Delete Task & File"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      </div>

                      {/* Mobile progress bar */}
                      <div className="mt-3 md:hidden">
                        <div className="flex justify-between text-xs font-semibold mb-1">
                          <span className="text-slate-400">Progress</span>
                          <span className="text-blue-400">{task.progress}%</span>
                        </div>
                        <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full ${
                              task.status === 'COMPLETED' ? 'bg-emerald-500' : 'bg-blue-500'
                            }`}
                            style={{ width: `${task.progress}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Tab 2: SMB Management */}
        {activeTab === 'smb' && (
          <div className="space-y-8">
            {/* Add SMB Server */}
            <div className="glass-panel p-6 rounded-2xl relative">
              <h2 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
                <Server className="w-5 h-5 text-blue-400" /> Connect SMB / Windows Network Share
              </h2>
              <p className="text-xs text-slate-400 mb-6">
                Connect your PC, Mac, Linux server, Router HDD, or NAS. You can stream movie files directly to the TV or download them.
              </p>

              <form onSubmit={handleSaveSmbShare} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Friendly Display Name
                    </label>
                    <input
                      type="text"
                      placeholder="My PC Movies"
                      value={smbName}
                      onChange={(e) => setSmbName(e.target.value)}
                      className="w-full bg-[#0E1524] border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Host / IP Address *
                    </label>
                    <input
                      type="text"
                      required
                      placeholder="192.168.1.100 or desktop-pc"
                      value={smbHost}
                      onChange={(e) => setSmbHost(e.target.value)}
                      className="w-full bg-[#0E1524] border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Share Folder Name *
                    </label>
                    <input
                      type="text"
                      required
                      placeholder="Movies or Shared"
                      value={smbShareName}
                      onChange={(e) => setSmbShareName(e.target.value)}
                      className="w-full bg-[#0E1524] border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Username (if required)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. guest or administrator"
                      value={smbUser}
                      onChange={(e) => setSmbUser(e.target.value)}
                      className="w-full bg-[#0E1524] border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                      Password
                    </label>
                    <input
                      type="password"
                      placeholder="••••••••"
                      value={smbPass}
                      onChange={(e) => setSmbPass(e.target.value)}
                      className="w-full bg-[#0E1524] border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition"
                    />
                  </div>
                </div>

                {/* Test Result Indicator */}
                {smbTestResult && (
                  <div
                    className={`p-3 rounded-xl text-xs font-semibold flex items-center gap-2 ${
                      smbTestResult.status === 'success'
                        ? 'bg-emerald-950/60 text-emerald-300 border border-emerald-500/30'
                        : 'bg-rose-950/60 text-rose-300 border border-rose-500/30'
                    }`}
                  >
                    {smbTestResult.status === 'success' ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                    ) : (
                      <AlertCircle className="w-4 h-4 text-rose-400" />
                    )}
                    <span>{smbTestResult.message || (smbTestResult.status === 'success' ? 'Connection test succeeded!' : 'Connection test failed.')}</span>
                  </div>
                )}

                <div className="flex items-center gap-3 pt-2">
                  <button
                    type="button"
                    onClick={handleTestSmb}
                    disabled={isTestingSmb || !smbHost || !smbShareName}
                    className="px-5 py-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white font-bold text-xs flex items-center gap-2 transition disabled:opacity-50"
                  >
                    {isTestingSmb ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4 text-amber-400" />}
                    <span>Test Connection</span>
                  </button>

                  <button
                    type="submit"
                    disabled={!smbHost || !smbShareName}
                    className="px-6 py-3 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-bold text-xs flex items-center gap-2 shadow-lg shadow-blue-500/25 transition disabled:opacity-50"
                  >
                    <Plus className="w-4 h-4" /> Save SMB Share
                  </button>
                </div>
              </form>
            </div>

            {/* Saved SMB Shares */}
            <div className="space-y-4">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Server className="w-4 h-4 text-blue-400" /> Saved SMB Servers ({smbShares.length})
              </h3>

              {smbShares.length === 0 ? (
                <div className="glass-panel p-8 rounded-2xl text-center">
                  <Server className="w-10 h-10 text-slate-600 mx-auto mb-2" />
                  <p className="text-xs text-slate-400">No SMB shares configured yet.</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {smbShares.map((share) => (
                    <div key={share.id} className="glass-card p-5 rounded-2xl space-y-3">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div className="p-3 bg-blue-500/10 rounded-xl text-blue-400">
                            <Folder className="w-5 h-5" />
                          </div>
                          <div>
                            <h4 className="text-sm font-bold text-white">{share.name}</h4>
                            <p className="text-xs text-slate-400 font-mono">smb://{share.host}/{share.shareName}</p>
                          </div>
                        </div>

                        <button
                          onClick={() => handleDeleteSmbShare(share.id)}
                          className="p-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 transition"
                          title="Delete Share"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>

                      <div className="pt-2 border-t border-white/5 flex items-center justify-between text-xs">
                        <span className="text-slate-500">User: {share.username || 'Anonymous'}</span>
                        <button
                          onClick={() => handleBrowseSmb(share, '')}
                          className="text-blue-400 hover:text-blue-300 font-semibold flex items-center gap-1 transition"
                        >
                          <span>Browse Files</span> <ArrowRight className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* SMB File Explorer Dialog/Section */}
            {currentShare && (
              <div className="glass-panel p-6 rounded-2xl space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm font-bold text-white">
                    <FolderOpen className="w-5 h-5 text-amber-400" />
                    <span>Browsing: {currentShare.name}</span>
                    <span className="text-xs text-slate-400 font-mono font-normal">/{currentPath}</span>
                  </div>
                  <button
                    onClick={() => setCurrentShare(null)}
                    className="text-xs text-slate-400 hover:text-white px-3 py-1 bg-white/5 rounded-lg"
                  >
                    Close Explorer
                  </button>
                </div>

                {isBrowsing ? (
                  <div className="p-8 text-center text-slate-400 text-xs flex items-center justify-center gap-2">
                    <RefreshCw className="w-4 h-4 animate-spin" /> Loading files...
                  </div>
                ) : browseItems.length === 0 ? (
                  <p className="text-xs text-slate-500 py-4">Folder is empty or inaccessible.</p>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2 max-h-80 overflow-y-auto pr-1">
                    {browseItems.map((item, idx) => (
                      <div
                        key={idx}
                        className="p-3 rounded-xl bg-white/5 hover:bg-white/10 flex items-center justify-between transition cursor-pointer"
                        onClick={() => {
                          if (item.isDirectory) {
                            handleBrowseSmb(currentShare, currentPath ? `${currentPath}/${item.name}` : item.name)
                          }
                        }}
                      >
                        <div className="flex items-center gap-2.5 truncate">
                          {item.isDirectory ? (
                            <Folder className="w-4 h-4 text-amber-400 flex-shrink-0" />
                          ) : (
                            <Play className="w-4 h-4 text-blue-400 flex-shrink-0" />
                          )}
                          <span className="text-xs font-semibold text-slate-200 truncate">{item.name}</span>
                        </div>

                        {!item.isDirectory && (
                          <div className="flex items-center gap-2 flex-shrink-0">
                            <span className="text-[10px] text-slate-400">{formatBytes(item.size)}</span>
                            <button
                              onClick={(e) => {
                                e.stopPropagation()
                                axios.post('/api/smb/play', {
                                  shareId: currentShare.id,
                                  filePath: currentPath ? `${currentPath}/${item.name}` : item.name,
                                  title: item.name
                                })
                                showToast('success', `Requested playback of ${item.name} on TV!`)
                              }}
                              className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded-md text-[10px] font-bold hover:bg-blue-500/30"
                            >
                              Play on TV
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-white/5 bg-[#0B0F19] py-4 text-center text-xs text-slate-500">
        dwPlayer Android TV Media System • Port 8191 • Clean Architecture
      </footer>
    </div>
  )
}
