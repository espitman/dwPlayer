import React, { useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUp, ChevronRight, Download, Folder, LayoutDashboard, Link2, ListPlus, LoaderCircle, Network, Pause, Play, RefreshCw, Server, Trash2 } from 'lucide-react'

const views = [
  { id: 'overview', label: 'Overview', number: '01', icon: LayoutDashboard },
  { id: 'downloads', label: 'Downloads', number: '02', icon: Download },
  { id: 'playlists', label: 'Playlists', number: '03', icon: ListPlus },
  { id: 'network', label: 'Network', number: '04', icon: Network },
]
const initialStorage = { freeSpace: '0 GB', totalSpace: '0 GB', usedPercent: 0, path: '' }

function Field({ label, children, className = '' }) { return <label className={`field ${className}`}><span>{label}</span>{children}</label> }
function EmptyState({ icon: Icon, children }) { return <div className="empty"><div><Icon aria-hidden="true" /><p>{children}</p></div></div> }
function StatusChip({ children }) { return <span className="status-chip">{children}</span> }
function PanelTitle({ title, copy, chip }) { return <div className="panel-head"><div><h2>{title}</h2>{copy && <p>{copy}</p>}</div>{chip && <StatusChip>{chip}</StatusChip>}</div> }

export default function App() {
  const [activeView, setActiveView] = useState(() => localStorage.getItem('dw-web-view') || 'overview')
  const [networkTab, setNetworkTab] = useState('webdav')
  const [message, setMessage] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [storage, setStorage] = useState(initialStorage)
  const [tasks, setTasks] = useState([])
  const [summary, setSummary] = useState({ total: 0, active: 0, paused: 0, failed: 0, completed: 0 })
  const [playlists, setPlaylists] = useState([])
  const [smbShares, setSmbShares] = useState([])
  const [webDavServers, setWebDavServers] = useState([])
  const [discovered, setDiscovered] = useState([])
  const [scanState, setScanState] = useState('NOT SCANNED')
  const [quick, setQuick] = useState({ url: '', fileName: '', playlistId: '' })
  const [downloadForm, setDownloadForm] = useState({ url: '', fileName: '', playlistId: '' })
  const [playlistName, setPlaylistName] = useState('')
  const [webDav, setWebDav] = useState({ name: '', serverUrl: '', username: '', password: '' })
  const [smb, setSmb] = useState({ name: '', host: '', shareName: '', username: '', password: '', domain: '', port: 445 })
  const [smbTest, setSmbTest] = useState(null)
  const [testingSmb, setTestingSmb] = useState(false)

  const toast = (type, text) => {
    setMessage({ type, text })
    window.clearTimeout(window.dwToastTimer)
    window.dwToastTimer = window.setTimeout(() => setMessage(null), 3200)
  }

  const loadDashboard = async (quiet = false) => {
    if (!quiet) setLoading(true)
    const results = await Promise.allSettled([
      axios.get('/api/storage/info'), axios.get('/api/status/live'), axios.get('/api/playlists'),
      axios.get('/api/smb/shares'), axios.get('/api/webdav/servers'), axios.get('/api/discovery/servers'),
    ])
    if (results[0].status === 'fulfilled') setStorage(results[0].value.data || initialStorage)
    if (results[1].status === 'fulfilled') { setTasks(results[1].value.data?.tasks || []); setSummary(results[1].value.data?.summary || {}) }
    if (results[2].status === 'fulfilled') setPlaylists(Array.isArray(results[2].value.data) ? results[2].value.data : [])
    if (results[3].status === 'fulfilled') setSmbShares(Array.isArray(results[3].value.data) ? results[3].value.data : [])
    if (results[4].status === 'fulfilled') setWebDavServers(Array.isArray(results[4].value.data) ? results[4].value.data : [])
    if (results[5].status === 'fulfilled') setDiscovered(Array.isArray(results[5].value.data) ? results[5].value.data : [])
    if (!quiet) setLoading(false)
  }

  useEffect(() => { loadDashboard(); const interval = window.setInterval(() => loadDashboard(true), 3000); return () => window.clearInterval(interval) }, [])
  const navigate = (id) => { setActiveView(id); localStorage.setItem('dw-web-view', id); window.scrollTo({ top: 0, behavior: 'smooth' }) }

  const submitDownload = async (event, values, clear) => {
    event.preventDefault(); if (!values.url.trim()) return; setSubmitting(true)
    try {
      const response = await axios.post('/api/download', { url: values.url.trim(), fileName: values.fileName.trim() || undefined, playlistId: values.playlistId || undefined })
      if (response.data?.status !== 'success') throw new Error(response.data?.message || 'Download could not be queued')
      clear(); toast('success', 'Download sent to the connected TV'); await loadDashboard(true)
    } catch (error) { toast('error', error.response?.data?.message || error.message || 'Download failed') }
    finally { setSubmitting(false) }
  }
  const taskAction = async (action, id) => {
    try {
      if (action === 'delete') await axios.delete(`/api/downloads/${id}?deleteFile=true`); else await axios.post(`/api/downloads/${id}/${action}`)
      toast('success', action === 'play' ? 'Playback started on TV' : 'Download updated'); await loadDashboard(true)
    } catch (error) { toast('error', error.response?.data?.message || `Could not ${action} download`) }
  }
  const createPlaylist = async (event) => {
    event.preventDefault(); if (!playlistName.trim()) return; setSubmitting(true)
    try { await axios.post('/api/playlists', { name: playlistName.trim() }); setPlaylistName(''); toast('success', 'Playlist created on TV'); await loadDashboard(true) }
    catch (error) { toast('error', error.response?.data?.message || 'Could not create playlist') } finally { setSubmitting(false) }
  }
  const deletePlaylist = async (id) => { try { await axios.delete(`/api/playlists/${id}`); toast('success', 'Playlist removed'); await loadDashboard(true) } catch (error) { toast('error', error.response?.data?.message || 'Could not remove playlist') } }
  const playPlaylist = async (id) => { try { await axios.post(`/api/playlists/${id}/play`); toast('success', 'Playlist started on TV') } catch (error) { toast('error', error.response?.data?.message || 'Playlist is empty') } }
  const saveWebDav = async (event) => {
    event.preventDefault(); setSubmitting(true)
    try { await axios.post('/api/webdav/servers', webDav); setWebDav({ name: '', serverUrl: '', username: '', password: '' }); toast('success', 'WebDAV source saved to TV'); await loadDashboard(true) }
    catch (error) { toast('error', error.response?.data?.message || 'WebDAV connection failed') } finally { setSubmitting(false) }
  }
  const testSmb = async () => {
    if (!smb.host || !smb.shareName) return; setTestingSmb(true); setSmbTest(null)
    try { const response = await axios.post('/api/smb/test', smb); setSmbTest(response.data) }
    catch (error) { setSmbTest({ status: 'error', message: error.response?.data?.message || error.message }) } finally { setTestingSmb(false) }
  }
  const saveSmb = async (event) => {
    event.preventDefault(); setSubmitting(true)
    try { await axios.post('/api/smb/shares', { ...smb, name: smb.name || `${smb.host}/${smb.shareName}` }); setSmb({ name: '', host: '', shareName: '', username: '', password: '', domain: '', port: 445 }); setSmbTest(null); toast('success', 'SMB share saved to TV'); await loadDashboard(true) }
    catch (error) { toast('error', error.response?.data?.message || 'Could not save SMB share') } finally { setSubmitting(false) }
  }
  const deleteSource = async (type, id) => { try { await axios.delete(`/api/${type}/${id}`); toast('success', 'Network source removed'); await loadDashboard(true) } catch (error) { toast('error', error.response?.data?.message || 'Could not remove source') } }
  const scanNetwork = async () => {
    setScanState('SCANNING…')
    try { const response = await axios.get('/api/discovery/servers'); const found = Array.isArray(response.data) ? response.data : []; setDiscovered(found); setScanState(found.length ? `${found.length} FOUND` : 'NO DEVICES FOUND'); toast('success', 'Network scan refreshed') }
    catch { setScanState('SCAN FAILED'); toast('error', 'Network scan failed') }
  }

  const sourceCount = smbShares.length + webDavServers.length
  const storagePercent = Number.isFinite(Number(storage.usedPercent)) ? Math.max(0, Math.min(100, Number(storage.usedPercent))) : 0
  const activeLabel = views.find(view => view.id === activeView)?.label || 'Overview'
  const playlistOptions = useMemo(() => playlists.map(item => <option key={item.id} value={item.id}>{item.name}</option>), [playlists])

  return <div className="app-shell">
    <aside className="sidebar">
      <button className="brand" onClick={() => navigate('overview')} aria-label="dwPlayer overview"><span className="brand-mark"><Play /></span><span><strong>dwPlayer</strong><small>TV companion</small></span></button>
      <nav className="primary-nav" aria-label="Primary navigation">{views.map(({ id, label, number, icon: Icon }) => <button key={id} className={activeView === id ? 'active' : ''} onClick={() => navigate(id)}><Icon /><span>{label}</span><em>{number}</em></button>)}</nav>
      <div className="device-card"><div><i /><strong>Living Room TV</strong></div><p>ANDROID TV · CONNECTED<br />LOCAL WEB COMPANION</p></div>
    </aside>
    <main className="main-shell">
      <header className="topbar"><div className="crumb">Living Room TV / <strong>{activeLabel}</strong></div><div className="storage"><span>{storage.freeSpace} free of {storage.totalSpace}</span><span className="meter"><i style={{ width: `${storagePercent}%` }} /></span></div></header>
      <div className="content">{loading && <div className="loading-line"><LoaderCircle /> Syncing with TV…</div>}
        {activeView === 'overview' && <section className="view">
          <div className="page-head"><div><p className="eyebrow">TV COMPANION</p><h1>Ready when the TV is.</h1><p className="lead">Send media, shape the queue, and manage network sources without reaching for the remote.</p></div><button className="btn btn-primary" onClick={() => navigate('downloads')}><Link2 />Send a link</button></div>
          <div className="overview-grid"><div className="panel"><PanelTitle title="Quick send" copy="Start a direct download on the connected TV." chip="TV ONLINE" /><form className="quick-form" onSubmit={e => submitDownload(e, quick, () => setQuick({ url: '', fileName: '', playlistId: '' }))}><Field label="Video download URL"><input className="input" type="url" required placeholder="https://example.com/movie.mkv" value={quick.url} onChange={e => setQuick({ ...quick, url: e.target.value })} /></Field><Field label="File name · optional"><input className="input" placeholder="Episode.01.mkv" value={quick.fileName} onChange={e => setQuick({ ...quick, fileName: e.target.value })} /></Field><Field className="form-wide" label="Assign to playlist · optional"><select className="select" value={quick.playlistId} onChange={e => setQuick({ ...quick, playlistId: e.target.value })}><option value="">No playlist · independent download</option>{playlistOptions}</select></Field><div className="action-row"><button className="btn btn-primary" disabled={submitting || !quick.url.trim()}>{submitting && <LoaderCircle className="spin" />}Start on TV</button></div></form></div>
            <aside className="panel"><PanelTitle title="Now connected" copy="Living Room TV" chip="ANDROID TV" /><div className="stat-list"><div><span>Downloads</span><strong>{summary.active ? `${summary.active} ACTIVE` : 'NONE ACTIVE'}</strong></div><div><span>Storage</span><strong>{storage.freeSpace} / {storage.totalSpace} FREE</strong></div><div><span>Network</span><strong>{sourceCount ? `${sourceCount} SOURCES` : 'LOCAL WI-FI'}</strong></div></div><div className="remote"><div className="remote-grid"><span /><button aria-label="Up" onClick={() => toast('success', 'Remote command ready on TV')}><ArrowUp /></button><span /><button aria-label="Left" onClick={() => toast('success', 'Remote command ready on TV')}><ArrowLeft /></button><button className="play-key" aria-label="Play or pause" onClick={() => toast('success', 'Remote command ready on TV')}><Play /></button><button aria-label="Right" onClick={() => toast('success', 'Remote command ready on TV')}><ArrowRight /></button><span /><button aria-label="Down" onClick={() => toast('success', 'Remote command ready on TV')}><ArrowDown /></button><span /></div></div></aside></div>
        </section>}
        {activeView === 'downloads' && <section className="view">
          <div className="page-head"><div><p className="eyebrow">OFFLINE QUEUE</p><h1>Downloads</h1><p className="lead">Send direct media files to Android TV and keep progress visible here.</p></div><button className="btn btn-primary" onClick={() => document.getElementById('download-url')?.focus()}>New download</button></div>
          <div className="workspace"><div className="panel list-panel"><div className="list-toolbar"><h2>Active TV tasks</h2><StatusChip>{summary.active || 0} ACTIVE</StatusChip></div>{tasks.length === 0 ? <EmptyState icon={Download}>No download tasks on the TV yet.</EmptyState> : <div className="task-list">{tasks.map(task => <div className="task-row" key={task.id}><span className="row-icon"><Download /></span><span className="row-copy"><strong>{task.fileName}</strong><small>{task.status} · {task.progress || 0}% {task.speed ? `· ${task.speed}` : ''}</small><span className="progress"><i style={{ width: `${task.progress || 0}%` }} /></span></span><span className="row-actions">{task.status === 'ACTIVE' && <button className="icon-btn" onClick={() => taskAction('pause', task.id)} aria-label="Pause"><Pause /></button>}{['PAUSED', 'FAILED'].includes(task.status) && <button className="icon-btn" onClick={() => taskAction('resume', task.id)} aria-label="Resume"><Play /></button>}{task.status === 'COMPLETED' && <button className="icon-btn" onClick={() => taskAction('play', task.id)} aria-label="Play on TV"><Play /></button>}<button className="icon-btn danger" onClick={() => taskAction('delete', task.id)} aria-label="Delete"><Trash2 /></button></span></div>)}</div>}</div>
            <aside className="panel form-panel"><h2>Send a download</h2><p>MP4, MKV, AVI and other direct video URLs.</p><form className="stack" onSubmit={e => submitDownload(e, downloadForm, () => setDownloadForm({ url: '', fileName: '', playlistId: '' }))}><Field label="Video URL"><input id="download-url" className="input" type="url" required placeholder="https://example.com/video.mp4" value={downloadForm.url} onChange={e => setDownloadForm({ ...downloadForm, url: e.target.value })} /></Field><Field label="Custom file name · optional"><input className="input" placeholder="Movie.2026.mkv" value={downloadForm.fileName} onChange={e => setDownloadForm({ ...downloadForm, fileName: e.target.value })} /></Field><Field label="Playlist · optional"><select className="select" value={downloadForm.playlistId} onChange={e => setDownloadForm({ ...downloadForm, playlistId: e.target.value })}><option value="">Independent download</option>{playlistOptions}</select></Field><button className="btn btn-primary" disabled={submitting || !downloadForm.url.trim()}>Start download on TV</button></form></aside></div>
        </section>}
        {activeView === 'playlists' && <section className="view">
          <div className="page-head"><div><p className="eyebrow">ORDERED PLAYBACK</p><h1>Playlists &amp; series</h1><p className="lead">Group episodes or films and keep their playback order synced with the TV.</p></div></div>
          <div className="workspace"><div className="panel list-panel"><div className="list-toolbar"><h2>Your playlists</h2><StatusChip>{playlists.length} {playlists.length === 1 ? 'PLAYLIST' : 'PLAYLISTS'}</StatusChip></div>{playlists.length === 0 ? <EmptyState icon={ListPlus}>No playlists saved on this TV.</EmptyState> : playlists.map(playlist => <div className="list-row" key={playlist.id}><span className="row-icon"><ListPlus /></span><span><strong>{playlist.name}</strong><small>{playlist.itemCount || 0} ITEMS · READY TO SYNC</small></span><span className="row-actions"><button className="icon-btn" aria-label="Play playlist" onClick={() => playPlaylist(playlist.id)}><Play /></button><button className="icon-btn danger" aria-label="Delete playlist" onClick={() => deletePlaylist(playlist.id)}><Trash2 /></button></span></div>)}</div><aside className="panel form-panel"><h2>Create playlist</h2><p>Use a season, series, or viewing-list name.</p><form className="stack" onSubmit={createPlaylist}><Field label="Playlist name"><input className="input" required placeholder="Breaking Bad · Season 1" value={playlistName} onChange={e => setPlaylistName(e.target.value)} /></Field><button className="btn btn-primary" disabled={submitting || !playlistName.trim()}>Create playlist</button></form></aside></div>
        </section>}
        {activeView === 'network' && <section className="view">
          <div className="page-head"><div><p className="eyebrow">LOCAL &amp; REMOTE SOURCES</p><h1>Network shares</h1><p className="lead">Connect WebDAV, phones, Windows, Mac, Linux, or NAS folders for streaming on TV.</p></div><button className="btn btn-primary" onClick={scanNetwork}><RefreshCw className={scanState === 'SCANNING…' ? 'spin' : ''} />Scan network</button></div>
          <div className="notice"><div><strong>Auto-discovery on local Wi-Fi</strong><p>Android phones, WebDAV servers, and NAS devices appear here.</p></div><StatusChip>{scanState}</StatusChip></div>{discovered.length > 0 && <div className="discovery-list">{discovered.map((item, index) => <span key={item.id || item.address || index}><Network />{item.name || item.serviceName || item.address || 'Discovered device'}</span>)}</div>}
          <div className="tabs" role="tablist"><button className={networkTab === 'webdav' ? 'active' : ''} onClick={() => setNetworkTab('webdav')}>WebDAV / phone</button><button className={networkTab === 'smb' ? 'active' : ''} onClick={() => setNetworkTab('smb')}>SMB share</button></div>
          <div className="workspace"><div className="panel list-panel"><div className="list-toolbar"><h2>Configured sources</h2><StatusChip>{sourceCount} SAVED</StatusChip></div>{sourceCount === 0 ? <EmptyState icon={Network}>No network sources saved on this TV.</EmptyState> : <>{webDavServers.map(server => <div className="list-row" key={server.id}><span className="row-icon"><Network /></span><span><strong>{server.name}</strong><small>{server.serverUrl}</small></span><span className="row-actions"><button className="icon-btn danger" onClick={() => deleteSource('webdav/servers', server.id)} aria-label="Delete WebDAV server"><Trash2 /></button></span></div>)}{smbShares.map(share => <div className="list-row" key={share.id}><span className="row-icon"><Folder /></span><span><strong>{share.name}</strong><small>SMB://{share.host}/{share.shareName}</small></span><span className="row-actions"><button className="icon-btn danger" onClick={() => deleteSource('smb/shares', share.id)} aria-label="Delete SMB share"><Trash2 /></button></span></div>)}</>}</div>
            <aside className="panel form-panel">{networkTab === 'webdav' ? <><h2>Add WebDAV server</h2><p>For Android phones, Nextcloud, or HTTP-range servers.</p><form className="stack" onSubmit={saveWebDav}><Field label="Server name"><input className="input" required placeholder="My phone or Nextcloud" value={webDav.name} onChange={e => setWebDav({ ...webDav, name: e.target.value })} /></Field><Field label="Server URL"><input className="input" type="url" required placeholder="http://192.168.1.50:8080/webdav" value={webDav.serverUrl} onChange={e => setWebDav({ ...webDav, serverUrl: e.target.value })} /></Field><div className="network-fields"><Field label="Username · optional"><input className="input" placeholder="user" value={webDav.username} onChange={e => setWebDav({ ...webDav, username: e.target.value })} /></Field><Field label="Password · optional"><input className="input" type="password" placeholder="••••••••" value={webDav.password} onChange={e => setWebDav({ ...webDav, password: e.target.value })} /></Field></div><button className="btn btn-primary" disabled={submitting}>Save WebDAV to TV</button></form></> : <><h2>Add SMB share</h2><p>For Windows, Mac, Linux, and NAS folders.</p><form className="stack" onSubmit={saveSmb}><Field label="Display name"><input className="input" placeholder="Home NAS" value={smb.name} onChange={e => setSmb({ ...smb, name: e.target.value })} /></Field><Field label="Host or IP address"><input className="input" required placeholder="192.168.1.50 or mynas.local" value={smb.host} onChange={e => setSmb({ ...smb, host: e.target.value })} /></Field><Field label="Share name"><input className="input" required placeholder="Movies" value={smb.shareName} onChange={e => setSmb({ ...smb, shareName: e.target.value })} /></Field><div className="network-fields"><Field label="Username · guest allowed"><input className="input" placeholder="admin" value={smb.username} onChange={e => setSmb({ ...smb, username: e.target.value })} /></Field><Field label="Port"><input className="input" inputMode="numeric" value={smb.port} onChange={e => setSmb({ ...smb, port: Number(e.target.value) || 445 })} /></Field></div>{smbTest && <div className={`result ${smbTest.status}`}>{smbTest.message || (smbTest.status === 'success' ? 'Connection succeeded' : 'Connection failed')}</div>}<div className="form-buttons"><button type="button" className="btn btn-secondary" onClick={testSmb} disabled={testingSmb || !smb.host || !smb.shareName}>{testingSmb ? <LoaderCircle className="spin" /> : <Server />}Test connection</button><button className="btn btn-primary" disabled={submitting}>Save SMB share to TV</button></div></form></>}</aside></div>
        </section>}
      </div>
    </main>
    {message && <div className={`toast ${message.type}`} role="status">{message.text}<ChevronRight /></div>}
  </div>
}
