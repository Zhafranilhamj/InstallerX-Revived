package com.rosan.installer.framework.privileged.core.execution.runtime

import android.content.Context
import android.os.ParcelFileDescriptor
import com.rosan.installer.IPrivilegedService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BasePrivilegedService :
    IPrivilegedService.Stub(),
    KoinComponent {
    protected val context by inject<Context>()

    /**
     * Backends without shell-level filesystem access (Dhizuku) cannot write OBB files.
     * [DefaultPrivilegedService] overrides this with the real privileged copy.
     */
    override fun writeObb(source: ParcelFileDescriptor, packageName: String, fileName: String): Boolean {
        runCatching { source.close() }
        return false
    }
}
