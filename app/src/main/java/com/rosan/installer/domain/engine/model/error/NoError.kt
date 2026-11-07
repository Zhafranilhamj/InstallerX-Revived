// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.model.error

/**
 * Shared sentinel for "this session has no error yet".
 *
 * A dedicated instance is used instead of a freshly allocated [Throwable] so that callers can
 * distinguish "no error" from a real failure and avoid paying for a stack trace on every reset.
 */
object NoError : Throwable("No error", null, false, false)
