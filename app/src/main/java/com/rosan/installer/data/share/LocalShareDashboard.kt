// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.share

/** Self-contained dashboard served by the built-in HTTP server. No external assets. */
internal object LocalShareDashboard {
    fun loginPage(error: Boolean = false): String = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>InstallerX - Local sharing</title><style>
body{font-family:system-ui,sans-serif;background:#10131a;color:#eef1f7;margin:0;padding:24px}
.card{max-width:560px;margin:6vh auto;background:#1c2130;border-radius:18px;padding:28px;box-shadow:0 12px 40px #0006}
h1{font-size:22px;margin:0 0 8px}p{color:#a9b2c6}input,button{font:inherit;border-radius:10px;border:1px solid #39415a;padding:12px;background:#111624;color:#eef1f7}
button{background:#5b7cfa;border:0;cursor:pointer;font-weight:600}label{display:block;margin:16px 0 6px}
input{width:100%;box-sizing:border-box}.err{color:#ff8a80}.row{display:flex;gap:10px;align-items:center}
</style></head><body><div class="card"><h1>InstallerX local sharing</h1>
<p>This device is protected by a PIN. Enter the PIN shown in the app.</p>
""" + (if (error) "<p class=\"err\">Incorrect PIN. Try again.</p>" else "") + """
<form method="post" action="/login"><label for="pin">PIN</label><input id="pin" name="pin" inputmode="numeric" autocomplete="off" autofocus>
<button type="submit" style="margin-top:16px;width:100%">Unlock</button></form></div></body></html>"""

    fun mainPage(pinEnabled: Boolean, versionName: String): String = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>InstallerX - Local sharing</title><style>
body{font-family:system-ui,sans-serif;background:#10131a;color:#eef1f7;margin:0;padding:24px}
.card{max-width:640px;margin:5vh auto;background:#1c2130;border-radius:18px;padding:28px;box-shadow:0 12px 40px #0006}
h1{font-size:22px;margin:0 0 6px}h2{font-size:15px;margin:26px 0 10px;color:#a9b2c6}p{color:#a9b2c6;line-height:1.5}
.drop{border:2px dashed #4a5578;border-radius:14px;padding:28px;text-align:center;cursor:pointer;transition:.2s}
.drop:hover{border-color:#5b7cfa;background:#232b40}input[type=file]{display:none}
button,.btn{background:#5b7cfa;color:#fff;border:0;border-radius:10px;padding:12px 18px;font:inherit;font-weight:600;cursor:pointer;display:inline-block}
#status{margin-top:14px;font-size:14px;color:#9fe0b0}.muted{font-size:12px;color:#7f8aa3}
ul{padding-left:18px}li{margin:4px 0;color:#c8d0e0}
</style></head><body><div class="card">
<h1>InstallerX local sharing</h1>
<p>Upload APK, APKS, APKM, XAPK or OBB files from this browser. Accepted packages open the installer on the receiving device.</p>
<div class="drop" id="drop">Drop a package here or <strong>browse</strong><input id="file" type="file" multiple accept=".apk,.apks,.apkm,.xapk,.zip,.obb"></div>
<div id="status"></div>
<h2>Received files</h2><ul id="files"><li class="muted">Loading…</li></ul>
<p class="muted">Local network only. """ + (if (pinEnabled) "PIN protection is on." else "PIN protection is off.") + " · " + escapeHtml(versionName) + """</p>
</div><script>
var fileInput=document.getElementById('file');var drop=document.getElementById('drop');var status=document.getElementById('status');
function refresh(){fetch('/api/files').then(function(r){return r.json()}).then(function(d){var ul=document.getElementById('files');ul.innerHTML='';
if(!d.files||!d.files.length){ul.innerHTML='<li class="muted">Nothing received yet</li>';return}
d.files.forEach(function(n){var li=document.createElement('li');li.textContent=n;ul.appendChild(li)})}).catch(function(){})}
function upload(files){if(!files||!files.length)return;var list=Array.prototype.slice.call(files);var sent=0;
function next(){if(!list.length){status.textContent='Sent '+sent+' file(s) to the installer.';refresh();return}
var f=list.shift();status.textContent='Uploading '+f.name+'…';var fd=new FormData();fd.append('file',f,f.name);
fetch('/upload',{method:'POST',body:fd}).then(function(r){return r.json()}).then(function(d){
if(d.ok){sent++;next()}else{status.textContent='Upload failed: '+(d.error||'unknown error')}})
.catch(function(e){status.textContent='Upload failed: '+e})}next()}
drop.addEventListener('click',function(){fileInput.click()});
fileInput.addEventListener('change',function(){upload(fileInput.files)});
drop.addEventListener('dragover',function(e){e.preventDefault()});
drop.addEventListener('drop',function(e){e.preventDefault();upload(e.dataTransfer.files)});
refresh();setInterval(refresh,5000);
</script></body></html>"""
}
